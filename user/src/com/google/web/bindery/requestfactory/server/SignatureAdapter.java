/*
 * Copyright 2010 Google Inc.
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
package com.google.web.bindery.requestfactory.server;

import com.google.gwt.dev.asm.signature.SignatureVisitor;

/**
 * An empty implementation of SignatureVisitor, used by
 * {@link RequestFactoryInterfaceValidator}. This is a copy of the dev package's
 * EmptySignatureVisitor.
 */
class SignatureAdapter implements SignatureVisitor {

  private static final SignatureAdapter ignore = new SignatureAdapter();

  public SignatureVisitor visitArrayType() {
    return ignore;
  }

  public void visitBaseType(char descriptor) {
  }

  public SignatureVisitor visitClassBound() {
    return ignore;
  }

  public void visitClassType(String name) {
  }

  public void visitEnd() {
  }

  public SignatureVisitor visitExceptionType() {
    return ignore;
  }

  public void visitFormalTypeParameter(String name) {
  }

  public void visitInnerClassType(String name) {
  }

  public SignatureVisitor visitInterface() {
    return ignore;
  }

  public SignatureVisitor visitInterfaceBound() {
    return ignore;
  }

  public SignatureVisitor visitParameterType() {
    return ignore;
  }

  public SignatureVisitor visitReturnType() {
    return ignore;
  }

  public SignatureVisitor visitSuperclass() {
    return ignore;
  }

  public void visitTypeArgument() {
  }

  public SignatureVisitor visitTypeArgument(char wildcard) {
    return ignore;
  }

  public void visitTypeVariable(String name) {
  }
}