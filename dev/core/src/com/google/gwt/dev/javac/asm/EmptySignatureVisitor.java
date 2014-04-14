/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.dev.javac.asm;

import com.google.gwt.dev.asm.Opcodes;
import com.google.gwt.dev.asm.signature.SignatureVisitor;

/**
 * Signature visitor that does nothing.
 *
 * Unlike the ASM-provided EmptyVisitor (which does not implement
 * SignatureVisitor), this class does not pass itself to unimplemented
 * sub-visitors, so that a subclass doesn't have to worry about calls for
 * something under a sub-visitor it doesn't care about.
 *
 * There is no need to call any superclass methods from any subclass as they do
 * nothing.
 */
public class EmptySignatureVisitor extends SignatureVisitor {

  protected static EmptySignatureVisitor ignore = new EmptySignatureVisitor();

  public EmptySignatureVisitor() {
    super(Opcodes.ASM4);
  }

  /**
   * Treated as a visitEnd for this visitor.
   */
  @Override
  public SignatureVisitor visitArrayType() {
    return ignore;
  }

  /**
   * Treated as a visitEnd for this visitor.
   */
  @Override
  public void visitBaseType(char descriptor) {
  }

  @Override
  public SignatureVisitor visitClassBound() {
    return ignore;
  }

  @Override
  public void visitClassType(String name) {
  }

  @Override
  public void visitEnd() {
  }

  @Override
  public SignatureVisitor visitExceptionType() {
    return ignore;
  }

  @Override
  public void visitFormalTypeParameter(String name) {
  }

  @Override
  public void visitInnerClassType(String name) {
  }

  @Override
  public SignatureVisitor visitInterface() {
    return ignore;
  }

  @Override
  public SignatureVisitor visitInterfaceBound() {
    return ignore;
  }

  @Override
  public SignatureVisitor visitParameterType() {
    return ignore;
  }

  @Override
  public SignatureVisitor visitReturnType() {
    return ignore;
  }

  @Override
  public SignatureVisitor visitSuperclass() {
    return ignore;
  }

  @Override
  public void visitTypeArgument() {
  }

  @Override
  public SignatureVisitor visitTypeArgument(char wildcard) {
    return ignore;
  }

  /**
   * Treated as a visitEnd for this visitor.
   */
  @Override
  public void visitTypeVariable(String name) {
  }
}
