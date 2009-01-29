/*
 * Copyright 2006 Google Inc.
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

/**
 * Generally unsupported. This class is provided so that the GWT compiler can
 * choke down class literal references.
 * 
 * @param <T> the type of the object
 */
public final class Class<T> {

  private static final int PRIMITIVE = 0x00000001;
  private static final int INTERFACE = 0x00000002;
  private static final int ARRAY = 0x00000004;
  private static final int ENUM = 0x00000008;

  /**
   * Create a Class object for an array.
   * 
   * @skip
   */
  static <T> Class<T> createForArray(String packageName, String className) {
    // Initialize here to avoid method inliner
    Class<T> clazz = new Class<T>();
    clazz.modifiers = ARRAY;
    clazz.superclass = Object.class;
    return clazz;
  }

  /**
   * Create a Class object for a class.
   * 
   * @skip
   */
  static <T> Class<T> createForClass(String packageName, String className,
      Class<? super T> superclass) {
    // Initialize here to avoid method inliner
    Class<T> clazz = new Class<T>();
    clazz.superclass = superclass;
    return clazz;
  }

  /**
   * Create a Class object for an enum.
   * 
   * @skip
   */
  static <T> Class<T> createForEnum(String packageName, String className,
      Class<? super T> superclass, JavaScriptObject enumConstantsFunc) {
    // Initialize here to avoid method inliner
    Class<T> clazz = new Class<T>();
    clazz.modifiers = ENUM;
    clazz.superclass = superclass;
    clazz.enumConstantsFunc = enumConstantsFunc;
    return clazz;
  }

  /**
   * Create a Class object for an interface.
   * 
   * @skip
   */
  static <T> Class<T> createForInterface(String packageName, String className) {
    // Initialize here to avoid method inliner
    Class<T> clazz = new Class<T>();
    clazz.modifiers = INTERFACE;
    return clazz;
  }

  /**
   * Create a Class object for a primitive.
   * 
   * @skip
   */
  static Class<?> createForPrimitive(String packageName, String className) {
    // Initialize here to avoid method inliner
    Class<?> clazz = new Class<Object>();
    clazz.modifiers = PRIMITIVE;
    return clazz;
  }

  @SuppressWarnings("unused")
  private JavaScriptObject enumConstantsFunc;

  private int modifiers;

  private Class<? super T> superclass;

  /**
   * Not publicly instantiable.
   * 
   * @skip
   */
  private Class() {
  }

  public boolean desiredAssertionStatus() {
    // This body is ignored by the JJS compiler and a new one is 
    // synthesized at compile-time based on the actual compilation arguments.  
    return false;
  }

  public native T[] getEnumConstants() /*-{
    return this.@java.lang.Class::enumConstantsFunc
        && (this.@java.lang.Class::enumConstantsFunc)();
  }-*/;

  public String getName() {
    return "[class]";
  }

  public Class<? super T> getSuperclass() {
    return superclass;
  }

  public boolean isArray() {
    return (modifiers & ARRAY) != 0;
  }

  public boolean isEnum() {
    return (modifiers & ENUM) != 0;
  }

  public boolean isInterface() {
    return (modifiers & INTERFACE) != 0;
  }

  public boolean isPrimitive() {
    return (modifiers & PRIMITIVE) != 0;
  }

  public String toString() {
    return (isInterface() ? "interface " : (isPrimitive() ? "" : "class "))
        + getName();
  }
}
