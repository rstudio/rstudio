/*
 * Copyright 2008 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.google.gwt.dev.jjs.impl;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.cfg.ConfigurationProperty;
import com.google.gwt.dev.cfg.Properties;
import com.google.gwt.dev.cfg.Property;
import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JArrayType;
import com.google.gwt.dev.jjs.ast.JClassLiteral;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JDeclaredType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JIntLiteral;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JNewArray;
import com.google.gwt.dev.jjs.ast.JNode;
import com.google.gwt.dev.jjs.ast.JPrimitiveType;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JRunAsync;
import com.google.gwt.dev.jjs.ast.JStringLiteral;
import com.google.gwt.dev.jjs.ast.JVisitor;
import com.google.gwt.dev.jjs.impl.ControlFlowAnalyzer.DependencyRecorder;
import com.google.gwt.dev.jjs.impl.FragmentExtractor.CfaLivenessPredicate;
import com.google.gwt.dev.jjs.impl.FragmentExtractor.LivenessPredicate;
import com.google.gwt.dev.jjs.impl.FragmentExtractor.NothingAlivePredicate;
import com.google.gwt.dev.jjs.impl.FragmentExtractor.StatementLogger;
import com.google.gwt.dev.js.ast.JsBlock;
import com.google.gwt.dev.js.ast.JsExprStmt;
import com.google.gwt.dev.js.ast.JsExpression;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsStatement;
import com.google.gwt.dev.js.ast.JsVars;
import com.google.gwt.dev.js.ast.JsVars.JsVar;
import com.google.gwt.dev.util.JsniRef;
import com.google.gwt.dev.util.collect.HashMap;
import com.google.gwt.dev.util.collect.HashSet;
import com.google.gwt.dev.util.collect.Lists;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

/**
 * <p>
 * Divides the code in a {@link JsProgram} into multiple fragments. The initial
 * fragment is sufficient to run all of the program's functionality except for
 * anything called in a callback supplied to
 * {@link com.google.gwt.core.client.GWT#runAsync(com.google.gwt.core.client.RunAsyncCallback)
 * GWT.runAsync()}. The remaining code should be downloadable via
 * {@link com.google.gwt.core.client.impl.AsyncFragmentLoader#inject(int)}.
 * </p>
 * 
 * <p>
 * The precise way the program is fragmented is an implementation detail that is
 * subject to change. Whenever the fragment strategy changes,
 * <code>AsyncFragmentLoader</code> must be updated in tandem. That said, the
 * current fragmentation strategy is to create an initial fragment, a leftovers
 * fragment, and one fragment per split point. Additionally, the splitter
 * computes an initial load sequence. All runAsync calls in the initial load
 * sequence are reached before any call not in the sequence. Further, any call
 * in the sequence is reached before any call later in the sequence.
 * </p>
 * 
 * <p>
 * The fragment for a split point contains different things depending on whether
 * it is in the initial load sequence or not. If it's in the initial load
 * sequence, then the fragment includes the code newly live once that split
 * point is crossed, that wasn't already live for the set of split points
 * earlier in the sequence. For a split point not in the initial load sequence,
 * the fragment contains only code exclusive to that split point, that is, code
 * that cannot be reached except via that split point. All other code goes into
 * the leftovers fragment.
 * </p>
 */
public class CodeSplitter {
  // TODO(rluble): This class needs a serious refactor to be able to add significant unit tests.

  /**
   * A dependency recorder that can record multiple dependency graphs. It has
   * methods for starting and finishing new dependency graphs.
   */
  public interface MultipleDependencyGraphRecorder extends DependencyRecorder {
    /**
     * Stop recording dependencies.
     */
    void close();

    /**
     * Stop recording the current dependency graph.
     */
    void endDependencyGraph();

    void open();

    /**
     * Start a new dependency graph. It can be an extension of a previously
     * recorded dependency graph, in which case the dependencies in the previous
     * graph will not be repeated.
     */
    void startDependencyGraph(String name, String extnds);
  }

  /**
   * A statement logger that immediately prints out everything live that it
   * sees.
   */
  private class EchoStatementLogger implements StatementLogger {
    public void logStatement(JsStatement stat, boolean isIncluded) {
      if (!isIncluded) {
        return;
      }
      if (stat instanceof JsExprStmt) {
        JsExpression expr = ((JsExprStmt) stat).getExpression();
        if (!(expr instanceof JsFunction)) {
          return;
        }
        JsFunction func = (JsFunction) expr;
        if (func.getName() == null) {
          return;
        }
        JMethod method = map.nameToMethod(func.getName());
        if (method == null) {
          return;
        }
        System.out.println(fullNameString(method));

      } else if (stat instanceof JsVars) {
        JsVars vars = (JsVars) stat;
        for (JsVar var : vars) {
          JField field = map.nameToField(var.getName());
          if (field != null) {
            System.out.println(fullNameString(field));
          }
        }
      }
    }
  }

  /**
   * A liveness predicate that is based on an exclusivity map.
   */
  private static class ExclusivityMapLivenessPredicate implements LivenessPredicate {
    private final int fragment;
    private final ExclusivityMap fragmentMap;

    public ExclusivityMapLivenessPredicate(ExclusivityMap fragmentMap, int fragment) {
      this.fragmentMap = fragmentMap;
      this.fragment = fragment;
    }

    public boolean isLive(JDeclaredType type) {
      return fragmentMap.isLiveInFragment(fragment, type);
    }

    public boolean isLive(JField field) {
      return fragmentMap.isLiveInFragment(fragment, field);
    }

    public boolean isLive(JMethod method) {
      return fragmentMap.isLiveInFragment(fragment, method);
    }

    public boolean isLive(String literal) {
      return fragmentMap.isLiveInFragment(fragment, literal);
    }

    public boolean miscellaneousStatementsAreLive() {
      return true;
    }
  }

  /**
   * A {@link MultipleDependencyGraphRecorder} that does nothing.
   */
  public static final MultipleDependencyGraphRecorder NULL_RECORDER =
      new MultipleDependencyGraphRecorder() {
        public void close() {
        }

        public void endDependencyGraph() {
        }

        public void methodIsLiveBecause(JMethod liveMethod, ArrayList<JMethod> dependencyChain) {
        }

        public void open() {
        }

        public void startDependencyGraph(String name, String extnds) {
        }
      };

  private static final String PROP_INITIAL_SEQUENCE = "compiler.splitpoint.initial.sequence";

  /**
   * A Java property that causes the fragment map to be logged.
   */
  private static String PROP_LOG_FRAGMENT_MAP = "gwt.jjs.logFragmentMap";

  public static ControlFlowAnalyzer computeInitiallyLive(JProgram jprogram) {
    return computeInitiallyLive(jprogram, NULL_RECORDER);
  }

  public static void exec(TreeLogger logger, JProgram jprogram, JsProgram jsprogram,
      JavaToJavaScriptMap map, MultipleDependencyGraphRecorder dependencyRecorder) {
    if (jprogram.getRunAsyncs().size() == 0) {
      // Don't do anything if there is no call to runAsync
      return;
    }
    Event codeSplitterEvent = SpeedTracerLogger.start(CompilerEventType.CODE_SPLITTER);
    dependencyRecorder.open();
    new CodeSplitter(logger, jprogram, jsprogram, map, dependencyRecorder).execImpl();
    dependencyRecorder.close();
    codeSplitterEvent.end();
  }

  /**
   * Find a split point as designated in the {@link #PROP_INITIAL_SEQUENCE}
   * configuration property.
   */
  public static int findSplitPoint(String refString, JProgram program, TreeLogger branch)
      throws UnableToCompleteException {
    Event codeSplitterEvent =
        SpeedTracerLogger.start(CompilerEventType.CODE_SPLITTER, "phase", "findSplitPoint");
    Map<String, List<Integer>> nameToSplitPoint = reverseByName(program.getRunAsyncs());

    if (refString.startsWith("@")) {
      JsniRef jsniRef = JsniRef.parse(refString);
      if (jsniRef == null) {
        branch.log(TreeLogger.ERROR, "Badly formatted JSNI reference in " + PROP_INITIAL_SEQUENCE
            + ": " + refString);
        throw new UnableToCompleteException();
      }
      final String lookupErrorHolder[] = new String[1];
      JNode referent =
          JsniRefLookup.findJsniRefTarget(jsniRef, program, new JsniRefLookup.ErrorReporter() {
            public void reportError(String error) {
              lookupErrorHolder[0] = error;
            }
          });
      if (referent == null) {
        TreeLogger resolveLogger =
            branch.branch(TreeLogger.ERROR, "Could not resolve JSNI reference: " + jsniRef);
        resolveLogger.log(TreeLogger.ERROR, lookupErrorHolder[0]);
        throw new UnableToCompleteException();
      }

      if (!(referent instanceof JMethod)) {
        branch.log(TreeLogger.ERROR, "Not a method: " + referent);
        throw new UnableToCompleteException();
      }

      JMethod method = (JMethod) referent;
      String canonicalName = ReplaceRunAsyncs.getImplicitName(method);
      List<Integer> splitPoints = nameToSplitPoint.get(canonicalName);
      if (splitPoints == null) {
        branch.log(TreeLogger.ERROR, "Method does not enclose a runAsync call: " + jsniRef);
        throw new UnableToCompleteException();
      }
      if (splitPoints.size() > 1) {
        branch.log(TreeLogger.ERROR, "Method includes multiple runAsync calls, "
            + "so it's ambiguous which one is meant: " + jsniRef);
        throw new UnableToCompleteException();
      }

      return splitPoints.get(0);
    }

    // Assume it's a raw class name
    List<Integer> splitPoints = nameToSplitPoint.get(refString);
    if (splitPoints == null || splitPoints.size() == 0) {
      branch.log(TreeLogger.ERROR, "No runAsync call is labelled with class " + refString);
      throw new UnableToCompleteException();
    }
    if (splitPoints.size() > 1) {
      branch.log(TreeLogger.ERROR, "More than one runAsync call is labelled with class "
          + refString);
      throw new UnableToCompleteException();
    }
    int result = splitPoints.get(0);
    codeSplitterEvent.end();
    return result;
  }

  public static int getExclusiveFragmentNumber(int splitPoint) {
    return splitPoint;
  }

  public static int getLeftoversFragmentNumber(int numSplitPoints) {
    return numSplitPoints + 1;
  }

  /**
   * Infer the number of split points for a given number of code fragments.
   */
  public static int numSplitPointsForFragments(int codeFragments) {
    assert (codeFragments != 2);

    if (codeFragments == 1) {
      return 0;
    }

    return codeFragments - 2;
  }

  /**
   * Choose an initial load sequence of split points for the specified program.
   * Do so by identifying split points whose code always load first, before any
   * other split points. As a side effect, modifies
   * {@link com.google.gwt.core.client.impl.AsyncFragmentLoader#initialLoadSequence}
   * in the program being compiled.
   * 
   * @throws UnableToCompleteException If the module specifies a bad load order
   */
  public static void pickInitialLoadSequence(TreeLogger logger, JProgram program,
      Properties properties) throws UnableToCompleteException {
    Event codeSplitterEvent =
        SpeedTracerLogger
            .start(CompilerEventType.CODE_SPLITTER, "phase", "pickInitialLoadSequence");
    TreeLogger branch =
        logger.branch(TreeLogger.TRACE, "Looking up initial load sequence for split points");
    LinkedHashSet<Integer> initialLoadSequence = new LinkedHashSet<Integer>();

    ConfigurationProperty prop;
    {
      Property p = properties.find(PROP_INITIAL_SEQUENCE);
      if (p == null) {
        throw new InternalCompilerException("Could not find configuration property "
            + PROP_INITIAL_SEQUENCE);
      }
      if (!(p instanceof ConfigurationProperty)) {
        throw new InternalCompilerException(PROP_INITIAL_SEQUENCE
            + " is not a configuration property");
      }
      prop = (ConfigurationProperty) p;
    }

    for (String refString : prop.getValues()) {
      int splitPoint = findSplitPoint(refString, program, branch);
      if (initialLoadSequence.contains(splitPoint)) {
        branch.log(TreeLogger.ERROR, "Split point specified more than once: " + refString);
      }
      initialLoadSequence.add(splitPoint);
    }

    logInitialLoadSequence(logger, initialLoadSequence);
    installInitialLoadSequenceField(program, initialLoadSequence);
    program.setSplitPointInitialSequence(new ArrayList<Integer>(initialLoadSequence));
    codeSplitterEvent.end();
  }

  /**
   * <p>
   * Computes the "maximum total script size" for one permutation. The total
   * script size for one sequence of split points reached is the sum of the
   * scripts that are downloaded for that sequence. The maximum total script
   * size is the maximum such size for all possible sequences of split points.
   * </p>
   * 
   * @param jsLengths The lengths of the fragments for the compilation of one
   *          permutation
   */
  public static int totalScriptSize(int[] jsLengths) {
    /*
     * The total script size is currently simple: it's the sum of all the
     * individual script files.
     */

    int maxTotalSize;
    int numSplitPoints = numSplitPointsForFragments(jsLengths.length);
    if (numSplitPoints == 0) {
      maxTotalSize = jsLengths[0];
    } else {
      // Add up the initial and exclusive fragments
      maxTotalSize = jsLengths[0];
      for (int sp = 1; sp <= numSplitPoints; sp++) {
        int excl = getExclusiveFragmentNumber(sp);
        maxTotalSize += jsLengths[excl];
      }

      // Add the leftovers
      maxTotalSize += jsLengths[getLeftoversFragmentNumber(numSplitPoints)];
    }
    return maxTotalSize;
  }

  /**
   * Compute the set of initially live code for this program. Such code must be
   * included in the initial download of the program.
   */
  private static ControlFlowAnalyzer computeInitiallyLive(JProgram jprogram,
      MultipleDependencyGraphRecorder dependencyRecorder) {
    dependencyRecorder.startDependencyGraph("initial", null);

    ControlFlowAnalyzer cfa = new ControlFlowAnalyzer(jprogram);
    cfa.setDependencyRecorder(dependencyRecorder);
    cfa.traverseEntryMethods();
    traverseClassArray(jprogram, cfa);
    traverseImmortalTypes(jprogram, cfa);
    dependencyRecorder.endDependencyGraph();
    return cfa;
  }

  /**
   * Extract the types from a set that happen to be declared types.
   */
  private static Set<JDeclaredType> declaredTypesIn(Set<JReferenceType> types) {
    Set<JDeclaredType> result = new HashSet<JDeclaredType>();
    for (JReferenceType type : types) {
      if (type instanceof JDeclaredType) {
        result.add((JDeclaredType) type);
      }
    }
    return result;
  }

  private static String fullNameString(JField field) {
    return field.getEnclosingType().getName() + "." + field.getName();
  }

  private static String fullNameString(JMethod method) {
    return method.getEnclosingType().getName() + "." + JProgram.getJsniSig(method);
  }

  /**
   * Installs the initial load sequence into AsyncFragmentLoader.BROWSER_LOADER.
   * The initializer looks like this:
   * 
   * <pre>
   * AsyncFragmentLoader BROWSER_LOADER = makeBrowserLoader(1, new int[]{});
   * </pre>
   * 
   * The second argument (<code>new int[]</code>) gets replaced by an array
   * corresponding to <code>initialLoadSequence</code>.
   */
  private static void installInitialLoadSequenceField(JProgram program,
      LinkedHashSet<Integer> initialLoadSequence) {
    // Arg 1 is initialized in the source as "new int[]{}".
    JMethodCall call = ReplaceRunAsyncs.getBrowserLoaderConstructor(program);
    JExpression arg1 = call.getArgs().get(1);
    assert arg1 instanceof JNewArray;
    JArrayType arrayType = program.getTypeArray(JPrimitiveType.INT);
    assert ((JNewArray) arg1).getArrayType() == arrayType;
    List<JExpression> initializers = new ArrayList<JExpression>(initialLoadSequence.size());
    for (int sp : initialLoadSequence) {
      initializers.add(JIntLiteral.get(sp));
    }
    JNewArray newArray =
        JNewArray.createInitializers(arg1.getSourceInfo(), arrayType, Lists
            .normalizeUnmodifiable(initializers));
    call.setArg(1, newArray);
  }

  private static <T> T last(T[] array) {
    return array[array.length - 1];
  }

  private static void logInitialLoadSequence(TreeLogger logger,
      LinkedHashSet<Integer> initialLoadSequence) {
    if (!logger.isLoggable(TreeLogger.TRACE)) {
      return;
    }

    StringBuffer message = new StringBuffer();
    message.append("Initial load sequence of split points: ");
    if (initialLoadSequence.isEmpty()) {
      message.append("(none)");
    } else {
      boolean first = true;
      for (int sp : initialLoadSequence) {
        if (first) {
          first = false;
        } else {
          message.append(", ");
        }
        message.append(sp);
      }
    }

    logger.log(TreeLogger.TRACE, message.toString());
  }

  private static Map<String, List<Integer>> reverseByName(List<JRunAsync> runAsyncs) {
    Map<String, List<Integer>> revmap = new HashMap<String, List<Integer>>();
    for (JRunAsync replacement : runAsyncs) {
      String name = replacement.getName();
      if (name != null) {
        List<Integer> list = revmap.get(name);
        if (list == null) {
          list = new ArrayList<Integer>();
          revmap.put(name, list);
        }
        list.add(replacement.getSplitPoint());
      }
    }
    return revmap;
  }

  /**
   * Any instance method in the magic Array class must be in the initial
   * download. The methods of that class are copied to a separate object the
   * first time class Array is touched, and any methods added later won't be
   * part of the copy.
   */
  private static void traverseClassArray(JProgram jprogram, ControlFlowAnalyzer cfa) {
    JDeclaredType typeArray = jprogram.getFromTypeMap("com.google.gwt.lang.Array");
    if (typeArray == null) {
      // It was pruned; nothing to do
      return;
    }

    cfa.traverseFromInstantiationOf(typeArray);
    for (JMethod method : typeArray.getMethods()) {
      if (method.needsVtable()) {
        cfa.traverseFrom(method);
      }
    }
  }

  /**
   * Any immortal codegen types must be part of the initial download.
   */
  private static void traverseImmortalTypes(JProgram jprogram,
      ControlFlowAnalyzer cfa) {
    for (JClassType type : jprogram.immortalCodeGenTypes) {
      cfa.traverseFromInstantiationOf(type);
      for (JMethod method : type.getMethods()) {
        if (!method.needsVtable()) {
          cfa.traverseFrom(method);
        }
      }
    }
  }

  private static <T> Set<T> union(Set<? extends T> set1, Set<? extends T> set2) {
    Set<T> union = new HashSet<T>();
    union.addAll(set1);
    union.addAll(set2);
    return union;
  }

  private final MultipleDependencyGraphRecorder dependencyRecorder;
  private final FragmentExtractor fragmentExtractor;
  private final LinkedHashSet<Integer> initialLoadSequence;

  /**
   * Code that is initially live when the program first downloads.
   */
  private final ControlFlowAnalyzer initiallyLive;
  private final JProgram jprogram;
  private final JsProgram jsprogram;

  /**
   * Computed during {@link #execImpl()}, so that intermediate steps of it can
   * be used as they are created.
   */
  private ControlFlowAnalyzer liveAfterInitialSequence;
  private final TreeLogger logger;
  private final boolean logging;
  private final JavaToJavaScriptMap map;
  private final Set<JMethod> methodsInJavaScript;
  private final int numEntries;

  private CodeSplitter(TreeLogger logger, JProgram jprogram, JsProgram jsprogram,
      JavaToJavaScriptMap map, MultipleDependencyGraphRecorder dependencyRecorder) {
    this.logger = logger.branch(TreeLogger.TRACE, "Splitting JavaScript for incremental download");
    this.jprogram = jprogram;
    this.jsprogram = jsprogram;
    this.map = map;
    this.dependencyRecorder = dependencyRecorder;
    this.initialLoadSequence = new LinkedHashSet<Integer>(jprogram.getSplitPointInitialSequence());

    numEntries = jprogram.getRunAsyncs().size() + 1;
    logging = Boolean.getBoolean(PROP_LOG_FRAGMENT_MAP);
    fragmentExtractor = new FragmentExtractor(jprogram, jsprogram, map);

    initiallyLive = computeInitiallyLive(jprogram, dependencyRecorder);

    methodsInJavaScript = fragmentExtractor.findAllMethodsInJavaScript();
  }

  /**
   * Create a new fragment and add it to the table of fragments.
   * 
   * @param splitPoint The split point to associate this code with
   * @param alreadyLoaded The code that should be assumed to have already been
   *          loaded
   * @param liveNow The code that is assumed live once this fragment loads;
   *          anything in here but not in <code>alreadyLoaded</code> will be
   *          included in the created fragment
   * @param stmtsToAppend Additional statements to append to the end of the new
   *          fragment
   * @param fragmentStats The list of fragments to append to
   */
  private void addFragment(int splitPoint, LivenessPredicate alreadyLoaded,
      LivenessPredicate liveNow, List<JsStatement> stmtsToAppend,
      Map<Integer, List<JsStatement>> fragmentStats) {
    if (logging) {
      System.out.println();
      System.out.println("==== Fragment " + fragmentStats.size() + " ====");
      fragmentExtractor.setStatementLogger(new EchoStatementLogger());
    }
    List<JsStatement> stats = fragmentExtractor.extractStatements(liveNow, alreadyLoaded);
    stats.addAll(stmtsToAppend);
    fragmentStats.put(splitPoint, stats);
  }

  /**
   * For each split point other than those in the initial load sequence, compute
   * a CFA that traces every other split point. For those that are in the
   * initial load sequence, add a <code>null</code> to the list.
   */
  private List<ControlFlowAnalyzer> computeAllButOneCfas() {
    String dependencyGraphNameAfterInitialSequence = dependencyGraphNameAfterInitialSequence();

    List<ControlFlowAnalyzer> allButOnes = new ArrayList<ControlFlowAnalyzer>();
    for (JRunAsync runAsync : jprogram.getRunAsyncs()) {
      int splitPoint = runAsync.getSplitPoint();
      if (isInitial(splitPoint)) {
        allButOnes.add(null);
        continue;
      }
      dependencyRecorder.startDependencyGraph("sp" + splitPoint,
          dependencyGraphNameAfterInitialSequence);
      ControlFlowAnalyzer cfa = new ControlFlowAnalyzer(liveAfterInitialSequence);
      cfa.setDependencyRecorder(dependencyRecorder);
      for (JRunAsync otherRunAsync : jprogram.getRunAsyncs()) {
        if (isInitial(otherRunAsync.getSplitPoint())) {
          continue;
        }
        if (otherRunAsync == runAsync) {
          continue;
        }
        cfa.traverseFromRunAsync(otherRunAsync);
      }
      dependencyRecorder.endDependencyGraph();
      allButOnes.add(cfa);
    }

    return allButOnes;
  }

  /**
   * Compute a CFA that covers the entire live code of the program.
   */
  private ControlFlowAnalyzer computeCompleteCfa() {
    dependencyRecorder.startDependencyGraph("total", null);
    ControlFlowAnalyzer everything = new ControlFlowAnalyzer(jprogram);
    everything.setDependencyRecorder(dependencyRecorder);
    everything.traverseEverything();
    dependencyRecorder.endDependencyGraph();
    return everything;
  }

  /**
   * The name of the dependency graph that corresponds to
   * {@link #liveAfterInitialSequence}.
   */
  private String dependencyGraphNameAfterInitialSequence() {
    if (initialLoadSequence.isEmpty()) {
      return "initial";
    } else {
      return "sp" + last(initialLoadSequence.toArray());
    }
  }

  /**
   * Map each program atom as exclusive to some split point, whenever possible.
   * Also fixes up load order problems that could result from splitting code
   * based on this assumption.
   */
  private ExclusivityMap determineExclusivity() {
    ExclusivityMap exclusivityMap = new ExclusivityMap();

    mapExclusiveAtoms(exclusivityMap);
    exclusivityMap.fixUpLoadOrderDependencies(logger, jprogram, methodsInJavaScript);

    return exclusivityMap;
  }

  private void execImpl() {
    Map<Integer, List<JsStatement>> fragmentStats = new HashMap<Integer, List<JsStatement>>();

    {
      /*
       * Compute the base fragment. It includes everything that is live when the
       * program starts.
       */
      LivenessPredicate alreadyLoaded = new NothingAlivePredicate();
      LivenessPredicate liveNow = new CfaLivenessPredicate(initiallyLive);
      List<JsStatement> noStats = new ArrayList<JsStatement>();
      addFragment(0, alreadyLoaded, liveNow, noStats, fragmentStats);
    }

    /*
     * Compute the base fragments, for split points in the initial load
     * sequence.
     */
    liveAfterInitialSequence = new ControlFlowAnalyzer(initiallyLive);
    String extendsCfa = "initial";
    for (int sp : initialLoadSequence) {
      LivenessPredicate alreadyLoaded = new CfaLivenessPredicate(liveAfterInitialSequence);
      String depGraphName = "sp" + sp;
      dependencyRecorder.startDependencyGraph(depGraphName, extendsCfa);
      extendsCfa = depGraphName;

      ControlFlowAnalyzer liveAfterSp = new ControlFlowAnalyzer(liveAfterInitialSequence);
      JRunAsync runAsync = jprogram.getRunAsyncs().get(sp - 1);
      assert runAsync.getSplitPoint() == sp;
      liveAfterSp.traverseFromRunAsync(runAsync);
      dependencyRecorder.endDependencyGraph();

      LivenessPredicate liveNow = new CfaLivenessPredicate(liveAfterSp);

      List<JsStatement> statsToAppend = fragmentExtractor.createOnLoadedCall(sp);

      addFragment(sp, alreadyLoaded, liveNow, statsToAppend, fragmentStats);

      liveAfterInitialSequence = liveAfterSp;
    }

    ExclusivityMap fragmentMap = determineExclusivity();

    /*
     * Compute the exclusively live fragments. Each includes everything
     * exclusively live after entry point i.
     */
    for (JRunAsync runAsync : jprogram.getRunAsyncs()) {
      int i = runAsync.getSplitPoint();
      if (isInitial(i)) {
        continue;
      }
      LivenessPredicate alreadyLoaded = new ExclusivityMapLivenessPredicate(fragmentMap,
          ExclusivityMap.NOT_EXCLUSIVE);
      LivenessPredicate liveNow = new ExclusivityMapLivenessPredicate(fragmentMap, i);
      List<JsStatement> statsToAppend = fragmentExtractor.createOnLoadedCall(i);
      addFragment(i, alreadyLoaded, liveNow, statsToAppend, fragmentStats);
    }

    /*
     * Compute the leftovers fragment.
     */
    {
      LivenessPredicate alreadyLoaded = new CfaLivenessPredicate(liveAfterInitialSequence);
      LivenessPredicate liveNow = new ExclusivityMapLivenessPredicate(fragmentMap, 0);
      List<JsStatement> statsToAppend = fragmentExtractor.createOnLoadedCall(numEntries);
      addFragment(numEntries, alreadyLoaded, liveNow, statsToAppend, fragmentStats);
    }

    // now install the new statements in the program fragments
    jsprogram.setFragmentCount(fragmentStats.size());
    for (int i = 0; i < fragmentStats.size(); i++) {
      JsBlock fragBlock = jsprogram.getFragmentBlock(i);
      fragBlock.getStatements().clear();
      fragBlock.getStatements().addAll(fragmentStats.get(i));
    }
  }

  /**
   * A map from program atoms to the split point, if any, that they are
   * exclusive to. Atoms not exclusive to any split point are either mapped to 0
   * or left out of the map entirely. Note that the map is incomplete; any entry
   * not included has not been proven to be exclusive. Also, note that the
   * initial load sequence is assumed to already be loaded.
   */
  private static class ExclusivityMap {

    public static final int NOT_EXCLUSIVE = 0;

    /**
     * Determine whether a field is live in a fragment.
     */
    public boolean isLiveInFragment(int fragment, JField field) {
      return isLiveInFragment(fragmentForField, field, fragment);
    }

    /**
     * Determine whether a method is live in a fragment.
     */
    public boolean isLiveInFragment(int fragment, JMethod method) {
      return isLiveInFragment(fragmentForMethod, method, fragment);
    }

    /**
     * Determine whether a string is live in a fragment.
     */
    public boolean isLiveInFragment(int fragment, String string) {
      return isLiveInFragment(fragmentForString, string, fragment);
    }

    /**
     * Determine whether a type is live in a fragment.
     */
    public boolean isLiveInFragment(int fragment, JDeclaredType type) {
      return isLiveInFragment(fragmentForType, type, fragment);
    }

    /**
     * Update fragment map so that fields that are not in notExclusiveAtoms are assigned to
     * the specified fragment.
     */
    public void updateFields(int fragment, Set<?> notExclusiveAtoms, Iterable<JField> fields) {
      updateMap(fragment, fragmentForField, notExclusiveAtoms, fields);
    }

    /**
     * Update fragment map so that methods that are not in notExclusiveAtoms are assigned to
     * the specified fragment.
     */
    public void updateMethods(int fragment, Set<?> notExclusiveAtoms, Iterable<JMethod> methods) {
      updateMap(fragment, fragmentForMethod, notExclusiveAtoms, methods);
    }

    /**
     * Update fragment map so that strings that are not in notExclusiveAtoms are assigned to
     * the specified fragment.
     */
    public void updateStrings(int fragment, Set<?> notExclusiveAtoms, Iterable<String> strings) {
      updateMap(fragment, fragmentForString, notExclusiveAtoms, strings);
    }

    /**
     * Update fragment map so that types that are not in notExclusiveAtoms are assigned to
     * the specified fragment.
     */
    public void updateTypes(int fragment, Set<?> notExclusiveAtoms, Iterable<JDeclaredType> types) {
      updateMap(fragment, fragmentForType, notExclusiveAtoms, types);
    }

    private Map<JField, Integer> fragmentForField = new HashMap<JField, Integer>();
    private Map<JMethod, Integer> fragmentForMethod = new HashMap<JMethod, Integer>();
    private Map<String, Integer> fragmentForString = new HashMap<String, Integer>();
    private Map<JDeclaredType, Integer> fragmentForType = new HashMap<JDeclaredType, Integer>();

    /**
     * Traverse {@code exp} and find all referenced JFields.
     */
    private static Set<JClassLiteral> classLiteralsIn(JExpression exp) {
      final Set<JClassLiteral> literals = new HashSet<JClassLiteral>();
      class ClassLiteralFinder extends JVisitor {
        @Override
        public void endVisit(JClassLiteral classLiteral, Context ctx) {
          literals.add(classLiteral);
        }
      }
      (new ClassLiteralFinder()).accept(exp);
      return literals;
    }

    /**
     * <p>
     * Patch up the fragment map to satisfy load-order dependencies, as described
     * in the comment of {@link LivenessPredicate}. Load-order dependencies can be
     * violated when an atom is mapped to 0 as a leftover, but it has some
     * load-order dependency on an atom that was put in an exclusive fragment.
     * </p>
     *
     * <p>
     * In general, it might be possible to split things better by considering load
     * order dependencies when building the fragment map. However, fixing them
     * after the fact makes CodeSplitter simpler. In practice, for programs tried
     * so far, there are very few load order dependency fixups that actually
     * happen, so it seems better to keep the compiler simpler.
     * </p>
     *
     * <p>
     * It would be safer and more robust to include the load order dependencies
     * in the general scheme and uniformly use control flow analysis to determine
     * dependencies instead of hand picking atoms to check and fix. Also note that
     * some of the control flow and load dependencies are introduced as the Java
     * AST is translated into JavaScript and hence not visible by ControlFlowAnalyzer.
     * </p>
     *
     * <p>
     * Furthermore, in some cases actual dependencies <i>differ</i> between Java AST and the
     * final JavaScript output. For example whether a field initialization is done at declaration
     * or during instance creation decided by
     * {@link GenerateJavaScriptAST.GenerateJavaScriptVisitor#initializeAtTopScope}. Mismatches
     * like these are handled explicitly by these fixup passes.
     * </p>
     */
    public void fixUpLoadOrderDependencies(TreeLogger logger, JProgram jprogram,
        Set<JMethod> methodsInJavaScript) {
      fixUpLoadOrderDependenciesForMethods(logger, jprogram, methodsInJavaScript);
      fixUpLoadOrderDependenciesForTypes(logger, jprogram);
      fixUpLoadOrderDependenciesForClassLiterals(logger, jprogram);
      fixUpLoadOrderDependenciesForFieldsInitializedToStrings(logger, jprogram);
    }

    /**
     * A class literal cannot be loaded until all the parameters to its createFor... class are.
     * Make sure that the strings are available for all class literals at the time they are
     * loaded and make sure that superclass class literals are loaded before.
     *
     */
    private void fixUpLoadOrderDependenciesForClassLiterals(TreeLogger logger, JProgram jprogram) {
      int numClassLitStrings = 0;
      int numFixups = 0;
      int numClassLiteralFixups = 0;
      /**
       * Consider all static fields of ClassLiteralHolder; the majority if not all its static
       * fields are class literal fields. It is safe to fix up extra fields.
       */
      Queue<JField> potentialClassLiteralFields = new ArrayDeque<JField>(
          jprogram.getTypeClassLiteralHolder().getFields());
      int numClassLiterals = potentialClassLiteralFields.size();

      while (!potentialClassLiteralFields.isEmpty()) {
        JField field = potentialClassLiteralFields.remove();
        if (!field.isStatic()) {
          continue;
        }

        int classLiteralFragment = getFragment(fragmentForField, field);
        JExpression initializer = field.getInitializer();

        // Fixup the string literals.
        for (String string : stringsIn(initializer)) {
          numClassLitStrings++;
          int stringFrag = getFragment(fragmentForString, string);
          if (stringFrag != classLiteralFragment && stringFrag != NOT_EXCLUSIVE) {
            numFixups++;
            fragmentForString.put(string, NOT_EXCLUSIVE);
          }
        }
        // Fixup the class literals.
        for (JClassLiteral superclassClassLiteral : classLiteralsIn(initializer)) {
          JField superclassClassLiteralField = superclassClassLiteral.getField();
          // Fix the super class literal and add it to the reexamined.
          int superclassClassLiteralFragment = getFragment(fragmentForField,
              superclassClassLiteralField);
          if (superclassClassLiteralFragment != classLiteralFragment
              && superclassClassLiteralFragment != NOT_EXCLUSIVE) {
            numClassLiteralFixups++;
            fragmentForField.put(superclassClassLiteralField, NOT_EXCLUSIVE);
            // Add the field back so that its superclass classliteral gets fixed if necessary.
            potentialClassLiteralFields.add(superclassClassLiteralField);
          }
        }
      }
      logger.log(TreeLogger.DEBUG, "Fixed up load-order dependencies by moving " + numFixups
          + " strings in class literal constructors to fragment 0, out of " + numClassLitStrings);
      logger.log(TreeLogger.DEBUG, "Fixed up load-order dependencies by moving " +
          numClassLiteralFixups + " fields in class literal constructors to fragment 0, out of " +
          numClassLiterals);
    }

    /**
     * Fixup string literals that appear in field initializers.
     *
     * <p>GenerateJavaScriptAST decides whether a field will be initialized at the declaration or
     * by the instance/class initialer when lowering to JavasScript.
     *
     * <p>Only literals are affeced and only string literals are relevant for code splitting.
     */
    private void fixUpLoadOrderDependenciesForFieldsInitializedToStrings(TreeLogger logger,
        JProgram jprogram) {
      final int[] numFixups = new int[1];
      final int[] numFieldStrings = new int[1];

      (new JVisitor() {
        @Override
        public void endVisit(JField field, Context ctx) {
          if (field.getInitializer() instanceof JStringLiteral) {
            numFieldStrings[0]++;

            String string = ((JStringLiteral) field.getInitializer()).getValue();
            int fieldFrag = getFragment(fragmentForField, field);
            int stringFrag = getFragment(fragmentForString, string);
            if (fieldFrag != stringFrag && stringFrag != NOT_EXCLUSIVE) {
              numFixups[0]++;
              fragmentForString.put(string, NOT_EXCLUSIVE);
            }
          }
        }
      }).accept(jprogram);


      logger.log(TreeLogger.DEBUG, "Fixed up load-order dependencies by moving " + numFixups[0]
          + " strings used to initialize fields to fragment 0, out of " + numFieldStrings[0]);
    }

    /**
     * Fixes up the load-order dependencies from instance methods to their enclosing types.
     */
    private void fixUpLoadOrderDependenciesForMethods(TreeLogger logger, JProgram jprogram,
        Set<JMethod> methodsInJavaScript) {
      int numFixups = 0;

      for (JDeclaredType type : jprogram.getDeclaredTypes()) {
        int typeFrag = getFragment(fragmentForType, type);
        if (typeFrag == NOT_EXCLUSIVE) {
          continue;
        }
        /*
        * If the type is in an exclusive fragment, all its instance methods
        * must be in the same one.
        */
        for (JMethod method : type.getMethods()) {
          if (method.needsVtable() && methodsInJavaScript.contains(method)
              && typeFrag != getFragment(fragmentForMethod, method)) {
            fragmentForType.put(type, NOT_EXCLUSIVE);
            numFixups++;
            break;
          }
        }
      }

      logger.log(TreeLogger.DEBUG,
          "Fixed up load-order dependencies for instance methods by moving " + numFixups
              + " types to fragment 0, out of " + jprogram.getDeclaredTypes().size());
    }

    /**
     * Fixes up load order dependencies from types to their supertypes.
     */
    private void fixUpLoadOrderDependenciesForTypes(TreeLogger logger, JProgram jprogram) {
      int numFixups = 0;
      Queue<JDeclaredType> typesToCheck =
          new ArrayDeque<JDeclaredType>(jprogram.getDeclaredTypes().size());
      typesToCheck.addAll(jprogram.getDeclaredTypes());

      while (!typesToCheck.isEmpty()) {
        JDeclaredType type = typesToCheck.remove();
        if (type.getSuperClass() != null) {
          int typeFrag = getFragment(fragmentForType, type);
          int supertypeFrag = getFragment(fragmentForType, type.getSuperClass());
          if (typeFrag != supertypeFrag && supertypeFrag != NOT_EXCLUSIVE) {
            numFixups++;
            fragmentForType.put(type.getSuperClass(), NOT_EXCLUSIVE);
            typesToCheck.add(type.getSuperClass());
          }
        }
      }

      logger.log(TreeLogger.DEBUG, "Fixed up load-order dependencies on supertypes by moving "
          + numFixups + " types to fragment 0, out of " + jprogram.getDeclaredTypes().size());
    }

    private static <T> int getFragment(Map<T, Integer> map, T key) {
      Integer value = map.get(key);
      return (value == null) ? NOT_EXCLUSIVE : value;
    }

    /**
     * An atom is live in a fragment if either it is exclusive to that fragment or not exclusive
     * to any fragment.
     */
    private static <T> boolean isLiveInFragment(Map<T, Integer> map, T atom, int expectedFragment) {
      Integer actualFragment = getFragment(map, atom);
      return (expectedFragment == actualFragment) || (actualFragment == NOT_EXCLUSIVE);
    }

    /**
     * Traverse {@code exp} and find all string literals within it.
     */
    private static Set<String> stringsIn(JExpression exp) {
      final Set<String> strings = new HashSet<String>();
      class StringFinder extends JVisitor {
        @Override
        public void endVisit(JStringLiteral stringLiteral, Context ctx) {
          strings.add(stringLiteral.getValue());
        }
      }
      (new StringFinder()).accept(exp);
      return strings;
    }

    private <T> void updateMap(int fragment, Map<T, Integer>  map, Set<?> notExclusiveAtoms,
                               Iterable<T> atoms) {
      for (T atom : atoms) {
        if (!notExclusiveAtoms.contains(atom)) {
          /*
          * Note that it is fine to overwrite a preexisting entry in the map. If
          * an atom is dead until split point i has been reached, and is also
          * dead until entry j has been reached, then it is dead until both have
          * been reached. Thus, it can be downloaded along with either i's or j's
          * code.
          */
          map.put(atom, fragment);
        }
      }
    }
  }

  private boolean isInitial(int entry) {
    return initialLoadSequence.contains(entry);
  }

  /**
   * Map atoms to exclusive fragments. Do this by trying to find code atoms that
   * are only needed by a single split point. Such code can be moved to the
   * exclusively live fragment associated with that split point.
   */
  private void mapExclusiveAtoms(ExclusivityMap fragmentMap) {
    List<ControlFlowAnalyzer> allButOnes = computeAllButOneCfas();

    ControlFlowAnalyzer everything = computeCompleteCfa();

    Set<JField> allFields = new HashSet<JField>();
    Set<JMethod> allMethods = new HashSet<JMethod>();

    for (JNode node : everything.getLiveFieldsAndMethods()) {
      if (node instanceof JField) {
        allFields.add((JField) node);
      }
      if (node instanceof JMethod) {
        allMethods.add((JMethod) node);
      }
    }
    allFields.addAll(everything.getFieldsWritten());

    for (JRunAsync runAsync : jprogram.getRunAsyncs()) {
      int splitPoint = runAsync.getSplitPoint();
      if (isInitial(splitPoint)) {
        continue;
      }
      ControlFlowAnalyzer allButOne = allButOnes.get(splitPoint - 1);
      Set<JNode> allLiveNodes =
          union(allButOne.getLiveFieldsAndMethods(), allButOne.getFieldsWritten());
      fragmentMap.updateFields(splitPoint, allLiveNodes, allFields);
      fragmentMap.updateMethods(splitPoint, allButOne.getLiveFieldsAndMethods(), allMethods);
      fragmentMap.updateStrings(splitPoint, allButOne.getLiveStrings(), everything
          .getLiveStrings());
      fragmentMap.updateTypes(splitPoint, declaredTypesIn(allButOne.getInstantiatedTypes()),
          declaredTypesIn(everything.getInstantiatedTypes()));
    }
  }
}
