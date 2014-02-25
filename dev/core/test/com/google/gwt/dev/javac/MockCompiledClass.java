package com.google.gwt.dev.javac;

import com.google.gwt.dev.cfg.LibraryGroupTest;

/**
 * Used by {@link LibraryGroupTest} and {@link MockCompilationUnit}.
 */
public class MockCompiledClass extends CompiledClass {

  public MockCompiledClass(CompiledClass enclosingClass, String internalName, String sourceName) {
    super(enclosingClass, internalName, sourceName);
  }

  @Override
  public byte[] getBytes() {
    return new byte[0];
  }
}