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
 * A field that is an enum constant.
 */
public class JEnumField extends JField {

  private int ordinal;

  public JEnumField(JProgram program, SourceInfo info, String name,
      int ordinal, JEnumType enclosingType, JClassType type) {
    super(program, info, name, enclosingType, type, true, true, true);
    this.ordinal = ordinal;
  }

  @Override
  public JEnumType getEnclosingType() {
    // TODO Auto-generated method stub
    return (JEnumType) super.getEnclosingType();
  }

  public int ordinal() {
    return ordinal;
  }

  // TODO: implement traverse?

}
