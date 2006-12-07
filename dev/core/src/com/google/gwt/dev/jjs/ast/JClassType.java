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
 * Java class type reference expression. 
 */
public class JClassType extends JReferenceType implements CanBeSetFinal {

  private final boolean isAbstract;
  private boolean isFinal;

  public JClassType(JProgram program, String name, boolean isAbstract, boolean isFinal) {
    super(program, name);
    this.isAbstract = isAbstract;
    this.isFinal = isFinal;
  }

  public boolean isAbstract() {
    return isAbstract;
  }

  public boolean isFinal() {
    return isFinal;
  }

  public void setFinal(boolean b) {
    isFinal = b;
  }

  public void traverse(JVisitor visitor) {
    if (visitor.visit(this)) {
      for (int i = 0; i < fields.size(); ++i) {
        JField field = (JField) fields.get(i);
        field.traverse(visitor);
      }
      for (int i = 0; i < methods.size(); ++i) {
        JMethod method = (JMethod) methods.get(i);
        method.traverse(visitor);
      }
    }
    visitor.endVisit(this);
  }

}
