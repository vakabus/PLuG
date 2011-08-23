package ch.usi.dag.disl.dislclass.parser;

import java.util.List;
import java.util.Map;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;

import ch.usi.dag.disl.dislclass.annotation.Processor;
import ch.usi.dag.disl.dislclass.localvar.LocalVars;
import ch.usi.dag.disl.dislclass.processor.Proc;
import ch.usi.dag.disl.dislclass.snippet.Snippet;
import ch.usi.dag.disl.exception.ParserException;
import ch.usi.dag.disl.exception.ProcessorParserException;
import ch.usi.dag.disl.exception.ReflectionException;
import ch.usi.dag.disl.exception.ScopeParserException;
import ch.usi.dag.disl.exception.SnippetParserException;
import ch.usi.dag.disl.exception.StaticAnalysisException;

public class ClassParser {

	SnippetParser snippetParser = new SnippetParser();
	ProcessorParser processorParser = new ProcessorParser();
	
	public void parse(byte[] classAsBytes) throws ParserException,
			SnippetParserException, ReflectionException, ScopeParserException,
			StaticAnalysisException, ProcessorParserException {

		// prepare class node
		ClassReader cr = new ClassReader(classAsBytes);
		ClassNode classNode = new ClassNode();
		cr.accept(classNode, 0);

		// decide according to processor annotation if it is a snippet or
		// processor
		
		// *** snippet ***
		if (classNode.invisibleAnnotations == null) {
			snippetParser.parse(classNode);
			return;
		}

		// *** processor ***
		
		// check for one annotation
		if (classNode.invisibleAnnotations.size() > 1) {
			throw new ParserException("Class " + classNode.name
					+ " may have only one anotation");
		}

		AnnotationNode annotation = 
			(AnnotationNode) classNode.invisibleAnnotations.get(0);

		Type annotationType = Type.getType(annotation.desc);

		// check for processor annotation
		if (! annotationType.equals(Type.getType(Processor.class))) {
			throw new ParserException("Class " + classNode.name
					+ " may have only Processor anotation");
		}
		
		processorParser.parse(classNode);
	}
	
	public LocalVars getAllLocalVars() {
		
		LocalVars merged = new LocalVars();
		
		// merge local variables from snippets and processors 
		merged.putAll(snippetParser.getAllLocalVars());
		merged.putAll(processorParser.getAllLocalVars());
		
		return merged;
	}
	
	public List<Snippet> getSnippets() {
		return snippetParser.getSnippets();
	}
	
	public Map<Type, Proc> getProcessors() {
		return processorParser.getProcessors();
	}
}
