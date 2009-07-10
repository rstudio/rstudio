package com.google.gwt.junit;

import com.google.gwt.junit.client.BenchmarkTest;
import com.google.gwt.junit.client.GWTTestCaseTest;
import com.google.gwt.junit.remote.BrowserManagerServerTest;
import com.google.gwt.junit.tools.GWTTestSuite;

import junit.framework.Test;

/**
 * Tests of the junit package.
 */
public class JUnitSuite {
  public static Test suite() {
    GWTTestSuite suite = new GWTTestSuite("Test for suite for com.google.gwt.junit");

    suite.addTestSuite(FakeMessagesMakerTest.class);

    // client
    // Suppressed due to flakiness on Linux
    // suite.addTestSuite(BenchmarkTest.class);
    suite.addTestSuite(GWTTestCaseTest.class);

    // These two are intended only to be run manually. See class comments
    // suite.addTestSuite(ParallelRemoteTest.class);
    // suite.addTestSuite(TestManualAsync.class);

    // remote
    // Run manually only, launches servers that die on port contention
    // suite.addTestSuite(BrowserManagerServerTest.class);

    return suite;
  }
}
