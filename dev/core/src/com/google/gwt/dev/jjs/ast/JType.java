/*
 * Copyright 2007 Google Inc.
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

import com.google.gwt.dev.jjs.SourceInfo;

/**
 * Base class for any types entity.
 */
public abstract class JType extends JNode implements HasName {

  protected final String name;
  private final JLiteral defaultValue;

  public JType(JProgram program, SourceInfo info, String name,
      JLiteral defaultValue) {
    super(program, info);
    this.name = name;
    this.defaultValue = defaultValue;
  }

  public abstract String getClassLiteralFactoryMethod();

  public JLiteral getDefaultValue() {
    return defaultValue;
  }

  public abstract String getJavahSignatureName();

  public abstract String getJsniSignatureName();

  public String getName() {
    return name;
  }

}
