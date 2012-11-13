package ch.usi.dag.disl.test.dispatch;

import java.util.LinkedList;

import ch.usi.dag.disl.annotation.After;
import ch.usi.dag.disl.marker.BasicBlockMarker;
import ch.usi.dag.disl.marker.BodyMarker;

public class DiSLClass {
	
	@After(marker = BasicBlockMarker.class, scope = "TargetClass.*")
	public static void invokedInstr(CodeLengthSC clsc) {
		
		CodeExecutedRE.bytecodesExecuted(clsc.codeSize());
	}
	
	@After(marker = BodyMarker.class, scope = "TargetClass.main")
	public static void testing() {
		
		CodeExecutedRE.testingBasic(true, (byte) 125, 's', (short) 50000,
				100000, 10000000000L, 1.5F, 2.5);
		
		CodeExecutedRE.testingAdvanced("Corect transfer of String", new Object(), Object.class, 0);

		CodeExecutedRE.testingAdvanced2(new LinkedList<String>(),
				new LinkedList<Integer>(), new LinkedList[0], new int[0],
				int[].class, 0, int.class, 0, LinkedList.class, 0,
				LinkedList.class.getClass(), 0);
		
		CodeExecutedRE.testingNull(null, null, null);
	}
}
