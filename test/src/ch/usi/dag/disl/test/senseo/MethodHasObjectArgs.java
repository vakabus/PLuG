package ch.usi.dag.disl.test.senseo;

import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.snippet.MarkedRegion;
import ch.usi.dag.disl.snippet.Snippet;

// check if at least one of the arguments is an Object 
// --> if "L" is in the description of the arguments
public class MethodHasObjectArgs extends NotInitNorClinit {
    @Override
    public boolean isApplicable(ClassNode classNode, MethodNode methodNode, Snippet snippet, MarkedRegion markedRegion) {
        String desc = methodNode.desc;
        return
            super.isApplicable(classNode, methodNode, snippet, markedRegion)
            && desc.substring(0, desc.indexOf(')')).contains("L");
    }
}
