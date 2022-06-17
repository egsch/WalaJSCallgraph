import com.ibm.wala.ipa.callgraph.AnalysisOptions.ReflectionOptions;
import java.nio.file.Path;

import picocli.CommandLine.Option;

public class CommandLineOptions {
    @Option(names = "--scripts", required = true, description = "The path to the script to analyze.")
    public String scriptPath;

    @Option(
            names = {"--cgoutput", "-o"},
            required = true,
            description = "Where to write the callgraph.")
    public Path callgraphOutput;

    @Option(
            names = "--reflectionSetting",
            description = "Valid values: ${COMPLETION-CANDIDATES}",
            defaultValue = "NONE")
    public ReflectionOptions reflection;

    // TODO: Rework config to allow direct setting of reflection options.
  /*  @Option(names="--numFlowToCastIterations", description="How many times should flows from "
      + "newInstance() calls to casts be analyzed?")
  public int numFlowToCastIterations;

  @Option(names="--ignoreMethodInoke", description="should calls to Method.invoke() be ignored?")
  public boolean ignoreMethodInvoke;

  @Option(names="--applicationClassesOnly", description = "should calls to Class.getMethod() "
      + "be modeled only for application classes?")
  public boolean applicationClassesOnly;

  @Option(names="--ignoreStringConstants", description = "should calls to reflective methods "
      + "with string constant arguments be ignored?")
  public boolean ignoreStringConstants;*/

    @Option(names = {"-h", "--help"}, usageHelp = true, description = "display this help message")
    boolean usageHelpRequested;

    @Option(
            names= {"--timeout", "-t"},
            description ="Timeout in milliseconds.",
            defaultValue = "7200000"
    )
    public long timeout;

    @Option(
            names = "--no-source-numbers",
            description = "Print out IR instead of source numbers."
    )
    public boolean noSourceNumbers;
    @Option(
            names = "--handleStaticInit",
            description =
                    "Should call graph construction handle "
                            + "possible invocations of static initializer methods?")
    public boolean handleStaticInit;
    
    // TODO: add actual description below
    @Option(
            names = "--handleCallApply",
            defaultValue = "true",
            description =
                    "Should call graph construction handle "
                            + "call apply? Does not affect ONE_CFA algorithm.")
    public boolean handleCallApply;

    @Option(
            names = "--enableCorrelationTracking",
            defaultValue = "true",
            description =
                    "Should call graph construction include "
                            + "correlation tracking? Does not affect ONE_CFA algorithm.")
    public boolean enableCorrelationTracking;
    
    @Option(
            names = "--useConstantSpecificKeys",
            description = "Use distinct instance keys for " + "distinct string constants?")
    public boolean useConstantSpecificKeys;

    @Option(
            names = "--useStacksForLexcialScoping",
            description = "Should analysis of lexical " + "scoping consider call stacks?")
    public boolean useStacksForLexicalScoping;

    @Option(
            names = "--useLexicalScopingForGlobals",
            description =
                    "Should global variables be " + "considered lexically-scoped from the root node?")
    public boolean useLexicalScopingForGlobals;

    @Option(
            names = "--maxNumberOfNodes",
            description =
                    "Maximum number of nodes that the "
                            + "callgraph is allowed to have. -1 means no restrictions.",
            defaultValue = "-1")
    public int maxNumberOfNodes;

    @Option(
            names = "--handleZeroLengthArray",
            description = "Should call graph construction handle " + "arrays of zero-length differently?")
    public boolean handleZeroLengthArray;

    @Option(
            names = "--cgalgo",
            description = "Valid values: ${COMPLETION-CANDIDATES}",
            defaultValue = "ZEROONE_CFA")
    public CallGraphBuilders callGraphBuilder;
}