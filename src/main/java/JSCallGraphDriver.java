/**
 * Code from https://github.com/wala/WALA-start/blob/master/src/main/java/com/ibm/wala/examples/drivers/JSCallGraphDriver.java
 * Used under Eclipse Public License 1.0
 */

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.io.FileWriter;
import java.util.Iterator;

import com.ibm.wala.cast.ipa.callgraph.StandardFunctionTargetSelector;
import com.ibm.wala.cast.ir.ssa.AstIRFactory;
import com.ibm.wala.cast.js.ipa.callgraph.JSAnalysisOptions;
import com.ibm.wala.cast.js.ipa.callgraph.JSCFABuilder;
import com.ibm.wala.cast.js.ipa.callgraph.JSCallGraphUtil;
import com.ibm.wala.cast.js.loader.JavaScriptLoader;
import com.ibm.wala.cast.js.loader.JavaScriptLoaderFactory;
import com.ibm.wala.cast.js.translator.CAstRhinoTranslatorFactory;
import com.ibm.wala.cast.js.util.JSCallGraphBuilderUtil;
import com.ibm.wala.cast.js.util.JSCallGraphBuilderUtil.CGBuilderType;
import com.ibm.wala.cast.tree.rewrite.CAstRewriterFactory;
import com.ibm.wala.cast.js.ipa.callgraph.JSZeroOrOneXCFABuilder;
import com.ibm.wala.cast.js.ipa.callgraph.LoadFileTargetSelector;
import com.ibm.wala.cast.js.ipa.callgraph.PropertyNameContextSelector;
import com.ibm.wala.cast.js.ipa.callgraph.correlations.extraction.CorrelatedPairExtractorFactory;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphStats;
import com.ibm.wala.ipa.callgraph.ClassTargetSelector;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.MethodTargetSelector;
import com.ibm.wala.ipa.callgraph.impl.ClassHierarchyClassTargetSelector;
import com.ibm.wala.ipa.callgraph.impl.ClassHierarchyMethodTargetSelector;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ssa.IRFactory;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXInstanceKeys;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.MonitorUtil;
import com.ibm.wala.util.WalaException;

import picocli.CommandLine;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.ProgramCounter;
import com.ibm.wala.classLoader.SourceModule;

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
        ClassLoader loader = JSCallGraphBuilderUtil.class.getClassLoader();
        URL script = JSCallGraphBuilderUtil.getURLforFile(inPath.getParent().toString(), inPath.getFileName().toString(), loader);
        JSCallGraphUtil.setTranslatorFactory(new CAstRhinoTranslatorFactory());
        CAstRewriterFactory<?, ?> preprocessor =
		        clo.enableCorrelationTracking
		            ? new CorrelatedPairExtractorFactory(JSCallGraphBuilderUtil.translatorFactory, script)
		            : null;
		JavaScriptLoaderFactory loaders = JSCallGraphBuilderUtil.makeLoaders(preprocessor);
		AnalysisScope scope = JSCallGraphBuilderUtil.makeScriptScope(inPath.getParent().toString(), inPath.getFileName().toString(), loaders, loader);
		IRFactory<IMethod> irFactory = AstIRFactory.makeDefaultFactory();
		IClassHierarchy cha = JSCallGraphBuilderUtil.makeHierarchy(scope, loaders);
		Iterable<Entrypoint> e = JSCallGraphBuilderUtil.makeScriptRoots(cha);
		JSAnalysisOptions options = JSCallGraphBuilderUtil.makeOptions(scope, cha, e);
		IAnalysisCacheView cache = JSCallGraphBuilderUtil.makeCache(irFactory);
		//Set options
	    options.setHandleCallApply(clo.handleCallApply);
		options.setMaxNumberOfNodes(clo.maxNumberOfNodes);
		options.setUseConstantSpecificKeys(clo.useConstantSpecificKeys);
		// The below options do not seem to have an effect on JavaScript analysis:
		//options.setReflectionOptions(AnalysisOptions.ReflectionOptions.NONE);
		//options.setHandleStaticInit(clo.handleStaticInit);
		//options.setHandleZeroLengthArray(clo.handleZeroLengthArray);
		//options.setUseStacksForLexicalScoping(false);
		//options.setUseLexicalScopingForGlobals(true);
		//options.setTraceStringConstants(false);
		//options.setMaxEvalBetweenTopo(0);
		//options.setMinEquationsForTopSort(100);
		//options.setUseLoadFileTargetSelector(false);
		
	    JSCFABuilder builder;
	    switch(clo.callGraphBuilder) {
			case ZEROONE_CFA:
				builder = new JSZeroOrOneXCFABuilder(cha, options, cache, null, null, ZeroXInstanceKeys.ALLOCATIONS, false);
				break;
			case ONE_CFA:
				builder = new JSZeroOrOneXCFABuilder(cha, options, cache, null, null, ZeroXInstanceKeys.ALLOCATIONS, true);
				break;
			default:
				throw new IllegalArgumentException("Invalid call graph algorithm.");
	    }
	    if (clo.enableCorrelationTracking)
	        builder.setContextSelector(
	            new PropertyNameContextSelector(
	                builder.getAnalysisCache(), 2, builder.getContextSelector()));
		
		com.ibm.wala.cast.util.Util.checkForFrontEndErrors(cha);
		
		/* Old code for call graph builder generation, saving for testing purposes
		 * TODO: Discover why new call graph builder gives different results than the one below
		 *  --- CHECK WHICH OPTIONS ARE DEFAULT IN OTHER DRIVERS*/
		/*if (clo.callGraphBuilder.toString().equals("ZEROONE_CFA")) {
			if (clo.handleCallApply) {
				if(clo.enableCorrelationTracking) {
					//builder = new JSZeroOrOneXCFABuilder(cha, options, cache, null, null, ZeroXInstanceKeys.ALLOCATIONS, false);
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
		}*/
		
		final long startTime = System.currentTimeMillis();
		
		MonitorUtil.IProgressMonitor pm = new MonitorUtil.IProgressMonitor() {
            private boolean cancelled;

            public void beginTask(String s, int i) {

            }

            public void subTask(String s) {

            }

            public void cancel() {
                cancelled = true;
            }

            public boolean isCanceled() {
                if (System.currentTimeMillis() - startTime > clo.timeout) {
                    cancelled = true;
                }
                return cancelled;
            }

            public void done() {

            }

            public void worked(int i) {

            }

            public String getCancelMessage() {
                return "Timed out.";
            }
        };
		
		//Build and save call graph
		CallGraph CG = builder.makeCallGraph(options, pm);
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