/*
 * Copyright 2008 Google Inc.
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
package com.google.gwt.dev.javac.typemodel.test;

/**
 * Enumerated type used in the
 * {@link com.google.gwt.core.ext.typeinfo.JEnumTypeTest}.
 */
public enum MyEnum {

  VAL2(-3) {
    @Override
    public int getId() {
      return instanceField;
    }
  },

  VAL1(-2) {
    @Override
    public int getId() {
      return instanceField;
    }
  },

  @Deprecated
  VAL0(-1) {
    @Override
    public int getId() {
      return instanceField;
    }
  };

  MyEnum(int instanceField) {
    this.instanceField = instanceField;
  }

  public final int instanceField;

  public static final MyEnum e = VAL2;

  public abstract int getId();
}
