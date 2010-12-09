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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Generic class.
 * 
 * @param <T>
 * 
 *          NOTE: It seems that the JDT 3.1 will not allow:
 *          GenericClass<Integer> if the definition of GenericClass is as
 *          follows: class GenericClass<T extends Serializable & Comparable<T>>
 *          implements Comparable<T> { ... }
 */
public class GenericClass<T extends Serializable> implements Comparable<T>,
    Serializable {
  /**
   * Non-static, generic inner class.
   * 
   * @param <U>
   */
  public class GenericInnerClass<U> {
    T t2;
    U u2;
  }

  /**
   * This class is not technically a generic class although it has a member that
   * references a type parameter from its enclosing type.
   */
  public class NonGenericInnerClass {
    T t3;
  }

  public class Foo {
    public class Bar {
      T t4;
    }
  }

  /**
   * Field of an inner class that is enclosed in a parameterized type.
   */
  transient GenericClass<Integer>.NonGenericInnerClass nonGenericInnerClassField;

  /**
   * NOTE: The following is disabled because it violates an assumption in TOB
   * line 1228.
   */
  // GenericClass.NonGenericInnerClass rawNonGenericInnerClassField;
  Class rawClazzField;

  /**
   * Field of a raw type.
   */
  ArrayList rawFieldType;

  /**
   * Field of a type parameter type.
   */
  T typeParameterField;

  /**
   * Parameterized with a array type argument.
   */
  List<T[]> parameterizedListField;

  public GenericClass() {
  }

  public GenericClass(T t) {
    this.typeParameterField = t;
  }

  public int compareTo(T o) {
    // TODO Auto-generated method stub
    return 0;
  }

  public T getT() {
    return typeParameterField;
  }

  /*
   * Generic method
   */
  public <U> U max(Collection<U> collection) {
    return collection.iterator().next();
  }

  /*
   * Generic method
   */
  public <U> U min(Collection<U> collection) {
    return collection.iterator().next();
  }

  public void setT(T t) throws Exception {
    this.typeParameterField = t;
  }
}