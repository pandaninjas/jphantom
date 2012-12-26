package jphantom.tree.graph;

import java.util.*;
import jphantom.tree.*;
import org.objectweb.asm.Type;
import static util.Utils.*;

public class Node
{
    private static final Map<Type,Node> cache = newMap();

    private final Type type;

    protected Node(Type type) {
        this.type = type;
    }

    public Type asType() {
        return type;
    }

    public static Node get(Type t)
    {
        if (!cache.containsKey(t))
            cache.put(t, new Node(t));

        return cache.get(t);
    }

    @Override
    public boolean equals(Object other)
    {
        if (other == this)
            return true;

        if (!(other instanceof Node))
            return false;

        Node o = (Node) other;
        return type.equals(o.type);
    }

    @Override
    public int hashCode() {
        return type.hashCode();
    }

    @Override
    public String toString() {
        return type.getClassName();
    }
}
