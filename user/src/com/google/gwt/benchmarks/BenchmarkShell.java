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
package com.google.gwt.benchmarks;

import com.google.gwt.benchmarks.client.Benchmark;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.junit.JUnitShell;
import com.google.gwt.junit.client.GWTTestCase;

import junit.framework.TestCase;
import junit.framework.TestResult;

import java.io.File;
import java.util.Date;

/**
 * This class is responsible for hosting BenchMarks test case execution.
 * 
 * @see JUnitShell
 */
public class BenchmarkShell {

  /**
   * Executes shutdown logic for JUnitShell
   * 
   * Sadly, there's no simple way to know when all unit tests have finished
   * executing. So this class is registered as a VM shutdown hook so that work
   * can be done at the end of testing - for example, writing out the reports.
   */
  private static class Shutdown implements Runnable {

    public void run() {
      try {
        String reportPath = System.getProperty(Benchmark.REPORT_PATH);
        if (reportPath == null || reportPath.trim().equals("")) {
          reportPath = System.getProperty("user.dir");
        }
        report.generate(reportPath + File.separator + "report-"
            + new Date().getTime() + ".xml");
      } catch (Exception e) {
        // It really doesn't matter how we got here.
        // Regardless of the failure, the VM is shutting down.
        e.printStackTrace();
      }
    }
  }

  /**
   * The result of benchmark runs.
   */
  private static BenchmarkReport report = new BenchmarkReport();

  private static boolean shutdownHookSet = false;

  /**
   * Called by {@link com.google.gwt.benchmarks.rebind.BenchmarkGenerator} to
   * add test meta data to the test report.
   * 
   * @return The {@link BenchmarkReport} that belongs to the singleton {@link
   *         JUnitShell}, or <code>null</code> if no such singleton exists.
   */
  public static BenchmarkReport getReport() {
    return report;
  }

  /**
   * @deprecated use {@link #runTest(GWTTestCase, TestResult)} instead
   */
  @Deprecated
  public static void runTest(String moduleName, TestCase testCase,
      TestResult testResult) throws UnableToCompleteException {
    if (!shutdownHookSet) {
      shutdownHookSet = true;
      Runtime.getRuntime().addShutdownHook(new Thread(new Shutdown()));
    }
    JUnitShell.runTest(moduleName, testCase, testResult);
  }

  public static void runTest(GWTTestCase testCase,
      TestResult testResult) throws UnableToCompleteException {
    if (!shutdownHookSet) {
      shutdownHookSet = true;
      Runtime.getRuntime().addShutdownHook(new Thread(new Shutdown()));
    }
    JUnitShell.runTest(testCase, testResult);
  }

  private BenchmarkShell() {
  }
}
