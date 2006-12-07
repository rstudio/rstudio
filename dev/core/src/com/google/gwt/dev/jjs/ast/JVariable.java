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
 * Base class for any storage location.
 */
public abstract class JVariable extends JNode implements CanBeFinal, HasName,
    HasType, HasSettableType {

  public JType type;
  public String name;
  public boolean isFinal;

  JVariable(JProgram program, String name, JType type, boolean isFinal) {
    super(program);
    this.name = name;
    this.type = type;
    this.isFinal = isFinal;
  }

  public String getName() {
    return name;
  }

  public JType getType() {
    return type;
  }

  public boolean isFinal() {
    return isFinal;
  }

  public void setType(JType newType) {
    type = newType;
  }

}
