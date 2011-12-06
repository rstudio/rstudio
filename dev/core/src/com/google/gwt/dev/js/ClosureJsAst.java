/*
 * Copyright 2011 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.google.gwt.dev.js;

import com.google.gwt.thirdparty.guava.common.base.Preconditions;
import com.google.gwt.thirdparty.javascript.jscomp.AbstractCompiler;
import com.google.gwt.thirdparty.javascript.jscomp.SourceAst;
import com.google.gwt.thirdparty.javascript.jscomp.SourceFile;
import com.google.gwt.thirdparty.javascript.rhino.InputId;
import com.google.gwt.thirdparty.javascript.rhino.Node;

/**
 * Maps the JavaScript AST to a Closure Compiler input source.
 */
public class ClosureJsAst implements SourceAst {

  private static final long serialVersionUID = 1L;

  /*
   * Root node of internal JS Compiler AST which represents the same source. In order to get the
   * tree, getAstRoot() has to be called.
   */
  private Node root;
  private final InputId inputId;

  public ClosureJsAst(InputId inputId, Node root) {
    Preconditions.checkNotNull(root);
    this.inputId = inputId;
    this.root = root;
  }

  @Override
  public void clearAst() {
    root = null;
  }

  @Override
  public Node getAstRoot(AbstractCompiler compiler) {
    return root;
  }

  @Override
  public InputId getInputId() {
    return inputId;
  }

  @Override
  public SourceFile getSourceFile() {
    return null;
  }

  public String getSourceName() {
    return null;
  }

  @Override
  public void setSourceFile(SourceFile file) {
    throw new UnsupportedOperationException(
        "ClosureJsAst cannot be associated with a SourceFile instance.");
  }
}
