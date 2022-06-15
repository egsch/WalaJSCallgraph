/**
 * Code from https://github.com/wala/WALA-start/blob/master/src/main/java/com/ibm/wala/examples/drivers/JSCallGraphDriver.java
 * Used under Eclipse Public License 1.0
 */

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.FileWriter;
import java.util.Iterator;

import com.ibm.wala.cast.js.ipa.callgraph.JSAnalysisOptions;
import com.ibm.wala.cast.js.ipa.callgraph.JSCFABuilder;
import com.ibm.wala.cast.js.ipa.callgraph.JSCallGraphUtil;
import com.ibm.wala.cast.js.loader.JavaScriptLoader;
import com.ibm.wala.cast.js.loader.JavaScriptLoaderFactory;
import com.ibm.wala.cast.js.translator.CAstRhinoTranslatorFactory;
import com.ibm.wala.cast.js.util.JSCallGraphBuilderUtil;
import com.ibm.wala.cast.js.util.JSCallGraphBuilderUtil.CGBuilderType;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphStats;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;

import picocli.CommandLine;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.ProgramCounter;

public class JSCallGraphDriver {

	/**
	 * Usage: JSCallGraphDriver
	 * @param args
	 * @throws WalaException 
	 * @throws CancelException 
	 * @throws IOException 
	 * @throws IllegalArgumentException 
	 * 
	 */
	/* Example Command Line options: 
	 * --scripts C:\Users\egsch\eclipse-workspace\jscallgraph\src\test\resources\functionstest.js --cgoutput C:\Users\egsch\eclipse-workspace\jscallgraph\src\test\resources\outputs.txt --reflectionSetting FULL
	 */
	private static CommandLineOptions clo;
	
	public static void main(String[] args) throws IllegalArgumentException, IOException, CancelException, WalaException {
		
		JSCallGraphDriver.clo = new CommandLineOptions();
        new CommandLine(clo).parseArgs(args);
        if (clo.usageHelpRequested) {
            CommandLine.usage(new CommandLineOptions(), System.out);
            return;
        }
		// Set up JS Call Graph Builder
		Path inPath = Paths.get(clo.scriptPath);
		JSCallGraphUtil.setTranslatorFactory(new CAstRhinoTranslatorFactory());
		JavaScriptLoaderFactory loaders = JSCallGraphBuilderUtil.makeLoaders();
		AnalysisScope scope = JSCallGraphBuilderUtil.makeScope(JSCallGraphBuilderUtil.makeSourceModules(inPath.getParent().toString(), inPath.getFileName().toString(), JSCallGraphBuilderUtil.class.getClassLoader()), loaders, JavaScriptLoader.JS);
		IClassHierarchy cha = JSCallGraphBuilderUtil.makeHierarchy(scope, loaders);
		Iterable<Entrypoint> e = JSCallGraphBuilderUtil.makeScriptRoots(cha);
		JSAnalysisOptions options = new JSAnalysisOptions(scope, e);
		options.setReflectionOptions(clo.reflection);
		options.setHandleStaticInit(clo.handleStaticInit);
		options.setHandleZeroLengthArray(clo.handleZeroLengthArray);
		options.setUseStacksForLexicalScoping(clo.useStacksForLexicalScoping);
		options.setUseLexicalScopingForGlobals(clo.useLexicalScopingForGlobals);
		/*
		 * maxNumberOfNodes does not seem to apply to JavaScript analysis
		 * (otherwise, something might be wrong with mechanism to set options)
		 * ex. options.setMaxNumberOfNodes(clo.maxNumberOfNodes);
		 */
		/*
		 * Options.set methods that are not implemented in clo:
		 *  (I do not know yet if any of these are applicable to JavaScript,
		 *   or to this project in general)
		 * options.setTraceStringConstants(true);
		 * options.setMaxEvalBetweenTopo(0);
		 * options.setMinEquationsForTopSort(0);
		 * options.setSSAOptions(null);
		 * options.setSelector(null);
		 * options.setTopologicalGrowthFactor(0);
		 * options.setUseConstantSpecificKeys(false);
		 * options.setUseLoadFileTargetSelector(false);
		 * 
		 */
		JSCFABuilder builder;
		if (clo.callGraphBuilder.toString().equals("ZEROONE_CFA")) {
			if (clo.handleCallApply) {
				if(clo.enableCorrelationTracking) {
					builder = JSCallGraphBuilderUtil.makeScriptCGBuilder(inPath.getParent().toString(), inPath.getFileName().toString(), CGBuilderType.ZERO_ONE_CFA, JSCallGraphBuilderUtil.class.getClassLoader());
				} else {
					builder = JSCallGraphBuilderUtil.makeScriptCGBuilder(inPath.getParent().toString(), inPath.getFileName().toString(), CGBuilderType.ZERO_ONE_CFA_WITHOUT_CORRELATION_TRACKING, JSCallGraphBuilderUtil.class.getClassLoader());
				}
			} else {
				// NOTE: Assumes correlation tracking enabled, as no option is available without call apply or correlation tracking
				builder = JSCallGraphBuilderUtil.makeScriptCGBuilder(inPath.getParent().toString(), inPath.getFileName().toString(), CGBuilderType.ZERO_ONE_CFA_NO_CALL_APPLY, JSCallGraphBuilderUtil.class.getClassLoader());
			}
		} else if (clo.callGraphBuilder.toString().equals("ONE_CFA")) {
			// NOTE: Assumes correlation tracking and call apply enabled
			builder = JSCallGraphBuilderUtil.makeScriptCGBuilder(inPath.getParent().toString(), inPath.getFileName().toString(), CGBuilderType.ONE_CFA, JSCallGraphBuilderUtil.class.getClassLoader());
		} else {
			throw new IllegalArgumentException("Invalid call graph algorithm.");
		}
		
		//Build and save call graph
		CallGraph CG = builder.makeCallGraph(options);
		System.out.println(CallGraphStats.getStats(CG));
		FileWriter fw = new FileWriter(clo.callgraphOutput.toString());
        for (CGNode cgn : CG) {
            Iterator<CallSiteReference> callSiteIterator = cgn.iterateCallSites();
            while (callSiteIterator.hasNext()) {
                CallSiteReference csi = callSiteIterator.next();
                SSAInstruction s = cgn.getIR().getPEI(new ProgramCounter(csi.getProgramCounter()));
                for (CGNode target : CG.getPossibleTargets(cgn, csi)) {
                    fw.write(String.format(
                            "%s\t%s\t%s\t%s\t%s\n",
                            cgn.getMethod(),
                            s.toString(),
                            cgn.getContext(),
                            target.getMethod().getSignature(),
                            target.getContext()));
                }
            }
        }
        
        System.out.println("Wrote callgraph to " + clo.callgraphOutput.toString());
        fw.close();
	}

}

/**
 * < Application, Lcfne/Demo, main([Ljava/lang/String;)V >	invokestatic < Application, Ljava/lang/Class, forName(Ljava/lang/String;)Ljava/lang/Class; >@2	Everywhere	java.lang.Class.forName(Ljava/lang/String;)Ljava/lang/Class;	Everywhere
<Code body of function Lprologue.js>	JSCall@186	Everywhere	Function.ctor()LRoot;	CallStringContext: [ prologue.js.do()LRoot;@186 ]
 * **/