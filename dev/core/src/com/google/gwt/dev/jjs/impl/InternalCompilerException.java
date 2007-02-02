/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.dev.jjs.impl;

import com.google.gwt.dev.jjs.ast.JNode;

import org.eclipse.jdt.internal.compiler.ast.ASTNode;

/**
 * Indicates the compiler encountered an unexpected and unsupported state of
 * operation.
 */
public class InternalCompilerException extends RuntimeException {

  public InternalCompilerException(String message) {
    super(message);
  }

  public InternalCompilerException(String message, Throwable cause) {
    super(message, cause);
  }

  public void addNode(ASTNode node) {
    // TODO Auto-generated method stub
  }

  public void addNode(JNode node) {
    // TODO Auto-generated method stub
  }

}
