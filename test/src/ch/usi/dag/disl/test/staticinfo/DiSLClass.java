package ch.usi.dag.disl.test.staticinfo;

import ch.usi.dag.disl.annotation.Before;
import ch.usi.dag.disl.snippet.marker.BodyMarker;
import ch.usi.dag.disl.staticinfo.analysis.StaticContext;

public class DiSLClass {
	
	@Before(marker = BodyMarker.class, scope = "TargetClass.this_is_a_method_name", order = 0)
	public static void precondition(StaticContext ci) {
		
		String mid = ci.getMethodName();
		System.out.println(mid);
		
		// caching test
		String mid2 = ci.getMethodName();
		System.out.println(mid2);
	}
	
	@Before(marker = BodyMarker.class, scope = "TargetClass.this_is_a_method_name", order = 1)
	public static void secondPrecondition(StaticContext ci) {
		
		// caching test
		String mid3 = ci.getMethodName();
		System.out.println(mid3);
	}
}