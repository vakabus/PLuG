package ch.usi.dag.disl.weaver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TryCatchBlockNode;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.SourceValue;

import ch.usi.dag.disl.marker.BodyMarker;
import ch.usi.dag.disl.snippet.Shadow;
import ch.usi.dag.disl.snippet.Snippet;
import ch.usi.dag.disl.util.AsmHelper;
import ch.usi.dag.disl.util.FrameHelper;
import ch.usi.dag.disl.util.cfg.CtrlFlowGraph;

public class WeavingInfo {

	private Map<AbstractInsnNode, AbstractInsnNode> weavingStart;
	private Map<AbstractInsnNode, AbstractInsnNode> weavingEnd;
	private Map<Shadow, AbstractInsnNode> weavingThrow;

	private Map<AbstractInsnNode, Integer> stackStart;
	private Map<AbstractInsnNode, Integer> stackEnd;

	private ArrayList<Snippet> sortedSnippets;

	private Frame<BasicValue>[] basicFrames;
	private Frame<SourceValue>[] sourceFrames;
	private Map<AbstractInsnNode, Frame<SourceValue>> sourceFrameMap;

	private Frame<BasicValue> retFrame;

	public WeavingInfo(ClassNode classNode, MethodNode methodNode,
			Map<Snippet, List<Shadow>> snippetMarkings) {

		weavingStart = new HashMap<AbstractInsnNode, AbstractInsnNode>();
		weavingEnd = new HashMap<AbstractInsnNode, AbstractInsnNode>();
		weavingThrow = new HashMap<Shadow, AbstractInsnNode>();

		stackStart = new HashMap<AbstractInsnNode, Integer>();
		stackEnd = new HashMap<AbstractInsnNode, Integer>();

		sortedSnippets = new ArrayList<Snippet>(snippetMarkings.keySet());
		Collections.sort(sortedSnippets);

		InsnList instructions = methodNode.instructions;

		List<LabelNode> tcb_ends = new LinkedList<LabelNode>();

		for (TryCatchBlockNode tcb : methodNode.tryCatchBlocks) {
			tcb_ends.add(tcb.end);
		}
		
		AbstractInsnNode lastIntruction = instructions.getLast();

		// initialize weaving start
		for (Snippet snippet : sortedSnippets) {
			for (Shadow region : snippetMarkings.get(snippet)) {

				AbstractInsnNode start = region.getRegionStart();

				if (weavingStart.get(start) == null) {

					LabelNode lstart = new LabelNode();
					instructions.insertBefore(start, lstart);
					weavingStart.put(start, lstart);
				}
			}
		}

		for (Snippet snippet : sortedSnippets) {
			for (Shadow region : snippetMarkings.get(snippet)) {

				AbstractInsnNode last = region.getRegionStart();

				for (AbstractInsnNode end : region.getRegionEnds()) {
					// initialize weaving end
					AbstractInsnNode wend = end;

					if (AsmHelper.isBranch(wend)) {
						wend = wend.getPrevious();
					}

					LabelNode lend = new LabelNode();
					instructions.insert(wend, lend);
					weavingEnd.put(end, lend);

					// initialize weaving athrow
					AbstractInsnNode iter = last;

					while (iter != null) {
						iter = iter.getNext();

						if (iter == end) {
							last = end;
							break;
						}
					}
				}

				if (snippet.getMarker() instanceof BodyMarker) {

					last = lastIntruction;
				} else {

					if (AsmHelper.isBranch(last)
							&& last.getOpcode() != Opcodes.ATHROW) {
						last = last.getPrevious();
					}

					while (tcb_ends.contains(last)) {
						last = last.getPrevious();
					}

				}

				LabelNode lthrow = new LabelNode();
				instructions.insert(last, lthrow);
				weavingThrow.put(region, lthrow);
			}
		}

		// initialize stack_start and stack_end
		for (Snippet snippet : sortedSnippets) {
			for (Shadow region : snippetMarkings.get(snippet)) {

				AbstractInsnNode start = region.getRegionStart();

				if (stackStart.get(start) == null) {
					stackStart.put(start, instructions.indexOf(start));
				}

				for (AbstractInsnNode end : region.getRegionEnds()) {
					if (AsmHelper.isBranch(end)) {
						stackEnd.put(end, instructions.indexOf(end));
					} else {
						stackEnd.put(end, instructions.indexOf(end.getNext()));
					}
				}

				AbstractInsnNode wend = weavingThrow.get(region);
				stackEnd.put(wend, instructions.indexOf(wend));
			}
		}

		basicFrames = FrameHelper.getBasicFrames(classNode.name, methodNode);
		sourceFrames = FrameHelper.getSourceFrames(classNode.name, methodNode);
		sourceFrameMap = new HashMap<AbstractInsnNode, Frame<SourceValue>>();
		
		for (int i = 0; i < sourceFrames.length; i++) {
			sourceFrameMap.put(methodNode.instructions.get(i), sourceFrames[i]);
		}

		List<AbstractInsnNode> ends = CtrlFlowGraph.build(methodNode).getEnds();
		AbstractInsnNode last_end = ends.get(ends.size()-1);
		retFrame = basicFrames[methodNode.instructions.indexOf(last_end)];		
	}

	public Map<AbstractInsnNode, AbstractInsnNode> getWeavingStart() {
		return weavingStart;
	}

	public Map<AbstractInsnNode, AbstractInsnNode> getWeavingEnd() {
		return weavingEnd;
	}

	public Map<Shadow, AbstractInsnNode> getWeavingThrow() {
		return weavingThrow;
	}

	public Map<AbstractInsnNode, Integer> getStackStart() {
		return stackStart;
	}

	public Map<AbstractInsnNode, Integer> getStackEnd() {
		return stackEnd;
	}

	public ArrayList<Snippet> getSortedSnippets() {
		return sortedSnippets;
	}

	public Frame<BasicValue>[] getBasicFrames() {
		return basicFrames;
	}

	public Frame<SourceValue>[] getSourceFrames() {
		return sourceFrames;
	}

	public Frame<BasicValue> getBasicFrame(int index) {
		return basicFrames[index];
	}

	public Frame<BasicValue> getRetFrame() {
		return retFrame;
	}

	public Frame<SourceValue> getSourceFrame(int index) {
		return sourceFrames[index];
	}

	public Frame<SourceValue> getSourceFrame(AbstractInsnNode instr) {
		return sourceFrameMap.get(instr);
	}

	public boolean stackNotEmpty(int index) {
		return basicFrames[index].getStackSize() > 0;
	}

	public InsnList backupStack(int index, int startFrom) {
		return FrameHelper.enter(basicFrames[index], startFrom);
	}

	public InsnList restoreStack(int index, int startFrom) {
		return FrameHelper.exit(basicFrames[index], startFrom);
	}

	public int getStackHeight(int index) {
		return FrameHelper.getOffset(basicFrames[index]);
	}
}
