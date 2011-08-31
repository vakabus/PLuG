package ch.usi.dag.disl.dislclass.annotation;

import ch.usi.dag.disl.guard.ProcessorGuard;

public @interface Processor {

	// NOTE if you want to change names, you need to change ProcessorParser class
	
	// NOTE because of implementation of annotations in java the defaults
	// are not retrieved from here but from SnippetParser

	Class<? extends ProcessorGuard> guard() default ProcessorGuard.class; // cannot be null :(
}
