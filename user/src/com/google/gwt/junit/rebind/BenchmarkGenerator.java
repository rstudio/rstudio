/*
 * Copyright 2007 Google Inc.
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
package com.google.gwt.junit.rebind;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.junit.JUnitShell;
import com.google.gwt.dev.generator.ast.ForLoop;
import com.google.gwt.dev.generator.ast.MethodCall;
import com.google.gwt.dev.generator.ast.Statement;
import com.google.gwt.dev.generator.ast.Statements;
import com.google.gwt.dev.generator.ast.StatementsList;
import com.google.gwt.user.rebind.SourceWriter;

import java.util.Map;
import java.util.List;
import java.util.Set;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.Collections;

/**
 * Implements a generator for Benchmark classes. Benchmarks require additional
 * code generation above and beyond standard JUnit tests.
 */
public class BenchmarkGenerator extends JUnitTestCaseStubGenerator {

  private static class MutableBoolean {
    boolean value;
  }

  private static final String BEGIN_PREFIX = "begin";

  private static final String BENCHMARK_PARAM_META = "gwt.benchmark.param";

  private static final String EMPTY_FUNC = "__emptyFunc";

  private static final String END_PREFIX = "end";

  private static final String ESCAPE_LOOP = "__escapeLoop";

  /**
   * Returns all the zero-argument JUnit test methods that do not have
   * overloads.
   *
   * @return Map<String,JMethod>
   */
  public static Map<String, JMethod> getNotOverloadedTestMethods(JClassType requestedClass) {
    Map<String, List<JMethod>> methods =
      getAllMethods(requestedClass, new MethodFilter() {
      public boolean accept(JMethod method) {
        return isJUnitTestMethod(method, true);
      }
    });
    
    // Create a new map to store the methods
    Map<String, JMethod> notOverloadedMethods = new HashMap<String, JMethod>();
    for (Map.Entry<String, List<JMethod>> entry : methods.entrySet()) {
      List<JMethod> methodOverloads = entry.getValue();
      if (methodOverloads.size() <= 1) {
        notOverloadedMethods.put(entry.getKey(), methodOverloads.get(0));
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
  public static Map<String, JMethod> getParameterizedTestMethods(JClassType requestedClass,
      TreeLogger logger) {

    Map<String, List<JMethod>> testMethods =
      getAllMethods(requestedClass, new MethodFilter() {
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
            + " has more than one overloaded version.\n" +
            "It will not be included in the test case execution.";
        logger.log(TreeLogger.WARN, msg, null);
        continue;
      }

      if (methods.size() == 1) {
        JMethod method = methods.get(0);
        if (method.getParameters().length != 0) {
          /* User probably goofed - otherwise why create a test method with
           * arguments but not the corresponding no-argument version? Would be
           * better if our benchmarking system didn't require the no-argument
           * test to make the benchmarks run correctly (JUnit artifact).
           */
          String msg = requestedClass + "." + name
              + " does not have a zero-argument overload.\n" +
              "It will not be included in the test case execution.";
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
            + " does not have a zero-argument overload.\n" +
            "It will not be included in the test case execution.";
        logger.log(TreeLogger.WARN, msg, null);
        continue;
      }

      overloadedMethods.put(entry.getKey(), overloadedMethod);
    }

    return overloadedMethods;
  }

  private static JMethod getBeginMethod(JClassType type, String name) {
    StringBuffer methodName = new StringBuffer(name);
    methodName.replace(0, "test".length(), BEGIN_PREFIX);
    return getMethod(type, methodName.toString());
  }

  private static JMethod getEndMethod(JClassType type, String name) {
    StringBuffer methodName = new StringBuffer(name);
    methodName.replace(0, "test".length(), END_PREFIX);
    return getMethod(type, methodName.toString());
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
    JUnitShell.getReport().addBenchmark(getRequestedClass(), getTypeOracle());
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
      boolean generateEscape, Statements recordCode, Statements breakCode) {
    Statements benchmarkCode = new StatementsList();
    List<Statements> benchStatements = benchmarkCode.getStatements();

    ForLoop loop = new ForLoop("int numLoops = 1", "true", "");
    benchStatements.add(loop);
    List<Statements> loopStatements = loop.getStatements();

    loopStatements
        .add(new Statement("long start = System.currentTimeMillis()"));
    ForLoop runLoop = new ForLoop("int i = 0", "i < numLoops", "++i", stmts);
    loopStatements.add(runLoop);

    // Put the rest of the code in 1 big statement to simplify things
    String benchCode =
        "long duration = System.currentTimeMillis() - start;\n\n" +

        "if ( duration < 150 ) {\n" +
        "  numLoops += numLoops;\n" +
        "  continue;\n" +
        "}\n\n" +

        "double durationMillis = duration * 1.0;\n" +
        "double numLoopsAsDouble = numLoops * 1.0;\n" +
        timeMillisName + " = durationMillis / numLoopsAsDouble";

    loopStatements.add(new Statement(benchCode));

    if (recordCode != null) {
      loopStatements.add(recordCode);
    }

    if (generateEscape) {
      loopStatements.add(new Statement(
          "if ( numLoops == 1 && duration > 1000 ) {\n" +
            breakCode.toString() + "\n" +
          "}\n\n"
      ));
    }

    loopStatements.add(new Statement("break"));

    return benchmarkCode;
  }

  /**
   * Generates code that executes <code>statements</code> for all possible
   * values of <code>params</code>. Exports a label named ESCAPE_LOOP that
   * points to the the "inner loop" that should be escaped to for a limited
   * variable.
   *
   * @return the generated code
   * TODO: Is this used anywhere?
   */
  private Statements executeForAllValues(JParameter[] methodParams,
      Map<String, String> params, Statements statements) {
    Statements root = new StatementsList();
    Statements currentContext = root;

    // Profile the setup and teardown costs for this test method
    // but only if 1 of them exists.
    for (int i = 0; i < methodParams.length; ++i) {
      JParameter methodParam = methodParams[i];
      String paramName = methodParam.getName();
      String paramValue = params.get(paramName);
      String typeName = methodParam.getType().getQualifiedSourceName();

      String iteratorName = "it_" + paramName;
      String initializer = "java.util.Iterator<" + typeName + "> " + iteratorName + " = "
          + paramValue + ".iterator()";
      ForLoop loop = new ForLoop(initializer, iteratorName + ".hasNext()", "");
      if (i == methodParams.length - 1) {
        loop.setLabel(ESCAPE_LOOP);
      }
      currentContext.getStatements().add(loop);
      loop.getStatements().add(new Statement(typeName + " " + paramName + " = "
          + iteratorName + ".next()"));
      currentContext = loop;
    }

    currentContext.getStatements().add(statements);

    return root;
  }

  private Statements genBenchTarget(JMethod beginMethod, JMethod endMethod,
      List<String> paramNames, Statements test) {
    Statements statements = new StatementsList();
    List<Statements> statementsList = statements.getStatements();

    if (beginMethod != null) {
      statementsList.add(
          new Statement(new MethodCall(beginMethod.getName(), paramNames)));
    }

    statementsList.add(test);

    if (endMethod != null) {
      statementsList
          .add(new Statement(new MethodCall(endMethod.getName(), null)));
    }

    return statements;
  }

  /**
   * Currently, the benchmarking subsystem does not support async Benchmarks,
   * so we need to generate some additional code that prevents the user
   * from entering async mode in their Benchmark, even though we're using
   * it internally.
   *
   * Generates the code for the "supportsAsync" functionality in the
   * translatable version of GWTTestCase. This includes:
   *
   *   - the supportsAsync flag
   *   - the supportsAsync method
   *   - the privateDelayTestFinish method
   *   - the privateFinishTest method
   *
   */
  private void generateAsyncCode() {
    SourceWriter writer = getSourceWriter();

    writer.println( "private boolean supportsAsync;" );
    writer.println();
    writer.println( "public boolean supportsAsync() {");
    writer.println( "  return supportsAsync;");
    writer.println( "}");
    writer.println();
    writer.println( "private void privateDelayTestFinish(int timeout) {" );
    writer.println( "  supportsAsync = true;");
    writer.println( "  try {");
    writer.println( "    delayTestFinish(timeout);");
    writer.println( "  } finally {");
    writer.println( "    supportsAsync = false;");
    writer.println( "  }");
    writer.println( "}");
    writer.println();
    writer.println( "private void privateFinishTest() {" );
    writer.println( "  supportsAsync = true;");
    writer.println( "  try {");
    writer.println( "    finishTest();");
    writer.println( "  } finally {");
    writer.println( "    supportsAsync = false;");
    writer.println( "  }");
    writer.println( "}");
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
   * Things actually get pretty squirrely in general when benchmarking function
   * call overhead, because, depending upon the benchmark, the compiler may
   * inline the benchmark into our benchmark loop, negating the cost we thought
   * we were measuring.
   *
   * The best way to deal with this is for users to write micro-benchmarks such
   * that the micro-benchmark does significantly more work than a function call.
   * For example, if micro-benchmarking a function call, perform the function
   * call 100K times within the microbenchmark itself.
   */
  private void generateEmptyFunc(SourceWriter writer) {
    writer.println("private native void " + EMPTY_FUNC + "() /*-{");
    writer.println("}-*/;");
    writer.println();
  }

  private Map<String,String> getParamMetaData(JMethod method,
      MutableBoolean isBounded) throws UnableToCompleteException {
    Map<String,String> params = new HashMap<String,String>();

    String[][] allValues = method.getMetaData(BENCHMARK_PARAM_META);

    if (allValues == null) {
      return params;
    }

    for (int i = 0; i < allValues.length; ++i) {
      String[] values = allValues[i];
      StringBuffer result = new StringBuffer();
      for (int j = 0; j < values.length; ++j) {
        result.append(values[j]);
        result.append(" ");
      }
      String expr = result.toString();
      String[] lhsAndRhs = expr.split("=");
      String paramName = lhsAndRhs[0].trim();
      String[] nameExprs = paramName.split(" ");
      if (nameExprs.length > 1 && nameExprs[1].equals("-limit")) {
        paramName = nameExprs[0];
        // Make sure this is the last parameter
        JParameter[] parameters = method.getParameters();
        if (! parameters[parameters.length - 1].getName().equals(paramName)) {
          JClassType cls = method.getEnclosingType();
          String msg = "Error at " + cls + "." + method.getName() + "\n" +
              "Only the last parameter of a method can be marked with the -limit flag.";
          logger.log(TreeLogger.ERROR, msg, null);
          throw new UnableToCompleteException();
        }

        isBounded.value = true;
      }
      String paramValue = lhsAndRhs[1].trim();
      params.put(paramName, paramValue);
    }

    return params;
  }

  private void implementParameterizedTestMethods() throws
      UnableToCompleteException {

    Map<String,JMethod> parameterizedMethods = getParameterizedTestMethods(
        getRequestedClass(), logger);
    SourceWriter sw = getSourceWriter();
    JClassType type = getRequestedClass();

    // For each test method, benchmark its:
    //   a) overhead (setup + teardown + loop + function calls) and
    //   b) execution time
    // for all possible parameter values
    for (Map.Entry<String,JMethod> entry : parameterizedMethods.entrySet() ) {
      String name = entry.getKey();
      JMethod method = entry.getValue();
      JMethod beginMethod = getBeginMethod(type, name);
      JMethod endMethod = getEndMethod(type, name);

      sw.println("public void " + name + "() {");
      sw.indent();
      sw.println("  privateDelayTestFinish( 2000 );");
      sw.println();

      MutableBoolean isBounded = new MutableBoolean();
      Map<String, String> params = getParamMetaData(method, isBounded);
      validateParams(method, params);

      JParameter[] methodParams = method.getParameters();
      List<String> paramNames = new ArrayList<String>(methodParams.length);
      for (int i = 0; i < methodParams.length; ++i) {
        paramNames.add(methodParams[i].getName());
      }

      List<String> paramValues = new ArrayList<String>(methodParams.length);
      for (int i = 0; i < methodParams.length; ++i) {
        paramValues.add(params.get(methodParams[i].getName()));
      }

      sw.print( "final java.util.List<Range<?>> ranges = java.util.Arrays.asList( new com.google.gwt.junit.client.Range<?>[] { " );

      for (int i = 0; i < paramNames.size(); ++i) {
        String paramName = paramNames.get(i);
        sw.print( params.get(paramName) );
        if (i != paramNames.size() - 1) {
          sw.print( ",");
        } else {
          sw.println( "} );" );
        }
        sw.print( " " );
      }

      sw.println(
          "final com.google.gwt.junit.client.impl.PermutationIterator permutationIt = new com.google.gwt.junit.client.impl.PermutationIterator( ranges );\n" +
          "com.google.gwt.user.client.DeferredCommand.addCommand( new com.google.gwt.user.client.IncrementalCommand() {\n" +
          "  public boolean execute() {\n" +
          "    privateDelayTestFinish( 10000 );\n" +
          "    if ( permutationIt.hasNext() ) {\n" +
          "      com.google.gwt.junit.client.impl.PermutationIterator.Permutation permutation = permutationIt.next();\n"
      );

      for (int i = 0; i < methodParams.length; ++i) {
        JParameter methodParam = methodParams[i];
        String typeName = methodParam.getType().getQualifiedSourceName();
        String paramName = paramNames.get(i);
        sw.println( "      " + typeName + " " + paramName + " = (" +
                    typeName + ") permutation.getValues().get(" + i + ");");
      }

      final String setupTimingName = "__setupTiming";
      final String testTimingName = "__testTiming";

      sw.println("double " + setupTimingName + " = 0;");
      sw.println("double " + testTimingName + " = 0;");

      Statements setupBench = genBenchTarget(beginMethod, endMethod, paramNames,
          new Statement(new MethodCall(EMPTY_FUNC, null)));
      Statements testBench = genBenchTarget(beginMethod, endMethod, paramNames,
          new Statement(new MethodCall(method.getName(), paramNames)));

      StringBuffer recordResultsCode = new StringBuffer(
          "com.google.gwt.junit.client.TestResults results = getTestResults();\n" +
          "com.google.gwt.junit.client.Trial trial = new com.google.gwt.junit.client.Trial();\n" +
          "trial.setRunTimeMillis( " + testTimingName + " - " + setupTimingName + " );\n" +
          "java.util.Map<String, String> variables = trial.getVariables();\n");

      for (String paramName : paramNames) {
        recordResultsCode.append("variables.put( \"")
            .append(paramName)
            .append("\", ")
            .append(paramName)
            .append(".toString() );\n");
      }

      recordResultsCode.append("results.getTrials().add( trial )");
      Statements recordCode = new Statement(recordResultsCode.toString());

      Statements breakCode = new Statement( "  permutationIt.skipCurrentRange()" );
      setupBench = benchmark(setupBench, setupTimingName, false, null, breakCode);
      testBench = benchmark(testBench, testTimingName, isBounded.value, recordCode, breakCode);

      Statements testAndSetup = new StatementsList();
      testAndSetup.getStatements().addAll(setupBench.getStatements());
      testAndSetup.getStatements().addAll(testBench.getStatements());

      sw.println( testAndSetup.toString() );

      sw.println(
          "      return true;\n" +
          "    }\n" +
          "    privateFinishTest();\n" +
          "    return false;\n" +
          "  }\n" +
          "} );\n"
      );

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
  private void implementZeroArgTestMethods() {
    Map<String, JMethod> zeroArgMethods =
      getNotOverloadedTestMethods(getRequestedClass());
    SourceWriter sw = getSourceWriter();
    JClassType type = getRequestedClass();

    for (Map.Entry<String, JMethod> entry : zeroArgMethods.entrySet()) {
      String name = entry.getKey();
      JMethod method = entry.getValue();
      JMethod beginMethod = getBeginMethod(type, name);
      JMethod endMethod = getEndMethod(type, name);

      sw.println("public void " + name + "() {");
      sw.indent();

      final String setupTimingName = "__setupTiming";
      final String testTimingName = "__testTiming";

      sw.println("double " + setupTimingName + " = 0;");
      sw.println("double " + testTimingName + " = 0;");

      Statements setupBench = genBenchTarget(beginMethod, endMethod,
          Collections.<String>emptyList(),
          new Statement(new MethodCall(EMPTY_FUNC, null)));

      StatementsList testStatements = new StatementsList();
      testStatements.getStatements().add(
          new Statement(new MethodCall("super." + method.getName(), null)));
      Statements testBench = genBenchTarget(beginMethod, endMethod,
          Collections.<String>emptyList(), testStatements);

      String recordResultsCode =
          "com.google.gwt.junit.client.TestResults results = getTestResults();\n"  +
          "com.google.gwt.junit.client.Trial trial = new com.google.gwt.junit.client.Trial();\n"  +
          "trial.setRunTimeMillis( " + testTimingName + " - " + setupTimingName + " );\n" +
          "results.getTrials().add( trial )";

      Statements breakCode = new Statement( "  break " + ESCAPE_LOOP );

      setupBench = benchmark(setupBench, setupTimingName, false, null, breakCode);
      testBench = benchmark(testBench, testTimingName, true,
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
            + BENCHMARK_PARAM_META +
            " for the parameter " + paramName + " on method " + method
            .getName();
        logger.log(TreeLogger.ERROR, msg, null);
        throw new UnableToCompleteException();
      }
    }
  }
}
