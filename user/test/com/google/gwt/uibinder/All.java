package com.google.gwt.uibinder;

import com.google.gwt.junit.tools.GWTTestSuite;

import junit.framework.Test;

/**
 * Convenience suite for running both UiBinder suites from an IDE. Its name
 * does not end in Suite to keep ant from running it redundantly.
 */
public class All {
  public static Test suite() {
    GWTTestSuite suite = new GWTTestSuite("All UiBinder tests");

    suite.addTest(UiBinderJreSuite.suite());
    suite.addTest(UiBinderGwtSuite.suite());

    return suite;
  }

  private All() {
  }
}