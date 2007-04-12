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
package com.google.gwt.junit.client;

/**
 * A type of {@link com.google.gwt.junit.client.GWTTestCase} which specifically
 * records performance results. {@link com.google.gwt.junit.client.Benchmark}s
 * have additional functionality above and beyond GWT's JUnit support for
 * standard <code>TestCases</code>.
 *
 * <ul>
 * <li>In a single <code>JUnit</code> run, the results of all executed
 * benchmarks are collected and stored in an XML report viewable with the
 * <code>benchmarkViewer</code>.</li>
 *
 * <li>GWT automatically removes jitter from your benchmark methods by running
 * them for a minimum period of time (150ms). GWT also optionally limits your
 * benchmark execution to a maximum period of time (1000ms).</li>
 *
 * <li>GWT supports "begin" and "end" test methods that separate setup and
 * teardown costs from the actual work being benchmarked. Simply name your
 * functions "begin[TestMethodName]" and "end[TestMethodName]" and they will
 * be executed before and after every execution of your test method. The
 * timings of these setup methods are not included in the test results.</li>
 *
 * <li>GWT supports test methods that have parameters. GWT will execute each
 * benchmark method multiple times in order to exhaustively test all the possible
 * combinations of parameter values. For each parameter that your test method
 * accepts, it should document it with the annotation,
 * <code>&#64;gwt.benchmark.param</code>.
 *
 * <p>The syntax for gwt.benchmark.param is
 * <code>&lt;param name&gt; = &lt;Iterable&gt;</code>. For example,
 *
 * <pre>
 * &#64;gwt.benchmark.param where = java.util.Arrays.asList(
 *   new Position[] { Position.BEGIN, Position.END, Position.VARIED } )
 * &#64;gwt.benchmark.param size -limit = insertRemoveRange
 * public void testArrayListRemoves(Position where, Integer size) { ... }
 * </pre></p>
 *
 * <p>In this example, the annotated function is executed with all the possible
 * permutations of <code>Position = (BEGIN, END, and VARIED)</code> and
 * <code>insertRemoveRange = IntRange( 64, Integer.MAX_VALUE, "*", 2 )</code>.
 * </p>
 *
 * <p>This particular example also demonstrates how GWT can automatically limit
 * the number of executions of your test. Your final parameter (in this example,
 * size) can optionally be decorated with -limit to indicate to GWT that
 * it should stop executing additional permutations of the test when the
 * execution time becomes too long (over 1000ms). So, in this example,
 * for each value of <code>Position</code>, <code>testArrayListRemoves</code>
 * will be executed for increasing values of <code>size</code> (beginning with
 * 64 and increasing in steps of 2), until either it reaches
 * <code>Integer.MAX_VALUE</code> or the execution time for the last
 * permutation is > 1000ms.</p>
 * </li>
 * </ul>
 *
 * <p>{@link Benchmark}s support the following annotations on each test method
 * in order to decorate each test with additional information useful for
 * reporting.</p>
 *
 * <ul>
 * <li>&#64;gwt.benchmark.category - The class name of the {@link Category} the
 * benchmark belongs to. This property may also be set at the
 * {@link com.google.gwt.junit.client.Benchmark} class level.</li>
 * </ul>
 *
 * <p>Please note that {@link Benchmark}s do not currently support asynchronous
 * testing mode. Calling
 * {@link com.google.gwt.junit.client.GWTTestCase#delayTestFinish(int)}
 * or {@link com.google.gwt.junit.client.GWTTestCase#finishTest()} will result
 * in an UnsupportedOperationException.</p>
 *
 * <h2>Examples of benchmarking in action</h2>
 *
 * <h3>A simple benchmark example</h3>
 * {@link com.google.gwt.examples.benchmarks.AllocBenchmark} is a simple example
 * of a basic benchmark that doesn't take advantage of most of benchmarking's
 * advanced features.
 *
 * {@example com.google.gwt.examples.benchmarks.AllocBenchmark}
 *
 * <h3>An advanced benchmark example</h3>
 * {@link com.google.gwt.examples.benchmarks.ArrayListAndVectorBenchmark} is a more
 * sophisticated example of benchmarking. It demonstrates the use of "begin"
 * and "end" test methods, parameterized test methods, and automatic
 * test execution limits.
 *
 *
 * {@example com.google.gwt.examples.benchmarks.ArrayListAndVectorBenchmark}
 */
public abstract class Benchmark extends GWTTestCase {

  /**
   * The name of the system property that specifies the location
   * where benchmark reports are both written to and read from.
   * Its value is <code>com.google.gwt.junit.reportPath</code>.
   *
   * If this system property is not set, the path defaults to the user's
   * current working directory.
   */
  public static final String REPORT_PATH = "com.google.gwt.junit.reportPath";
}
