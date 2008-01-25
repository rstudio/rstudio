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
package java.lang;

import com.google.gwt.core.client.JavaScriptObject;

import java.io.Serializable;

/**
 * The first-class representation of an enumeration.
 * 
 * @param <E>
 */
public abstract class Enum<E extends Enum<E>> implements Comparable<E>,
    Serializable {

  protected static <T extends Enum<T>> T valueOf(JavaScriptObject map,
      String name) {
    T result = Enum.<T> valueOf0(map, "_" + name);
    if (result == null) {
      throw new IllegalArgumentException(name);
    }
    return result;
  }

  private static native <T extends Enum<T>> T valueOf0(JavaScriptObject map,
      String name) /*-{
    return map[name] || null;
  }-*/;

  private final String name;

  private final int ordinal;

  protected Enum(String name, int ordinal) {
    this.name = name;
    this.ordinal = ordinal;
  }

  public final int compareTo(E other) {
    // TODO: will a bridge method do the cast for us?
    // if (this.getDeclaringClass() != other.getDeclaringClass()) {
    // throw new ClassCastException();
    // }
    return this.ordinal - other.ordinal;
  }

  @Override
  public final boolean equals(Object other) {
    return this == other;
  }

  @SuppressWarnings("unchecked")
  public final Class<E> getDeclaringClass() {
    Class clazz = getClass();
    Class superclass = clazz.getSuperclass();

    return (superclass == Enum.class) ? clazz : superclass;
  }

  @Override
  public final int hashCode() {
    return System.identityHashCode(this);
  }

  public final String name() {
    return name;
  }

  public final int ordinal() {
    return ordinal;
  }

  @Override
  public String toString() {
    return name;
  }

}
