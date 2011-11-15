package ch.usi.dag.disl.test.args;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.snippet.Snippet;
import ch.usi.dag.disl.marker.MarkedRegion;
import ch.usi.dag.disl.guard.SnippetGuard;

public class HasArgsGuard  implements SnippetGuard {
	@Override
	public boolean isApplicable(ClassNode classNode, MethodNode methodNode,
			Snippet snippet, MarkedRegion markedRegion) {
		
		if(Type.getArgumentTypes(methodNode.desc).length>0)
			return true;
		return false;
	}
}
