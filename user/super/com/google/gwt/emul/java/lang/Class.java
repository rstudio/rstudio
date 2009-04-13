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
  static <T> Class<T> createForArray(String packageName, String className,
      Class<?> componentType) {
    // Initialize here to avoid method inliner
    Class<T> clazz = new Class<T>();
    clazz.simpleName = componentType.getSimpleName() + "[]";
    clazz.typeName = packageName + className;
    clazz.modifiers = ARRAY;
    clazz.superclass = Object.class;
    clazz.componentType = componentType;
    return clazz;
  }

  /**
   * Create a Class object for a class.
   * 
   * @skip
   */
  static <T> Class<T> createForClass(String packageName, String className,
      JavaScriptObject classSeed, Class<? super T> superclass) {
    // Initialize here to avoid method inliner
    Class<T> clazz = new Class<T>();
    clazz.simpleName = className;
    clazz.typeName = packageName + className;
    clazz.classSeed = classSeed;
    clazz.superclass = superclass;
    return clazz;
  }

  /**
   * Create a Class object for an enum.
   * 
   * @skip
   */
  static <T> Class<T> createForEnum(String packageName, String className,
      JavaScriptObject classSeed, Class<? super T> superclass,
      JavaScriptObject enumConstantsFunc) {
    // Initialize here to avoid method inliner
    Class<T> clazz = new Class<T>();
    clazz.simpleName = className;
    clazz.typeName = packageName + className;
    clazz.classSeed = classSeed;
    clazz.modifiers = (enumConstantsFunc != null) ? ENUM : 0;
    clazz.superclass = clazz.enumSuperclass = superclass;
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
    clazz.simpleName = className;
    clazz.typeName = packageName + className;
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
    clazz.simpleName = className;
    clazz.typeName = packageName + className;
    clazz.modifiers = PRIMITIVE;
    return clazz;
  }

  int modifiers;

  private JavaScriptObject classSeed;

  /**
   * This is a separate field from typeName so that typeName and the
   * createForFoo parameters can be pruned when class metadata is disabled.
   */
  private String classSeedName;

  private Class<?> componentType;

  @SuppressWarnings("unused")
  private JavaScriptObject enumConstantsFunc;

  private Class<? super T> enumSuperclass;

  private String simpleName;

  private String typeName;

  private Class<? super T> superclass;

  /**
   * Not publicly instantiable.
   * 
   * @skip
   */
  private Class() {
  }

  public boolean desiredAssertionStatus() {
    /*
     * This body is ignored by the JJS compiler and a new one is synthesized at
     * compile-time based on the actual compilation arguments.
     */
    return false;
  }

  public Class<?> getComponentType() {
    return componentType;
  }

  public native T[] getEnumConstants() /*-{
    return this.@java.lang.Class::enumConstantsFunc
        && (this.@java.lang.Class::enumConstantsFunc)();
  }-*/;

  /**
   * Used by Enum to allow getSuperclass() to be pruned.
   */
  public Class<? super T> getEnumSuperclass() {
    return enumSuperclass;
  }

  public String getName() {
    // This body may be replaced by the compiler
    return typeName;
  }

  /**
   * Used by the compiler to implement {@link #getName()} when class metadata
   * has been disabled.
   */
  public String getNameFromClassSeed() {
    if (classSeedName != null) {
      // Do nothing

    } else if (classSeed == null) {
      /*
       * Uninstantiable types will have stable, but inconsistent type names.
       * This would tend to occur when referencing interface types or concrete
       * types that were pruned.
       */
      classSeedName = "Class$" + System.identityHashCode(this);

    } else {
      // Compute the name from the class seed
      String fnToString = classSeed.toString().trim();
      int index = fnToString.indexOf("(");
      assert index != -1;
      int start = fnToString.startsWith("function") ? 8 : 0;
      classSeedName = "Class$" + fnToString.substring(start, index).trim();
    }

    assert classSeedName != null;
    return classSeedName;
  }

  public String getSimpleName() {
    // This body may be replaced by the compiler
    return simpleName;
  }

  public Class<? super T> getSuperclass() {
    // This body may be replaced by the compiler
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
