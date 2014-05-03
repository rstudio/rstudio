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
package com.google.gwt.dev.jjs.impl.codesplitter;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.dev.jjs.ast.JClassType;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JRunAsync;
import com.google.gwt.dev.jjs.impl.ControlFlowAnalyzer;
import com.google.gwt.dev.jjs.impl.JavaToJavaScriptMap;
import com.google.gwt.dev.js.ast.JsBlock;
import com.google.gwt.dev.js.ast.JsContext;
import com.google.gwt.dev.js.ast.JsModVisitor;
import com.google.gwt.dev.js.ast.JsNumericEntry;
import com.google.gwt.dev.js.ast.JsProgram;
import com.google.gwt.dev.js.ast.JsStatement;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;
import com.google.gwt.thirdparty.guava.common.base.Predicate;
import com.google.gwt.thirdparty.guava.common.collect.Collections2;
import com.google.gwt.thirdparty.guava.common.collect.Iterables;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Maps;
import com.google.gwt.thirdparty.guava.common.collect.Multimap;


import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
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
 * <p>Code splitting is implemented in a two stage process: the first stage decides how runAsyncs
 * are grouped together and the second decides the code in each fragment by computing the
 * appropriate control flow analyses.
 * </p>
 *
 * <p> The first stage is implemented by a {@link FragmentPartitionStrategy}, that can use fast but
 * unsound analyses to determine how to group the runAsyncs together, its only requirement is that
 * the result is a partition of a subset of the runAsyncs. Currently two strategies are implemented:
 * (1) {@link OneToOneFragmentPartitionStrategy} that assigns each runAsync to one fragment (as in
 * the original CodeSplitter), and (2) {@link MergeBySimilarityFragmentPartitionStrategy}</p> where
 * runAsyncs are pairwise merged together into a predetermined maximum number of fragments is a
 * way that is estimated to minimize the leftover fragment size (which is the strategy previously
 * attempted in the now obsolete CodeSplitter2. Additionally if the option
 * {@link CodeSplitters.MIN_FRAGMENT_SIZE} is set, this strategy also merge fragments that are
 * smaller than the minimum fragments size together and if the resulting combined fragment is still
 * smaller than the minimum fragment size it is left out of the fragmentation so that it is merged
 * into the leftovers by the second stage.
 * <p>
 *
 * <p>
 * The second stage assigns program atoms that to fragments that exclusively use them, setting up
 * and extra fragment for the non exclusive atoms and the runAsyncs that were not assigned to any
 * fragment. Code that is activated when the application starts and runAsyncs that are part of the
 * {@link CodeSplitters.PROP_INITIAL_SEQUENCE} are excluded from the first stage and are treated as
 * special initial fragments that will be downloaded when the application starts.
 * Whenever this second stage is changed  <code>AsyncFragmentLoader</code> must be updated
 * in tandem.
 * </p>
 *
 * <p>
 * The fragment for a runAsync point contains different things depending on whether
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

  public static ControlFlowAnalyzer computeInitiallyLive(JProgram jprogram) {
    return computeInitiallyLive(jprogram, MultipleDependencyGraphRecorder.NULL_RECORDER);
  }

  public static void exec(TreeLogger logger, JProgram jprogram, JsProgram jsprogram,
      JavaToJavaScriptMap map, int expectedFragmentCount, int minFragmentSize,
      MultipleDependencyGraphRecorder dependencyRecorder) {
    if (jprogram.getRunAsyncs().isEmpty()) {
      // Don't do anything if there is no call to runAsync
      return;
    }
    Event codeSplitterEvent = SpeedTracerLogger.start(CompilerEventType.CODE_SPLITTER);
    dependencyRecorder.open();
    new CodeSplitter(logger, jprogram, jsprogram, map, expectedFragmentCount, minFragmentSize,
        dependencyRecorder).execImpl();
    dependencyRecorder.close();
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

  public static int computeTotalSize(int[] jsLengths) {
    /*
     * The total script size is currently simple: it's the sum of all the
     * individual script files.
     *
     * TODO(rluble): This function seems unnecessary and out of place here.
     */

    int totalSize = 0;
    for (int size : jsLengths) {
      totalSize += size;
    }
    return totalSize;
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
    computeLivenessFromCodeGenTypes(jprogram, cfa);
    dependencyRecorder.endDependencyGraph();
    return cfa;
  }

  /**
   * Any immortal codegen types must be part of the initial download.
   */
  private static void computeLivenessFromCodeGenTypes(JProgram jprogram,
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

  /**
   * Group run asyncs that have the same class literal as the first parameter in the two parameter
   * GWT.runAsync call.
   */
  private static Collection<Collection<JRunAsync>> groupAsyncsByClassLiteral(
      Collection<JRunAsync> runAsyncs) {
    Collection<Collection<JRunAsync>> result = Lists.newArrayList();
    Multimap<String, JRunAsync> asyncsGroupedByName =
        CodeSplitters.computeRunAsyncsByName(runAsyncs, true);
    // Add runAsyncs that have class literals in groups.
    result.addAll(asyncsGroupedByName.asMap().values());
    // Add all the rest.
    result.addAll(CodeSplitters.getListOfLists(Collections2.filter(runAsyncs,
        new Predicate<JRunAsync>() {
          @Override
          public boolean apply(JRunAsync runAsync) {
            return !runAsync.hasExplicitClassLiteral();
          }
        })));
    return result;
  }

  private final MultipleDependencyGraphRecorder dependencyRecorder;
  private final FragmentExtractor fragmentExtractor;
  private final LinkedHashSet<JRunAsync> initialLoadSequence;

  /**
   * Code that is initially live when the program first downloads.
   */
  private final ControlFlowAnalyzer initiallyLiveCfa;
  private final JProgram jprogram;
  private final JsProgram jsprogram;

  /**
   * Computed during {@link #execImpl()}, so that intermediate steps of it can
   * be used as they are created.
   */
  private ControlFlowAnalyzer initialSequenceCfa;
  private final TreeLogger logger;
  private final boolean logFragmentMap;
  private final JavaToJavaScriptMap map;
  private final Set<JMethod> methodsInJavaScript;

  private final List<Fragment> fragments = Lists.newArrayList();

  private final FragmentPartitionStrategy partitionStrategy;

  private CodeSplitter(TreeLogger logger, JProgram jprogram, JsProgram jsprogram,
      JavaToJavaScriptMap map, int expectedFragmentCount, int minFragmentSize,
      MultipleDependencyGraphRecorder dependencyRecorder) {
    this.logger = logger.branch(TreeLogger.TRACE, "Splitting JavaScript for incremental download");
    this.jprogram = jprogram;
    this.jsprogram = jsprogram;
    this.map = map;
    this.dependencyRecorder = dependencyRecorder;
    this.initialLoadSequence = jprogram.getInitialAsyncSequence();
    assert initialLoadSequence != null;

    logFragmentMap = Boolean.getBoolean(CodeSplitters.PROP_LOG_FRAGMENT_MAP);
    fragmentExtractor = new FragmentExtractor(jprogram, jsprogram, map);

    initiallyLiveCfa = computeInitiallyLive(jprogram, dependencyRecorder);

    methodsInJavaScript = fragmentExtractor.findAllMethodsInJavaScript();

    // TODO(rluble): expected fragment count is not enforced. the actual number
    // of fragments may be more or less....
    partitionStrategy = expectedFragmentCount > 0 ?
        new MergeBySimilarityFragmentPartitionStrategy(
            CodeSplitters.getNumberOfExclusiveFragmentFromExpectedFragmentCount(
                initialLoadSequence.size(), expectedFragmentCount), minFragmentSize) :
        new OneToOneFragmentPartitionStrategy();
  }

  /**
   * Compute the statements that go into a fragment.
   *
   * @param fragmentId the fragment number
   * @param alreadyLoaded The code that should be assumed to have already been
   *          loaded
   * @param liveNow The code that is assumed live once this fragment loads;
   *          anything in here but not in <code>alreadyLoaded</code> will be
   *          included in the created fragment
   */
  private List<JsStatement>  statementsForFragment(int fragmentId,
      LivenessPredicate alreadyLoaded, LivenessPredicate liveNow) {
    if (logFragmentMap) {
      System.out.println();
      System.out.println("==== Fragment " + fragmentId + " ====");
      fragmentExtractor.setStatementLogger(new EchoStatementLogger(map));
    }
    return fragmentExtractor.extractStatements(liveNow, alreadyLoaded);
  }

  /**
   * For each exclusive fragment (those that are not part of the initial load sequence) compute
   * a CFA that traces every split point not in the fragment.
   */
  private Map<Fragment, ControlFlowAnalyzer> computeComplementCfaForFragments(
      Collection<Fragment> exclusiveFragments) {
    String dependencyGraphNameAfterInitialSequence = dependencyGraphNameAfterInitialSequence();

    Map<Fragment, ControlFlowAnalyzer> notLiveCfaByFragment = Maps.newHashMap();

    for (Fragment fragment : exclusiveFragments) {
      assert fragment.isExclusive();

      dependencyRecorder.startDependencyGraph("sp" + fragment.getFragmentId(),
          dependencyGraphNameAfterInitialSequence);
      ControlFlowAnalyzer cfa = new ControlFlowAnalyzer(initialSequenceCfa);
      cfa.setDependencyRecorder(dependencyRecorder);
      for (Fragment otherFragment : exclusiveFragments) {
        // don't trace the initial fragments as they have already been traced and their atoms are
        // already in {@code initialSequenceCfa}
        if (otherFragment.isInitial()) {
          continue;
        }
        if (otherFragment == fragment) {
          continue;
        }
        for (JRunAsync otherRunAsync : otherFragment.getRunAsyncs()) {
          cfa.traverseFromRunAsync(otherRunAsync);
        }
      }
      dependencyRecorder.endDependencyGraph();
      notLiveCfaByFragment.put(fragment, cfa);
    }
    return notLiveCfaByFragment;
  }

  /**
   * Compute a CFA that covers the entire live code of the program.
   */
  private ControlFlowAnalyzer computeCompleteCfa() {
    dependencyRecorder.startDependencyGraph("total", null);
    ControlFlowAnalyzer completeCfa = new ControlFlowAnalyzer(jprogram);
    completeCfa.setDependencyRecorder(dependencyRecorder);
    completeCfa.traverseEverything();
    dependencyRecorder.endDependencyGraph();
    return completeCfa;
  }

  /**
   * The name of the dependency graph that corresponds to
   * {@link #initialSequenceCfa}.
   */
  private String dependencyGraphNameAfterInitialSequence() {
    if (initialLoadSequence.isEmpty()) {
      return "initial";
    } else {
      return "sp" + Iterables.getLast(initialLoadSequence).getRunAsyncId();
    }
  }

  /**
   * Map each program atom as exclusive to some split point, whenever possible.
   * Also fixes up load order problems that could result from splitting code
   * based on this assumption.
   */
  private ExclusivityMap computeExclusivityMapWithFixups(Collection<Fragment> exclusiveFragments) {
    ControlFlowAnalyzer completeCfa = computeCompleteCfa();
    Map<Fragment, ControlFlowAnalyzer> notLiveCfaByFragment =
        computeComplementCfaForFragments(exclusiveFragments);
    ExclusivityMap exclusivityMap =  ExclusivityMap.computeExclusivityMap(exclusiveFragments,
        completeCfa, notLiveCfaByFragment);
    exclusivityMap.fixUpLoadOrderDependencies(logger, jprogram, methodsInJavaScript);
    return exclusivityMap;
  }

  /**
   * The current implementation of code splitting divides the program into fragments. There are
   * four different types of fragment.
   *   - initial download: the part of the program that will execute from the entry point and is
   *     not part of any runAsync. This fragment is implicit and there is not representation of
   *     it in the code splitter.
   *   - initial fragments: some runAsyncs are forced to be in the initial download by listing them
   *     in the {@link CodeSplitters.PROP_INITIAL_SEQUENCE} property. A separate fragment (Type.INITIAL)
   *     is created for each of there splitpoints and each contains only one splitpoit.
   *   - exclusive fragments: the remaining runAsyncs are assigned to some exclusive fragment. Many
   *     splitpoints may be in the same fragment but each of these splitpoints is in one and only
   *     one fragment. The fragmentation strategy assigns splitpoints to fragments.
   *   - leftover fragments: this is an artificial fragment that will contain all the atoms that
   *     are not in the initial and are not exclusive to a fragment.
   *
   *<p>Code splitting is a three stage process:
   *   - first the initial fragment are determined.
   *   - then a fragmentation strategy is run to partition runAsyncs into exclusive fragments.
   *   - lastly atoms that are not exclusive are assigned to the LEFT_OVERS fragment.
   */
  private void execImpl() {

    Fragment lastInitialFragment = null;

    // Fragments are numbered from 0.
    int nextFragmentIdToAssign = 0;

    // Step #1: Decide how to map splitpoints to fragments.
    {
      /*
       * Compute the base fragment. It includes everything that is live when the
       * program starts.
       */
      LivenessPredicate alreadyLoaded = new NothingAlivePredicate();
      LivenessPredicate liveNow = new CfaLivenessPredicate(initiallyLiveCfa);
      Fragment fragment =
          new Fragment(Fragment.Type.INITIAL);
      fragment.setFragmentId(nextFragmentIdToAssign++);
      List<JsStatement> statementsForFragment = statementsForFragment(fragment.getFragmentId(),
          alreadyLoaded, liveNow);
      fragment.setStatements(statementsForFragment);
      lastInitialFragment = fragment;
      fragments.add(fragment);
    }

    /*
     * Compute the base fragments, for split points in the initial load
     * sequence.
     */
    initialSequenceCfa = new ControlFlowAnalyzer(initiallyLiveCfa);
    String extendsCfa = "initial";
    List<Integer> initialFragmentNumberSequence = new ArrayList<Integer>();
    for (JRunAsync runAsync : initialLoadSequence) {
      LivenessPredicate alreadyLoaded = new CfaLivenessPredicate(initialSequenceCfa);

      String depGraphName = "sp" + runAsync.getRunAsyncId();
      dependencyRecorder.startDependencyGraph(depGraphName, extendsCfa);
      extendsCfa = depGraphName;

      ControlFlowAnalyzer liveAfterSp = new ControlFlowAnalyzer(initialSequenceCfa);
      liveAfterSp.traverseFromRunAsync(runAsync);
      dependencyRecorder.endDependencyGraph();

      LivenessPredicate liveNow = new CfaLivenessPredicate(liveAfterSp);

      Fragment fragment = new Fragment(Fragment.Type.INITIAL, lastInitialFragment);
      fragment.setFragmentId(nextFragmentIdToAssign++);
      fragment.addRunAsync(runAsync);
      List<JsStatement> statements = statementsForFragment(fragment.getFragmentId(),
          alreadyLoaded, liveNow);
      statements.addAll(fragmentExtractor.createOnLoadedCall(fragment.getFragmentId()));
      fragment.setStatements(statements);
      fragments.add(fragment);
      lastInitialFragment = fragment;

      initialFragmentNumberSequence.add(fragment.getFragmentId());
      initialSequenceCfa = liveAfterSp;
    }

    // Set the initial fragment sequence.
    jprogram.setInitialFragmentIdSequence(initialFragmentNumberSequence);

    Collection<Collection<JRunAsync>> groupedNonInitialRunAsyncs =
        groupAsyncsByClassLiteral(Collections2.filter(jprogram.getRunAsyncs(),
            new Predicate<JRunAsync>() {
              @Override
              public boolean apply(JRunAsync jRunAsync) {
                return !isInitial(jRunAsync);
              }
            }
        ));

    // Decide exclusive fragments according to the preselected partitionStrategy.
    Collection<Fragment>  exclusiveFragments =
        partitionStrategy.partitionIntoFragments(logger, initialSequenceCfa,
            groupedNonInitialRunAsyncs);

    Fragment leftOverFragment =
        new Fragment(Fragment.Type.NOT_EXCLUSIVE, lastInitialFragment);


    int firstExclusiveFragmentNumber = nextFragmentIdToAssign;
    // Assign fragment numbers to exclusive fragments.
    for (Fragment fragment : exclusiveFragments) {
      fragment.setFragmentId(nextFragmentIdToAssign++);
      fragment.addImmediateAncestors(leftOverFragment);
    }

    // From here numbers are unchanged,
    // Determine which atoms actually land in each exclusive fragment.
    ExclusivityMap exclusivityMap = computeExclusivityMapWithFixups(exclusiveFragments);

    /*
     * Populate the exclusively live fragments. Each includes everything
     * exclusively live after entry point i.
     */
    for (Fragment fragment : exclusiveFragments) {
      assert fragment.isExclusive();

      LivenessPredicate alreadyLoaded = exclusivityMap.getLivenessPredicate(
          ExclusivityMap.NOT_EXCLUSIVE);
      LivenessPredicate liveNow = exclusivityMap.getLivenessPredicate(fragment);
      List<JsStatement> statements = statementsForFragment(fragment.getFragmentId(),
          alreadyLoaded, liveNow);
      fragment.setStatements(statements);
      fragment.addStatements(
          fragmentExtractor.createOnLoadedCall(fragment.getFragmentId()));
    }

    fragments.addAll(exclusiveFragments);

    /*
     * Populate the leftovers fragment.
     */
    {
      LivenessPredicate alreadyLoaded = new CfaLivenessPredicate(initialSequenceCfa);
      LivenessPredicate liveNow = exclusivityMap.getLivenessPredicate(ExclusivityMap.NOT_EXCLUSIVE);
      leftOverFragment.setFragmentId(nextFragmentIdToAssign++);
      List<JsStatement> statements = statementsForFragment(leftOverFragment.getFragmentId(),
          alreadyLoaded, liveNow);
      statements.addAll(fragmentExtractor.createOnLoadedCall(leftOverFragment.getFragmentId()));
      leftOverFragment.setStatements(statements);
      fragments.add(leftOverFragment);
    }

    // now install the new statements in the program fragments
    jsprogram.setFragmentCount(fragments.size());
    for (int i = 0; i < fragments.size(); i++) {
      JsBlock fragBlock = jsprogram.getFragmentBlock(i);
      fragBlock.getStatements().clear();
      fragBlock.getStatements().addAll(fragments.get(i).getStatements());
    }

    // Pass the fragment partitioning information to JProgram.
    jprogram.setFragmentPartitioningResult(
        new FragmentPartitioningResult(fragments, jprogram.getRunAsyncs().size()));

    // Lastly patch up the JavaScript AST
    replaceFragmentId();
  }

  private boolean isInitial(JRunAsync runAsync) {
    return initialLoadSequence.contains(runAsync);
  }

  /**
   * Patch up the fragment loading code in the JavaScript AST.
   *
   * <p>Initially GWT.runAsyncs are replaced in the {@link ReplaceRunAsyncs} pass and some code
   * is added to the AST that references the fragment for a runAsync. At that stage (before any
   * code splitting has occurred) each unique runAsync id and the number of runAsyncs are embedded
   * in the AST as "tagged" JsNumbericEntry. After code splitting those entries need to be replaced
   * by the frament ids associatied with each runAsync and the total number of fragments.
   * </p>
   */
  private void replaceFragmentId() {
    // TODO(rluble): this approach where the ast is patched  is not very clean. Maybe the fragment
    // information should be data instead of code in the ast.
    final FragmentPartitioningResult result = jprogram.getFragmentPartitioningResult();
    (new JsModVisitor() {
      @Override
      public void endVisit(JsNumericEntry x, JsContext ctx) {
        if (x.getKey().equals("RunAsyncFragmentIndex")) {
          int fragmentId = result.getFragmentForRunAsync(x.getValue());
          x.setValue(fragmentId);
        }
        // this is actually the fragmentId for the leftovers fragment.
        if (x.getKey().equals("RunAsyncFragmentCount")) {
         x.setValue(jsprogram.getFragmentCount() - 1);
        }
      }
    }).accept(jsprogram);
  }
}
