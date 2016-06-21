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

import static javaemul.internal.InternalPreconditions.checkCriticalArgument;
import static javaemul.internal.InternalPreconditions.checkNotNull;

import com.google.gwt.core.client.JavaScriptObject;

import java.io.Serializable;

import jsinterop.annotations.JsIgnore;
import jsinterop.annotations.JsType;

/**
 * The first-class representation of an enumeration.
 *
 * @param <E>
 */
@JsType
public abstract class Enum<E extends Enum<E>> implements Comparable<E>, Serializable {

  @JsIgnore
  public static <T extends Enum<T>> T valueOf(Class<T> enumType, String name) {
    JavaScriptObject enumValueOfFunc = checkNotNull(enumType).enumValueOfFunc;
    checkCriticalArgument(enumValueOfFunc != null);
    checkNotNull(name);
    return invokeValueOf(enumValueOfFunc, name);
  }

  protected static <T extends Enum<T>> JavaScriptObject createValueOfMap(
      T[] enumConstants) {
    JavaScriptObject result = JavaScriptObject.createObject();
    for (T value : enumConstants) {
       put0(result, ":" + value.name(), value);
    }
    return result;
  }

  protected static <T extends Enum<T>> T valueOf(JavaScriptObject map, String name) {
    checkNotNull(name);

    T result = Enum.<T> get0(map, ":" + name);
    checkCriticalArgument(result != null, "Enum constant undefined: %s", name);
    return result;
  }

  private static native <T extends Enum<T>> T get0(JavaScriptObject map,
      String name) /*-{
    return map[name];
  }-*/;

  private static native <T extends Enum<T>> T invokeValueOf(
      JavaScriptObject enumValueOfFunc, String name) /*-{
    return enumValueOfFunc(name);
  }-*/;

  private static native <T extends Enum<T>> void put0(JavaScriptObject map,
      String name, T value) /*-{
    map[name] = value;
  }-*/;

  private final String name;

  private final int ordinal;

  protected Enum(String name, int ordinal) {
    this.name = name;
    this.ordinal = ordinal;
  }

  @Override
  public final int compareTo(E other) {
    // TODO: will a bridge method do the cast for us?
    // if (this.getDeclaringClass() != other.getDeclaringClass()) {
    // throw new ClassCastException();
    // }
    return this.ordinal - ((Enum) other).ordinal;
  }

  @Override
  public final boolean equals(Object other) {
    return this == other;
  }

  @JsIgnore
  @SuppressWarnings("unchecked")
  public final Class<E> getDeclaringClass() {
    Class clazz = getClass();
    assert clazz != null : "clazz";

    // Don't use getSuperclass() to allow that method to be pruned for most
    // class types
    Class superclass = clazz.getEnumSuperclass();
    assert superclass != null : "superclass";

    return (superclass == Enum.class) ? clazz : superclass;
  }

  @Override
  public final int hashCode() {
    return super.hashCode();
  }

  public final String name() {
    return name != null ? name : "" + ordinal;
  }

  public final int ordinal() {
    return ordinal;
  }

  @Override
  public String toString() {
    return name();
  }
}
