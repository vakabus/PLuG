package ch.usi.dag.disl.processor.generator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;

import ch.usi.dag.disl.dislclass.processor.Proc;
import ch.usi.dag.disl.dislclass.snippet.ProcInvocation;
import ch.usi.dag.disl.dislclass.snippet.Snippet;
import ch.usi.dag.disl.dislclass.snippet.marker.MarkedRegion;
import ch.usi.dag.disl.exception.DiSLFatalException;
import ch.usi.dag.disl.exception.ProcessorException;

public class ProcGenerator {

	Map<Proc, ProcInstance> insideMethodPIs = new HashMap<Proc, ProcInstance>();

	public PIResolver compute(String fullMethodName, MethodNode methodNode,
			Map<Snippet, List<MarkedRegion>> snippetMarkings)
			throws ProcessorException {

		PIResolver piResolver = new PIResolver();

		// for each snippet
		for (Snippet snippet : snippetMarkings.keySet()) {

			Map<AbstractInsnNode, ProcInvocation> invokedProcs = snippet
					.getCode().getInvokedProcessors();

			for (MarkedRegion markedRegion : snippetMarkings.get(snippet)) {

				// for each processor defined in snippet
				for (AbstractInsnNode instr : invokedProcs.keySet()) {

					ProcInvocation prcInv = invokedProcs.get(instr);

					ProcInstance prcInst = null;

					// NOTE: the result is automatically inserted into
					// PIResolver
					switch (prcInv.getProcApplyType()) {

					case INSIDE_METHOD: {
						prcInst = computeInsideMethod(methodNode,
								prcInv.getProcessor());
						break;
					}

					case BEFORE_INVOCATION: {
						prcInst = computeBeforeInvocation(fullMethodName, instr,
								prcInv.getProcessor());
						break;
					}

					default:
						throw new DiSLFatalException(
								"Proc computation not defined");
					}

					// add result to processor instance resolver
					piResolver.set(snippet, markedRegion, instr, prcInst);
				}
			}
		}

		return piResolver;
	}

	private ProcInstance computeInsideMethod(MethodNode methodNode,
			Proc processor) {

		// all instances of inside method processor will be the same
		// if we have one, we can use it multiple times

		ProcInstance procInst = insideMethodPIs.get(processor);

		if (procInst == null) {
			procInst = createProcInstance(methodNode.desc, processor);
		}

		return procInst;
	}

	private ProcInstance computeBeforeInvocation(String fullMethodName,
			AbstractInsnNode instr, Proc processor) throws ProcessorException {

		if(! (instr instanceof MethodInsnNode)) {
			throw new ProcessorException("Processor " + processor.getName() +
					" is not applied before method invocation in method " +
					fullMethodName);
		}
		
		MethodInsnNode methodInvocation = (MethodInsnNode) instr; 
		
		return createProcInstance(methodInvocation.desc, processor);
	}

	private ProcInstance createProcInstance(String desc,
			Proc processor) {

		// TODO ! processors implement
		return null;
	}
}
