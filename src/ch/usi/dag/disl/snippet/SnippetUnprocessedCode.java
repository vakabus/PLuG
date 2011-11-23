package ch.usi.dag.disl.snippet;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.FieldInsnNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.JumpInsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LdcInsnNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.TryCatchBlockNode;

import ch.usi.dag.disl.coderep.Code;
import ch.usi.dag.disl.coderep.UnprocessedCode;
import ch.usi.dag.disl.exception.ProcessorException;
import ch.usi.dag.disl.exception.ReflectionException;
import ch.usi.dag.disl.exception.StaticContextGenException;
import ch.usi.dag.disl.localvar.LocalVars;
import ch.usi.dag.disl.marker.BytecodeMarker;
import ch.usi.dag.disl.marker.Marker;
import ch.usi.dag.disl.processorcontext.ProcessorContext;
import ch.usi.dag.disl.processorcontext.ProcessorMode;
import ch.usi.dag.disl.snippet.processor.Proc;
import ch.usi.dag.jborat.runtime.DynamicBypass;

public class SnippetUnprocessedCode extends UnprocessedCode {

	private String className;
	private String methodName;
	private boolean dynamicBypass;
	private boolean usesProcessorContext;

	public SnippetUnprocessedCode(String className, String methodName,
			InsnList instructions, List<TryCatchBlockNode> tryCatchBlocks,
			Set<String> declaredStaticContexts, boolean usesDynamicContext,
			boolean dynamicBypass, boolean usesProcessorContext) {
		super(instructions, tryCatchBlocks, declaredStaticContexts,
				usesDynamicContext);
		this.className = className;
		this.methodName = methodName;
		this.dynamicBypass = dynamicBypass;
		this.usesProcessorContext = usesProcessorContext;
	}

	public SnippetCode process(LocalVars allLVs,
			Map<Type, Proc> processors, Marker marker, boolean allDynamicBypass)
			throws StaticContextGenException, ReflectionException,
			ProcessorException {

		// process code
		Code code = super.process(allLVs);

		// process snippet code
		
		InsnList instructions = code.getInstructions();
		List<TryCatchBlockNode> tryCatchBlocks = code.getTryCatchBlocks();
		
		// *** CODE PROCESSING ***
		// !NOTE ! : Code processing has to be done before "processors in use"
		// analysis otherwise the instruction reference produced by this
		// analysis may be wrong
		// NOTE: methods are modifying arguments

		if (allDynamicBypass || dynamicBypass) {
			insertDynamicBypass(instructions, tryCatchBlocks);
		}

		// *** CODE ANALYSIS ***

		Map<Integer, ProcInvocation> invokedProcessors = 
			new HashMap<Integer, ProcInvocation>();

		AbstractInsnNode[] instructionArray = instructions.toArray();
		for (int i = 0; i < instructionArray.length; ++i) {

			AbstractInsnNode instr = instructionArray[i];

			// *** Parse processors in use ***
			// no other modifications to the code should be done before weaving
			// otherwise, produced instruction reference can be invalid

			ProcessorInfo processor = 
				insnInvokesProcessor(instr, i, processors, marker);

			if (processor != null) {
				invokedProcessors.put(processor.getInstrPos(),
						processor.getProcInvoke());
				continue;
			}
		}

		return new SnippetCode(instructions, tryCatchBlocks,
				code.getReferencedSLVs(), code.getReferencedTLVs(),
				code.containsHandledException(), code.getStaticContexts(),
				code.usesDynamicContext(), usesProcessorContext,
				invokedProcessors);
	}

	private static class ProcessorInfo {

		private Integer instrPos;
		private ProcInvocation procInvoke;

		public ProcessorInfo(Integer instrPos, ProcInvocation procInvoke) {
			super();
			this.instrPos = instrPos;
			this.procInvoke = procInvoke;
		}

		public Integer getInstrPos() {
			return instrPos;
		}

		public ProcInvocation getProcInvoke() {
			return procInvoke;
		}
	}

	private ProcessorInfo insnInvokesProcessor(AbstractInsnNode instr, int i,
			Map<Type, Proc> processors, Marker marker)
			throws ProcessorException, ReflectionException {

		final String APPLY_METHOD = "apply";

		// check method invocation
		if (!(instr instanceof MethodInsnNode)) {
			return null;
		}

		MethodInsnNode min = (MethodInsnNode) instr;

		// check if the invocation is processor invocation
		if (!(min.owner.equals(Type.getInternalName(ProcessorContext.class))
				&& min.name.equals(APPLY_METHOD))) {
			return null;
		}

		// resolve load parameter instruction
		AbstractInsnNode secondParam = instr.getPrevious();
		AbstractInsnNode firstParam = secondParam.getPrevious();

		// first parameter has to be loaded by LDC
		if (firstParam == null || firstParam.getOpcode() != Opcodes.LDC) {
			throw new ProcessorException("In snippet " + className + "."
					+ methodName + " - pass the first (class)"
					+ " argument of a ProcMethod.apply method direcltly."
					+ " ex: ProcMethod.apply(ProcMethod.class,"
					+ " ProcessorMode.METHOD_ARGS)");
		}

		// second parameter has to be loaded by GETSTATIC
		if (secondParam == null || secondParam.getOpcode() != Opcodes.GETSTATIC) {
			throw new ProcessorException("In snippet " + className + "."
					+ methodName + " - pass the second (type)"
					+ " argument of a ProcMethod.apply method direcltly."
					+ " ex: ProcMethod.apply(ProcMethod.class,"
					+ " ProcessorMode.METHOD_ARGS)");
		}

		Object asmType = ((LdcInsnNode) firstParam).cst;

		if (!(asmType instanceof Type)) {
			throw new ProcessorException("In snippet " + className + "."
					+ methodName + " - unsupported processor type "
					+ asmType.getClass().toString());
		}

		Type processorType = (Type) asmType;
		
		ProcessorMode procApplyType = ProcessorMode
				.valueOf(((FieldInsnNode) secondParam).name);
		
		// if the processor apply type is CALLSITE_ARGS
		// the only allowed marker is BytecodeMarker
		if(ProcessorMode.CALLSITE_ARGS.equals(procApplyType)
				&& marker.getClass() != BytecodeMarker.class) {
			throw new ProcessorException(
					"ArgsProcessor applied in mode CALLSITE_ARGS in method "
					+ className + "." + methodName
					+ " can be used only with BytecodeMarker");
		}

		Proc processor = processors.get(processorType);

		if (processor == null) {
			throw new ProcessorException("In snippet " + className + "."
					+ methodName + " - unknow processor used: "
					+ processorType.getClassName());
		}

		ProcInvocation prcInv = new ProcInvocation(processor, procApplyType);

		// get instruction index

		return new ProcessorInfo(i, prcInv);
	}

	private void insertDynamicBypass(InsnList instructions,
			List<TryCatchBlockNode> tryCatchBlocks) {

		// inserts
		// DynamicBypass.activate();
		// try {
		// ... original code
		// } finally {
		// DynamicBypass.deactivate();
		// }

		// create method nodes
		Type typeDB = Type.getType(DynamicBypass.class);
		MethodInsnNode mtdActivate = new MethodInsnNode(Opcodes.INVOKESTATIC,
				typeDB.getInternalName(), "activate", "()V");
		MethodInsnNode mtdDeactivate = new MethodInsnNode(Opcodes.INVOKESTATIC,
				typeDB.getInternalName(), "deactivate", "()V");

		// add try label at the beginning
		LabelNode tryBegin = new LabelNode();
		instructions.insert(tryBegin);

		// add invocation of activate at the beginning
		instructions.insert(mtdActivate.clone(null));

		// ## try {

		// ## }

		// add try label at the end
		LabelNode tryEnd = new LabelNode();
		instructions.add(tryEnd);

		// ## after normal flow

		// add invocation of deactivate - normal flow
		instructions.add(mtdDeactivate.clone(null));

		// normal flow should jump after handler
		LabelNode handlerEnd = new LabelNode();
		instructions.add(new JumpInsnNode(Opcodes.GOTO, handlerEnd));

		// ## after abnormal flow - exception handler

		// add handler begin
		LabelNode handlerBegin = new LabelNode();
		instructions.add(handlerBegin);

		// TODO ! snippet should not throw an exception - the solution should contain try-finally for each block regardless of dynamic bypass and should fail immediately
		// add invocation of deactivate - abnormal flow
		instructions.add(mtdDeactivate.clone(null));
		// throw exception again
		instructions.add(new InsnNode(Opcodes.ATHROW));

		// add handler end
		instructions.add(handlerEnd);

		// ## add handler to the list
		tryCatchBlocks.add(new TryCatchBlockNode(tryBegin, tryEnd,
				handlerBegin, null));
	}
}
