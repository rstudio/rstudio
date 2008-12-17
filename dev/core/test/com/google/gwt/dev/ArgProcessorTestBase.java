package com.google.gwt.dev;

import com.google.gwt.util.tools.Utility;

import junit.framework.TestCase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;

/**
 * Base class for argument processor testing.
 */
public abstract class ArgProcessorTestBase extends TestCase {

  private static class MockOutputStream extends OutputStream {
    private boolean isEmpty = true;

    public boolean isEmpty() {
      return isEmpty;
    }

    @Override
    public void write(byte[] b, int off, int len) throws IOException {
      isEmpty = false;
    }

    @Override
    public void write(int b) throws IOException {
      isEmpty = false;
    }
  }

  /*
   * The "compute installation directory" dance.
   */
  static {
    String oldValue = System.getProperty("gwt.devjar");
    System.setProperty("gwt.devjar", "gwt-dev-windows.jar");
    Utility.getInstallPath();
    if (oldValue == null) {
      System.getProperties().remove("gwt.devjar");
    } else {
      System.setProperty("gwt.devjar", oldValue);
    }
  }

  protected static void assertProcessFailure(ArgProcessorBase argProcessor,
      String... args) {
    PrintStream oldErrStream = System.err;
    MockOutputStream myErrStream = new MockOutputStream();
    try {
      System.setErr(new PrintStream(myErrStream, true));
      assertFalse(argProcessor.processArgs(args));
    } finally {
      System.setErr(oldErrStream);
    }
    assertFalse(myErrStream.isEmpty());
  }

  protected static void assertProcessSuccess(ArgProcessorBase argProcessor,
      String... args) {
    PrintStream oldErrStream = System.err;
    ByteArrayOutputStream myErrStream = new ByteArrayOutputStream();
    try {
      System.setErr(new PrintStream(myErrStream, true));
      if (!argProcessor.processArgs(args)) {
        fail(new String(myErrStream.toByteArray()));
      }
      assertEquals(0, myErrStream.size());
    } finally {
      System.setErr(oldErrStream);
    }
  }
}
