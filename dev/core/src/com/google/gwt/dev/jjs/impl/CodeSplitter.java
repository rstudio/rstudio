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
import com.google.gwt.dev.jjs.impl.FragmentExtractor.LivenessPredicate;
import com.google.gwt.dev.jjs.impl.FragmentExtractor.NothingAlivePredicate;
import com.google.gwt.dev.jjs.impl.FragmentExtractor.StatementLogger;
import com.google.gwt.dev.js.ast.JsBlock;
import com.google.gwt.dev.js.ast.JsExprStmt;
import com.google.gwt.dev.js.ast.JsExpression;
import com.google.gwt.dev.js.ast.JsFunction;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsStatement;
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
 * current fragmentation strategy is to create one fragment for each call to
 * <code>runAsync()</code>. Each such fragment holds the code that is
 * exclusively needed by that particular call to <code>runAsync()</code>. Any
 * code needed by two or more calls to <code>runAsync()</code> is placed in
 * the initial fragment.
 * </p>
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
              if (method != null && method.getEnclosingType() != null) {
                System.out.println(method.getEnclosingType().getName() + "."
                    + JProgram.getJsniSig(method));
              }
            }
          }
        }
      }
    }
  }

  /**
   * A map from program atoms to the fragment they should be placed in.
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
   * A Java property that causes the fragment map to be logged.
   * 
   * TODO(spoon) save the logging data to an auxiliary compiler output and, if
   * the logging is not too slow, always enable it.
   */
  private static String PROP_LOG_FRAGMENT_MAP = "gwt.jjs.logFragmentMap";

  public static void exec(JProgram jprogram, JsProgram jsprogram,
      JavaToJavaScriptMap map) {
    new CodeSplitter(jprogram, jsprogram, map).execImpl();
  }

  private static <T> int getOrZero(Map<T, Integer> map, T key) {
    Integer value = map.get(key);
    return (value == null) ? 0 : value;
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
  private JProgram jprogram;
  private JsProgram jsprogram;
  private final boolean logging;
  private JavaToJavaScriptMap map;
  private final int numEntries;

  private CodeSplitter(JProgram jprogram, JsProgram jsprogram,
      JavaToJavaScriptMap map) {
    this.jprogram = jprogram;
    this.jsprogram = jsprogram;
    this.map = map;
    numEntries = jprogram.entryMethods.size();
    logging = Boolean.getBoolean(PROP_LOG_FRAGMENT_MAP);
    fieldToLiteralOfClass = FragmentExtractor.buildFieldToClassLiteralMap(jprogram);
  }

  /**
   * For each split point other than the initial one (0), compute a CFA that
   * traces every other split point.
   */
  private List<ControlFlowAnalyzer> computeAllButOneCfas() {
    // Reusing initiallyLive for each entry gives a significant speedup
    ControlFlowAnalyzer initiallyLive = new ControlFlowAnalyzer(jprogram);
    traverseEntry(initiallyLive, 0);
    initiallyLive.finishTraversal();

    List<ControlFlowAnalyzer> allButOnes = new ArrayList<ControlFlowAnalyzer>(
        numEntries - 1);

    for (int entry = 1; entry < numEntries; entry++) {
      ControlFlowAnalyzer cfa = new ControlFlowAnalyzer(initiallyLive);
      traverseAllButEntry(cfa, entry);
      cfa.finishTraversal();
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
    everything.finishTraversal();
    return everything;
  }

  private void execImpl() {
    if (numEntries == 1) {
      // Don't do anything if there is no call to runAsync
      return;
    }

    PerfLogger.start("CodeSplitter");

    // Map code to the fragments
    FragmentMap fragmentMap = mapFragments();

    List<List<JsStatement>> fragmentStats = new ArrayList<List<JsStatement>>(
        numEntries);

    // Save the extractor for reuse
    FragmentExtractor fragmentExtractor = new FragmentExtractor(jprogram,
        jsprogram, map);

    // Extract the code for each fragment, according to fragmentMap
    for (int i = 0; i < numEntries; i++) {
      LivenessPredicate pred = new FragmentMapLivenessPredicate(fragmentMap, i);

      LivenessPredicate alreadyLoaded;
      if (i == 0) {
        alreadyLoaded = new NothingAlivePredicate();
      } else {
        alreadyLoaded = new FragmentMapLivenessPredicate(fragmentMap, 0);
      }

      if (logging) {
        System.out.println();
        System.out.println("==== Fragment " + i + " ====");
        fragmentExtractor.setStatementLogger(new EchoStatementLogger());
      }

      List<JsStatement> entryStats = fragmentExtractor.extractStatements(pred,
          alreadyLoaded);

      if (i > 0) {
        /*
         * The fragment mapper drops all calls to entry methods. Add them back.
         */
        fragmentExtractor.addCallsToEntryMethods(i, entryStats);
      }

      fragmentStats.add(entryStats);
    }

    // Install the new statements in the program fragments
    jsprogram.setFragmentCount(numEntries);
    for (int i = 0; i < fragmentStats.size(); i++) {
      JsBlock fragBlock = jsprogram.getFragmentBlock(i);
      fragBlock.getStatements().clear();
      fragBlock.getStatements().addAll(fragmentStats.get(i));
    }

    PerfLogger.end();
  }

  /**
   * Mostly it is okay to load code before it is needed. However, there are some
   * exceptions, where merely loading a code atom requires that some other atom
   * has also been loaded. To address such situations, move the load-time
   * dependencies to fragment 0, so they are sure to be available.
   */
  private void fixUpLoadOrderDependencies(FragmentMap fragmentMap) {
    fixUpLoadOrderDependenciesForTypes(fragmentMap);
    fixUpLoadOrderDependenciesForClassLiterals(fragmentMap);
  }

  /**
   * A class literal cannot be loaded until its associate strings are. Make sure
   * that the strings are available for all class literals at the time they are
   * loaded.
   */
  private void fixUpLoadOrderDependenciesForClassLiterals(
      FragmentMap fragmentMap) {
    for (JField field : fragmentMap.fields.keySet()) {
      JClassLiteral classLit = fieldToLiteralOfClass.get(field);
      if (classLit != null) {
        int classLitFrag = fragmentMap.fields.get(field);
        for (String string : stringsIn(field.getInitializer())) {
          int stringFrag = getOrZero(fragmentMap.strings, string);
          if (stringFrag != classLitFrag && stringFrag != 0) {
            fragmentMap.strings.put(string, 0);
          }
        }
      }
    }
  }

  /**
   * The setup code for a class cannot be loaded before the setup code for its
   * superclass.
   */
  private void fixUpLoadOrderDependenciesForTypes(FragmentMap fragmentMap) {
    Queue<JReferenceType> typesToCheck = new ArrayBlockingQueue<JReferenceType>(
        jprogram.getDeclaredTypes().size());
    typesToCheck.addAll(jprogram.getDeclaredTypes());
    while (!typesToCheck.isEmpty()) {
      JReferenceType type = typesToCheck.remove();
      if (type.extnds != null) {
        int typeFrag = getOrZero(fragmentMap.types, type);
        int supertypeFrag = getOrZero(fragmentMap.types, type.extnds);
        if (typeFrag != supertypeFrag && supertypeFrag != 0) {
          fragmentMap.types.put(type.extnds, 0);
          typesToCheck.add(type.extnds);
        }
      }
    }
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

    for (int entry = 1; entry < numEntries; entry++) {
      ControlFlowAnalyzer allButOne = allButOnes.get(entry - 1);
      updateMap(entry, fragmentMap.fields, allButOne.getLiveFieldsAndMethods(),
          allFields);
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
  private void traverseEntry(ControlFlowAnalyzer cfa, int entry) {
    for (JMethod entryMethod : jprogram.entryMethods.get(entry)) {
      cfa.traverseFrom(entryMethod);
    }
    if (entry == 0) {
      /*
       * Include class literal factories for simplicity. It is possible to move
       * them out, if they are only needed by one fragment, but they are tiny,
       * so it does not look like it is worth the complexity in the compiler.
       */
      cfa.traverseFromClassLiteralFactories();
    }
  }
}
