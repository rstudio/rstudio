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
package com.google.gwt.core.ext.typeinfo;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.generator.GenUtil;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Provides type-related information about a set of source files, including doc
 * comment metadata.
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
 * assert(s1 == s2);
 * assert(o == s1.getSuperclass());
 * JParameterizedType ls = typeOracle.parse(&quot;java.util.List&lt;java.lang.String&gt;&quot;);
 * assert(ls.getTypeArgs()[0] == s1);
 * </pre>
 * 
 * </p>
 */
public class TypeOracle {
  /**
   * A reserved metadata tag to indicates that a field type, method return type
   * or method parameter type is intended to be parameterized. Note that
   * constructor type parameters are not supported at present.
   */
  public static final String TAG_TYPEARGS = "gwt.typeArgs";
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

  static String combine(String[] strings, int startIndex) {
    StringBuffer sb = new StringBuffer();
    for (int i = startIndex; i < strings.length; i++) {
      String s = strings[i];
      sb.append(s);
    }
    return sb.toString();
  }

  static String[] modifierBitsToNames(int bits) {
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

    if (0 != (bits & MOD_ABSTRACT)) {
      strings.add("abstract");
    }

    if (0 != (bits & MOD_FINAL)) {
      strings.add("final");
    }

    if (0 != (bits & MOD_NATIVE)) {
      strings.add("native");
    }

    if (0 != (bits & MOD_TRANSIENT)) {
      strings.add("transient");
    }

    if (0 != (bits & MOD_VOLATILE)) {
      strings.add("volatile");
    }

    return strings.toArray(NO_STRINGS);
  }

  /**
   * Returns true if the type has been invalidated because it is in the set of
   * invalid types or if it is a parameterized type and either it raw type or
   * any one of its type args has been invalidated.
   * 
   * @param type type to check
   * @param invalidTypes set of type known to be invalid
   * @return true if the type has been invalidated
   */
  private static boolean isInvalidatedTypeRecursive(JType type,
      Set<JRealClassType> invalidTypes) {
    if (type instanceof JParameterizedType) {
      JParameterizedType parameterizedType = (JParameterizedType) type;
      if (isInvalidatedTypeRecursive(parameterizedType.getBaseType(),
          invalidTypes)) {
        return true;
      }

      JType[] typeArgs = parameterizedType.getTypeArgs();
      for (int i = 0; i < typeArgs.length; ++i) {
        JType typeArg = typeArgs[i];

        if (isInvalidatedTypeRecursive(typeArg, invalidTypes)) {
          return true;
        }
      }

      return false;
    } else {
      return invalidTypes.contains(type);
    }
  }

  private final Set<JRealClassType> allTypes = new HashSet<JRealClassType>();

  private final Map<JType, JArrayType> arrayTypes = new IdentityHashMap<JType, JArrayType>();

  /**
   * A set of invalidated types queued up to be removed on the next
   * {@link #reset()}.
   */
  private final Set<JRealClassType> invalidatedTypes = new HashSet<JRealClassType>();

  private JClassType javaLangObject;

  private final Map<String, JPackage> packages = new HashMap<String, JPackage>();

  private final Map<String, List<JParameterizedType>> parameterizedTypes = new HashMap<String, List<JParameterizedType>>();

  /**
   * A list of recently-added types that will be fully initialized on the next
   * call to {@link #finish(TreeLogger)}.
   */
  private final List<JRealClassType> recentTypes = new ArrayList<JRealClassType>();

  private int reloadCount = 0;

  private final Map<String, List<JWildcardType>> wildcardTypes = new HashMap<String, List<JWildcardType>>();

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
  public JPackage findPackage(String pkgName) {
    return packages.get(pkgName);
  }

  /**
   * Finds a class or interface given its fully-qualified name. For nested
   * classes, use its source name rather than its binary name (that is, use a
   * "." rather than a "$").
   * 
   * @return <code>null</code> if the type is not found
   */
  public JClassType findType(String name) {
    // Try the dotted pieces, right to left.
    //
    int i = name.length() - 1;
    while (i >= 0) {
      int dot = name.lastIndexOf('.', i);
      String pkgName = "";
      String typeName = name;
      if (dot != -1) {
        pkgName = name.substring(0, dot);
        typeName = name.substring(dot + 1);
        i = dot - 1;
      } else {
        i = -1;
      }
      JClassType result = findType(pkgName, typeName);
      if (result != null) {
        return result;
      }
    }
    return null;
  }

  /**
   * Finds a type given its package-relative name. For nested classes, use its
   * source name rather than its binary name (that is, use a "." rather than a
   * "$").
   * 
   * @return <code>null</code> if the type is not found
   */
  public JClassType findType(String pkgName, String typeName) {
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
   * Called after a block of new types are added.
   * 
   * TODO: make not public?
   */
  public void finish(TreeLogger logger) {
    JClassType[] newTypes = recentTypes.toArray(NO_JCLASSES);
    computeHierarchyRelationships(newTypes);
    consumeTypeArgMetaData(logger, newTypes);
    recentTypes.clear();
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
  public JPackage[] getPackages() {
    return packages.values().toArray(NO_JPACKAGES);
  }

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
  public JParameterizedType getParameterizedType(JGenericType genericType,
      JClassType enclosingType, JClassType[] typeArgs) {
    if (genericType == null) {
      throw new NullPointerException("genericType");
    }

    if (genericType.isMemberType() && !genericType.isStatic()) {
      if (genericType.getEnclosingType().isGenericType() != null
          && enclosingType.isParameterized() == null
          && enclosingType.isRawType() == null) {
        /*
         * If the generic type is a non-static member type enclosed by a generic
         * type then the enclosing type for this parameterized type should be
         * raw or parameterized.
         */
        throw new IllegalArgumentException("Generic type '"
            + genericType.getParameterizedQualifiedSourceName()
            + "' is a non-static member type, but the enclosing type '"
            + enclosingType.getQualifiedSourceName()
            + "' is not a parameterized or raw type");
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

    // Uses the generated string signature to intern parameterized types.
    //
    JParameterizedType parameterized = new JParameterizedType(genericType,
        enclosingType, typeArgs);

    // TODO: parameterized qualified source name does not account for the type
    // args of the enclosing type
    String sig = parameterized.getParameterizedQualifiedSourceName();
    List<JParameterizedType> candidates = parameterizedTypes.get(sig);
    if (candidates == null) {
      candidates = new ArrayList<JParameterizedType>();
      parameterizedTypes.put(sig, candidates);
    } else {
      for (JParameterizedType candidate : candidates) {
        if (candidate.hasTypeArgs(typeArgs)) {
          return candidate;
        }
      }
    }

    candidates.add(parameterized);

    return parameterized;
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
  public JParameterizedType getParameterizedType(JGenericType genericType,
      JClassType[] typeArgs) {
    return getParameterizedType(genericType, null, typeArgs);
  }

  public long getReloadCount() {
    return reloadCount;
  }

  /**
   * Finds a type given its fully qualified name. For nested classes, use its
   * source name rather than its binary name (that is, use a "." rather than a
   * "$").
   * 
   * @return the specified type
   */
  public JClassType getType(String name) throws NotFoundException {
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
  public JClassType getType(String pkgName, String topLevelTypeSimpleName)
      throws NotFoundException {
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
  public JClassType[] getTypes() {
    return allTypes.toArray(NO_JCLASSES);
  }

  public JWildcardType getWildcardType(JWildcardType.BoundType boundType,
      JClassType typeBound) {
    JWildcardType wildcardType = new JWildcardType(boundType, typeBound);
    String sig = wildcardType.getQualifiedSourceName();
    List<JWildcardType> candidates = wildcardTypes.get(sig);
    if (candidates == null) {
      candidates = new ArrayList<JWildcardType>();
      wildcardTypes.put(sig, candidates);
    } else {
      for (JWildcardType candidate : candidates) {
        if (candidate.boundsMatch(wildcardType)) {
          return candidate;
        }
      }
    }

    candidates.add(wildcardType);

    return wildcardType;
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
  public JType parse(String type) throws TypeOracleException {
    // Remove all internal and external whitespace.
    //
    type = type.replaceAll("\\\\s", "");

    // Recursively parse.
    //
    return parseImpl(type);
  }

  /**
   * Reset this type oracle for rebuild.
   * 
   * TODO: make this not public.
   */
  public void reset() {
    recentTypes.clear();
    if (!invalidatedTypes.isEmpty()) {
      invalidateTypes(invalidatedTypes);
      invalidatedTypes.clear();
      ++reloadCount;
    }
  }

  /**
   * Convenience method to sort class types in a consistent way. Note that the
   * order is subject to change and is intended to generate an "aesthetically
   * pleasing" order rather than a computationally reliable order.
   */
  public void sort(JClassType[] types) {
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
  public void sort(JConstructor[] ctors) {
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
  public void sort(JField[] fields) {
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
  public void sort(JMethod[] methods) {
    Arrays.sort(methods, new Comparator<JMethod>() {
      public int compare(JMethod m1, JMethod m2) {
        String name1 = m1.getName();
        String name2 = m2.getName();
        return name1.compareTo(name2);
      }
    });
  }

  void addNewType(JRealClassType newType) {
    allTypes.add(newType);
    recentTypes.add(newType);
  }

  void invalidate(JRealClassType realClassType) {
    invalidatedTypes.add(realClassType);
  }

  private void computeHierarchyRelationships(JClassType[] types) {
    // For each type, walk up its hierarchy chain and tell each supertype
    // about its subtype.
    for (int i = 0; i < types.length; i++) {
      JClassType type = types[i];
      type.notifySuperTypes();
    }
  }

  private void consumeTypeArgMetaData(TreeLogger logger, JClassType[] types) {
    if (GenUtil.warnAboutMetadata()) {
      logger = logger.branch(
          TreeLogger.DEBUG,
          "Scanning source for uses of the deprecated "
              + TAG_TYPEARGS
              + " javadoc annotation; please use Java parameterized types instead",
          null);
    }
    for (int i = 0; i < types.length; i++) {
      JClassType type = types[i];
      // CTORS not supported yet

      TreeLogger branch = logger.branch(TreeLogger.DEBUG, "Type "
          + type.getQualifiedSourceName(), null);

      consumeTypeArgMetaData(branch, type.getMethods());
      consumeTypeArgMetaData(branch, type.getFields());
    }
  }

  private void consumeTypeArgMetaData(TreeLogger logger, JField[] fields) {
    TreeLogger branch;
    for (int i = 0; i < fields.length; i++) {
      JField field = fields[i];

      String[][] tokensArray = field.getMetaData(TAG_TYPEARGS);
      if (tokensArray.length == 0) {
        // No tag.
        continue;
      }

      try {
        String msg = "Field " + field.getName();
        branch = logger.branch(TreeLogger.TRACE, msg, null);

        if (tokensArray.length > 1) {
          // Too many.
          branch.log(TreeLogger.WARN, "Metadata error on field '"
              + field.getName() + "' in type '" + field.getEnclosingType()
              + "': expecting at most one " + TAG_TYPEARGS
              + " (the last one will be used)", null);
        }

        // (1) Parse it.
        // (2) Update the field's type.
        // If it wasn't a valid parameterized type, parse() would've thrown.
        //
        JType fieldType = field.getType();
        String[] token = tokensArray[tokensArray.length - 1];
        JType resultingType = determineActualType(branch, fieldType, token, 0);

        if (GenUtil.warnAboutMetadata()) {
          branch.log(TreeLogger.WARN, "Deprecated use of " + TAG_TYPEARGS
              + " for field " + field.getName() + "; Please use "
              + resultingType.getParameterizedQualifiedSourceName()
              + " as the field's type", null);
        }

        field.setType(resultingType);
      } catch (UnableToCompleteException e) {
        // Continue; the problem will have been logged.
        //
      }
    }
  }

  private void consumeTypeArgMetaData(TreeLogger logger, JMethod[] methods) {
    TreeLogger branch;
    for (int i = 0; i < methods.length; i++) {
      JMethod method = methods[i];

      String[][] tokensArray = method.getMetaData(TAG_TYPEARGS);
      if (tokensArray.length == 0) {
        // No tag.
        continue;
      }
      try {
        String msg = "Method " + method.getReadableDeclaration();
        branch = logger.branch(TreeLogger.TRACE, msg, null);

        // Okay, parse each one and correlate it to a part of the decl.
        //
        boolean returnTypeHandled = false;
        Set<JParameter> paramsAlreadySet = new HashSet<JParameter>();
        for (int j = 0; j < tokensArray.length; j++) {
          String[] tokens = tokensArray[j];
          // It is either referring to the return type or a parameter type.
          //
          if (tokens.length == 0) {
            // Expecting at least something.
            //
            branch.log(TreeLogger.WARN,
                "Metadata error: expecting tokens after " + TAG_TYPEARGS, null);
            throw new UnableToCompleteException();
          }

          // See if the first token is a parameter name.
          //
          JParameter param = method.findParameter(tokens[0]);
          if (param != null) {
            if (!paramsAlreadySet.contains(param)) {
              // These are type args for a param.
              //
              JType resultingType = determineActualType(branch,
                  param.getType(), tokens, 1);
              param.setType(resultingType);

              if (GenUtil.warnAboutMetadata()) {
                branch.log(TreeLogger.WARN, "Deprecated use of " + TAG_TYPEARGS
                    + " for parameter " + param.getName() + "; Please use "
                    + resultingType.getParameterizedQualifiedSourceName()
                    + " as the parameter's type", null);
              }
              paramsAlreadySet.add(param);
            } else {
              // This parameter type has already been set.
              //
              msg = "Metadata error: duplicate attempt to specify type args for parameter '"
                  + param.getName() + "'";
              branch.log(TreeLogger.WARN, msg, null);
              throw new UnableToCompleteException();
            }
          } else {
            // It's either referring to the return type or a bad param name.
            //
            if (!returnTypeHandled) {
              JType resultingType = determineActualType(branch,
                  method.getReturnType(), tokens, 0);
              method.setReturnType(resultingType);

              if (GenUtil.warnAboutMetadata()) {
                branch.log(TreeLogger.WARN, "Deprecated use of " + TAG_TYPEARGS
                    + " for the return type; Please use "
                    + resultingType.getParameterizedQualifiedSourceName()
                    + " as the method's return type", null);
              }
              returnTypeHandled = true;
            } else {
              // The return type has already been set.
              //
              msg = "Metadata error: duplicate attempt to specify type args for the return type";
              branch.log(TreeLogger.WARN, msg, null);
            }
          }
        }
      } catch (UnableToCompleteException e) {
        // Continue; will already have been logged.
        //
      }
    }
  }

  /*
   * Given a declared type and some number of type arguments determine what the
   * actual type should be.
   */
  private JType determineActualType(TreeLogger logger, JType declType,
      String[] tokens, int startIndex) throws UnableToCompleteException {
    // These are type args for a param.
    //
    JType leafType = declType.getLeafType();
    String typeName = leafType.getQualifiedSourceName();
    JType resultingType = parseTypeArgTokens(logger, typeName, tokens,
        startIndex);
    JArrayType arrayType = declType.isArray();
    if (arrayType != null) {
      arrayType.setLeafType(resultingType);

      return declType;
    }

    return resultingType;
  }

  private void invalidateTypes(Set<JRealClassType> invalidTypes) {
    removeInvalidatedArrayTypes(invalidTypes);
    removeInvalidatedParameterizedTypes(invalidTypes);
    removeTypes(invalidTypes);
  }

  private JType parseImpl(String type) throws NotFoundException,
      ParseException, BadTypeArgsException {
    if (type.endsWith("[]")) {
      String remainder = type.substring(0, type.length() - 2);
      JType componentType = parseImpl(remainder);
      return getArrayType(componentType);
    }

    if (type.endsWith(">")) {
      int bracket = type.indexOf('<');
      if (bracket == -1) {
        throw new ParseException(
            "Mismatched brackets; expected '<' to match subsequent '>'");
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
        throw new BadTypeArgsException(
            "Only classes and interface can be parameterized, so "
                + rawType.getQualifiedSourceName()
                + " cannot be used in this context");
      } else if (rawType.isGenericType() == null) {
        throw new BadTypeArgsException(
            "'"
                + rawType.getQualifiedSourceName()
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

    JType result = JPrimitiveType.valueOf(type);
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

  private void parseTypeArgComponent(List<JClassType> typeArgList,
      String typeArgComponent) throws NotFoundException, ParseException,
      BadTypeArgsException {
    JType typeArg = parseImpl(typeArgComponent);
    if (typeArg.isPrimitive() != null) {
      // Cannot be primitive.
      //
      throw new BadTypeArgsException(
          "Type arguments cannot be primitives, so '"
              + typeArg.getQualifiedSourceName()
              + "' cannot be used in this context");
    }

    typeArgList.add((JClassType) typeArg);
  }

  /**
   * Returns an array of types specified inside of a gwt.typeArgs javadoc
   * annotation.
   */
  private JClassType[] parseTypeArgContents(String typeArgContents)
      throws ParseException, NotFoundException, BadTypeArgsException {
    List<JClassType> typeArgList = new ArrayList<JClassType>();

    int start = 0;
    for (int offset = 0, length = typeArgContents.length(); offset < length; ++offset) {
      char ch = typeArgContents.charAt(offset);
      switch (ch) {
        case '<':
          // scan for closing '>' while ignoring commas
          for (int depth = 1; depth > 0;) {
            if (++offset == length) {
              throw new ParseException(
                  "Mismatched brackets; expected '<' to match subsequent '>'");
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

  private JType parseTypeArgTokens(TreeLogger logger, String maybeRawType,
      String[] tokens, int startIndex) throws UnableToCompleteException {
    String munged = combine(tokens, startIndex).trim();
    String toParse = maybeRawType + munged;
    JType parameterizedType;
    try {
      parameterizedType = parse(toParse);
    } catch (IllegalArgumentException e) {
      logger.log(TreeLogger.WARN, e.getMessage(), e);
      throw new UnableToCompleteException();
    } catch (TypeOracleException e) {
      logger.log(TreeLogger.WARN, e.getMessage(), e);
      throw new UnableToCompleteException();
    }
    return parameterizedType;
  }

  /**
   * Remove any array type whose leaf type has been invalidated.
   * 
   * @param invalidTypes set of types that have been invalidated.
   */
  private void removeInvalidatedArrayTypes(Set<JRealClassType> invalidTypes) {
    arrayTypes.keySet().removeAll(invalidTypes);
  }

  /**
   * Remove any parameterized type that was invalidated because either its raw
   * type or any one of its type arguments was invalidated.
   * 
   * @param invalidTypes set of types known to have been invalidated
   */
  private void removeInvalidatedParameterizedTypes(
      Set<JRealClassType> invalidTypes) {
    Iterator<List<JParameterizedType>> listIterator = parameterizedTypes.values().iterator();

    while (listIterator.hasNext()) {
      List<JParameterizedType> list = listIterator.next();
      Iterator<JParameterizedType> typeIterator = list.iterator();
      while (typeIterator.hasNext()) {
        JType type = typeIterator.next();
        if (isInvalidatedTypeRecursive(type, invalidTypes)) {
          typeIterator.remove();
        }
      }
    }
  }

  /**
   * Removes the specified types from the type oracle.
   * 
   * @param invalidTypes set of types to remove
   */
  private void removeTypes(Set<JRealClassType> invalidTypes) {
    for (Iterator<JRealClassType> iter = invalidTypes.iterator(); iter.hasNext();) {
      JClassType classType = iter.next();

      allTypes.remove(classType);

      JPackage pkg = classType.getPackage();
      if (pkg != null) {
        pkg.remove(classType);
      }

      classType.removeFromSupertypes();

      iter.remove();
    }
  }
}
