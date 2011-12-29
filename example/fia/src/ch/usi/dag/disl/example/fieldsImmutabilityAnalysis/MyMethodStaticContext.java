package ch.usi.dag.disl.example.fieldsImmutabilityAnalysis;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;

import ch.usi.dag.disl.staticcontext.MethodStaticContext;

public class MyMethodStaticContext extends MethodStaticContext {
	public String getAllocationSite() {
        int idx = 0;

        for(AbstractInsnNode instr = staticContextData.getRegionStart(); instr != null; instr = instr.getPrevious()) {
            idx++;
        }

		return thisMethodFullName() + thisMethodDescriptor() + "[" + String.valueOf(idx) + "]";
	}

	public String getFieldId() {
		AbstractInsnNode instr = staticContextData.getRegionStart();

		if (instr.getOpcode() == Opcodes.PUTFIELD || instr.getOpcode() == Opcodes.GETFIELD) {
			return ((FieldInsnNode) instr).owner + ":" + ((FieldInsnNode) instr).name + ":" + ((FieldInsnNode) instr).desc.replace('.', '/');
		} else {
			throw new RuntimeException("[MyMethodStaticContext.getFieldId] Error! Improper use of getFieldId()");
		}
	}
}