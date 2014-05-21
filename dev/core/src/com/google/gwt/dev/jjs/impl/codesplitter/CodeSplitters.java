/*
 * Copyright 2013 Google Inc.
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
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.cfg.ConfigProps;
import com.google.gwt.dev.jjs.ast.JArrayType;
import com.google.gwt.dev.jjs.ast.JExpression;
import com.google.gwt.dev.jjs.ast.JMethod;
import com.google.gwt.dev.jjs.ast.JMethodCall;
import com.google.gwt.dev.jjs.ast.JNewArray;
import com.google.gwt.dev.jjs.ast.JNode;
import com.google.gwt.dev.jjs.ast.JNumericEntry;
import com.google.gwt.dev.jjs.ast.JPrimitiveType;
import com.google.gwt.dev.jjs.ast.JProgram;
import com.google.gwt.dev.jjs.ast.JRunAsync;
import com.google.gwt.dev.jjs.impl.JsniRefLookup;
import com.google.gwt.dev.util.JsniRef;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.thirdparty.guava.common.base.Function;
import com.google.gwt.thirdparty.guava.common.base.Joiner;
import com.google.gwt.thirdparty.guava.common.collect.Collections2;
import com.google.gwt.thirdparty.guava.common.collect.LinkedListMultimap;
import com.google.gwt.thirdparty.guava.common.collect.Lists;
import com.google.gwt.thirdparty.guava.common.collect.Multimap;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * Utility function related to code splitting.
 */
public class CodeSplitters {
  /**
   * Choose an initial load sequence of split points for the specified program.
   * Do so by identifying split points whose code always load first, before any
   * other split points. As a side effect, modifies
   * {@link com.google.gwt.core.client.impl.AsyncFragmentLoader#initialLoadSequence}
   * in the program being compiled.
   *
   * @throws UnableToCompleteException If the module specifies a bad load order
   */
  public static void pickInitialLoadSequence(TreeLogger logger,
      JProgram program, ConfigProps config) throws UnableToCompleteException {
    SpeedTracerLogger.Event codeSplitterEvent =
        SpeedTracerLogger
            .start(CompilerEventType.CODE_SPLITTER, "phase", "pickInitialLoadSequence");
    TreeLogger branch =
        logger.branch(TreeLogger.TRACE, "Looking up initial load sequence for split points");
    LinkedHashSet<JRunAsync> asyncsInInitialLoadSequence = new LinkedHashSet<JRunAsync>();

    List<String> initialSequence = config.getStrings(PROP_INITIAL_SEQUENCE);
    for (String runAsyncReference : initialSequence) {
      JRunAsync runAsync = findRunAsync(runAsyncReference, program, branch);
      if (asyncsInInitialLoadSequence.contains(runAsync)) {
        branch.log(TreeLogger.ERROR, "Split point specified more than once: " + runAsyncReference);
      }
      asyncsInInitialLoadSequence.add(runAsync);
    }

    logInitialLoadSequence(logger, asyncsInInitialLoadSequence);
    installInitialLoadSequenceField(program, asyncsInInitialLoadSequence);
    program.setInitialAsyncSequence(asyncsInInitialLoadSequence);
    codeSplitterEvent.end();
  }

  /**
   * Find a split point as designated in the {@link #PROP_INITIAL_SEQUENCE}
   * configuration property.
   */
  public static JRunAsync findRunAsync(String refString, JProgram program, TreeLogger branch)
      throws UnableToCompleteException {
    SpeedTracerLogger.Event codeSplitterEvent =
        SpeedTracerLogger.start(CompilerEventType.CODE_SPLITTER, "phase", "findRunAsync");
    Multimap<String, JRunAsync> splitPointsByRunAsyncName =
        computeRunAsyncsByName(program.getRunAsyncs(), false);

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
            @Override
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
      Collection<JRunAsync> splitPoints = splitPointsByRunAsyncName.get(canonicalName);
      if (splitPoints == null) {
        branch.log(TreeLogger.ERROR, "Method does not enclose a runAsync call: " + jsniRef);
        throw new UnableToCompleteException();
      }
      if (splitPoints.size() > 1) {
        branch.log(TreeLogger.ERROR, "Method includes multiple runAsync calls, "
            + "so it's ambiguous which one is meant: " + jsniRef);
        throw new UnableToCompleteException();
      }

      assert splitPoints.size() == 1;
      return splitPoints.iterator().next();
    }

    // Assume it's a raw class name
    Collection<JRunAsync> splitPoints = splitPointsByRunAsyncName.get(refString);
    if (splitPoints == null || splitPoints.size() == 0) {
      branch.log(TreeLogger.ERROR, "No runAsync call is labelled with class " + refString);
      throw new UnableToCompleteException();
    }
    if (splitPoints.size() > 1) {
      branch.log(TreeLogger.ERROR, "More than one runAsync call is labelled with class "
          + refString);
      throw new UnableToCompleteException();
    }
    assert splitPoints.size() == 1;
    JRunAsync result = splitPoints.iterator().next();
    codeSplitterEvent.end();

    return result;
  }

  /**
   * Returns the collection of asyncs as a collection of singleton collections containing one
   * async each.
   */
  static Collection<Collection<JRunAsync>> getListOfLists(Collection<JRunAsync> runAsyncs) {
    return Collections2.transform(runAsyncs, new Function<JRunAsync, Collection<JRunAsync>>() {
      @Override
      public Collection<JRunAsync> apply(JRunAsync runAsync) {
        return Lists.newArrayList(runAsync);
      }
    });
  }

  /**
   * Returns the number of exclusive fragments from the expected number of fragments. The
   * result is expectedFragmentCount - (initials + 1) - 1 (for leftovers).
   *
   */
  public static int getNumberOfExclusiveFragmentFromExpectedFragmentCount(
      int numberOfInitialAsyncs, int expectedFragmentCount) {
    return Math.max(0, expectedFragmentCount - (numberOfInitialAsyncs + 1) - 1);
  }

  /**
   * A Java property that causes the fragment map to be logged.
   */
  static String PROP_LOG_FRAGMENT_MAP = "gwt.jjs.logFragmentMap";
  static final String PROP_INITIAL_SEQUENCE = "compiler.splitpoint.initial.sequence";
  public static final String MIN_FRAGMENT_SIZE = "compiler.splitpoint.leftovermerge.size";

  private static void logInitialLoadSequence(TreeLogger logger,
       LinkedHashSet<JRunAsync> initialLoadSequence) {
    if (!logger.isLoggable(TreeLogger.TRACE)) {
      return;
    }

    StringBuffer message = new StringBuffer();
    message.append("Initial load sequence of split points: ");
    if (initialLoadSequence.isEmpty()) {
      message.append("(none)");
    } else {
      Collection<Integer> runAsyncIds = Collections2.transform(initialLoadSequence,
          new Function<JRunAsync, Integer>() {
            @Override
            public Integer apply(JRunAsync runAsync) {
              return runAsync.getRunAsyncId();
            }
          });
      message.append(Joiner.on(", ").join(runAsyncIds));
    }

    logger.log(TreeLogger.TRACE, message.toString());
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
      LinkedHashSet<JRunAsync> initialLoadSequence) {
    // Arg 1 is initialized in the source as "new int[]{}".
    JMethodCall call = ReplaceRunAsyncs.getBrowserLoaderConstructor(program);
    JExpression arg1 = call.getArgs().get(1);
    assert arg1 instanceof JNewArray;
    JArrayType arrayType = program.getTypeArray(JPrimitiveType.INT);
    assert ((JNewArray) arg1).getArrayType() == arrayType;
    List<JExpression> initializers = new ArrayList<JExpression>(initialLoadSequence.size());

    // RunAsyncFramentIndex will later be replaced by the fragment the async is in.
    // TODO(rluble): this approach is not very clean, ideally the load sequence should be
    // installed AFTER code splitting when the fragment ids are known; rather than inserting
    // a placeholder in the AST and patching the ast later.
    for (JRunAsync runAsync : initialLoadSequence) {
      initializers.add(new JNumericEntry(call.getSourceInfo(), "RunAsyncFragmentIndex",
          runAsync.getRunAsyncId()));
    }
    JNewArray newArray =
        JNewArray.createInitializers(arg1.getSourceInfo(), arrayType,
            Lists.newArrayList(initializers));
    call.setArg(1, newArray);
  }

  static Multimap<String, JRunAsync> computeRunAsyncsByName(Collection<JRunAsync> runAsyncs,
      boolean onlyExplicitNames) {
    Multimap<String, JRunAsync> runAsyncsByName = LinkedListMultimap.create();
    for (JRunAsync runAsync : runAsyncs) {
      String name = runAsync.getName();
      if (name == null || (onlyExplicitNames && !runAsync.hasExplicitClassLiteral())) {
        continue;
      }
      runAsyncsByName.put(name, runAsync);
    }
    return runAsyncsByName;
  }
}
