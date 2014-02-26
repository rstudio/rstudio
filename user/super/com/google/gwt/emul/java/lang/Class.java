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

import java.lang.reflect.Type;

/**
 * Generally unsupported. This class is provided so that the GWT compiler can
 * choke down class literal references.
 *
 * @param <T> the type of the object
 */
public final class Class<T> implements Type {

  private static final int PRIMITIVE = 0x00000001;
  private static final int INTERFACE = 0x00000002;
  private static final int ARRAY = 0x00000004;
  private static final int ENUM = 0x00000008;

  /**
   * Create a Class object for an array.<p>
   *
   * Arrays are not registered in the prototype table and get the class literal explicitely at
   * construction.<p>
   *
   * @skip
   */
  static <T> Class<T> createForArray(String packageName, String className,
      JavaScriptObject typeId, Class<?> componentType) {
    // Initialize here to avoid method inliner
    Class<T> clazz = new Class<T>();
    if (clazz.isClassMetadataEnabled()) {
      initializeNames(clazz, packageName, className);
    } else {
      synthesizeClassNamesFromTypeId(clazz, typeId);
    }
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
      JavaScriptObject typeId, Class<? super T> superclass) {
    // Initialize here to avoid method inliner
    Class<T> clazz = new Class<T>();
    if (clazz.isClassMetadataEnabled()) {
      initializeNames(clazz, packageName, className);
    } else {
      synthesizeClassNamesFromTypeId(clazz, typeId);
    }
    maybeSetClassLiteral(typeId, clazz);
    clazz.superclass = superclass;
    return clazz;
  }

  /**
   * Create a Class object for an enum.
   *
   * @skip
   */
  static <T> Class<T> createForEnum(String packageName, String className,
      JavaScriptObject typeId, Class<? super T> superclass,
      JavaScriptObject enumConstantsFunc, JavaScriptObject enumValueOfFunc) {
    // Initialize here to avoid method inliner
    Class<T> clazz = new Class<T>();
    if (clazz.isClassMetadataEnabled()) {
      initializeNames(clazz, packageName, className);
    } else {
      synthesizeClassNamesFromTypeId(clazz, typeId);
    }
    maybeSetClassLiteral(typeId, clazz);
    clazz.modifiers = (enumConstantsFunc != null) ? ENUM : 0;
    clazz.superclass = clazz.enumSuperclass = superclass;
    clazz.enumConstantsFunc = enumConstantsFunc;
    clazz.enumValueOfFunc = enumValueOfFunc;
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
    if (clazz.isClassMetadataEnabled()) {
      initializeNames(clazz, packageName, className);
    } else {
      synthesizeClassNamesFromTypeId(clazz, null);
    }
    clazz.modifiers = INTERFACE;
    return clazz;
  }

  /**
   * Create a Class object for a primitive.
   *
   * @skip
   */
  static Class<?> createForPrimitive(String packageName, String className,
      String primitiveTypeId) {
    // Initialize here to avoid method inliner
    Class<?> clazz = new Class<Object>();
    if (clazz.isClassMetadataEnabled()) {
      initializeNames(clazz, packageName, className);
    } else {
      synthesizePrimitiveNamesFromTypeId(clazz, primitiveTypeId);
    }
    clazz.modifiers = PRIMITIVE;
    return clazz;
  }

  /**
    * Used by {@link WebModePayloadSink} to create uninitialized instances.
    */
   static native JavaScriptObject getPrototypeForClass(Class<?> clazz) /*-{
     var typeId = clazz.@java.lang.Class::typeId;
     var prototype = @com.google.gwt.lang.JavaClassHierarchySetupUtil::prototypesByTypeId[typeId];
     // TODO(rluble): introduce pragma annotation to indicate that this function should not be
     // inlined.
     clazz = null; // HACK: prevent pruning via inlining by using param as lvalue
     return prototype;
   }-*/;

  public static boolean isClassMetadataEnabled() {
    // This body may be replaced by the compiler
    return true;
  }

  /**
   * null implies non-instantiable type, with no entries in
   * {@link JavaClassHierarchySetupUtil::prototypesByTypeId}.
   */
  static native boolean isInstantiable(JavaScriptObject typeId) /*-{
    return !!typeId;
  }-*/;


  /**
   * Install class literal into prototype.clazz field (if type is instantiable) such that
   * Object.getClass() returning this.clazz returns the literal. Also stores typeId on class literal
   * for looking up prototypes given a literal. This is used for deRPC at the moment, but may be
   * used to implement Class.newInstance() in the future.
   *
   * If the prototype for typeId has not yet been created, then install the literal into a
   * placeholder array to differentiate the two cases.
   */
  static native void maybeSetClassLiteral(JavaScriptObject typeId, Class<?> clazz) /*-{
    var proto;
    if (!typeId) {
      // Type is not instantiable, hence not registered in the metadata table.
      return;
    }
    clazz.@java.lang.Class::typeId = typeId;
    // Guarantees virtual method won't be pruned by using a JSNI ref
    // This is required because deRPC needs to call it.
    var prototype  = @java.lang.Class::getPrototypeForClass(Ljava/lang/Class;)(clazz);
    // A class literal may be referenced prior to an async-loaded vtable setup
    // For example, class literal lives in inital fragment,
    // but type is instantiated in another fragment
    if (!prototype) {
      // Leave a place holder for now to be filled in by __defineClass__ later.
      // TODO(rluble): Do not rely on the fact that if the entry is an array it is a placeholder.
      @com.google.gwt.lang.JavaClassHierarchySetupUtil::prototypesByTypeId[typeId] = [clazz];
      return;
    }
    // Type already registered in the metadata table, install the class literal in the appropriate
    // prototype field.
    prototype.@java.lang.Object::___clazz = clazz;
  }-*/;

  /**
   * Initiliazes {@code clazz} names from metadata.
   * <p>
   * Only called if metadata is NOT disabled.
   */
  static void initializeNames(Class<?> clazz, String packageName,
      String className) {
    clazz.typeName = packageName + className;
    clazz.simpleName = className;
  }

  /**
   * Initiliazes {@code clazz} names from typeIds.
   * <p>
   * Only called if metadata IS disabled.
   */
  static void synthesizeClassNamesFromTypeId(Class<?> clazz, JavaScriptObject typeId) {
    /*
     * The initial "" + in the below code is to prevent clazz.hashCode() from
     * being autoboxed. The class literal creation code is run very early
     * during application start up, before class Integer has been initialized.
     */
    clazz.typeName = "Class$"
        + (isInstantiable(typeId) ? "S" + typeId : "" + clazz.hashCode());
    clazz.simpleName = clazz.typeName;
  }

  /**
   * Sets the class object for primitives.
   */
  static void synthesizePrimitiveNamesFromTypeId(Class<?> clazz, String primitiveTypeId) {
    clazz.typeName = "Class$" + primitiveTypeId;
    clazz.simpleName = clazz.typeName;
  }

  JavaScriptObject enumValueOfFunc;

  int modifiers;

  private Class<?> componentType;

  @SuppressWarnings("unused")
  private JavaScriptObject enumConstantsFunc;

  private Class<? super T> enumSuperclass;

  private Class<? super T> superclass;

  private String simpleName;

  private String typeName;

  private int typeId;

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

  public Class<?> getComponentType() {
    return componentType;
  }

  public native T[] getEnumConstants() /*-{
    return this.@java.lang.Class::enumConstantsFunc
        && (this.@java.lang.Class::enumConstantsFunc)();
  }-*/;

  public String getName() {
    return typeName;
  }

  public String getSimpleName() {
    return simpleName;
  }

  public Class<? super T> getSuperclass() {
    if (isClassMetadataEnabled()) {
      return superclass;
    } else {
      return null;
    }
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

  /**
   * Used by Enum to allow getSuperclass() to be pruned.
   */
  Class<? super T> getEnumSuperclass() {
    return enumSuperclass;
  }
}
