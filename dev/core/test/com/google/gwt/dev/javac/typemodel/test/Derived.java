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
 * 
 * Derived<T> Overridable Methods (methods from java.lang.Object not shown):
 * Derived<T>.m(Integer) Derived<T>.m(Number) Derived<T>.m(Integer)
 * Derived<T>.<T extends Serializable> void m(T t)
 */
public class Derived<T> extends Base<T> {
  public void m(Integer i) {
    System.out.println("Derived<T>.m(Integer)");
  } // new Overload

  /**
   * Overrides Base<T>.m(T)
   * 
   * NOTE: this is commented out because JDT 3.1 will report it as an error,
   * even though javac 1.5 allows this.
   */
  // @Override
  // public void m(Object t) {
  // System.out.println("Derived<T>.m(Object)");
  // } //

  /**
   * Overrides Base<T>.<N extends Number>.m(N)
   */
  @Override
  public void m(Number n) {
    System.out.println("Derived<T>.m(Number)");
  } // overrides m(N)

  /**
   * Overloads m
   * 
   * @param <T>
   * @param t
   */
  public <T extends Serializable> void m(T t) {
    System.out.println("Derived<T>.<T extends Serializable>.m(T)");
  }
}
