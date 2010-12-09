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

import java.io.Serializable;

/**
 * Test a generic class that extends a generic class.
 */
public class GenericSubclass<U extends Serializable> extends GenericClass<U> {
  GenericClass<Integer> child;

  public GenericSubclass(U t) {
    super(t);
  }

  // TODO: This triggers a name clash problem with JDT 3.1 but not with JDT
  // 3.3.0 or with javac 1.5.06.
  // public void setT(Object t) {
  // // this should override GenericClass<U>.setT(T t);
  // }
}
