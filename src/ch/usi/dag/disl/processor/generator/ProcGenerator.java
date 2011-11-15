package ch.usi.dag.disl.processor.generator;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.exception.DiSLFatalException;
import ch.usi.dag.disl.exception.ProcessorException;
import ch.usi.dag.disl.guard.ProcessorGuard;
import ch.usi.dag.disl.guard.ProcessorMethodGuard;
import ch.usi.dag.disl.marker.MarkedRegion;
import ch.usi.dag.disl.processor.ProcessorMode;
import ch.usi.dag.disl.processor.generator.struct.Proc;
import ch.usi.dag.disl.processor.generator.struct.ProcArgType;
import ch.usi.dag.disl.processor.generator.struct.ProcMethod;
import ch.usi.dag.disl.snippet.ProcInvocation;
import ch.usi.dag.disl.snippet.Snippet;

public class ProcGenerator {

	Map<Proc, ProcInstance> insideMethodPIs = new HashMap<Proc, ProcInstance>();

	private static class PMGuardData {
		
		private ClassNode classNode;
		private MethodNode methodNode;
		private Snippet snippet;
		private MarkedRegion markedRegion;
		private ProcInvocation prcInv;
		
		public PMGuardData(ClassNode classNode, MethodNode methodNode,
				Snippet snippet, MarkedRegion markedRegion,
				ProcInvocation prcInv) {
			super();
			this.classNode = classNode;
			this.methodNode = methodNode;
			this.snippet = snippet;
			this.markedRegion = markedRegion;
			this.prcInv = prcInv;
		}

		public ClassNode getClassNode() {
			return classNode;
		}

		public MethodNode getMethodNode() {
			return methodNode;
		}

		public Snippet getSnippet() {
			return snippet;
		}

		public MarkedRegion getMarkedRegion() {
			return markedRegion;
		}

		public ProcInvocation getPrcInv() {
			return prcInv;
		}
	}
	
	public PIResolver compute(ClassNode classNode, MethodNode methodNode,
			Map<Snippet, List<MarkedRegion>> snippetMarkings)
			throws ProcessorException {

		PIResolver piResolver = new PIResolver();

		// for each snippet
		for (Snippet snippet : snippetMarkings.keySet()) {

			Map<Integer, ProcInvocation> invokedProcs = snippet.getCode()
					.getInvokedProcessors();

			for (MarkedRegion markedRegion : snippetMarkings.get(snippet)) {

				// for each processor defined in snippet
				for (Integer instrPos : invokedProcs.keySet()) {

					ProcInvocation prcInv = invokedProcs.get(instrPos);

					Proc processor = prcInv.getProcessor();
					ProcessorGuard prcGuard = processor.getGuard();

					// evaluate processor guard
					if(prcGuard != null && ! prcGuard.isApplicable(classNode,
							methodNode, snippet, markedRegion, prcInv)) {
						// if not applicable - skipp
						continue;
					}
					
					ProcInstance prcInst = null;
					PMGuardData pmgd = new PMGuardData(classNode, methodNode,
							snippet, markedRegion, prcInv);

					// handle apply type
					switch (prcInv.getProcApplyType()) {

					case METHOD_ARGS: {
						prcInst = computeInsideMethod(methodNode,
								prcInv.getProcessor(), pmgd);
						break;
					}

					case CALLSITE_ARGS: {
						prcInst = computeBeforeInvocation(
								classNode.name + "." + methodNode.name,
								markedRegion, prcInv.getProcessor(), pmgd);
						break;
					}

					default:
						throw new DiSLFatalException(
								"Proc computation not defined");
					}
					
					if(prcInst != null) {
						// add result to processor instance resolver
						piResolver.set(snippet, markedRegion, instrPos, prcInst);
					}
				}
			}
		}

		return piResolver;
	}

	private ProcInstance computeInsideMethod(MethodNode methodNode,
			Proc processor, PMGuardData pmgd) {

		// all instances of inside method processor will be the same
		// if we have one, we can use it multiple times

		ProcInstance procInst = insideMethodPIs.get(processor);

		if (procInst == null) {
			procInst = createProcInstance(ProcessorMode.METHOD_ARGS,
					methodNode.desc, processor, pmgd);
		}

		return procInst;
	}

	private ProcInstance computeBeforeInvocation(String fullMethodName,
			MarkedRegion markedRegion, Proc processor, PMGuardData pmgd)
			throws ProcessorException {

		// NOTE: SnippetUnprocessedCode checks that CALLSITE_ARGS is
		// used only with BytecodeMarker
		
		// because it is BytecodeMarker, it should have only one end 
		if(markedRegion.getEnds().size() > 1) {
			throw new DiSLFatalException(
					"Expected only one end in marked region");
		}
		
		// get instruction from the method code
		// the method invocation is the instruction marked as end
		AbstractInsnNode instr = markedRegion.getEnds().get(0);

		// check - method invocation
		if (!(instr instanceof MethodInsnNode)) {
			throw new ProcessorException("ArgsProcessor " + processor.getName()
					+ " is not applied before method invocation in method "
					+ fullMethodName);
		}

		MethodInsnNode methodInvocation = (MethodInsnNode) instr;

		return createProcInstance(ProcessorMode.CALLSITE_ARGS,
				methodInvocation.desc, processor, pmgd);
	}

	private ProcInstance createProcInstance(ProcessorMode procApplyType,
			String methodDesc, Proc processor, PMGuardData pmgd) {

		List<ProcMethodInstance> procMethodInstances = 
			new LinkedList<ProcMethodInstance>();

		// get argument types
		Type[] argTypeArray = Type.getArgumentTypes(methodDesc);
		
		// create processor method instances for each argument if applicable
		for (int i = 0; i < argTypeArray.length; ++i) {

			List<ProcMethodInstance> pmis = createMethodInstances(i,
					argTypeArray.length, argTypeArray[i], processor, pmgd);

			procMethodInstances.addAll(pmis);
		}

		if(procMethodInstances.isEmpty()) {
			return null;
		}

		// create new processor instance
		return new ProcInstance(procApplyType, procMethodInstances);
	}

	private List<ProcMethodInstance> createMethodInstances(int argPos,
			int argsCount, Type argType, Proc processor, PMGuardData pmgd) {

		ProcArgType methodArgType = ProcArgType.valueOf(argType);
		
		List<ProcMethodInstance> result = new LinkedList<ProcMethodInstance>();
		
		// traverse all methods and find the proper ones
		for (ProcMethod method : processor.getMethods()) {
			
			// check argument type
			if(method.getTypes().contains(methodArgType)) {
				
				ProcMethodInstance pmi = new ProcMethodInstance(argPos,
						argsCount, methodArgType, method.getCode());
				
				if(method.insertTypeName()) {
					pmi.setArgTypeName(argType.getInternalName());
				}

				// check guard
				if(isPMGuardApplicable(method.getGuard(), pmgd, pmi, argType)) {

					// add method
					result.add(pmi);
				}
			}
		}
		
		return result;
	}

	private boolean isPMGuardApplicable(
			ProcessorMethodGuard guard, PMGuardData pmgd,
			ProcMethodInstance pmi, Type exactType) {

		if(guard == null) {
			return true;
		}
		
		// evaluate processor method guard
		return guard.isApplicable(pmgd.getClassNode(), pmgd.getMethodNode(),
				pmgd.getSnippet(), pmgd.getMarkedRegion(), pmgd.getPrcInv(),
				pmi, exactType);
	}
}
