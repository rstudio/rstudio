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
import com.google.gwt.dev.jjs.ast.Context;
import com.google.gwt.dev.jjs.ast.JClassLiteral;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JField;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JNode;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JReferenceType;
import com.google.gwt.dev.jjs.ast.JStringLiteral;
import com.google.gwt.dev.jjs.ast.JVisitor;
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
import com.google.gwt.dev.util.PerfLogger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ArrayBlockingQueue;

/**
 * <p>
 * Divides the code in a {@link JsProgram} into multiple fragments. The initial
 * fragment is sufficient to run all of the program's functionality except for
 * anything called in a callback supplied to
 * {@link com.google.gwt.core.client.GWT#runAsync(com.google.gwt.core.client.RunAsyncCallback) GWT.runAsync()}.
 * The remaining code should be downloadable via
 * {@link com.google.gwt.core.client.AsyncFragmentLoader#inject(int)}.
 * </p>
 * 
 * <p>
 * The precise way the program is fragmented is an implementation detail that is
 * subject to change. Whenever the fragment strategy changes,
 * <code>AsyncFragmentLoader</code> must be updated in tandem. That said, the
 * current fragmentation strategy is to create an initial fragment and then
 * three more fragments for each split point. For each split point, there is:
 * </p>
 * 
 * <ul>
 * <li>a secondary base fragment, which is downloaded if this split point is
 * the first one reached. It contains enough code to continue running as soon as
 * it downloads.
 * <li>an exclusively live fragment, which is downloaded if this split point is
 * reached but is not the first one. It includes only that code that is
 * exclusively needed by this split point.
 * <li>a leftovers fragment, which includes all code that is in none of: the
 * initial download, any exclusive fragment, or the secondary base fragment for
 * this split point.
 * </ul>
 */
public class CodeSplitter {
  /**
   * A statement logger that immediately prints out everything live that it
   * sees.
   */
  public class EchoStatementLogger implements StatementLogger {
    public void logStatement(JsStatement stat, boolean isIncluded) {
      if (isIncluded) {
        if (stat instanceof JsExprStmt) {
          JsExpression expr = ((JsExprStmt) stat).getExpression();
          if (expr instanceof JsFunction) {
            JsFunction func = (JsFunction) expr;
            if (func.getName() != null) {
              JMethod method = map.nameToMethod(func.getName());
              if (method != null) {
                System.out.println(fullNameString(method));
              }
            }
          }
        }

        if (stat instanceof JsVars) {
          JsVars vars = (JsVars) stat;
          for (JsVar var : vars) {
            JField field = map.nameToField(var.getName());
            if (field != null) {
              System.out.println(fullNameString(field));
            }
            String string = map.stringLiteralForName(var.getName());
            if (string != null) {
              System.out.println("STRING " + var.getName());
            }
          }
        }
      }
    }
  }

  /**
   * A map from program atoms to the fragment they should be placed in. An entry
   * of 0 means it did not go into any fragment in particular.
   */
  private static class FragmentMap {
    public Map<JField, Integer> fields = new HashMap<JField, Integer>();
    public Map<JMethod, Integer> methods = new HashMap<JMethod, Integer>();
    public Map<String, Integer> strings = new HashMap<String, Integer>();
    public Map<JReferenceType, Integer> types = new HashMap<JReferenceType, Integer>();
  }

  /**
   * A liveness predicate that is based on a fragment map. See
   * {@link #mapFragments()}. Note that all non-zero fragments are assumed to
   * load after fragment 0, and so everything in fragment 0 is always live.
   */
  private static class FragmentMapLivenessPredicate implements
      LivenessPredicate {
    private final int fragment;
    private final FragmentMap fragmentMap;

    public FragmentMapLivenessPredicate(FragmentMap fragmentMap, int fragment) {
      this.fragmentMap = fragmentMap;
      this.fragment = fragment;
    }

    public boolean isLive(JField field) {
      return checkMap(fragmentMap.fields, field);
    }

    public boolean isLive(JMethod method) {
      return checkMap(fragmentMap.methods, method);
    }

    public boolean isLive(JReferenceType type) {
      return checkMap(fragmentMap.types, type);
    }

    public boolean isLive(String literal) {
      return checkMap(fragmentMap.strings, literal);
    }

    public boolean miscellaneousStatementsAreLive() {
      return true;
    }

    private <T> boolean checkMap(Map<T, Integer> map, T x) {
      Integer entryForX = map.get(x);
      if (entryForX == null) {
        // unrecognized items are always live
        return true;
      } else {
        return (fragment == entryForX) || (entryForX == 0);
      }
    }
  }

  /**
   * A liveness predicate that checks two separate underlying predicates.
   */
  private static class UnionLivenessPredicate implements LivenessPredicate {
    private final LivenessPredicate pred1;
    private final LivenessPredicate pred2;

    public UnionLivenessPredicate(LivenessPredicate pred1,
        LivenessPredicate pred2) {
      this.pred1 = pred1;
      this.pred2 = pred2;
    }

    public boolean isLive(JField field) {
      return pred1.isLive(field) || pred2.isLive(field);
    }

    public boolean isLive(JMethod method) {
      return pred1.isLive(method) || pred2.isLive(method);
    }

    public boolean isLive(JReferenceType type) {
      return pred1.isLive(type) || pred2.isLive(type);
    }

    public boolean isLive(String literal) {
      return pred1.isLive(literal) || pred2.isLive(literal);
    }

    public boolean miscellaneousStatementsAreLive() {
      return pred1.miscellaneousStatementsAreLive()
          || pred2.miscellaneousStatementsAreLive();
    }
  }

  /**
   * A Java property that causes the fragment map to be logged.
   * 
   * TODO(spoon) save the logging data to an auxiliary compiler output and, if
   * the logging is not too slow, always enable it.
   */
  private static String PROP_LOG_FRAGMENT_MAP = "gwt.jjs.logFragmentMap";

  public static void exec(TreeLogger logger, JProgram jprogram,
      JsProgram jsprogram, JavaToJavaScriptMap map) {
    if (jprogram.entryMethods.size() == 1) {
      // Don't do anything if there is no call to runAsync
      return;
    }

    new CodeSplitter(logger, jprogram, jsprogram, map).execImpl();
  }

  private static Map<JField, JClassLiteral> buildFieldToClassLiteralMap(
      JProgram jprogram) {
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

  private static String fullNameString(JField field) {
    return field.getEnclosingType().getName() + "." + field.getName();
  }

  private static String fullNameString(JMethod method) {
    return method.getEnclosingType().getName() + "."
        + JProgram.getJsniSig(method);
  }

  private static <T> int getOrZero(Map<T, Integer> map, T key) {
    Integer value = map.get(key);
    return (value == null) ? 0 : value;
  }

  private static <T> Set<T> union(Set<? extends T> set1, Set<? extends T> set2) {
    Set<T> union = new HashSet<T>();
    union.addAll(set1);
    union.addAll(set2);
    return union;
  }

  private static <T> void updateMap(int entry, Map<T, Integer> map,
      Set<?> liveWithoutEntry, Iterable<T> all) {
    for (T each : all) {
      if (!liveWithoutEntry.contains(each)) {
        /*
         * Note that it is fine to overwrite a preexisting entry in the map. If
         * an atom is dead until split point i has been reached, and is also
         * dead until entry j has been reached, then it is dead until both have
         * been reached. Thus, it can be downloaded along with either i's or j's
         * code.
         */
        map.put(each, entry);
      }
    }
  }

  private final Map<JField, JClassLiteral> fieldToLiteralOfClass;
  private final FragmentExtractor fragmentExtractor;

  /**
   * Code that is initially live when the program first downloads.
   */
  private final ControlFlowAnalyzer initiallyLive;
  private JProgram jprogram;
  private JsProgram jsprogram;
  private final TreeLogger logger;
  private final boolean logging;
  private JavaToJavaScriptMap map;
  private final Set<JMethod> methodsInJavaScript;
  private final int numEntries;

  private CodeSplitter(TreeLogger logger, JProgram jprogram,
      JsProgram jsprogram, JavaToJavaScriptMap map) {
    this.logger = logger.branch(TreeLogger.TRACE,
        "Splitting JavaScript for incremental download");
    this.jprogram = jprogram;
    this.jsprogram = jsprogram;
    this.map = map;

    numEntries = jprogram.entryMethods.size();
    logging = Boolean.getBoolean(PROP_LOG_FRAGMENT_MAP);
    fieldToLiteralOfClass = buildFieldToClassLiteralMap(jprogram);
    fragmentExtractor = new FragmentExtractor(jprogram, jsprogram, map);

    initiallyLive = new ControlFlowAnalyzer(jprogram);
    traverseEntry(initiallyLive, 0);

    methodsInJavaScript = fragmentExtractor.findAllMethodsInJavaScript();
  }

  /**
   * Create a new fragment and add it to the list.
   * 
   * @param alreadyLoaded The code that should be assumed to have already been
   *          loaded
   * @param liveNow The code that needs to be live once this fragment loads
   * @param statsToAppend Additional statements to append to the end of the new
   *          fragment
   * @param fragmentStats The list of fragments to append to
   */
  private void addFragment(LivenessPredicate alreadyLoaded,
      LivenessPredicate liveNow, List<JsStatement> statsToAppend,
      List<List<JsStatement>> fragmentStats) {
    if (logging) {
      System.out.println();
      System.out.println("==== Fragment " + fragmentStats.size() + " ====");
      fragmentExtractor.setStatementLogger(new EchoStatementLogger());
    }
    List<JsStatement> stats = fragmentExtractor.extractStatements(liveNow,
        alreadyLoaded);
    stats.addAll(statsToAppend);
    fragmentStats.add(stats);
  }

  /**
   * For each split point other than the initial one (0), compute a CFA that
   * traces every other split point.
   */
  private List<ControlFlowAnalyzer> computeAllButOneCfas() {
    List<ControlFlowAnalyzer> allButOnes = new ArrayList<ControlFlowAnalyzer>(
        numEntries - 1);

    for (int entry = 1; entry < numEntries; entry++) {
      ControlFlowAnalyzer cfa = new ControlFlowAnalyzer(initiallyLive);
      traverseAllButEntry(cfa, entry);
      // Traverse leftoversFragmentHasLoaded, because it should not
      // go into any of the exclusive fragments.
      cfa.traverseFromLeftoversFragmentHasLoaded();
      allButOnes.add(cfa);
    }

    return allButOnes;
  }

  /**
   * Compute a CFA that covers the entire live code of the program.
   */
  private ControlFlowAnalyzer computeCompleteCfa() {
    ControlFlowAnalyzer everything = new ControlFlowAnalyzer(jprogram);
    for (int entry = 0; entry < numEntries; entry++) {
      traverseEntry(everything, entry);
    }
    everything.traverseFromLeftoversFragmentHasLoaded();
    return everything;
  }

  private void execImpl() {
    PerfLogger.start("CodeSplitter");

    FragmentMap fragmentMap = mapFragments();

    List<List<JsStatement>> fragmentStats = new ArrayList<List<JsStatement>>(
        3 * numEntries - 2);

    {
      /*
       * Compute the base fragment. It includes everything that is live when the
       * program starts.
       */
      LivenessPredicate alreadyLoaded = new NothingAlivePredicate();
      LivenessPredicate liveNow = new CfaLivenessPredicate(initiallyLive);
      List<JsStatement> noStats = new ArrayList<JsStatement>();
      addFragment(alreadyLoaded, liveNow, noStats, fragmentStats);
    }

    /*
     * Compute the exclusively live fragments. Each includes everything
     * exclusively live after entry point i.
     */
    for (int i = 1; i < numEntries; i++) {
      LivenessPredicate alreadyLoaded = new FragmentMapLivenessPredicate(
          fragmentMap, 0);
      LivenessPredicate liveNow = new FragmentMapLivenessPredicate(fragmentMap,
          i);
      List<JsStatement> statsToAppend = fragmentExtractor.createCallsToEntryMethods(i);
      addFragment(alreadyLoaded, liveNow, statsToAppend, fragmentStats);
    }

    /*
     * Add secondary base fragments and their associated leftover fragments.
     */
    for (int base = 1; base < numEntries; base++) {
      ControlFlowAnalyzer baseCfa = new ControlFlowAnalyzer(initiallyLive);
      traverseEntry(baseCfa, base);
      LivenessPredicate baseLive = new CfaLivenessPredicate(baseCfa);

      // secondary base
      List<JsStatement> baseStatsToAppend = fragmentExtractor.createCallsToEntryMethods(base);
      addFragment(new CfaLivenessPredicate(initiallyLive), baseLive,
          baseStatsToAppend, fragmentStats);

      // leftovers
      LivenessPredicate globalLeftoversLive = new FragmentMapLivenessPredicate(
          fragmentMap, 0);
      LivenessPredicate associatedExclusives = new FragmentMapLivenessPredicate(
          fragmentMap, base);
      // Be sure to add in anything in the exclusives for base that is
      // not in its secondary base.
      LivenessPredicate leftoversLive = new UnionLivenessPredicate(
          globalLeftoversLive, associatedExclusives);
      List<JsStatement> statsToAppend = fragmentExtractor.createCallToLeftoversFragmentHasLoaded();
      addFragment(baseLive, leftoversLive, statsToAppend, fragmentStats);
    }

    // now install the new statements in the program fragments
    jsprogram.setFragmentCount(fragmentStats.size());
    for (int i = 0; i < fragmentStats.size(); i++) {
      JsBlock fragBlock = jsprogram.getFragmentBlock(i);
      fragBlock.getStatements().clear();
      fragBlock.getStatements().addAll(fragmentStats.get(i));
    }

    PerfLogger.end();
  }

  /**
   * <p>
   * Patch up the fragment map to satisfy load-order dependencies, as described
   * in the comment of {@link LivenessPredicate}. Load-order dependencies can
   * be violated when an atom is mapped to 0 as a leftover, but it has some
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
   */
  private void fixUpLoadOrderDependencies(FragmentMap fragmentMap) {
    fixUpLoadOrderDependenciesForMethods(fragmentMap);
    fixUpLoadOrderDependenciesForTypes(fragmentMap);
    fixUpLoadOrderDependenciesForClassLiterals(fragmentMap);
    fixUpLoadOrderDependenciesForFieldsInitializedToStrings(fragmentMap);
  }

  private void fixUpLoadOrderDependenciesForClassLiterals(
      FragmentMap fragmentMap) {
    int numClassLitStrings = 0;
    int numFixups = 0;
    for (JField field : fragmentMap.fields.keySet()) {
      JClassLiteral classLit = fieldToLiteralOfClass.get(field);
      if (classLit != null) {
        int classLitFrag = fragmentMap.fields.get(field);
        for (String string : stringsIn(field.getInitializer())) {
          numClassLitStrings++;
          int stringFrag = getOrZero(fragmentMap.strings, string);
          if (stringFrag != classLitFrag && stringFrag != 0) {
            numFixups++;
            fragmentMap.strings.put(string, 0);
          }
        }
      }
    }
    logger.log(TreeLogger.DEBUG, "Fixed up load-order dependencies by moving "
        + numFixups
        + " strings in class literal constructors to fragment 0, out of "
        + numClassLitStrings);
  }

  private void fixUpLoadOrderDependenciesForFieldsInitializedToStrings(
      FragmentMap fragmentMap) {
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
          fragmentMap.strings.put(string, 0);
        }
      }
    }

    logger.log(TreeLogger.DEBUG, "Fixed up load-order dependencies by moving "
        + numFixups
        + " strings used to initialize fields to fragment 0, out of "
        + +numFieldStrings);
  }

  private void fixUpLoadOrderDependenciesForMethods(FragmentMap fragmentMap) {
    int numFixups = 0;

    for (JReferenceType type : jprogram.getDeclaredTypes()) {
      int typeFrag = getOrZero(fragmentMap.types, type);

      if (typeFrag != 0) {
        /*
         * If the type is in an exclusive fragment, all its instance methods
         * must be in the same one.
         */
        for (JMethod method : type.methods) {
          if (!method.isStatic() && methodsInJavaScript.contains(method)) {
            int methodFrag = getOrZero(fragmentMap.methods, method);
            if (methodFrag != typeFrag) {
              fragmentMap.types.put(type, 0);
              numFixups++;
              break;
            }
          }
        }
      }
    }

    logger.log(TreeLogger.DEBUG,
        "Fixed up load-order dependencies for instance methods by moving "
            + numFixups + " types to fragment 0, out of "
            + jprogram.getDeclaredTypes().size());
  }

  private void fixUpLoadOrderDependenciesForTypes(FragmentMap fragmentMap) {
    int numFixups = 0;
    Queue<JReferenceType> typesToCheck = new ArrayBlockingQueue<JReferenceType>(
        jprogram.getDeclaredTypes().size());
    typesToCheck.addAll(jprogram.getDeclaredTypes());

    while (!typesToCheck.isEmpty()) {
      JReferenceType type = typesToCheck.remove();
      if (type.extnds != null) {
        int typeFrag = getOrZero(fragmentMap.types, type);
        int supertypeFrag = getOrZero(fragmentMap.types, type.extnds);
        if (typeFrag != supertypeFrag && supertypeFrag != 0) {
          numFixups++;
          fragmentMap.types.put(type.extnds, 0);
          typesToCheck.add(type.extnds);
        }
      }
    }

    logger.log(TreeLogger.DEBUG,
        "Fixed up load-order dependencies on supertypes by moving " + numFixups
            + " types to fragment 0, out of "
            + jprogram.getDeclaredTypes().size());
  }

  /**
   * Map code to fragments. Do this by trying to find code atoms that are only
   * needed by a single split point. Such code can be moved to the exclusively
   * live fragment associated with that split point.
   */
  private void mapExclusiveAtoms(FragmentMap fragmentMap) {
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

    for (int entry = 1; entry < numEntries; entry++) {
      ControlFlowAnalyzer allButOne = allButOnes.get(entry - 1);
      Set<JNode> allLiveNodes = union(allButOne.getLiveFieldsAndMethods(),
          allButOne.getFieldsWritten());
      updateMap(entry, fragmentMap.fields, allLiveNodes, allFields);
      updateMap(entry, fragmentMap.methods,
          allButOne.getLiveFieldsAndMethods(), allMethods);
      updateMap(entry, fragmentMap.strings, allButOne.getLiveStrings(),
          everything.getLiveStrings());
      updateMap(entry, fragmentMap.types, allButOne.getInstantiatedTypes(),
          everything.getInstantiatedTypes());
    }
  }

  /**
   * Map each program atom to a fragment. Atoms are mapped to a non-zero
   * fragment whenever they are known not to be needed whenever that fragment's
   * split point has not been reached. Any atoms that cannot be so mapped are
   * left in fragment zero.
   */
  private FragmentMap mapFragments() {
    FragmentMap fragmentMap = new FragmentMap();

    mapExclusiveAtoms(fragmentMap);
    fixUpLoadOrderDependencies(fragmentMap);

    return fragmentMap;
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

  /**
   * Traverse all code in the program except for that reachable only via
   * fragment <code>frag</code>. This does not call
   * {@link ControlFlowAnalyzer#finishTraversal()}.
   */
  private void traverseAllButEntry(ControlFlowAnalyzer cfa, int entry) {
    for (int otherEntry = 0; otherEntry < numEntries; otherEntry++) {
      if (otherEntry != entry) {
        traverseEntry(cfa, otherEntry);
      }
    }
  }

  /**
   * Traverse all code in the program that is reachable via fragment
   * <code>frag</code>. This does not call
   * {@link ControlFlowAnalyzer#finishTraversal()}.
   */
  private void traverseEntry(ControlFlowAnalyzer cfa, int splitPoint) {
    for (JMethod entryMethod : jprogram.entryMethods.get(splitPoint)) {
      cfa.traverseFrom(entryMethod);
    }
    if (splitPoint == 0) {
      /*
       * Include class literal factories for simplicity. It is possible to move
       * them out, if they are only needed by one fragment, but they are tiny,
       * so it does not seem worth the complexity in the compiler.
       */
      cfa.traverseFromClassLiteralFactories();
    }
  }
}
