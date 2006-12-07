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
 * Java interface type definition. 
 */
public class JInterfaceType extends JReferenceType {

  JInterfaceType(JProgram program, String name) {
    super(program, name);
  }

  public boolean isAbstract() {
    return true;
  }

  public boolean isFinal() {
    return false;
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
