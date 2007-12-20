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

import java.util.ArrayList;
import java.util.List;

/**
 * Java enum type reference expression.
 */
public class JEnumType extends JClassType {
  /*
   * TODO: implement traverse?
   */

  public final List<JEnumField> enumList = new ArrayList<JEnumField>();

  public JEnumType(JProgram program, SourceInfo info, String name) {
    super(program, info, name, false, false);
    this.extnds = program.getTypeJavaLangEnum();
  }

  @Override
  public String getClassLiteralFactoryMethod() {
    return "Class.createForEnum";
  }

  @Override
  public JEnumType isEnumOrSubclass() {
    return this;
  }
}
