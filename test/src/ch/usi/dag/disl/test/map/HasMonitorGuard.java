package ch.usi.dag.disl.test.map;

import java.util.Iterator;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;

import ch.usi.dag.disl.guard.SnippetGuard;
import ch.usi.dag.disl.snippet.Shadow;

//check if the method has at least one monitorenter on its body.
public class HasMonitorGuard implements SnippetGuard {
	@Override
	public boolean isApplicable(Shadow shadow) {
		InsnList inslist = shadow.getMethodNode().instructions;
		
		Iterator<AbstractInsnNode> it=inslist.iterator();
		while(it.hasNext()) {
			if(it.next().getOpcode() == Opcodes.MONITORENTER)
				return true;
		}
		return false;
		
	}
}
