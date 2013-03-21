package ch.usi.dag.disl.exclusion;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import ch.usi.dag.disl.cbloader.ManifestHelper;
import ch.usi.dag.disl.cbloader.ManifestHelper.ManifestInfo;
import ch.usi.dag.disl.exception.ExclusionPrepareException;
import ch.usi.dag.disl.exception.ManifestInfoException;
import ch.usi.dag.disl.exception.ScopeParserException;
import ch.usi.dag.disl.scope.Scope;
import ch.usi.dag.disl.scope.ScopeImpl;
import ch.usi.dag.disl.util.Constants;

public abstract class ExclusionSet {

	private static final String PROP_EXCLIST = "disl.exclusionList";
	private static final String excListPath = 
			System.getProperty(PROP_EXCLIST, null);
	
	private static final String JAR_PATH_BEGIN = "/";
	private static final String JAR_PATH_END = "!";
	
	private static final char JAR_ENTRY_DELIM = '/';
	private static final char CLASS_DELIM = '.';
	private static final String ALL_METHODS = ".*";
	
	public static Set<Scope> prepare() throws ScopeParserException,
			ManifestInfoException, ExclusionPrepareException {

		Set<Scope> exclSet = defaultExcludes();
		exclSet.addAll(instrumentationJar());
		exclSet.addAll(readExlusionList());
		
		return exclSet;
	}

	private static Set<Scope> defaultExcludes() throws ScopeParserException {
		
		// if appended to the package name (for scoping purposes),
		// excludes all methods in all classes in the package and sub-packages
		final String EXCLUDE_CLASSES = ".*.*";
		
		Set<Scope> exclSet = new HashSet<Scope>();
		
		// DiSL classes
		exclSet.add(new ScopeImpl(
				"ch.usi.dag.disl" + EXCLUDE_CLASSES));
		
		// DiSLRE classes
		exclSet.add(new ScopeImpl(
				"ch.usi.dag.dislre" + EXCLUDE_CLASSES));
		
		return exclSet;
	}
	
	private static Set<Scope> instrumentationJar()
			throws ManifestInfoException, ExclusionPrepareException,
			ScopeParserException {

		try {
		
			// add classes from instrumentation jar
			
			Set<Scope> exclSet = new HashSet<Scope>();
			
			// get DiSL manifest info
			ManifestInfo mi = ManifestHelper.getDiSLManifestInfo();
			
			// no manifest found
			if(mi == null) {
				return exclSet;
			}
			
			// get URL of the instrumentation jar manifest
			URL manifestURL = 
					ManifestHelper.getDiSLManifestInfo().getResource();
			
			// manifest path contains "file:" + jar name + "!" + manifest path
			String manifestPath = manifestURL.getPath();
			
			// extract jar path
			int jarPathBegin = manifestPath.indexOf(JAR_PATH_BEGIN);
			int jarPathEnd = manifestPath.indexOf(JAR_PATH_END);
			String jarPath = manifestPath.substring(jarPathBegin, jarPathEnd);
			
			// open jar... 
			JarFile jarFile = new JarFile(jarPath);
			
			// ... and iterate over items
			Enumeration<JarEntry> entries = jarFile.entries();
			while (entries.hasMoreElements()) {
				
				JarEntry entry = entries.nextElement();
				
				// get entry name
				String entryName = entry.getName();
				
				// add all classes to the exclusion list
				if (entryName.endsWith(Constants.CLASS_EXT)) {
					
					String className = 
							entryName.replace(JAR_ENTRY_DELIM, CLASS_DELIM);
					
					// remove class ext
					int classNameEnd = 
							className.lastIndexOf(Constants.CLASS_EXT);
					className = className.substring(0, classNameEnd);
					
					// add exclusion for all methods
					String classExcl = className + ALL_METHODS;
					
					exclSet.add(new ScopeImpl(classExcl));
				}
			}
			
			jarFile.close();
			
			return exclSet;
		
		} catch(IOException e) {
			throw new ExclusionPrepareException(e);
		}
	}
	
	private static Set<Scope> readExlusionList()
			throws ExclusionPrepareException, ScopeParserException {

		final String COMMENT_START = "#";
		
		try {
		
			Set<Scope> exclSet = new HashSet<Scope>();

			// if exclusion list path exits
			if(excListPath != null) {
			
				// read exclusion list line by line
				Scanner scanner = new Scanner(new FileInputStream(excListPath));
				while (scanner.hasNextLine()) {
					
					String line = scanner.nextLine();
					
					if(! line.startsWith(COMMENT_START)) {
						exclSet.add(new ScopeImpl(line));
					}
				}
	
				scanner.close();
			}

			return exclSet;
		
		} catch(FileNotFoundException e) {
			throw new ExclusionPrepareException(e);
		}
	}
}
