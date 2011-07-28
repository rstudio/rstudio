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

  static native String asString(int number) /*-{
    // for primitives, the seedId isn't a number, but a string like ' Z'
    return typeof(number) == 'number' ?  "S" + (number < 0 ? -number : number) : number;
  }-*/;

  /**
   * Create a Class object for an array.
   * 
   * @skip
   */
  static <T> Class<T> createForArray(String packageName, String className,
      int seedId, Class<?> componentType) {
    // Initialize here to avoid method inliner
    Class<T> clazz = new Class<T>();
    setName(clazz, packageName, className, seedId != 0 ? -seedId : 0);
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
      int seedId, Class<? super T> superclass) {
    // Initialize here to avoid method inliner
    Class<T> clazz = new Class<T>();
    setName(clazz, packageName, className, seedId);
    clazz.superclass = superclass;
    return clazz;
  }

  /**
   * Create a Class object for an enum.
   * 
   * @skip
   */
  static <T> Class<T> createForEnum(String packageName, String className,
      int seedId, Class<? super T> superclass,
      JavaScriptObject enumConstantsFunc, JavaScriptObject enumValueOfFunc) {
    // Initialize here to avoid method inliner
    Class<T> clazz = new Class<T>();
    setName(clazz, packageName, className, seedId);
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
    setName(clazz, packageName, className, 0);
    clazz.modifiers = INTERFACE;
    return clazz;
  }

  /**
   * Create a Class object for a primitive.
   * 
   * @skip
   */
  static Class<?> createForPrimitive(String packageName, String className,
      int seedId) {
    // Initialize here to avoid method inliner
    Class<?> clazz = new Class<Object>();
    setName(clazz, packageName, className, seedId);
    clazz.modifiers = PRIMITIVE;
    return clazz;
  }

  /**
    * Used by {@link WebModePayloadSink} to create uninitialized instances.
    */
   static native JavaScriptObject getSeedFunction(Class<?> clazz) /*-{
     var func = @com.google.gwt.lang.SeedUtil::seedTable[clazz.@java.lang.Class::seedId];
     clazz = null; // HACK: prevent pruning via inlining by using param as lvalue
     return func;
   }-*/;

  static boolean isClassMetadataEnabled() {
    // This body may be replaced by the compiler
    return true;
  }

  /**
   * null or 0 implies lack of seed function / non-instantiable type
   */
  static native boolean isInstantiable(int seedId) /*-{
    return typeof (seedId) == 'number' && seedId > 0;
  }-*/;

  /**
   * null implies pruned.
   */
  static native boolean isInstantiableOrPrimitive(int seedId) /*-{
    return seedId != null && seedId != 0;
  }-*/;

  /**
   * Install class literal into seed.prototype.clazz field such that
   * Object.getClass() returning this.clazz returns the literal. Also stores
   * seedId on class literal for looking up prototypes given a literal. This
   * is used for deRPC at the moment, but may be used to implement
   * Class.newInstance() in the future.
   */
  static native void setClassLiteral(int seedId, Class<?> clazz) /*-{
    var proto;
    clazz.@java.lang.Class::seedId = seedId;
    // String is the exception to the usual vtable setup logic
    if (seedId == 2) {
      proto = String.prototype
    } else {
      if (seedId > 0) {
        // Guarantees virtual method won't be pruned by using a JSNI ref
        // This is required because deRPC needs to call it.
        var seed = @java.lang.Class::getSeedFunction(Ljava/lang/Class;)(clazz);
        // A class literal may be referenced prior to an async-loaded vtable setup
        // For example, class literal lives in inital fragment,
        // but type is instantiated in another fragment
        if (seed) {
          proto = seed.prototype;
        } else {
          // Leave a place holder for now to be filled in by __defineSeed__ later
          seed = @com.google.gwt.lang.SeedUtil::seedTable[seedId] = function(){};
          seed.@java.lang.Object::___clazz = clazz;
          return;
        }
      } else {
        return;
      }
    }
    proto.@java.lang.Object::___clazz = clazz;
  }-*/;

  /**
   * The seedId parameter can take on the following values:
   * > 0 =>  type is instantiable class
   * < 0 => type is instantiable array
   * null => type is not instantiable
   * string => type is primitive
   */
  static void setName(Class<?> clazz, String packageName, String className,
      int seedId) {
    if (clazz.isClassMetadataEnabled()) {
      clazz.typeName = packageName + className;
    } else {
      /*
       * The initial "" + in the below code is to prevent clazz.hashCode() from
       * being autoboxed. The class literal creation code is run very early
       * during application start up, before class Integer has been initialized.
       */
      clazz.typeName = "Class$"
          + (isInstantiableOrPrimitive(seedId) ? asString(seedId) : "" + clazz.hashCode());
    }

    if (isInstantiable(seedId)) {
      setClassLiteral(seedId, clazz);
    }
  }

  JavaScriptObject enumValueOfFunc;

  int modifiers;

  private Class<?> componentType;

  @SuppressWarnings("unused")
  private JavaScriptObject enumConstantsFunc;

  private Class<? super T> enumSuperclass;

  private Class<? super T> superclass;

  private String typeName;

  private int seedId;

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
