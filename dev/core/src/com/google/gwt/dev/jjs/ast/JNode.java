// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.jjs.ast;

import com.google.gwt.dev.jjs.impl.ToStringGenerationVisitor;
import com.google.gwt.dev.util.TextOutputOnCharArray;

/**
 * Base class for all visitable AST nodes.
 */
public abstract class JNode implements JVisitable {
  /*
   * The current visitor pattern uses reflection, but it could be 
   * replaced by direct code for better performance.
   */
  protected final JProgram fProgram;

  protected JNode(JProgram program) {
    if (program == null) {
      assert (this instanceof JProgram);
      fProgram = (JProgram) this;
    } else {
      fProgram = program;
    }
  }

  // Causes source generation to delegate to the one visitor
  public final String toString() {
    TextOutputOnCharArray p = new TextOutputOnCharArray(false);
    ToStringGenerationVisitor v = new ToStringGenerationVisitor(p);
    traverse(v);
    return new String(p.getText());
  }
}
