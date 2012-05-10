package ch.usi.dag.disl.marker;

import java.util.LinkedList;
import java.util.List;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.snippet.Shadow.WeavingRegion;
import ch.usi.dag.disl.util.AsmHelper;

public class AfterInitBodyMarker extends AbstractMarker {

	@Override
	public List<MarkedRegion> mark(MethodNode method) {

		List<MarkedRegion> regions = new LinkedList<MarkedRegion>();

		MarkedRegion region = new MarkedRegion(
				AsmHelper.findFirstValidMark(method));

		for (AbstractInsnNode instr : method.instructions.toArray()) {

			int opcode = instr.getOpcode();

			if (opcode >= Opcodes.IRETURN && opcode <= Opcodes.RETURN) {
				region.addExitPoint(instr.getPrevious());
			}
		}

		WeavingRegion wregion = region.computeDefaultWeavingRegion(method);
		wregion.setAfterThrowEnd(method.instructions.getLast());
		region.setWeavingRegion(wregion);
		regions.add(region);
		return regions;
	}

}
