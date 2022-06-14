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
import com.ibm.wala.cast.loader.SingleClassLoaderFactory;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.CallGraphStats;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.Language;
import com.ibm.wala.classLoader.ProgramCounter;

public class JSCallGraphDriver {

	/**
	 * Usage: JSCallGraphDriver path_to_js_file path_to_output_txt_file
	 * @param args
	 * @throws WalaException 
	 * @throws CancelException 
	 * @throws IOException 
	 * @throws IllegalArgumentException 
	 * 
	 */
	public static void main(String[] args) throws IllegalArgumentException, IOException, CancelException, WalaException {
		// Sets up JS Call Graph Builder
		Path inPath = Paths.get(args[0]);
		JSCallGraphUtil.setTranslatorFactory(new CAstRhinoTranslatorFactory());
		JavaScriptLoaderFactory loaders = JSCallGraphBuilderUtil.makeLoaders();
		AnalysisScope scope = JSCallGraphBuilderUtil.makeScope(JSCallGraphBuilderUtil.makeSourceModules(inPath.getParent().toString(), inPath.getFileName().toString(), JSCallGraphBuilderUtil.class.getClassLoader()), loaders, JavaScriptLoader.JS);
		IClassHierarchy cha = JSCallGraphBuilderUtil.makeHierarchy(scope, loaders);
		Iterable<Entrypoint> e = JSCallGraphBuilderUtil.makeScriptRoots(cha);
		//Configures Options
		JSAnalysisOptions options = new JSAnalysisOptions(scope, e);
		//In progress: option integration, ex. options.setReflectionOptions(null);
		
		JSCFABuilder builder = JSCallGraphBuilderUtil.makeScriptCGBuilder(inPath.getParent().toString(), inPath.getFileName().toString());
		//Builds Call Graph
		CallGraph CG = builder.makeCallGraph(options);
		System.out.println(CallGraphStats.getStats(CG));
		FileWriter fw = new FileWriter(args[1]);
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
        
        System.out.println("Wrote callgraph to " + args[1]);
        fw.close();
	}

}

/**
 * < Application, Lcfne/Demo, main([Ljava/lang/String;)V >	invokestatic < Application, Ljava/lang/Class, forName(Ljava/lang/String;)Ljava/lang/Class; >@2	Everywhere	java.lang.Class.forName(Ljava/lang/String;)Ljava/lang/Class;	Everywhere
<Code body of function Lprologue.js>	JSCall@186	Everywhere	Function.ctor()LRoot;	CallStringContext: [ prologue.js.do()LRoot;@186 ]
 * **/