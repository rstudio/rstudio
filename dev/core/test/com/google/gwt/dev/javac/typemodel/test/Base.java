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
package com.google.gwt.dev.javac.typemodel.test;

import java.io.Serializable;

/**
 * Class used to test
 * {@link com.google.gwt.core.ext.typeinfo.JClassType#getOverridableMethods()}.
 */
public class Base<T> {
  void m(T t) {
    System.out.println("Base<T>.m(T)");
  }

  <N extends Number> void m(N n) {
    System.out.println("Base<T>.m(N)");
  }

  static <N extends Serializable> void serialize(N n) {
    System.out.println("Base<T>.<N extends Serializable>.serialize(N)");
  }
}
