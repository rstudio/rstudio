/*
 * Copyright 2011 Google Inc.
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
import com.google.gwt.dev.js.ast.JsBlock;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsStatement;
import com.google.gwt.dev.util.JsniRef;
import com.google.gwt.dev.util.collect.HashMap;
import com.google.gwt.dev.util.collect.Lists;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;
import com.google.gwt.thirdparty.guava.common.base.Predicate;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * Splits the GWT module into multiple downloads. <p>
 * 
 * The code split will divide the code base into multiple <code>spitpoints</code> based
 * on dependency informations computed using {@link ControlFlowAnalyzer}. Each undividable
 * elements of a GWT program: {@link JField}, {@link JMethod}, {@link JDeclaredType} or
 * String literal called <code>atom</code> will be assigned to a set of spitpoints based on
 * dependency information.
 * 
 * A Fragment partitioning will then use the split point assignments and divided the atom
 * into a set of fragments. A fragment will be a single unit of download for a client's
 * code.
 * 
 * TODO(acleung): Rename to CodeSplitter upon completion.
 * TODO(acleung): Figure out how to integrate with SOYC and dependency tracker.
 * TODO(acleung): Insert SpeedTracer calls at performance sensitive places.
 * TODO(acleung): Insert logger calls to generate meaningful logs.
 * TODO(acleung): Modify the fragment loader.
 * TODO(acleung): May be add back the odd heuristics if needed.
 */
public class CodeSplitter2 {
  
  /**
   * A dependency recorder that can record multiple dependency graphs. It has
   * methods for starting and finishing new dependency graphs.
   * 
   * TODO(acleung): This is currently broken. I need to investiage more on how we inform SOYC
   * the dependency results.
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
   * Marks the type of partition heuristics 
   */
  public enum ParitionHeuristics {
    /**
     * A one-to-one split point to fragment partition with no fragment merging.
     * Basically the 'old' algorithm.
     */
    BIJECTIVE,
    
    /**
     * Greedily merge two piece of fragment if they share the most code
     * together.
     */
    EDGE_GREEDY,
  }
  
  /**
   * Maps an atom to a set of split point that can be live (NOT necessary exclusively)
   * when that split point is activated. The split points are represented by a bit
   * set where S[i] is set if the atom needs to be live when split point i is live.
   */
  private static class LiveSplitPointMap {
    private static <T> boolean setLive(Map<T, BitSet> map, T atom, int splitPoint) {
      BitSet liveSet = map.get(atom);
      if (liveSet == null) {
        liveSet = new BitSet();
        liveSet.set(splitPoint);
        map.put(atom, liveSet);
        return true;
      } else {
        if (liveSet.get(splitPoint)) {
          return false;
        } else {
          liveSet.set(splitPoint);
          return true;
        }
      }
    }
    public Map<JField, BitSet> fields = new HashMap<JField, BitSet>();
    public Map<JMethod, BitSet> methods = new HashMap<JMethod, BitSet>();
    public Map<String, BitSet> strings = new HashMap<String, BitSet>();

    public Map<JDeclaredType, BitSet> types = new HashMap<JDeclaredType, BitSet>();
    
    boolean setLive(JDeclaredType type, int splitPoint) {
      return setLive(types, type, splitPoint);
    }
     
    boolean setLive(JField field, int splitPoint) {
      return setLive(fields, field, splitPoint);
    }
    
    boolean setLive(JMethod method, int splitPoint) {
      return setLive(methods, method, splitPoint);
    }
    
    boolean setLive(String string, int splitPoint) {
      return setLive(strings, string, splitPoint);
    }
  }
  
  /**
   * LivenessPredicate to tell the fragment extractor what is live.
   */
  private static class LiveSplitPointsPredicate implements LivenessPredicate {
    private final LiveSplitPointMap map;
    private final Predicate<BitSet> mask;
    LiveSplitPointsPredicate(LiveSplitPointMap map, Predicate<BitSet> mask) {
      this.map = map;
      this.mask = mask;
    }
    
    @Override
    public boolean isLive(JDeclaredType type) {
      return isLive(map.types, type);
    }

    @Override
    public boolean isLive(JField field) {
      return isLive(map.fields, field);
    }
    
    @Override
    public boolean isLive(JMethod method) {
      return isLive(map.methods, method);
    }
    
    @Override
    public boolean isLive(String literal) {
      return isLive(map.strings, literal);
    }
    
    @Override
    public boolean miscellaneousStatementsAreLive() {
      return true;
    }
    
    private <T> boolean isLive(Map<T, BitSet> map, T atom) {
      BitSet value = map.get(atom);
      return value != null && mask.apply(value);
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

  /**
   * Number of split points to merge, this should be configurable by user later.
   */
  public static final int NUM_SPLITPOINTS_TO_MERGE = 12;
  
  /**
   * The property key for a list of initially loaded split points.
   */
  private static final String PROP_INITIAL_SEQUENCE = "compiler.splitpoint.initial.sequence";
  
  public static ControlFlowAnalyzer computeInitiallyLive(JProgram jprogram) {
    return computeInitiallyLive(jprogram, NULL_RECORDER);
  }
  
  public static ControlFlowAnalyzer computeInitiallyLive(
      JProgram jprogram, MultipleDependencyGraphRecorder dependencyRecorder) {
    // Control Flow Analysis from a split point.
    ControlFlowAnalyzer cfa = new ControlFlowAnalyzer(jprogram);    
    dependencyRecorder.startDependencyGraph("initial", null);
    cfa.setDependencyRecorder(dependencyRecorder);
    cfa.traverseEntryMethods();
    traverseClassArray(jprogram, cfa);
    traverseImmortalTypes(jprogram, cfa);
    dependencyRecorder.endDependencyGraph();
    return cfa;
  }
  
  public static void exec(TreeLogger logger, JProgram jprogram, JsProgram jsprogram,
      JavaToJavaScriptMap map, MultipleDependencyGraphRecorder dependencyRecorder) {
    if (jprogram.getRunAsyncs().size() == 0) {
      // Don't do anything if there is no call to runAsync
      return;
    }
    Event codeSplitterEvent = SpeedTracerLogger.start(CompilerEventType.CODE_SPLITTER);
    new CodeSplitter2(logger, jprogram, jsprogram, map, dependencyRecorder).execImpl();
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

    installInitialLoadSequenceField(program, initialLoadSequence);
    program.setSplitPointInitialSequence(new ArrayList<Integer>(initialLoadSequence));
    codeSplitterEvent.end();
  }
  private static Map<JField, JClassLiteral> buildFieldToClassLiteralMap(JProgram jprogram) {
    final Map<JField, JClassLiteral> map = new HashMap<JField, JClassLiteral>();
    class BuildFieldToLiteralVisitor extends JVisitor {
      @Override
      public void endVisit(JClassLiteral lit, Context ctx) {
        map.put(lit.getField(), lit);
      }
    }
    (new BuildFieldToLiteralVisitor()).accept(jprogram);
    return map;
  }
  private static <T> void countShardedAtomsOfType(Map<T, BitSet> livenessMap, int[][] matrix) {
    // Count the number of atoms shared only by 
    for (Entry<T, BitSet> fieldLiveness : livenessMap.entrySet()) {
      BitSet liveSplitPoints = fieldLiveness.getValue();
      
      if (liveSplitPoints.get(0)) {
        continue; 
      }
      if (liveSplitPoints.cardinality() != 2) {
        continue;
      }
      
      int start = liveSplitPoints.nextSetBit(0);
      int end = liveSplitPoints.nextSetBit(start + 1);
      matrix[start][end]++;
    }
  }

  private static <T> int getOrZero(Map<T, BitSet> map, T key) {
    BitSet value = map.get(key);
    if (value != null && value.cardinality() == 1) {
      return value.nextSetBit(0);
    }
    return 0;
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
  
  private static ControlFlowAnalyzer recordLiveSet(
      ControlFlowAnalyzer cfa, LiveSplitPointMap liveness, int idx) {       
    for (JNode node : cfa.getLiveFieldsAndMethods()) {
      if (node instanceof JField) {
        liveness.setLive((JField) node, idx);
      }
      if (node instanceof JMethod) {
        liveness.setLive((JMethod) node, idx);
      }      
    }
    
    for (JField node : cfa.getFieldsWritten()) {
      if (node instanceof JField) {
        liveness.setLive((JField) node, idx);
      }  
    }
    
    for (String s : cfa.getLiveStrings()) {
      liveness.setLive(s, idx);
    }
    
    for (JReferenceType t : cfa.getInstantiatedTypes()) {
      if (t instanceof JDeclaredType) {
        liveness.setLive((JDeclaredType) t, idx);
      }
    }
    return cfa;
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

  private final Map<JField, JClassLiteral> fieldToLiteralOfClass;

  private FragmentExtractor fragmentExtractor;
  
  /**
   * List of fragments that needs to be in the initial load, in that order.
   */
  private final LinkedHashSet<Integer> initialLoadSequence;
  
  /**
   * CFA result of all the initially live atoms.
   */
  private ControlFlowAnalyzer initiallyLive = null;
  
  private final JProgram jprogram;

  private final JsProgram jsprogram;

  private final LiveSplitPointMap liveness = new LiveSplitPointMap();
  
  private final Set<JMethod> methodsInJavaScript;
  
  /**
   * Maps a split-point number to a fragment number.
   * 
   * splitPointToFragmmentMap[x] = y implies split point #x is in fragment #y.
   * 
   * TODO(acleung): We could use some better abstraction for this. I feel this
   * piece of information will be shared with many parts of the codegen process.
   */
  private final int[] splitPointToFragmentMap;

  private CodeSplitter2(TreeLogger logger, JProgram jprogram, JsProgram jsprogram,
      JavaToJavaScriptMap map, MultipleDependencyGraphRecorder dependencyRecorder) {
    this.jprogram = jprogram;
    this.jsprogram = jsprogram;
    this.fragmentExtractor = new FragmentExtractor(jprogram, jsprogram, map);
    this.initialLoadSequence = new LinkedHashSet<Integer>(jprogram.getSplitPointInitialSequence());
    
    // Start out to assume split gets it's own fragment. We'll merge them later.
    this.splitPointToFragmentMap = new int[jprogram.getRunAsyncs().size() + 1];
    for (int i = 0; i < splitPointToFragmentMap.length; i++) {
      splitPointToFragmentMap[i] = i;
    }
    
    // TODO(acleung): I don't full understand this. This is mostly from the old
    // algorithm which patches up certain dependency after the control flow analysis.
    fieldToLiteralOfClass = buildFieldToClassLiteralMap(jprogram);
    fragmentExtractor = new FragmentExtractor(jprogram, jsprogram, map);
 
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
    List<JsStatement> stats = fragmentExtractor.extractStatements(liveNow, alreadyLoaded);
    stats.addAll(stmtsToAppend);
    fragmentStats.put(splitPoint, stats);
  }
  
  private ControlFlowAnalyzer computeLiveSet(
      ControlFlowAnalyzer initiallyLive, LiveSplitPointMap liveness, JRunAsync runAsync) {
    // Control Flow Analysis from a split point.
    ControlFlowAnalyzer cfa = new ControlFlowAnalyzer(initiallyLive);
    cfa.traverseFromRunAsync(runAsync);   
    recordLiveSet(cfa, liveness, runAsync.getSplitPoint());
    return cfa;
  }
  
  /**
   * This is the high level algorithm of the pass.
   */
  private void execImpl() {
    
    // Step #1: Compute all the initially live atoms that are part of entry points
    // class inits..etc.
    initiallyLive = computeInitiallyLive(jprogram, NULL_RECORDER);
    recordLiveSet(initiallyLive, liveness, 0);
 
    // Step #2: Incrementally add each split point that are classified as initial load sequence.
    // Also, any atoms added here will be added to the initially live set as well. The liveness
    for (JRunAsync runAsync : jprogram.getRunAsyncs()) {
      if (initialLoadSequence.contains(runAsync.getSplitPoint())) {
        initiallyLive = computeLiveSet(initiallyLive, liveness, runAsync);
      }
    }
    
    // Step #3: Similar to #2 but this time, we independently compute the live set of each
    // split point that is not part of the initial load.
    for (JRunAsync runAsync : jprogram.getRunAsyncs()) {
      if (!initialLoadSequence.contains(runAsync.getSplitPoint())) {
        computeLiveSet(initiallyLive, liveness, runAsync);
      }
    }
    
    // Step #4: Fix up the rare load order dependencies.
    fixUpLoadOrderDependencies(liveness, -1);

    // Step #5: Now the LiveSplitPointMap will contain all the livEness information we need,
    // partition the fragments by focusing on making the initial download and
    // leftover fragment download as small as possible.
    partitionFragments();
    
    // Step #6: Extract fragments using the partition algorithm.
    extractStatements(initiallyLive);    
  }
  
  private void extractStatements(ControlFlowAnalyzer initiallyLive) {
    Map<Integer, List<JsStatement>> fragmentStats = new LinkedHashMap<Integer, List<JsStatement>>();
    
    // Initial download
    {
      LivenessPredicate alreadyLoaded = new NothingAlivePredicate();
      LivenessPredicate liveNow = new CfaLivenessPredicate(initiallyLive);
      List<JsStatement> noStats = new ArrayList<JsStatement>();
      addFragment(0, alreadyLoaded, liveNow, noStats, fragmentStats);
    }
    
    final List<Predicate<BitSet>> exclusivePredicates = new LinkedList<Predicate<BitSet>>();
    
    // Signifies what has been already loaded in the initial fragment.
    LivenessPredicate alreadyLoaded = new LiveSplitPointsPredicate(liveness, new Predicate<BitSet>() {
      @Override
      public boolean apply(BitSet value) {
        // Live if it is used by the first fragment.
        if (value.get(0)) {
          return true;
        } else {            
          for (int sp : initialLoadSequence) {
            if (value.get(sp)) {
              return true;
            }
          }
        }
        return false;
      }
    });

    // Search for all the atoms that are exclusively needed in each split point.
    for (int i = 1; i < splitPointToFragmentMap.length; i++) {
      
      // This mean split point [i] has been merged with another split point, ignore it.
      if (splitPointToFragmentMap[i] != i) {
        continue;
      }
      
      // This was needed in the initial load sequence, ignore it.
      if (initialLoadSequence.contains(i)) {
        continue;
      }
      
      // Creates a mask that is used to check if an atom is exclusively needed by the current
      // fragment as well as all the fragment is was merged with.
      final BitSet mask = new BitSet(splitPointToFragmentMap.length);
      mask.set(i);
      for (int j = i + 1; j < splitPointToFragmentMap.length; j++) {
        if (initialLoadSequence.contains(j)) {
          continue;
        }
        if (splitPointToFragmentMap[j] == i) {
          mask.set(j);
        }
      }
      
      // Creates a predicate
      final Predicate<BitSet> pred = new Predicate<BitSet>() {
        @Override
        public boolean apply(BitSet value) {
          if (value.get(0)) {
            return true;
          }

          for (int sp : initialLoadSequence) {
            if (value.get(sp)) {
              return true;
            }
          }
          
          BitSet valueOrMask = (BitSet) value.clone();
          valueOrMask.or(mask);
          
          // Returns true if it at least matches one of the set field in the mask but not
          // having a field set that is unset in the mask.
          return value.intersects(mask) && valueOrMask.cardinality() <= mask.cardinality();
        }
      };
      exclusivePredicates.add(pred);
      
      LivenessPredicate liveNow = new LiveSplitPointsPredicate(liveness, pred);
      List<JsStatement> statsToAppend = fragmentExtractor.createOnLoadedCall(i);
      addFragment(i, alreadyLoaded, liveNow, statsToAppend, fragmentStats);
    }
    
    // Left over fragments.
    {      
      LivenessPredicate liveNow = new LiveSplitPointsPredicate(liveness, new Predicate<BitSet>() {

        @Override
        public boolean apply(BitSet value) {
          for (Predicate<BitSet> p : exclusivePredicates) {
            if (p.apply(value)) {
              return false;
            }
          }
          return true;
        }
      });
      
      List<JsStatement> statsToAppend = fragmentExtractor.createOnLoadedCall(splitPointToFragmentMap.length);
      addFragment(splitPointToFragmentMap.length, alreadyLoaded, liveNow, statsToAppend, fragmentStats);
    }
    
    // now install the new statements in the program fragments
    jsprogram.setFragmentCount(fragmentStats.size());
    int count = 0;
    for (int i : fragmentStats.keySet()) {
      JsBlock fragBlock = jsprogram.getFragmentBlock(count++);
      fragBlock.getStatements().clear();
      fragBlock.getStatements().addAll(fragmentStats.get(i));
    }

    // TODO(acleung): This is an hack-ish way to put share the fragment map.
    // jprogram.splitPointToFragmentMap = splitPointToFragmentMap;
  }

  private void fixUpLoadOrderDependencies(LiveSplitPointMap fragmentMap, int splitPoint) {
    fixUpLoadOrderDependenciesForMethods(fragmentMap, splitPoint);
    fixUpLoadOrderDependenciesForTypes(fragmentMap, splitPoint);
    fixUpLoadOrderDependenciesForClassLiterals(fragmentMap, splitPoint);
    fixUpLoadOrderDependenciesForFieldsInitializedToStrings(fragmentMap, splitPoint);
  }
  private void fixUpLoadOrderDependenciesForClassLiterals(LiveSplitPointMap fragmentMap, int splitPoint) {
    int numClassLitStrings = 0;
    int numFixups = 0;
    for (JField field : fragmentMap.fields.keySet()) {
      JClassLiteral classLit = fieldToLiteralOfClass.get(field);
      if (classLit != null) {
        BitSet value = fragmentMap.fields.get(field);
        int classLitFrag = 0;
        for (int i = 0; i < value.cardinality(); i++) {
          classLitFrag = value.nextSetBit(classLitFrag);
          for (String string : stringsIn(field.getInitializer())) {
            numClassLitStrings++;
            int stringFrag = getOrZero(fragmentMap.strings, string);
            if (stringFrag != classLitFrag && stringFrag != 0) {
              numFixups++;
              fragmentMap.setLive(string, 0);
            }
          }
          classLitFrag++;
        }
      }
    }
  }

  private void fixUpLoadOrderDependenciesForFieldsInitializedToStrings(LiveSplitPointMap fragmentMap, int splitPoint) {
    int numFixups = 0;
    int numFieldStrings = 0;

    for (JField field : fragmentMap.fields.keySet()) {
      if (field.getInitializer() instanceof JStringLiteral) {
        numFieldStrings++;

        String string = ((JStringLiteral) field.getInitializer()).getValue();
        int fieldFrag = getOrZero(fragmentMap.fields, field);
        int stringFrag = getOrZero(fragmentMap.strings, string);
        if (fieldFrag != stringFrag && stringFrag != 0) {
          numFixups++;
          fragmentMap.setLive(string, 0);
        }
      }
    }
  }
  
  private void fixUpLoadOrderDependenciesForMethods(LiveSplitPointMap fragmentMap, int splitPoint) {
    int numFixups = 0;

    for (JDeclaredType type : jprogram.getDeclaredTypes()) {
      int typeFrag = getOrZero(fragmentMap.types, type);

      if (typeFrag != 0) {
        /*
         * If the type is in an exclusive fragment, all its instance methods
         * must be in the same one.
         */
        for (JMethod method : type.getMethods()) {
          if (method.needsVtable() && methodsInJavaScript.contains(method)) {
            int methodFrag = getOrZero(fragmentMap.methods, method);
            if (methodFrag != typeFrag) {
              fragmentMap.setLive(type, 0);
              numFixups++;
              break;
            }
          }
        }
      }
    }
  }

  private void fixUpLoadOrderDependenciesForTypes(LiveSplitPointMap fragmentMap, int splitPoint) {
    int numFixups = 0;
    Queue<JDeclaredType> typesToCheck =
        new ArrayBlockingQueue<JDeclaredType>(jprogram.getDeclaredTypes().size());
    typesToCheck.addAll(jprogram.getDeclaredTypes());

    while (!typesToCheck.isEmpty()) {
      JDeclaredType type = typesToCheck.remove();
      if (type.getSuperClass() != null) {
        int typeFrag = getOrZero(fragmentMap.types, type);
        int supertypeFrag = getOrZero(fragmentMap.types, type.getSuperClass());
        if (typeFrag != supertypeFrag && supertypeFrag != 0) {
          numFixups++;
          fragmentMap.setLive(type.getSuperClass(), 0);
          typesToCheck.add(type.getSuperClass());
        }
      }
    }
  }

  /**
   * We haves pinned down that fragment partition is an NP-Complete problem that maps right to
   * weight graph partitioning.
   */
  private void partitionFragments() {
    // TODO(acleung): Currently this only use the Edge Greedy heuristics.
    if (true) {
      partitionFragmentUsingEdgeGreedy();
    }
  }

  /**
   * Partition aggressively base on the edge information. If two split points share
   * lots of 
   */
  private void partitionFragmentUsingEdgeGreedy() {
    // This matrix serves as an adjanccy matrix of split points.
    // An edge from a to b with weight of x imples split point a and b shares x atoms exclusively.
    int[][] matrix = new int[splitPointToFragmentMap.length][splitPointToFragmentMap.length];
    countShardedAtomsOfType(liveness.fields, matrix);
    countShardedAtomsOfType(liveness.methods, matrix);
    countShardedAtomsOfType(liveness.strings, matrix);
    countShardedAtomsOfType(liveness.types, matrix);

    for (int c = 0; c < NUM_SPLITPOINTS_TO_MERGE; c++) {
      int bestI = 0, bestJ = 0, max = 0;
      for (int i = 1; i < splitPointToFragmentMap.length; i++) {
        if (initialLoadSequence.contains(i)) {
          continue;
        }
        for (int j = 1; j < splitPointToFragmentMap.length; j++) {
          if (initialLoadSequence.contains(j)) {
            continue;
          }
          if (matrix[i][j] > max &&
              // Unmerged.
              splitPointToFragmentMap[i] == i &&
              splitPointToFragmentMap[j] == j) {
            bestI = i;
            bestJ = j;
            max = matrix[i][j];
          }
        }
      }
      
      if (max == 0) {
        break;
      }
      splitPointToFragmentMap[bestJ] = bestI;
      splitPointToFragmentMap[bestI] = -1;        
      matrix[bestI][bestJ] = 0;
      System.out.println("merging: " + bestI + " " + bestJ);
    }
    
    for (int i = 0; i < splitPointToFragmentMap.length; i++) {
      if (splitPointToFragmentMap[i] < 0) {
        splitPointToFragmentMap[i] = i;
      }
    }
  }
  
  /**
   * Traverse <code>exp</code> and find all string literals within it.
   */
  private Set<String> stringsIn(JExpression exp) {
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
}
