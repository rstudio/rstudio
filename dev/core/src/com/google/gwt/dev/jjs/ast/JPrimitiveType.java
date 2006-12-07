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
package com.google.gwt.dev.jjs.ast;

/**
 * Base class for all Java primitive types.  
 */
public class JPrimitiveType extends JType {

  private final String signatureName;

  /**
   * These are only supposed to be constructed by JProgram.
   */
  JPrimitiveType(JProgram program, String name, String signatureName, JLiteral defaultValue) {
    super(program, name, defaultValue);
    this.signatureName = signatureName;
  }

  public String getJavahSignatureName() {
    return signatureName;
  }

  public String getJsniSignatureName() {
    return signatureName;
  }

  public void traverse(JVisitor visitor) {
    if (visitor.visit(this)) {
    }
    visitor.endVisit(this);
  }

}
