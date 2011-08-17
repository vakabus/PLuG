package ch.usi.dag.disl.annotation;

public @interface ThreadLocal {

	// NOTE if you want to change names, you need to change ClassParser class
	// in start subproject
	
	// NOTE because of implementation of annotations in java the defaults
	// are not retrieved from here but from SnippetParser
	
	boolean inheritable() default(false);
	// TODO ! default value should be parsed from asm code
	String defaultVal() default("");
}
