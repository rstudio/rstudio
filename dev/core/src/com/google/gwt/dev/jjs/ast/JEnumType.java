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
import com.google.gwt.dev.util.collect.Lists;

import java.util.List;

/**
 * Java enum type reference expression.
 */
public class JEnumType extends JClassType {
  /*
   * TODO: implement traverse?
   */

  private List<JEnumField> enumList = Lists.create();
  private boolean isOrdinalized = false;

  public static final String VALUES_ARRAY_NAME = "$VALUES";

  public JEnumType(SourceInfo info, String name, boolean isAbstract) {
    super(info, name, isAbstract, false);
  }

  public JEnumType(SourceInfo info, String name, boolean isAbstract, JsInteropType interopType) {
    super(info, name, isAbstract, false, interopType);
  }

  @Override
  public void addField(JField field) {
    if (field instanceof JEnumField) {
      JEnumField enumField = (JEnumField) field;
      int ordinal = enumField.ordinal();
      while (ordinal >= enumList.size()) {
        enumList = Lists.add(enumList, null);
      }
      enumList = Lists.set(enumList, ordinal, enumField);
    }
    super.addField(field);
  }

  @Override
  public String getClassLiteralFactoryMethod() {
    return "Class.createForEnum";
  }

  /**
   * Returns the list of enum fields in this enum.
   */
  public List<JEnumField> getEnumList() {
    return enumList;
  }

  @Override
  public JEnumType isEnumOrSubclass() {
    return this;
  }

  public boolean isOrdinalized() {
    return isOrdinalized;
  }

  public void setOrdinalized() {
    isOrdinalized = true;
  }
}
