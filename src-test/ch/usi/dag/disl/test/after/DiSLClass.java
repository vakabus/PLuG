package ch.usi.dag.disl.test.after;

import ch.usi.dag.disl.annotation.After;
import ch.usi.dag.disl.annotation.AfterThrowing;
import ch.usi.dag.disl.marker.BodyMarker;

public class DiSLClass {
	
	@After(marker = BodyMarker.class, scope = "TargetClass.print(boolean)", order = 0)
	public static void after() {
		System.out.println("after");
	}
	
	@AfterThrowing(marker = BodyMarker.class, scope = "TargetClass.print(boolean)", order = 1)
	public static void afterThrowning() {
		System.out.println("afterThrowning");
	}
}