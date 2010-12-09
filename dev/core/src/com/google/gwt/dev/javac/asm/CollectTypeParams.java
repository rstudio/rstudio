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

import com.google.gwt.dev.javac.typemodel.JTypeParameter;

import java.util.List;

/**
 * Collects formal type parameters into a JTypeParameter list.
 */
public class CollectTypeParams extends EmptySignatureVisitor {

  private final List<JTypeParameter> typeParams;

  /**
   * Collect declared type parameters from a generic signature.
   * 
   * @param typeParams list to store type parameters in
   */
  public CollectTypeParams(List<JTypeParameter> typeParams) {
    this.typeParams = typeParams;
  }

  @Override
  public void visitFormalTypeParameter(String name) {
    typeParams.add(new JTypeParameter(name, typeParams.size()));
  }
}
