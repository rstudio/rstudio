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
package com.google.gwt.benchmarks.rebind;

import com.google.gwt.benchmarks.BenchmarkShell;
import com.google.gwt.benchmarks.client.IterationTimeLimit;
import com.google.gwt.benchmarks.client.RangeEnum;
import com.google.gwt.benchmarks.client.RangeField;
import com.google.gwt.benchmarks.client.Setup;
import com.google.gwt.benchmarks.client.Teardown;
import com.google.gwt.benchmarks.client.impl.BenchmarkResults;
import com.google.gwt.benchmarks.client.impl.IterableAdapter;
import com.google.gwt.benchmarks.client.impl.PermutationIterator;
import com.google.gwt.benchmarks.client.impl.Trial;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.dev.generator.ast.ForLoop;
import com.google.gwt.dev.generator.ast.MethodCall;
import com.google.gwt.dev.generator.ast.Statement;
import com.google.gwt.dev.generator.ast.Statements;
import com.google.gwt.dev.generator.ast.StatementsList;
import com.google.gwt.junit.rebind.JUnitTestCaseStubGenerator;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.IncrementalCommand;
import com.google.gwt.user.rebind.SourceWriter;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Implements a generator for Benchmark classes. Benchmarks require additional
 * code generation above and beyond standard JUnit tests.
 */
public class BenchmarkGenerator extends JUnitTestCaseStubGenerator {

  private static class MutableLong {
    long value;
  }

  private static final String BEGIN_PREFIX = "begin";

  private static final String BENCHMARK_PARAM_META = "gwt.benchmark.param";

  private static final String BENCHMARK_RESULTS_CLASS = BenchmarkResults.class.getName();

  private static long defaultTimeout = -1;

  private static final String EMPTY_FUNC = "__emptyFunc";

  private static final String END_PREFIX = "end";

  private static final String ESCAPE_LOOP = "__escapeLoop";

  private static final String ITERABLE_ADAPTER_CLASS = IterableAdapter.class.getName();

  private static final String PERMUTATION_ITERATOR_CLASS = PermutationIterator.class.getName();

  private static final String TRIAL_CLASS = Trial.class.getName();

  /**
   * Returns all the zero-argument JUnit test methods that do not have
   * overloads.
   * 
   * @return Map<String,JMethod>
   */
  public static Map<String, JMethod> getNotOverloadedTestMethods(
      JClassType requestedClass) {
    Map<String, List<JMethod>> methods = getAllMethods(requestedClass,
        new MethodFilter() {
          public boolean accept(JMethod method) {
            return isJUnitTestMethod(method, true);
          }
        });

    // Create a new map to store the methods
    Map<String, JMethod> notOverloadedMethods = new HashMap<String, JMethod>();
    for (Map.Entry<String, List<JMethod>> entry : methods.entrySet()) {
      List<JMethod> methodOverloads = entry.getValue();
      if (methodOverloads.size() == 1) {
        JMethod overload = methodOverloads.get(0);
        if (overload.getParameters().length == 0) {
          notOverloadedMethods.put(entry.getKey(), overload);
        }
      }
    }

    return notOverloadedMethods;
  }

  /**
   * Returns all the JUnit test methods that are overloaded test methods with
   * parameters. Does not include the zero-argument test methods.
   * 
   * @return Map<String,JMethod>
   */
  public static Map<String, JMethod> getParameterizedTestMethods(
      JClassType requestedClass, TreeLogger logger) {
    Map<String, List<JMethod>> testMethods = getAllMethods(requestedClass,
        new MethodFilter() {
          public boolean accept(JMethod method) {
            return isJUnitTestMethod(method, true);
          }
        });

    // Create a new mapping to return
    Map<String, JMethod> overloadedMethods = new HashMap<String, JMethod>();

    // Remove all non-overloaded test methods
    for (Map.Entry<String, List<JMethod>> entry : testMethods.entrySet()) {
      String name = entry.getKey();
      List<JMethod> methods = entry.getValue();

      if (methods.size() > 2) {
        String msg = requestedClass + "." + name
            + " has more than one overloaded version"
            + "; it will not be included in the test case execution";
        logger.log(TreeLogger.WARN, msg, null);
        continue;
      }

      if (methods.size() == 1) {
        JMethod method = methods.get(0);
        if (method.getParameters().length != 0) {
          /*
           * User probably goofed - otherwise why create a test method with
           * arguments but not the corresponding no-argument version? Would be
           * better if our benchmarking system didn't require the no-argument
           * test to make the benchmarks run correctly (JUnit artifact).
           */
          String msg = requestedClass + "." + name
              + " does not have a zero-argument overload"
              + "; it will not be included in the test case execution";
          logger.log(TreeLogger.WARN, msg, null);
        }
        // Only a zero-argument version, we don't need to process it.
        continue;
      }

      JMethod method1 = methods.get(0);
      JMethod method2 = methods.get(1);
      JMethod noArgMethod = null;
      JMethod overloadedMethod = null;

      if (method1.getParameters().length == 0) {
        noArgMethod = method1;
      } else {
        overloadedMethod = method1;
      }

      if (method2.getParameters().length == 0) {
        noArgMethod = method2;
      } else {
        overloadedMethod = method2;
      }

      if (noArgMethod == null) {
        String msg = requestedClass + "." + name
            + " does not have a zero-argument overload"
            + "; it will not be included in the test case execution";
        logger.log(TreeLogger.WARN, msg, null);
        continue;
      }

      overloadedMethods.put(entry.getKey(), overloadedMethod);
    }

    return overloadedMethods;
  }

  private static JMethod getBeginMethod(JClassType type, JMethod method) {
    Setup setup = method.getAnnotation(Setup.class);
    String methodName;
    if (setup != null) {
      methodName = setup.value();
    } else {
      methodName = new StringBuffer(method.getName()).replace(0,
          "test".length(), BEGIN_PREFIX).toString();
    }
    return getMethod(type, methodName);
  }

  private static JMethod getEndMethod(JClassType type, JMethod method) {
    Teardown teardown = method.getAnnotation(Teardown.class);
    String methodName;
    if (teardown != null) {
      methodName = teardown.value();
    } else {
      methodName = new StringBuffer(method.getName()).replace(0,
          "test".length(), END_PREFIX).toString();
    }
    return getMethod(type, methodName);
  }

  private static JMethod getMethod(JClassType type, MethodFilter filter) {
    Map<String, List<JMethod>> map = getAllMethods(type, filter);
    Set<Map.Entry<String, List<JMethod>>> entrySet = map.entrySet();
    if (entrySet.size() == 0) {
      return null;
    }
    List<JMethod> methods = entrySet.iterator().next().getValue();
    return methods.get(0);
  }

  private static JMethod getMethod(JClassType type, final String name) {
    return getMethod(type, new MethodFilter() {
      public boolean accept(JMethod method) {
        return method.getName().equals(name);
      }
    });
  }

  @Override
  public void writeSource() throws UnableToCompleteException {
    super.writeSource();

    generateEmptyFunc(getSourceWriter());
    implementZeroArgTestMethods();
    implementParameterizedTestMethods();
    generateAsyncCode();
    BenchmarkShell.getReport().addBenchmark(logger, getRequestedClass());
  }

  /**
   * Generates benchmarking code which wraps <code>stmts</code> The timing
   * result is a double in units of milliseconds. It's value is placed in the
   * variable named, <code>timeMillisName</code>.
   * 
   * @return The set of Statements containing the benchmark code along with the
   *         wrapped <code>stmts</code>
   */
  private Statements benchmark(Statements stmts, String timeMillisName,
      long bound, Statements recordCode, Statements breakCode) {
    Statements benchmarkCode = new StatementsList();
    List<Statements> benchStatements = benchmarkCode.getStatements();

    ForLoop loop = new ForLoop("int numLoops = 1", "true", "");
    benchStatements.add(loop);
    List<Statements> loopStatements = loop.getStatements();

    loopStatements.add(new Statement("long start = System.currentTimeMillis()"));
    ForLoop runLoop = new ForLoop("int i = 0", "i < numLoops", "++i", stmts);
    loopStatements.add(runLoop);

    // Put the rest of the code in 1 big statement to simplify things
    String benchCode = "long duration = System.currentTimeMillis() - start;\n\n"
        +

        "if ( duration < 150 ) {\n"
        + "  numLoops += numLoops;\n"
        + "  continue;\n"
        + "}\n\n"
        +

        "double durationMillis = duration * 1.0;\n"
        + "double numLoopsAsDouble = numLoops * 1.0;\n"
        + timeMillisName
        + " = durationMillis / numLoopsAsDouble";

    loopStatements.add(new Statement(benchCode));

    if (recordCode != null) {
      loopStatements.add(recordCode);
    }

    if (bound != 0) {
      loopStatements.add(new Statement("if ( numLoops == 1 && duration > "
          + bound + " ) {\n" + breakCode.toString() + "\n" + "}\n\n"));
    }

    loopStatements.add(new Statement("break"));

    return benchmarkCode;
  }

  private boolean fieldExists(JClassType type, String fieldName) {
    JField field = type.findField(fieldName);
    if (field == null) {
      JClassType superClass = type.getSuperclass();
      // noinspection SimplifiableIfStatement
      if (superClass == null) {
        return false;
      }
      return fieldExists(superClass, fieldName);
    }
    return true;
  }

  private Statements genBenchTarget(JMethod beginMethod, JMethod endMethod,
      List<String> paramNames, Statements test) {
    Statements statements = new StatementsList();
    List<Statements> statementsList = statements.getStatements();

    if (beginMethod != null) {
      statementsList.add(new Statement(new MethodCall(beginMethod.getName(),
          paramNames)));
    }

    statementsList.add(test);

    if (endMethod != null) {
      statementsList.add(new Statement(
          new MethodCall(endMethod.getName(), null)));
    }

    return statements;
  }

  /**
   * Currently, the benchmarking subsystem does not support async Benchmarks, so
   * we need to generate some additional code that prevents the user from
   * entering async mode in their Benchmark, even though we're using it
   * internally.
   * 
   * <p>
   * Generates the code for the "supportsAsync" functionality in the
   * translatable version of GWTTestCase. This includes:
   * <ul>
   * <li>the supportsAsync flag</li>
   * <li>the supportsAsync method</li>
   * <li>the privateDelayTestFinish method</li>
   * <li>the privateFinishTest method</li>
   * </ul>
   * </p>
   */
  private void generateAsyncCode() {
    SourceWriter writer = getSourceWriter();

    writer.println("private boolean supportsAsync;");
    writer.println();
    writer.println("public boolean supportsAsync() {");
    writer.println("  return supportsAsync;");
    writer.println("}");
    writer.println();
    writer.println("private void privateDelayTestFinish(int timeout) {");
    writer.println("  supportsAsync = true;");
    writer.println("  try {");
    writer.println("    delayTestFinish(timeout);");
    writer.println("  } finally {");
    writer.println("    supportsAsync = false;");
    writer.println("  }");
    writer.println("}");
    writer.println();
    writer.println("private void privateFinishTest() {");
    writer.println("  supportsAsync = true;");
    writer.println("  try {");
    writer.println("    finishTest();");
    writer.println("  } finally {");
    writer.println("    supportsAsync = false;");
    writer.println("  }");
    writer.println("}");
    writer.println();
  }

  /**
   * Generates an empty JSNI function to help us benchmark function call
   * overhead.
   * 
   * We prevent our empty function call from being inlined by the compiler by
   * making it a JSNI call. This works as of 1.3 RC 2, but smarter versions of
   * the compiler may be able to inline JSNI.
   * 
   * Things actually get pretty squirrelly in general when benchmarking function
   * call overhead, because, depending upon the benchmark, the compiler may
   * inline the benchmark into our benchmark loop, negating the cost we thought
   * we were measuring.
   * 
   * The best way to deal with this is for users to write micro-benchmarks such
   * that the micro-benchmark does significantly more work than a function call.
   * For example, if micro-benchmarking a function call, perform the function
   * call 100K times within the micro-benchmark itself.
   */
  private void generateEmptyFunc(SourceWriter writer) {
    writer.println("private native void " + EMPTY_FUNC + "() /*-{");
    writer.println("}-*/;");
    writer.println();
  }

  private Map<String, String> getAnnotationMetaData(JMethod method,
      MutableLong bound) throws UnableToCompleteException {

    IterationTimeLimit limit = method.getAnnotation(IterationTimeLimit.class);
    // noinspection SimplifiableIfStatement
    if (limit == null) {
      bound.value = getDefaultTimeout();
    } else {
      bound.value = limit.value();
    }

    Map<String, String> paramMetaData = new HashMap<String, String>();

    JParameter[] params = method.getParameters();

    for (JParameter param : params) {
      RangeField rangeField = param.getAnnotation(RangeField.class);
      if (rangeField != null) {
        String fieldName = rangeField.value();
        JClassType enclosingType = method.getEnclosingType();
        if (!fieldExists(enclosingType, fieldName)) {
          logger.log(TreeLogger.ERROR, "The RangeField annotation on "
              + enclosingType + " at " + method + " specifies a field, "
              + fieldName + ", which could not be found. Perhaps it is "
              + "mis-spelled?", null);
          throw new UnableToCompleteException();
        }
        paramMetaData.put(param.getName(), fieldName);
        continue;
      }
      RangeEnum rangeEnum = param.getAnnotation(RangeEnum.class);
      if (rangeEnum != null) {
        Class<? extends Enum<?>> enumClass = rangeEnum.value();
        // Handle inner classes
        String className = enumClass.getName().replace('$', '.');
        paramMetaData.put(param.getName(), className + ".values()");
        continue;
      }

      String msg = "The parameter, " + param.getName() + ", on method, "
          + method.getName() + ", must have it's range specified"
          + "by a RangeField or RangeEnum annotation.";
      logger.log(TreeLogger.ERROR, msg, null);
      throw new UnableToCompleteException();
    }

    return paramMetaData;
  }

  private synchronized long getDefaultTimeout()
      throws UnableToCompleteException {
    if (defaultTimeout != -1) {
      return defaultTimeout;
    }
    Method m = null;
    try {
      m = IterationTimeLimit.class.getDeclaredMethod("value");
      defaultTimeout = (Long) m.getDefaultValue();
    } catch (Exception e) {
      /*
       * Possibly one of: - NullPointerException (if somehow TimeLimit weren't
       * an annotation or value() didn't have a default). -
       * NoSuchMethodException if we somehow spelled value wrong -
       * TypeNotPresentException if somehow value were some type of Class that
       * couldn't be loaded instead of long It really doesn't make any
       * difference, because regardless of what could possibly have failed,
       * we'll still need to go this route.
       */
      logger.log(TreeLogger.ERROR,
          "Unable to retrieve the default benchmark time limit", e);
      throw new UnableToCompleteException();
    }

    return defaultTimeout;
  }

  private void implementParameterizedTestMethods()
      throws UnableToCompleteException {

    Map<String, JMethod> parameterizedMethods = getParameterizedTestMethods(
        getRequestedClass(), logger);
    SourceWriter sw = getSourceWriter();
    JClassType type = getRequestedClass();

    // For each test method, benchmark its:
    // a) overhead (setup + teardown + loop + function calls) and
    // b) execution time
    // for all possible parameter values
    for (Map.Entry<String, JMethod> entry : parameterizedMethods.entrySet()) {
      String name = entry.getKey();
      JMethod method = entry.getValue();
      JMethod beginMethod = getBeginMethod(type, method);
      JMethod endMethod = getEndMethod(type, method);

      sw.println("public void " + name + "() {");
      sw.indent();
      sw.println("  privateDelayTestFinish( 2000 );");
      sw.println();

      MutableLong bound = new MutableLong();
      Map<String, String> metaDataByParams = getAnnotationMetaData(method,
          bound);
      validateParams(method, metaDataByParams);

      JParameter[] methodParams = method.getParameters();
      List<String> paramNames = new ArrayList<String>(methodParams.length);
      for (int i = 0; i < methodParams.length; ++i) {
        paramNames.add(methodParams[i].getName());
      }

      sw.print("final java.util.List<Iterable<?>> iterables = java.util.Arrays.asList( new Iterable<?>[] { ");

      for (int i = 0; i < paramNames.size(); ++i) {
        String paramName = paramNames.get(i);
        sw.print(ITERABLE_ADAPTER_CLASS + ".toIterable("
            + metaDataByParams.get(paramName) + ")");
        if (i != paramNames.size() - 1) {
          sw.print(",");
        } else {
          sw.println("} );");
        }
        sw.print(" ");
      }

      sw.println("final " + PERMUTATION_ITERATOR_CLASS
          + " permutationIt = new " + PERMUTATION_ITERATOR_CLASS
          + "(iterables);\n" + DeferredCommand.class.getName()
          + ".addCommand( new " + IncrementalCommand.class.getName() + "() {\n"
          + "  public boolean execute() {\n"
          + "    privateDelayTestFinish( 10000 );\n"
          + "    if ( permutationIt.hasNext() ) {\n" + "      "
          + PERMUTATION_ITERATOR_CLASS
          + ".Permutation permutation = permutationIt.next();\n");

      for (int i = 0; i < methodParams.length; ++i) {
        JParameter methodParam = methodParams[i];
        String typeName = methodParam.getType().getQualifiedSourceName();
        String paramName = paramNames.get(i);
        sw.println("      " + typeName + " " + paramName + " = (" + typeName
            + ") permutation.getValues().get(" + i + ");");
      }

      final String setupTimingName = "__setupTiming";
      final String testTimingName = "__testTiming";

      sw.println("double " + setupTimingName + " = 0;");
      sw.println("double " + testTimingName + " = 0;");

      Statements setupBench = genBenchTarget(beginMethod, endMethod,
          paramNames, new Statement(new MethodCall(EMPTY_FUNC, null)));
      Statements testBench = genBenchTarget(beginMethod, endMethod, paramNames,
          new Statement(new MethodCall(method.getName(), paramNames)));

      StringBuffer recordResultsCode = new StringBuffer(BENCHMARK_RESULTS_CLASS
          + " results = __getOrCreateTestResult();\n" + TRIAL_CLASS
          + " trial = new " + TRIAL_CLASS + "();\n"
          + "trial.setRunTimeMillis( " + testTimingName + " - "
          + setupTimingName + " );\n"
          + "java.util.Map<String, String> variables = trial.getVariables();\n");

      for (String paramName : paramNames) {
        recordResultsCode.append("variables.put( \"").append(paramName).append(
            "\", ").append(paramName).append(".toString() );\n");
      }

      recordResultsCode.append("results.getTrials().add( trial )");
      Statements recordCode = new Statement(recordResultsCode.toString());

      Statements breakCode = new Statement("  permutationIt.skipCurrentRange()");
      setupBench = benchmark(setupBench, setupTimingName, 0, null, breakCode);
      testBench = benchmark(testBench, testTimingName, bound.value, recordCode,
          breakCode);

      Statements testAndSetup = new StatementsList();
      testAndSetup.getStatements().addAll(setupBench.getStatements());
      testAndSetup.getStatements().addAll(testBench.getStatements());

      sw.println(testAndSetup.toString());

      sw.println("      return true;\n" + "    }\n"
          + "    privateFinishTest();\n" + "    return false;\n" + "  }\n"
          + "} );\n");

      sw.outdent();
      sw.println("}");
    }
  }

  /**
   * Overrides the zero-arg test methods that don't have any
   * overloaded/parameterized versions.
   * 
   * TODO(tobyr) This code shares a lot of similarity with
   * implementParameterizedTestMethods and they should probably be refactored
   * into a single function.
   */
  private void implementZeroArgTestMethods() throws UnableToCompleteException {
    Map<String, JMethod> zeroArgMethods = getNotOverloadedTestMethods(getRequestedClass());
    SourceWriter sw = getSourceWriter();
    JClassType type = getRequestedClass();

    for (Map.Entry<String, JMethod> entry : zeroArgMethods.entrySet()) {
      String name = entry.getKey();
      JMethod method = entry.getValue();
      JMethod beginMethod = getBeginMethod(type, method);
      JMethod endMethod = getEndMethod(type, method);

      sw.println("public void " + name + "() {");
      sw.indent();

      final String setupTimingName = "__setupTiming";
      final String testTimingName = "__testTiming";

      sw.println("double " + setupTimingName + " = 0;");
      sw.println("double " + testTimingName + " = 0;");

      Statements setupBench = genBenchTarget(beginMethod, endMethod,
          Collections.<String> emptyList(), new Statement(new MethodCall(
              EMPTY_FUNC, null)));

      StatementsList testStatements = new StatementsList();
      testStatements.getStatements().add(
          new Statement(new MethodCall("super." + method.getName(), null)));
      Statements testBench = genBenchTarget(beginMethod, endMethod,
          Collections.<String> emptyList(), testStatements);

      String recordResultsCode = BENCHMARK_RESULTS_CLASS
          + " results = __getOrCreateTestResult();\n" + TRIAL_CLASS
          + " trial = new " + TRIAL_CLASS + "();\n"
          + "trial.setRunTimeMillis( " + testTimingName + " - "
          + setupTimingName + " );\n" + "results.getTrials().add( trial )";

      Statements breakCode = new Statement("  break " + ESCAPE_LOOP);

      setupBench = benchmark(setupBench, setupTimingName, 0, null, breakCode);
      testBench = benchmark(testBench, testTimingName, getDefaultTimeout(),
          new Statement(recordResultsCode), breakCode);
      ForLoop loop = (ForLoop) testBench.getStatements().get(0);
      loop.setLabel(ESCAPE_LOOP);

      sw.println(setupBench.toString());
      sw.println(testBench.toString());

      sw.outdent();
      sw.println("}");
    }
  }

  private void validateParams(JMethod method, Map<String, String> params)
      throws UnableToCompleteException {
    JParameter[] methodParams = method.getParameters();
    for (JParameter methodParam : methodParams) {
      String paramName = methodParam.getName();
      String paramValue = params.get(paramName);

      if (paramValue == null) {
        String msg = "Could not find the meta data attribute "
            + BENCHMARK_PARAM_META + " for the parameter " + paramName
            + " on method " + method.getName();
        logger.log(TreeLogger.ERROR, msg, null);
        throw new UnableToCompleteException();
      }
    }
  }
}
