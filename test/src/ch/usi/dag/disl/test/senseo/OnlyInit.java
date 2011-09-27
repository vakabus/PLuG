package ch.usi.dag.disl.test.senseo;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.dislclass.snippet.Snippet;
import ch.usi.dag.disl.dislclass.snippet.marker.MarkedRegion;
import ch.usi.dag.disl.guard.SnippetGuard;

public class OnlyInit implements SnippetGuard {
    @Override
    public boolean isApplicable(ClassNode classNode, MethodNode methodNode, Snippet snippet, MarkedRegion markedRegion) {
        return (methodNode.name.equals("<init>") && !classNode.name.equals("java/lang/Object")) ? true : false;
    }
}
