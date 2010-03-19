/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.dev.shell;

import java.lang.reflect.Member;

/**
 * This class is used to represent a synthetic field called "class" that allows
 * JSNI references to class literals.
 */
public class SyntheticClassMember implements Member {
  private final Class<?> clazz;

  public SyntheticClassMember(Class<?> clazz) {
    this.clazz = clazz;
  }

  public Class<?> getDeclaringClass() {
    return clazz;
  }

  public int getModifiers() {
    return Member.PUBLIC;
  }

  public String getName() {
    return "class";
  }

  public boolean isSynthetic() {
    return false;
  }
}