package ch.usi.dag.disl.dislclass.code;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import ch.usi.dag.disl.dislclass.localvar.SyntheticLocalVar;
import ch.usi.dag.disl.dislclass.localvar.ThreadLocalVar;
import ch.usi.dag.disl.util.AsmHelper;

public class Code {

	private InsnList instructions;
	private List<TryCatchBlockNode> tryCatchBlocks;
	private Set<SyntheticLocalVar> referencedSLVs;
	private Set<ThreadLocalVar> referencedTLVs;
	// the code contains handler that handles exception and doesn't propagate
	// it further - can cause stack inconsistency that has to be handled
	private boolean containsHandledException;

	public Code(InsnList instructions, List<TryCatchBlockNode> tryCatchBlocks,
			Set<SyntheticLocalVar> referencedSLVs,
			Set<ThreadLocalVar> referencedTLVs,
			boolean containsHandledException) {
		super();
		this.instructions = instructions;
		this.tryCatchBlocks = tryCatchBlocks;
		this.referencedSLVs = referencedSLVs;
		this.referencedTLVs = referencedTLVs;
		this.containsHandledException = containsHandledException;
	}
	
	public InsnList getInstructions() {
		return instructions;
	}

	public List<TryCatchBlockNode> getTryCatchBlocks() {
		return tryCatchBlocks;
	}

	public Set<SyntheticLocalVar> getReferencedSLVs() {
		return referencedSLVs;
	}
	
	public Set<ThreadLocalVar> getReferencedTLVs() {
		return referencedTLVs;
	}

	public boolean containsHandledException() {
		return containsHandledException;
	}
	
	public Code clone() {
		
		Map<LabelNode, LabelNode> map = 
			AsmHelper.createLabelMap(instructions);
		
		return new Code(AsmHelper.cloneInsnList(instructions, map),
				AsmHelper.cloneTryCatchBlocks(tryCatchBlocks, map),
				new HashSet<SyntheticLocalVar>(referencedSLVs),
				new HashSet<ThreadLocalVar>(referencedTLVs),
				containsHandledException);
	}
}
