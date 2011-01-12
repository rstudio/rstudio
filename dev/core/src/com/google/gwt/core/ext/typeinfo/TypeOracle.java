/*
 * Copyright 2010 Google Inc.
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
package com.google.gwt.core.ext.typeinfo;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Set;

/**
 * Provides type-related information about a set of source files.
 * <p>
 * All type objects exposed, such as
 * {@link com.google.gwt.core.ext.typeinfo.JClassType} and others, have a stable
 * identity relative to this type oracle instance. Consequently, you can
 * reliably compare object identity of any objects this type oracle produces.
 * For example, the following code relies on this stable identity guarantee:
 *
 * <pre>
 * JClassType o = typeOracle.getJavaLangObject();
 * JClassType s1 = typeOracle.getType(&quot;java.lang.String&quot;);
 * JClassType s2 = typeOracle.getType(&quot;java.lang.String&quot;);
 * assert (s1 == s2);
 * assert (o == s1.getSuperclass());
 * JParameterizedType ls = typeOracle.parse(&quot;java.util.List&lt;java.lang.String&gt;&quot;);
 * assert (ls.getTypeArgs()[0] == s1);
 * </pre>
 *
 * </p>
 */
public abstract class TypeOracle {

  /**
   * A reserved metadata tag to indicates that a field type, method return type
   * or method parameter type is intended to be parameterized. Note that
   * constructor type parameters are not supported at present.
   *
   * @deprecated gwt.typeArgs is not longer supported
   */
  @Deprecated
  public static final String TAG_TYPEARGS = "gwt.typeArgs";

  /**
   * Convenience method to sort class types in a consistent way. Note that the
   * order is subject to change and is intended to generate an "aesthetically
   * pleasing" order rather than a computationally reliable order.
   */
  public static void sort(JClassType[] types) {
    Arrays.sort(types, new Comparator<JClassType>() {
      public int compare(JClassType type1, JClassType type2) {
        String name1 = type1.getQualifiedSourceName();
        String name2 = type2.getQualifiedSourceName();
        return name1.compareTo(name2);
      }
    });
  }

  /**
   * Convenience method to sort constructors in a consistent way. Note that the
   * order is subject to change and is intended to generate an "aesthetically
   * pleasing" order rather than a computationally reliable order.
   */
  public static void sort(JConstructor[] ctors) {
    Arrays.sort(ctors, new Comparator<JConstructor>() {
      public int compare(JConstructor o1, JConstructor o2) {
        // Nothing for now; could enhance to sort based on parameter list
        return 0;
      }
    });
  }

  /**
   * Convenience method to sort fields in a consistent way. Note that the order
   * is subject to change and is intended to generate an "aesthetically
   * pleasing" order rather than a computationally reliable order.
   */
  public static void sort(JField[] fields) {
    Arrays.sort(fields, new Comparator<JField>() {
      public int compare(JField f1, JField f2) {
        String name1 = f1.getName();
        String name2 = f2.getName();
        return name1.compareTo(name2);
      }
    });
  }

  /**
   * Convenience method to sort methods in a consistent way. Note that the order
   * is subject to change and is intended to generate an "aesthetically
   * pleasing" order rather than a computationally reliable order.
   */
  public static void sort(JMethod[] methods) {
    Arrays.sort(methods, new Comparator<JMethod>() {
      public int compare(JMethod m1, JMethod m2) {
        String name1 = m1.getName();
        String name2 = m2.getName();
        return name1.compareTo(name2);
      }
    });
  }

  /**
   * Attempts to find a package by name. All requests for the same package
   * return the same package object.
   *
   * @return <code>null</code> if the package could not be found
   */
  public abstract JPackage findPackage(String pkgName);

  /**
   * Finds a class or interface given its fully-qualified name.
   *
   * @param name fully-qualified class/interface name - for nested classes, use
   *          its source name rather than its binary name (that is, use a "."
   *          rather than a "$")
   *
   * @return <code>null</code> if the type is not found
   */
  public abstract JClassType findType(String name);

  /**
   * Finds a type given its package-relative name. For nested classes, use its
   * source name rather than its binary name (that is, use a "." rather than a
   * "$").
   *
   * @return <code>null</code> if the type is not found
   */
  public abstract JClassType findType(String pkgName, String typeName);

  /**
   * Gets the type object that represents an array of the specified type. The
   * returned type always has a stable identity so as to guarantee that all
   * calls to this method with the same argument return the same object.
   *
   * @param componentType the component type of the array, which can itself be
   *          an array type
   * @return a type object representing an array of the component type
   */
  public abstract JArrayType getArrayType(JType componentType);

  /**
   * Gets a reference to the type object representing
   * <code>java.lang.Object</code>.
   */
  public abstract JClassType getJavaLangObject();

  /**
   * Ensure that a package with the specified name exists as well as its parent
   * packages.
   */
  public abstract JPackage getOrCreatePackage(String name);

  /**
   * Gets a package by name. All requests for the same package return the same
   * package object.
   *
   * @return the package object associated with the specified name
   */
  public abstract JPackage getPackage(String pkgName) throws NotFoundException;

  /**
   * Gets an array of all packages known to this type oracle.
   *
   * @return an array of packages, possibly of zero-length
   */
  public abstract JPackage[] getPackages();

  /**
   * Gets the parameterized type object that represents the combination of a
   * specified raw type and a set of type arguments. The returned type always
   * has a stable identity so as to guarantee that all calls to this method with
   * the same arguments return the same object.
   *
   * @param genericType a generic base class
   * @param enclosingType
   * @param typeArgs the type arguments bound to the specified generic type
   * @return a type object representing this particular binding of type
   *         arguments to the specified generic
   * @throws IllegalArgumentException if the parameterization of a non-static
   *           member type does not specify an enclosing type or if not enough
   *           arguments were specified to parameterize the generic type
   * @throws NullPointerException if genericType is <code>null</code>
   */
  public abstract JParameterizedType getParameterizedType(
      JGenericType genericType, JClassType enclosingType, JClassType[] typeArgs);

  /**
   * Gets the parameterized type object that represents the combination of a
   * specified raw type and a set of type arguments. The returned type always
   * has a stable identity so as to guarantee that all calls to this method with
   * the same arguments return the same object.
   *
   * @param genericType a generic base class
   * @param typeArgs the type arguments bound to the specified generic type
   * @return a type object representing this particular binding of type
   *         arguments to the specified generic
   * @throws IllegalArgumentException if the generic type is a non-static member
   *           type or if not enough arguments were specified to parameterize
   *           the generic type
   * @throws NullPointerException if genericType is <code>null</code>
   */
  public abstract JParameterizedType getParameterizedType(
      JGenericType genericType, JClassType[] typeArgs);

  /**
   * @deprecated This method will always return 0 because a TypeOracle never
   *             gets reloaded anymore. Callers should not rely on this value to
   *             manage static state.
   */
  @Deprecated
  public abstract long getReloadCount();

  /**
   * Returns the single implementation type for an interface returned via
   * {@link #getSingleJsoImplInterfaces()} or <code>null</code> if no JSO
   * implementation is defined.
   */
  public abstract JClassType getSingleJsoImpl(JClassType intf);

  /**
   * Returns an unmodifiable, live view of all interface types that are
   * implemented by exactly one JSO subtype.
   */
  public abstract Set<? extends JClassType> getSingleJsoImplInterfaces();

  /**
   * Finds a type given its fully qualified name. For nested classes, use its
   * source name rather than its binary name (that is, use a "." rather than a
   * "$").
   *
   * @return the specified type
   */
  public abstract JClassType getType(String name) throws NotFoundException;

  /**
   * Finds a type given its package-relative name. For nested classes, use its
   * source name rather than its binary name (that is, use a "." rather than a
   * "$").
   *
   * @return the specified type
   */
  public abstract JClassType getType(String pkgName,
      String topLevelTypeSimpleName) throws NotFoundException;

  /**
   * Gets all types, both top-level and nested.
   *
   * @return an array of types, possibly of zero length
   */
  public abstract JClassType[] getTypes();

  public abstract JWildcardType getWildcardType(
      JWildcardType.BoundType boundType, JClassType typeBound);

  /**
   * Parses the string form of a type to produce the corresponding type object.
   * The types that can be parsed include primitives, class and interface names,
   * simple parameterized types (those without wildcards or bounds), and arrays
   * of the preceding.
   * <p>
   * Examples of types that can be parsed by this method.
   * <ul>
   * <li><code>int</code></li>
   * <li><code>java.lang.Object</code></li>
   * <li><code>java.lang.String[]</code></li>
   * <li><code>char[][]</code></li>
   * <li><code>void</code></li>
   * <li><code>List&lt;Shape&gt;</code></li>
   * <li><code>List&lt;List&lt;Shape&gt;&gt;</code></li>
   * </ul>
   * </p>
   *
   * @param type a type signature to be parsed
   * @return the type object corresponding to the parse type
   */

  public abstract JType parse(String type) throws TypeOracleException;
}
