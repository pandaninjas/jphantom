package jphantom.tree.closure;

import java.util.*;
import jphantom.tree.*;
import jphantom.tree.graph.*;
import org.objectweb.asm.Type;
import org.jgrapht.DirectedGraph;
import org.jgrapht.graph.SimpleDirectedGraph;
import org.jgrapht.alg.TransitiveClosure;
import static org.jgrapht.Graphs.*;
import static util.Utils.*;

/** @author George Balatsouras */
public class CopyingSnapshot extends PseudoSnapshot
{
    private final DirectedGraph<Node,Edge> graph;
    private final SimpleDirectedGraph<Node,Edge> closedGraph = 
        new SimpleDirectedGraph<Node,Edge>(Edge.factory);

    public CopyingSnapshot(ClassHierarchy other)
    {
        // Make defensive copy
        super(new IncrementalClassHierarchy(other));

        // Try to add missing types
        new Importer(hierarchy).execute();

        // Create graph representation
        this.graph = new GraphConverter(hierarchy).convert();

        // Compute the transitive closure of the class hierarchy
        addGraph(closedGraph, graph);
        TransitiveClosure.INSTANCE.closeSimpleDirectedGraph(closedGraph);
    }

    @Override
    public void addClass(Type clazz, Type superclass, Type[] interfaces) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addInterface(Type iface, Type[] superInterfaces) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Type> getAllSupertypes(Type obj)
        throws IncompleteSupertypesException
    {
        Set<Type> supertypes = newSet();
        
        for (Node n : successorListOf(closedGraph, Node.get(obj)))
            supertypes.add(n.asType());

        for (Type s : supertypes)
            if (!hierarchy.contains(s))
                throw new IncompleteSupertypesException(supertypes);

        return supertypes;
    }

    // TODO: extends AbstactSnapshot, implement remaining methods
}
