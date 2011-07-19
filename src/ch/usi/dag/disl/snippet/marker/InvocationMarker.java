package ch.usi.dag.disl.snippet.marker;

import java.util.LinkedList;
import java.util.List;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

public class InvocationMarker extends AbstractMarker implements Marker {

	@Override
	public List<MarkedRegion> mark(MethodNode method) {
		
		List<MarkedRegion> regions = new LinkedList<MarkedRegion>();
		InsnList ilst = method.instructions;

		for (AbstractInsnNode instruction : ilst.toArray()) {
			
			if (instruction instanceof MethodInsnNode) {
				
				regions.add(new MarkedRegion(method, instruction, instruction));
			}
		}

		return regions;
	}
}
