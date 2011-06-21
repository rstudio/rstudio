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
package com.google.gwt.user.rebind.rpc;

import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.ConfigurationProperty;
import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.SelectionProperty;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.typeinfo.JArrayType;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;

import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

class Shared {

  /**
   * Property used to control whether or not the RPC system will emit warnings
   * when a type has final fields.
   */
  public static final String RPC_PROP_SUPPRESS_NON_STATIC_FINAL_FIELD_WARNINGS =
      "gwt.suppressNonStaticFinalFieldWarnings";

  /**
   * Multi-valued configuration property used to list classes that are
   * (potentially) enhanced with server-only fields, to be handled specially by
   * RPC.
   */
  public static final String RPC_ENHANCED_CLASSES = "rpc.enhancedClasses";

  /**
   * Capitalizes a name.
   * 
   * @param name the string to be capitalized
   * @return the capitalized string
   */
  static String capitalize(String name) {
    return name.substring(0, 1).toUpperCase(Locale.US) + name.substring(1);
  }

  /**
   * Returns a Set of names of classes that may be enhanced with extra
   * server-only fields.
   * 
   * @param propertyOracle The propertyOracle used to access the relevant
   *          configuration property.
   * @return a Set of Strings, or null.
   */
  static Set<String> getEnhancedTypes(PropertyOracle propertyOracle) {
    try {
      ConfigurationProperty prop = propertyOracle.getConfigurationProperty(RPC_ENHANCED_CLASSES);
      return Collections.unmodifiableSet(new HashSet<String>(prop.getValues()));
    } catch (BadPropertyValueException e) {
      return null;
    }
  }

  static String getStreamReadMethodNameFor(JType type) {
    return "read" + getCallSuffix(type);
  }

  static String getStreamWriteMethodNameFor(JType type) {
    return "write" + getCallSuffix(type);
  }

  /**
   * Returns <code>true</code> if warnings should not be emitted for final
   * fields in serializable types.
   */
  static boolean shouldSuppressNonStaticFinalFieldWarnings(TreeLogger logger,
      PropertyOracle propertyOracle) {
    return getBooleanProperty(logger, propertyOracle,
        RPC_PROP_SUPPRESS_NON_STATIC_FINAL_FIELD_WARNINGS, false);
  }

  /**
   * Computes a good name for a class related to the specified type, such that
   * the computed name can be a top-level class in the same package as the
   * specified type.
   * 
   * <p>
   * This method does not currently check for collisions between the synthesized
   * name and an existing top-level type in the same package. It is actually
   * tricky to do so, because on subsequent runs, we'll view our own generated
   * classes as collisions. There's probably some trick we can use in the future
   * to make it totally bulletproof.
   * </p>
   * 
   * @param type the name of the base type, whose name will be built upon to
   *          synthesize a new type name
   * @param suffix a suffix to be used to make the new synthesized type name
   * @return an array of length 2 such that the first element is the package
   *         name and the second element is the synthesized class name
   */
  static String[] synthesizeTopLevelClassName(JClassType type, String suffix) {
    // Gets the basic name of the type. If it's a nested type, the type name
    // will contains dots.
    //
    String className;
    String packageName;

    JType leafType = type.getLeafType();
    if (leafType.isPrimitive() != null) {
      className = leafType.getSimpleSourceName();
      packageName = "com.google.gwt.user.client.rpc.core";
    } else {
      JClassType classOrInterface = leafType.isClassOrInterface();
      assert (classOrInterface != null);
      className = classOrInterface.getName();
      packageName = classOrInterface.getPackage().getName();
    }

    JArrayType isArray = type.isArray();
    if (isArray != null) {
      className += "_Array_Rank_" + isArray.getRank();
    }

    // Add the meaningful suffix.
    //
    className += suffix;

    // Make it a top-level name.
    //
    className = className.replace('.', '_');

    return new String[] {packageName, className};
  }

  /**
   * Determines whether a particular type needs to be cast to become its final
   * type. Primitives and Strings do not, as they are read directly as the
   * correct type. All other Objects need a cast, except for Object itself.
   * 
   * @param type the type in question
   * @return <code>true</code> if the results of a read method must be cast,
   *         otherwise <code>false</code>.
   */
  static boolean typeNeedsCast(JType type) {
    return type.isPrimitive() == null && !type.getQualifiedSourceName().equals("java.lang.String")
        && !type.getQualifiedSourceName().equals("java.lang.Object");
  }

  private static boolean getBooleanProperty(TreeLogger logger, PropertyOracle propertyOracle,
      String propertyName, boolean defaultValue) {
    try {
      SelectionProperty prop = propertyOracle.getSelectionProperty(logger, propertyName);
      String propVal = prop.getCurrentValue();
      if (propVal != null && propVal.length() > 0) {
        return Boolean.valueOf(propVal);
      }
    } catch (BadPropertyValueException e) {
      // Just return the default value.
    }
    return defaultValue;
  }

  /**
   * Gets the suffix needed to make a call for a particular type. For example,
   * the <code>int</code> class needs methods named "readInt" and "writeInt".
   * 
   * @param type the type in question
   * @return the suffix of the method to call
   */
  private static String getCallSuffix(JType type) {
    JParameterizedType isParameterized = type.isParameterized();
    if (isParameterized != null) {
      return getCallSuffix(isParameterized.getRawType());
    } else if (type.isPrimitive() != null) {
      if (type == JPrimitiveType.BOOLEAN) {
        return "Boolean";
      } else if (type == JPrimitiveType.BYTE) {
        return "Byte";
      } else if (type == JPrimitiveType.CHAR) {
        return "Char";
      } else if (type == JPrimitiveType.DOUBLE) {
        return "Double";
      } else if (type == JPrimitiveType.FLOAT) {
        return "Float";
      } else if (type == JPrimitiveType.INT) {
        return "Int";
      } else if (type == JPrimitiveType.LONG) {
        return "Long";
      } else if (type == JPrimitiveType.SHORT) {
        return "Short";
      } else {
        return null;
      }
    } else if (type.getQualifiedSourceName().equals("java.lang.String")) {
      return "String";
    } else {
      return "Object";
    }
  }
}
