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
package com.google.gwt.core.ext.soyc.impl;

import com.google.gwt.core.ext.soyc.ClassMember;
import com.google.gwt.core.ext.soyc.FieldMember;
import com.google.gwt.core.ext.soyc.Member;
import com.google.gwt.core.ext.soyc.MethodMember;

/**
 * Provides implementation of common Member functions.
 */
public abstract class AbstractMember implements Member {

  @Override
  public abstract String getSourceName();

  @Override
  public ClassMember isClass() {
    return this instanceof ClassMember ? (ClassMember) this : null;
  }

  @Override
  public FieldMember isField() {
    return this instanceof FieldMember ? (FieldMember) this : null;
  }

  @Override
  public MethodMember isMethod() {
    return this instanceof MethodMember ? (MethodMember) this : null;
  }
}