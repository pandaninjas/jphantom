package jphantom.dataflow;

import java.util.*;
import jphantom.*;
import jphantom.exc.*;
import jphantom.tree.*;
import jphantom.tree.closure.*;
import org.objectweb.asm.tree.analysis.*;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

import static util.Utils.*;

public class TypeInterpreter extends BasicInterpreter implements Opcodes, StandardTypes
{
    private static final Map<Type,BasicValue> values = newMap();
    protected static final BasicValue NULL_VALUE = new BasicValue(NULL_TYPE);

    static {
        values.put(NULL_TYPE, NULL_VALUE);
    }

    private ClassHierarchy hier;
    private ClassHierarchy.Snapshot closure;

    public TypeInterpreter(ClassHierarchy hier) {
        this(ASM4, hier);
    }

    public TypeInterpreter(int api, ClassHierarchy hier) {
        super(api);
        this.hier = hier;
        this.closure = new PseudoSnapshot(hier);
    }

    public static final BasicValue getValue(final Type type)
    {
        if (!values.containsKey(type))
            values.put(type, new BasicValue(type));
        return values.get(type);
    }

    @Override
    public BasicValue newValue(final Type type)
    {
        BasicValue val = super.newValue(type);

        // For reference types, change the value by including the exact type

        return BasicValue.REFERENCE_VALUE.equals(val) ? getValue(type) : val;
    }

    @Override
    public BasicValue binaryOperation(
        final AbstractInsnNode insn,
        final BasicValue value1,
        final BasicValue value2) throws AnalyzerException
    {
        if (insn.getOpcode() == AALOAD) {
            return value1.equals(NULL_VALUE) ? 
                NULL_VALUE :
                newValue(ArrayType.elementOf(value1.getType()));
        }
        return super.binaryOperation(insn, value1, value2);
    }

    @Override
    public BasicValue merge(final BasicValue v, final BasicValue w)
    {
        if (v.equals(w))
            return v;

        // Incompatible types

        if (!v.isReference() || !w.isReference())
            return BasicValue.UNINITIALIZED_VALUE;

        // Null types

        if (v.equals(NULL_VALUE))
            return w;

        if (w.equals(NULL_VALUE))
            return v;

        final Type a = v.getType();
        final Type b = w.getType();

        // Array types

        if (a.getSort() == Type.ARRAY && b.getSort() == Type.ARRAY)
        {
            if (a.getDimensions() == b.getDimensions()) {

                // Get basic elements

                final Type ae = a.getElementType();
                final Type be = b.getElementType();

                // Create new array type

                if (ae.getSort() == Type.OBJECT && be.getSort() == Type.OBJECT)
                {
                    Type fcs;
                    try {
                        fcs = closure.firstCommonSuperclass(ae, be);
                    } catch (IncompleteSupertypesException exc) {
                        throw new IllegalStateException(exc);
                    }

                    return newValue(
                        ArrayType.fromElementType(fcs, a.getDimensions())
                    );
                }
            }
        }
            
        if (a.getSort() == Type.ARRAY || b.getSort() == Type.ARRAY)
            return newValue(OBJECT);

        // Class / Interface types

        assert a.getSort() == Type.OBJECT;
        assert b.getSort() == Type.OBJECT;

        try {
            return newValue(closure.firstCommonSuperclass(a, b));
        } catch (IncompleteSupertypesException exc) {
            throw new IllegalStateException(exc);
        }
    }
}
