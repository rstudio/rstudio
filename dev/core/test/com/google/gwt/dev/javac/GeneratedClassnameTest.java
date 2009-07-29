package com.google.gwt.dev.javac;

import junit.framework.TestCase;

public class GeneratedClassnameTest extends TestCase {

  /**
   * Test if {@link CompilingClassLoader#isClassnameGenerated(String)} works
   * correctly.
   */
  public void testGeneratedClassnames() {
    String namesToAccept[] = {
        "Test$1", "Test$10", "Test$Foo$1", "Test$1$Foo", "Test$10$Foo",
        "$$345", "Test$1$Foo$", "Test$1Foo", "Test$2Foo", "Test$Foo$1Bar"};
    String namesToReject[] = {"Test1", "TestFoo", "Test$Foo$Bar", "$345"};

    for (String name : namesToAccept) {
      assertTrue("className = " + name + " should have been accepted",
          CompilationUnit.isClassnameGenerated(name));
    }

    for (String name : namesToReject) {
      assertFalse("className = " + name + " should not have been accepted",
          CompilationUnit.isClassnameGenerated(name));
    }
  }
}
