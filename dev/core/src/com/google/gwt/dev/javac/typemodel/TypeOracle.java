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
package com.google.gwt.dev.javac.typemodel;

import com.google.gwt.core.ext.typeinfo.BadTypeArgsException;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.ParseException;
import com.google.gwt.core.ext.typeinfo.TypeOracleException;
import com.google.gwt.core.ext.typeinfo.JWildcardType.BoundType;
import com.google.gwt.dev.javac.JavaSourceParser;
import com.google.gwt.dev.jjs.InternalCompilerException;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.util.Name;
import com.google.gwt.dev.util.collect.HashMap;
import com.google.gwt.dev.util.collect.IdentityHashMap;

import org.apache.commons.collections.map.AbstractReferenceMap;
import org.apache.commons.collections.map.ReferenceIdentityMap;
import org.apache.commons.collections.map.ReferenceMap;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Provides type-related information about a set of types.
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
 * 
 */
public class TypeOracle extends com.google.gwt.core.ext.typeinfo.TypeOracle {

  private static class ParameterizedTypeKey {
    private final JClassType enclosingType;
    private final JGenericType genericType;
    private final com.google.gwt.core.ext.typeinfo.JClassType[] typeArgs;

    public ParameterizedTypeKey(JGenericType genericType, JClassType enclosingType,
        com.google.gwt.core.ext.typeinfo.JClassType[] typeArgs) {
      this.genericType = genericType;
      this.enclosingType = enclosingType;
      this.typeArgs = typeArgs;
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof ParameterizedTypeKey)) {
        return false;
      }
      ParameterizedTypeKey other = (ParameterizedTypeKey) obj;
      return genericType == other.genericType && enclosingType == other.enclosingType
          && Arrays.equals(typeArgs, other.typeArgs);
    }

    @Override
    public int hashCode() {
      return 29 * genericType.hashCode() + 17
          * ((enclosingType == null) ? 0 : enclosingType.hashCode()) + Arrays.hashCode(typeArgs);
    }
  }

  private static class WildCardKey {
    private final BoundType boundType;
    private final JClassType typeBound;

    public WildCardKey(BoundType boundType, JClassType typeBound) {
      this.boundType = boundType;
      this.typeBound = typeBound;
    }

    @Override
    public boolean equals(Object obj) {
      if (!(obj instanceof WildCardKey)) {
        return false;
      }
      WildCardKey other = (WildCardKey) obj;
      return boundType == other.boundType && typeBound == other.typeBound;
    }

    @Override
    public int hashCode() {
      return 29 * typeBound.hashCode() + boundType.hashCode();
    }
  }

  static final int MOD_ABSTRACT = 0x00000001;
  static final int MOD_FINAL = 0x00000002;
  static final int MOD_NATIVE = 0x00000004;
  static final int MOD_PRIVATE = 0x00000008;
  static final int MOD_PROTECTED = 0x00000010;
  static final int MOD_PUBLIC = 0x00000020;
  static final int MOD_STATIC = 0x00000040;
  static final int MOD_TRANSIENT = 0x00000080;
  static final int MOD_VOLATILE = 0x00000100;

  static final Annotation[] NO_ANNOTATIONS = new Annotation[0];
  static final JClassType[] NO_JCLASSES = new JClassType[0];
  static final JConstructor[] NO_JCTORS = new JConstructor[0];
  static final JField[] NO_JFIELDS = new JField[0];
  static final JMethod[] NO_JMETHODS = new JMethod[0];
  static final JPackage[] NO_JPACKAGES = new JPackage[0];
  static final JParameter[] NO_JPARAMS = new JParameter[0];
  static final JType[] NO_JTYPES = new JType[0];
  static final String[][] NO_STRING_ARR_ARR = new String[0][];
  static final String[] NO_STRINGS = new String[0];

  private static final String JSO_CLASS = "com.google.gwt.core.client.JavaScriptObject";

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

  static String[] modifierBitsToNamesForField(int bits) {
    List<String> strings = modifierBitsToNamesForMethodsAndFields(bits);

    if (0 != (bits & MOD_VOLATILE)) {
      strings.add("volatile");
    }
    
    if (0 != (bits & MOD_TRANSIENT)) {
      strings.add("transient");
    }
   
    return strings.toArray(NO_STRINGS);
  }
  
  static String[] modifierBitsToNamesForMethod(int bits) {
    List<String> strings = modifierBitsToNamesForMethodsAndFields(bits);
    
    if (0 != (bits & MOD_ABSTRACT)) {
      strings.add("abstract");
    }
    
    if (0 != (bits & MOD_NATIVE)) {
      strings.add("native");
    } 
    
    return strings.toArray(NO_STRINGS);
  }

  private static JClassType[] cast(com.google.gwt.core.ext.typeinfo.JClassType[] extTypeArgs) {
    JClassType[] result = new JClassType[extTypeArgs.length];
    System.arraycopy(extTypeArgs, 0, result, 0, extTypeArgs.length);
    return result;
  }
  
  /**
   * Converts modifier bits, which are common to fields and methods, to
   * readable names.
   * 
   * @see TypeOracle#modifierBitsToNamesForField(int) modifierBitsToNamesForField
   * @see TypeOracle#modifierBitsToNamesForMethod(int) modifierBitsToNamesForMethod
   */
  private static List<String> modifierBitsToNamesForMethodsAndFields(int bits) {
    List<String> strings = new ArrayList<String>();

    // The order is based on the order in which we want them to appear.
    //
    if (0 != (bits & MOD_PUBLIC)) {
      strings.add("public");
    }

    if (0 != (bits & MOD_PRIVATE)) {
      strings.add("private");
    }

    if (0 != (bits & MOD_PROTECTED)) {
      strings.add("protected");
    }

    if (0 != (bits & MOD_STATIC)) {
      strings.add("static");
    }

    if (0 != (bits & MOD_FINAL)) {
      strings.add("final");
    }

    return strings;
  }

  /**
   * A map of fully-qualify source names (ie, use "." rather than "$" for nested
   * classes) to JRealClassTypes.
   */
  private final Map<String, JRealClassType> allTypes = new HashMap<String, JRealClassType>();

  /**
   * Cached types that represent Arrays of other types. These types are created
   * as needed.
   */
  @SuppressWarnings("unchecked")
  private final Map<JType, JArrayType> arrayTypes = new ReferenceIdentityMap(
      AbstractReferenceMap.WEAK, AbstractReferenceMap.WEAK, true);

  /**
   * Cached singleton type representing <code>java.lang.Object</code>.
   */
  private JClassType javaLangObject;

  private final JavaSourceParser javaSourceParser = new JavaSourceParser();

  /**
   * Maps SingleJsoImpl interfaces to the implementing JSO subtype.
   */
  private final Map<JClassType, JClassType> jsoSingleImpls =
      new IdentityHashMap<JClassType, JClassType>();

  /**
   * Cached map of all packages thus far encountered.
   */
  private final Map<String, JPackage> packages = new HashMap<String, JPackage>();

  /**
   * Subclasses of generic types that have type parameters filled in. These
   * types are created as needed.
   */
  @SuppressWarnings("unchecked")
  private final Map<ParameterizedTypeKey, JParameterizedType> parameterizedTypes =
      new ReferenceMap(AbstractReferenceMap.HARD, AbstractReferenceMap.WEAK, true);

  /**
   * A list of recently-added types that will be fully initialized on the next
   * call to {@link #finish}.
   */
  private final List<JRealClassType> recentTypes = new ArrayList<JRealClassType>();

  private JWildcardType unboundWildCardType;

  @SuppressWarnings("unchecked")
  private final Map<WildCardKey, JWildcardType> wildcardTypes = new ReferenceMap(
      AbstractReferenceMap.HARD, AbstractReferenceMap.WEAK, true);

  public TypeOracle() {
    // Always create the default package.
    //
    getOrCreatePackage("");
  }

  /**
   * Attempts to find a package by name. All requests for the same package
   * return the same package object.
   * 
   * @return <code>null</code> if the package could not be found
   */
  @Override
  public JPackage findPackage(String pkgName) {
    return packages.get(pkgName);
  }

  /**
   * Finds a class or interface given its fully-qualified name.
   * 
   * @param name fully-qualified class/interface name - for nested classes, use
   *          its source name rather than its binary name (that is, use a "."
   *          rather than a "$")
   * 
   * @return <code>null</code> if the type is not found
   */
  @Override
  public JClassType findType(String name) {
    assert Name.isSourceName(name);
    return allTypes.get(name);
  }

  /**
   * Finds a type given its package-relative name. For nested classes, use its
   * source name rather than its binary name (that is, use a "." rather than a
   * "$").
   * 
   * @return <code>null</code> if the type is not found
   */
  @Override
  public JClassType findType(String pkgName, String typeName) {
    assert Name.isSourceName(typeName);
    JPackage pkg = findPackage(pkgName);
    if (pkg != null) {
      JClassType type = pkg.findType(typeName);
      if (type != null) {
        return type;
      }
    }
    return null;
  }

  /**
   * Gets the type object that represents an array of the specified type. The
   * returned type always has a stable identity so as to guarantee that all
   * calls to this method with the same argument return the same object.
   * 
   * @param componentType the component type of the array, which can itself be
   *          an array type
   * @return a type object representing an array of the component type
   */
  @Override
  public JArrayType getArrayType(JType componentType) {
    JArrayType arrayType = arrayTypes.get(componentType);
    if (arrayType == null) {
      arrayType = new JArrayType(componentType, this);
      arrayTypes.put(componentType, arrayType);
    }
    return arrayType;
  }

  /**
   * Gets a reference to the type object representing
   * <code>java.lang.Object</code>.
   */
  @Override
  public JClassType getJavaLangObject() {
    if (javaLangObject == null) {
      javaLangObject = findType("java.lang.Object");
      assert javaLangObject != null;
    }
    return javaLangObject;
  }

  /**
   * Ensure that a package with the specified name exists as well as its parent
   * packages.
   */
  @Override
  public JPackage getOrCreatePackage(String name) {
    int i = name.lastIndexOf('.');
    if (i != -1) {
      // Ensure the parent package is also created.
      //
      getOrCreatePackage(name.substring(0, i));
    }

    JPackage pkg = packages.get(name);
    if (pkg == null) {
      pkg = new JPackage(name);
      packages.put(name, pkg);
    }
    return pkg;
  }

  /**
   * Gets a package by name. All requests for the same package return the same
   * package object.
   * 
   * @return the package object associated with the specified name
   */
  @Override
  public JPackage getPackage(String pkgName) throws NotFoundException {
    JPackage result = findPackage(pkgName);
    if (result == null) {
      throw new NotFoundException(pkgName);
    }
    return result;
  }

  /**
   * Gets an array of all packages known to this type oracle.
   * 
   * @return an array of packages, possibly of zero-length
   */
  @Override
  public JPackage[] getPackages() {
    return packages.values().toArray(NO_JPACKAGES);
  }

  /**
   * Gets the parameterized type object that represents the combination of a
   * specified raw type and a set of type arguments. The returned type always
   * has a stable identity so as to guarantee that all calls to this method with
   * the same arguments return the same object.
   * 
   * @param extGenericType a generic base class
   * @param extEnclosingType
   * @param extTypeArgs the type arguments bound to the specified generic type
   * @return a type object representing this particular binding of type
   *         arguments to the specified generic
   * @throws IllegalArgumentException if the parameterization of a non-static
   *           member type does not specify an enclosing type or if not enough
   *           arguments were specified to parameterize the generic type
   * @throws NullPointerException if genericType is <code>null</code>
   */
  @Override
  public JParameterizedType getParameterizedType(
      com.google.gwt.core.ext.typeinfo.JGenericType extGenericType,
      com.google.gwt.core.ext.typeinfo.JClassType extEnclosingType,
      com.google.gwt.core.ext.typeinfo.JClassType[] extTypeArgs) {
    JGenericType genericType = (JGenericType) extGenericType;
    JClassType enclosingType = (JClassType) extEnclosingType;
    JClassType[] typeArgs = cast(extTypeArgs);
    ParameterizedTypeKey key = new ParameterizedTypeKey(genericType, enclosingType, typeArgs);
    JParameterizedType result = parameterizedTypes.get(key);
    if (result != null) {
      return result;
    }

    if (genericType.isMemberType() && !genericType.isStatic()) {
      if (genericType.getEnclosingType().isGenericType() != null
          && enclosingType.isParameterized() == null && enclosingType.isRawType() == null) {
        /*
         * If the generic type is a non-static member type enclosed by a generic
         * type then the enclosing type for this parameterized type should be
         * raw or parameterized.
         */
        throw new IllegalArgumentException("Generic type '"
            + genericType.getParameterizedQualifiedSourceName()
            + "' is a non-static member type, but the enclosing type '"
            + enclosingType.getQualifiedSourceName() + "' is not a parameterized or raw type");
      }
    }

    JTypeParameter[] typeParameters = genericType.getTypeParameters();
    if (typeArgs.length < typeParameters.length) {
      throw new IllegalArgumentException(
          "Not enough type arguments were specified to parameterize '"
              + genericType.getParameterizedQualifiedSourceName() + "'");
    } else {
      /*
       * TODO: Should WARN if we specify too many type arguments but we have no
       * logger.
       */
    }

    // TODO: validate that the type arguments satisfy the generic type parameter
    // bounds if any were specified

    result = new JParameterizedType(genericType, enclosingType, typeArgs);
    parameterizedTypes.put(key, result);
    return result;
  }

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
  @Override
  public JParameterizedType getParameterizedType(
      com.google.gwt.core.ext.typeinfo.JGenericType genericType,
      com.google.gwt.core.ext.typeinfo.JClassType[] typeArgs) {
    return getParameterizedType(genericType, null, typeArgs);
  }

  /**
   * @deprecated This method will always return 0 because a TypeOracle never
   *             gets reloaded anymore. Callers should not rely on this value to
   *             manage static state.
   */
  @Deprecated
  @Override
  public long getReloadCount() {
    return 0;
  }

  /**
   * Returns the single implementation type for an interface returned via
   * {@link #getSingleJsoImplInterfaces()} or <code>null</code> if no JSO
   * implementation is defined.
   */
  @Override
  public JClassType getSingleJsoImpl(com.google.gwt.core.ext.typeinfo.JClassType intf) {
    assert intf.isInterface() == intf;
    return jsoSingleImpls.get(intf);
  }

  /**
   * Returns an unmodifiable, live view of all interface types that are
   * implemented by exactly one JSO subtype.
   */
  @Override
  public Set<? extends com.google.gwt.core.ext.typeinfo.JClassType> getSingleJsoImplInterfaces() {
    return Collections.unmodifiableSet(jsoSingleImpls.keySet());
  }

  /**
   * Finds a type given its fully qualified name. For nested classes, use its
   * source name rather than its binary name (that is, use a "." rather than a
   * "$").
   * 
   * @return the specified type
   */
  @Override
  public JClassType getType(String name) throws NotFoundException {
    assert Name.isSourceName(name);
    JClassType type = findType(name);
    if (type == null) {
      throw new NotFoundException(name);
    }
    return type;
  }

  /**
   * Finds a type given its package-relative name. For nested classes, use its
   * source name rather than its binary name (that is, use a "." rather than a
   * "$").
   * 
   * @return the specified type
   */
  @Override
  public JClassType getType(String pkgName, String topLevelTypeSimpleName) throws NotFoundException {
    assert Name.isSourceName(topLevelTypeSimpleName);
    JClassType type = findType(pkgName, topLevelTypeSimpleName);
    if (type == null) {
      throw new NotFoundException(pkgName + "." + topLevelTypeSimpleName);
    }
    return type;
  }

  /**
   * Gets all types, both top-level and nested.
   * 
   * @return an array of types, possibly of zero length
   */
  @Override
  public JClassType[] getTypes() {
    Collection<JRealClassType> values = allTypes.values();
    JClassType[] result = values.toArray(new JClassType[values.size()]);
    Arrays.sort(result, new Comparator<JClassType>() {
      public int compare(JClassType o1, JClassType o2) {
        return o1.getQualifiedSourceName().compareTo(o2.getQualifiedSourceName());
      }
    });
    return result;
  }

  @Override
  public JWildcardType getWildcardType(
      com.google.gwt.core.ext.typeinfo.JWildcardType.BoundType boundType,
      com.google.gwt.core.ext.typeinfo.JClassType extTypeBound) {
    // Special fast case for <? extends Object>
    // TODO(amitmanjhi): make sure this actually does speed things up!
    JClassType typeBound = (JClassType) extTypeBound;
    if (typeBound == getJavaLangObject() && boundType == BoundType.UNBOUND) {
      if (unboundWildCardType == null) {
        unboundWildCardType = new JWildcardType(boundType, typeBound);
      }
      return unboundWildCardType;
    }
    // End special case / todo.

    WildCardKey key = new WildCardKey(boundType, typeBound);
    JWildcardType result = wildcardTypes.get(key);
    if (result != null) {
      return result;
    }

    result = new JWildcardType(boundType, typeBound);
    wildcardTypes.put(key, result);
    return result;
  }

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
  @Override
  public JType parse(String type) throws TypeOracleException {
    // Remove all internal and external whitespace.
    //
    type = type.replaceAll("\\\\s", "");

    // Recursively parse.
    //
    return parseImpl(type);
  }

  void addNewType(JRealClassType newType) {
    String fqcn = newType.getQualifiedSourceName();
    allTypes.put(fqcn, newType);
    recentTypes.add(newType);
  }

  /**
   * Called to add a source reference for a top-level class type.
   */
  void addSourceReference(JRealClassType type, Resource sourceFile) {
    javaSourceParser.addSourceForType(type, sourceFile);
  }

  /**
   * Called after a block of new types are added.
   */
  void finish() {
    JClassType[] newTypes = recentTypes.toArray(new JClassType[recentTypes.size()]);
    computeHierarchyRelationships(newTypes);
    computeSingleJsoImplData(newTypes);
    recentTypes.clear();
  }

  JavaSourceParser getJavaSourceParser() {
    return javaSourceParser;
  }

  private List<JClassType> classChain(JClassType cls) {
    LinkedList<JClassType> chain = new LinkedList<JClassType>();
    while (cls != null) {
      chain.addFirst(cls);
      cls = cls.getSuperclass();
    }
    return chain;
  }

  /**
   * Determines whether the given class fully implements an interface (either
   * directly or via inherited methods).
   */
  private boolean classFullyImplements(JClassType cls, JClassType intf) {
    // If the interface has at least 1 method, then the class must at
    // least nominally implement the interface.
    if ((intf.getMethods().length > 0) && !intf.isAssignableFrom(cls)) {
      return false;
    }

    // Check to see whether it implements all the interfaces methods.
    for (JMethod meth : intf.getInheritableMethods()) {
      if (!classImplementsMethod(cls, meth)) {
        return false;
      }
    }
    return true;
  }

  private boolean classImplementsMethod(JClassType cls, JMethod meth) {
    while (cls != null) {
      JMethod found = cls.findMethod(meth.getName(), meth.getParameterTypes());
      if ((found != null) && !found.isAbstract()) {
        return true;
      }
      cls = cls.getSuperclass();
    }
    return false;
  }

  private void computeHierarchyRelationships(JClassType[] types) {
    // For each type, walk up its hierarchy chain and tell each supertype
    // about its subtype.
    for (JClassType type : types) {
      type.notifySuperTypes();
    }
  }

  /**
   * Updates the list of jsoSingleImpl types from recently-added types.
   */
  private void computeSingleJsoImplData(JClassType... newTypes) {
    JClassType jsoType = findType(JSO_CLASS);
    if (jsoType == null) {
      return;
    }

    for (JClassType type : newTypes) {
      if (!jsoType.isAssignableFrom(type)) {
        continue;
      }

      for (JClassType intf : JClassType.getFlattenedSuperTypeHierarchy(type)) {
        // If intf refers to a JParameterizedType, we need to use its generic
        // base type instead.
        if (intf instanceof JParameterizedType) {
          intf = ((JParameterizedType) intf).getBaseType();
        }

        if (intf.isInterface() == null) {
          // Not an interface
          continue;
        }

        if (intf.getOverridableMethods().length == 0) {
          /*
           * Record a tag interface as being implemented by JSO, since they
           * don't actually have any methods and we want to avoid spurious
           * messages about multiple JSO types implementing a common interface.
           */
          jsoSingleImpls.put(intf, jsoType);
          continue;
        }

        /*
         * If the previously-registered implementation type for a SingleJsoImpl
         * interface is a subtype of the type we're currently looking at, we
         * want to choose the least-derived class.
         */
        JClassType previousType = jsoSingleImpls.get(intf);
        if (previousType == null) {
          jsoSingleImpls.put(intf, type);
        } else if (type.isAssignableFrom(previousType)) {
          jsoSingleImpls.put(intf, type);
        } else if (type.isAssignableTo(previousType)) {
          // Do nothing
        } else {
          // Special case: If two JSOs implement the same interface, but they
          // share a common base class that fully implements that interface,
          // then choose that base class.
          JClassType impl = findFullyImplementingBase(intf, type, previousType);
          if (impl != null) {
            jsoSingleImpls.put(intf, impl);
          } else {
            throw new InternalCompilerException("Already seen an implementing JSO subtype ("
                + previousType.getName() + ") for interface (" + intf.getName()
                + ") while examining newly-added type (" + type.getName() + "). This is a bug in "
                + "JSORestrictionsChecker.");
          }
        }
      }
    }
  }

  /**
   * Determines whether both classes A and B share a common superclass which
   * fully implements the given interface.
   */
  private JClassType findFullyImplementingBase(JClassType intf, JClassType a, JClassType b) {
    JClassType common = findNearestCommonBase(a, b);
    if (classFullyImplements(common, intf)) {
      return common;
    }
    return null;
  }

  /**
   * Finds the nearest common base class of the given classes.
   */
  private JClassType findNearestCommonBase(JClassType a, JClassType b) {
    List<JClassType> as = classChain(a);
    List<JClassType> bs = classChain(b);

    JClassType match = null;
    Iterator<JClassType> ait = as.iterator();
    Iterator<JClassType> bit = bs.iterator();
    while (ait.hasNext() && bit.hasNext()) {
      a = ait.next();
      b = bit.next();
      if (a.equals(b)) {
        match = a;
      } else {
        break;
      }
    }
    return match;
  }

  private JType parseImpl(String type) throws NotFoundException, ParseException,
      BadTypeArgsException {
    if (type.endsWith("[]")) {
      String remainder = type.substring(0, type.length() - 2);
      JType componentType = parseImpl(remainder);
      return getArrayType(componentType);
    }

    if (type.endsWith(">")) {
      int bracket = type.indexOf('<');
      if (bracket == -1) {
        throw new ParseException("Mismatched brackets; expected '<' to match subsequent '>'");
      }

      // Resolve the raw type.
      //
      String rawTypeName = type.substring(0, bracket);
      JType rawType = parseImpl(rawTypeName);
      if (rawType.isParameterized() != null) {
        // The raw type cannot itself be parameterized.
        //
        throw new BadTypeArgsException(
            "Only non-parameterized classes and interface can be parameterized");
      } else if (rawType.isClassOrInterface() == null) {
        // The raw type must be a class or interface
        // (not an array or primitive).
        //
        throw new BadTypeArgsException("Only classes and interface can be parameterized, so "
            + rawType.getQualifiedSourceName() + " cannot be used in this context");
      } else if (rawType.isGenericType() == null) {
        throw new BadTypeArgsException("'" + rawType.getQualifiedSourceName()
            + "' is not a generic type; only generic types can be parameterized");
      }

      // Resolve each type argument.
      //
      String typeArgContents = type.substring(bracket + 1, type.length() - 1);
      JClassType[] typeArgs = parseTypeArgContents(typeArgContents);

      // Intern this type.
      //
      return getParameterizedType(rawType.isGenericType(), typeArgs);
    }

    JType result = JPrimitiveType.parse(type);
    if (result != null) {
      return result;
    }

    result = findType(type);
    if (result != null) {
      return result;
    }

    throw new NotFoundException("Unable to recognize '" + type
        + "' as a type name (is it fully qualified?)");
  }

  private void parseTypeArgComponent(List<JClassType> typeArgList, String typeArgComponent)
      throws NotFoundException, ParseException, BadTypeArgsException {
    JType typeArg = parseImpl(typeArgComponent);
    if (typeArg.isPrimitive() != null) {
      // Cannot be primitive.
      //
      throw new BadTypeArgsException("Type arguments cannot be primitives, so '"
          + typeArg.getQualifiedSourceName() + "' cannot be used in this context");
    }

    typeArgList.add((JClassType) typeArg);
  }

  /**
   * Returns an array of types specified inside of a gwt.typeArgs javadoc
   * annotation.
   */
  private JClassType[] parseTypeArgContents(String typeArgContents) throws ParseException,
      NotFoundException, BadTypeArgsException {
    List<JClassType> typeArgList = new ArrayList<JClassType>();

    int start = 0;
    for (int offset = 0, length = typeArgContents.length(); offset < length; ++offset) {
      char ch = typeArgContents.charAt(offset);
      switch (ch) {
        case '<':
          // scan for closing '>' while ignoring commas
          for (int depth = 1; depth > 0;) {
            if (++offset == length) {
              throw new ParseException("Mismatched brackets; expected '<' to match subsequent '>'");
            }

            char ich = typeArgContents.charAt(offset);
            if (ich == '<') {
              ++depth;
            } else if (ich == '>') {
              --depth;
            }
          }
          break;
        case '>':
          throw new ParseException("No matching '<' for '>'");
        case ',':
          String typeArgComponent = typeArgContents.substring(start, offset);
          parseTypeArgComponent(typeArgList, typeArgComponent);
          start = offset + 1;
          break;
        default:
          break;
      }
    }

    String typeArgComponent = typeArgContents.substring(start);
    parseTypeArgComponent(typeArgList, typeArgComponent);

    JClassType[] typeArgs = typeArgList.toArray(new JClassType[typeArgList.size()]);
    return typeArgs;
  }
}
