package ch.usi.dag.disl.snippet;

import java.util.Set;

import org.objectweb.asm.tree.InsnList;

import ch.usi.dag.disl.snippet.marker.Marker;
import ch.usi.dag.disl.snippet.scope.Scope;
import ch.usi.dag.disl.snippet.syntheticlocal.SyntheticLocalVar;
import ch.usi.dag.disl.staticinfo.analysis.Analysis;

public class Snippet implements Comparable<Snippet> {

	protected Class<?> annotationClass;
	protected Marker marker;
	protected Scope scope;
	protected int order;
	protected InsnList asmCode;
	protected Set<SyntheticLocalVar> syntheticLocalVars;
	private Set<Class<? extends Analysis>> analyses;
	
	public Snippet(Class<?> annotationClass,
			Marker marker,
			Scope scope,
			int order, InsnList asmCode,
			Set<SyntheticLocalVar> syntheticLocalVars,
			Set<Class<? extends Analysis>> analyses) {
		super();

		this.annotationClass = annotationClass;
		this.marker = marker;
		this.scope = scope;
		this.order = order;
		this.asmCode = asmCode;
		this.syntheticLocalVars = syntheticLocalVars;
		this.analyses = analyses;
	}

	public Class<?> getAnnotationClass() {
		return annotationClass;
	}

	public Marker getMarker() {
		return marker;
	}

	public Scope getScope() {
		return scope;
	}

	public int getOrder() {
		return order;
	}

	public InsnList getAsmCode() {
		return asmCode;
	}
	
	public Set<SyntheticLocalVar> getSyntheticLocalVars() {
		return syntheticLocalVars;
	}

	public Set<Class<? extends Analysis>> getAnalyses() {
		return analyses;
	}

	
	public int compareTo(Snippet o) {
		return order - o.getOrder();
	}
}
