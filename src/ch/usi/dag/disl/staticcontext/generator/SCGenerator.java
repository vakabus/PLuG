package ch.usi.dag.disl.staticcontext.generator;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import ch.usi.dag.disl.exception.ReflectionException;
import ch.usi.dag.disl.exception.StaticContextGenException;
import ch.usi.dag.disl.snippet.Shadow;
import ch.usi.dag.disl.snippet.Snippet;
import ch.usi.dag.disl.snippet.StaticContextMethod;
import ch.usi.dag.disl.staticcontext.StaticContext;
import ch.usi.dag.disl.util.Constants;
import ch.usi.dag.disl.util.ReflectionHelper;

public class SCGenerator {

	class StaticContextKey {

		private Shadow shadow;
		private String methodID;

		public StaticContextKey(Shadow shadow, String methodID) {
			super();
			this.shadow = shadow;
			this.methodID = methodID;
		}

		@Override
		public int hashCode() {

			final int prime = 31;
			int result = 1;

			result = prime * result + getOuterType().hashCode();

			result = prime * result
					+ ((shadow == null) ? 0 : shadow.hashCode());

			result = prime * result
					+ ((methodID == null) ? 0 : methodID.hashCode());

			return result;
		}

		@Override
		public boolean equals(Object obj) {

			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			StaticContextKey other = (StaticContextKey) obj;
			if (!getOuterType().equals(other.getOuterType()))
				return false;

			if (shadow == null) {
				if (other.shadow != null) {
					return false;
				}
			} else if (!shadow.equals(other.shadow)) {
				return false;
			}

			if (methodID == null) {
				if (other.methodID != null) {
					return false;
				}
			} else if (!methodID.equals(other.methodID)) {
				return false;
			}

			return true;
		}

		private SCGenerator getOuterType() {
			return SCGenerator.this;
		}
	}

	Map<StaticContextKey, Object> staticInfoData = new HashMap<StaticContextKey, Object>();

	public SCGenerator(Map<Class<?>, Object> staticContextInstances,
			Map<Snippet, List<Shadow>> snippetMarkings)
			throws ReflectionException, StaticContextGenException {

		computeStaticInfo(staticContextInstances, snippetMarkings);
	}
	
	private StaticContextKey createStaticInfoKey(Shadow shadow,
			String infoClass, String infoMethod) {
		
		String methodID = infoClass + Constants.STATIC_CONTEXT_METHOD_DELIM
				+ infoMethod;
		
		return new StaticContextKey(shadow, methodID);
	}
	
	public boolean contains(Shadow shadow, String infoClass,
			String infoMethod) {
		
		StaticContextKey sck = 
			createStaticInfoKey(shadow, infoClass, infoMethod);
		
		return staticInfoData.containsKey(sck);
	}

	public Object get(Shadow shadow, String infoClass, String infoMethod) {

		StaticContextKey sck = 
			createStaticInfoKey(shadow, infoClass, infoMethod);
		
		return staticInfoData.get(sck);
	}

	// Call static context for each snippet and each marked region and create
	// a static info values
	private void computeStaticInfo(
			Map<Class<?>, Object> staticContextInstances,
			Map<Snippet, List<Shadow>> snippetMarkings)
			throws ReflectionException, StaticContextGenException {

		for (Snippet snippet : snippetMarkings.keySet()) {

			for (Shadow shadow : snippetMarkings.get(snippet)) {

				for (String stConMehodName : snippet.getCode()
						.getStaticContexts().keySet()) {

					// get static context method
					StaticContextMethod stAnMethod = snippet.getCode()
							.getStaticContexts().get(stConMehodName);

					// get static context instance
					Class<?> methodClass = stAnMethod.getReferencedClass();
					Object scInst = staticContextInstances.get(methodClass);

					// ... or create new one
					if (scInst == null) {

						scInst = ReflectionHelper.createInstance(methodClass);

						// and store for later use
						staticContextInstances.put(methodClass, scInst);
					}

					// recast context object to interface
					StaticContext scIntr = (StaticContext) scInst;

					// compute static data using context
					Object result = scIntr
							.computeStaticData(stAnMethod.getMethod(), shadow);

					// store the result
					setSI(shadow, stConMehodName, result);
				}
			}
		}
	}

	private void setSI(Shadow shadow, String methodID, Object value) {

		staticInfoData.put(new StaticContextKey(shadow, methodID), value);
	}
}