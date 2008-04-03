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
package com.google.gwt.dev.jdt;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.CompilationUnitProvider;
import com.google.gwt.core.ext.typeinfo.HasMetaData;
import com.google.gwt.core.ext.typeinfo.HasTypeParameters;
import com.google.gwt.core.ext.typeinfo.JAbstractMethod;
import com.google.gwt.core.ext.typeinfo.JAnnotationMethod;
import com.google.gwt.core.ext.typeinfo.JAnnotationType;
import com.google.gwt.core.ext.typeinfo.JArrayType;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JConstructor;
import com.google.gwt.core.ext.typeinfo.JEnumConstant;
import com.google.gwt.core.ext.typeinfo.JEnumType;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JGenericType;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JPackage;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JRealClassType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.JTypeParameter;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.core.ext.typeinfo.JWildcardType.BoundType;
import com.google.gwt.dev.jdt.CacheManager.Mapper;
import com.google.gwt.dev.util.Empty;
import com.google.gwt.dev.util.PerfLogger;
import com.google.gwt.dev.util.Util;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.AbstractVariableDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.AnnotationMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.ArrayInitializer;
import org.eclipse.jdt.internal.compiler.ast.ClassLiteralAccess;
import org.eclipse.jdt.internal.compiler.ast.Clinit;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Initializer;
import org.eclipse.jdt.internal.compiler.ast.Javadoc;
import org.eclipse.jdt.internal.compiler.ast.MemberValuePair;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.NameReference;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeParameter;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.ast.Wildcard;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.impl.Constant;
import org.eclipse.jdt.internal.compiler.lookup.ArrayBinding;
import org.eclipse.jdt.internal.compiler.lookup.BaseTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.BinaryTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.jdt.internal.compiler.lookup.LocalTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.MethodBinding;
import org.eclipse.jdt.internal.compiler.lookup.ParameterizedTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.RawTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.ReferenceBinding;
import org.eclipse.jdt.internal.compiler.lookup.SourceTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.TypeIds;
import org.eclipse.jdt.internal.compiler.lookup.TypeVariableBinding;
import org.eclipse.jdt.internal.compiler.lookup.WildcardBinding;

import java.io.BufferedReader;
import java.io.CharArrayReader;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;

/**
 * Builds a {@link com.google.gwt.core.ext.typeinfo.TypeOracle} from a set of
 * compilation units.
 * <p>
 * For example,
 * 
 * <pre>
 * TypeOracleBuilder b = new TypeOracleBuilder();
 * b.addCompilationUnit(unit1);
 * b.addCompilationUnit(unit2);
 * b.addCompilationUnit(unit3);
 * b.excludePackage(&quot;example.pkg&quot;);
 * TypeOracle oracle = b.build(logger);
 * JClassType[] allTypes = oracle.getTypes();
 * </pre>
 */
public class TypeOracleBuilder {
  private static final JClassType[] NO_JCLASSES = new JClassType[0];
  private static final Pattern PATTERN_WHITESPACE = Pattern.compile("\\s");

  public static String computeBinaryClassName(JType type) {
    JPrimitiveType primitiveType = type.isPrimitive();
    if (primitiveType != null) {
      return primitiveType.getJNISignature();
    }

    JArrayType arrayType = type.isArray();
    if (arrayType != null) {
      JType component = arrayType.getComponentType();
      if (component.isClassOrInterface() != null) {
        return "[L" + computeBinaryClassName(arrayType.getComponentType())
            + ";";
      } else {
        return "[" + computeBinaryClassName(arrayType.getComponentType());
      }
    }

    JParameterizedType parameterizedType = type.isParameterized();
    if (parameterizedType != null) {
      return computeBinaryClassName(parameterizedType.getBaseType());
    }

    JClassType classType = type.isClassOrInterface();
    assert (classType != null);

    JClassType enclosingType = classType.getEnclosingType();
    if (enclosingType != null) {
      return computeBinaryClassName(enclosingType) + "$"
          + classType.getSimpleSourceName();
    }

    return classType.getQualifiedSourceName();
  }

  static boolean parseMetaDataTags(char[] unitSource, HasMetaData hasMetaData,
      Javadoc javadoc) {

    int start = javadoc.sourceStart;
    int end = javadoc.sourceEnd;
    char[] comment = CharOperation.subarray(unitSource, start, end + 1);
    if (comment == null) {
      comment = new char[0];
    }
    BufferedReader reader = new BufferedReader(new CharArrayReader(comment));
    String activeTag = null;
    final List<String> tagValues = new ArrayList<String>();
    try {
      String line = reader.readLine();
      boolean firstLine = true;
      while (line != null) {
        if (firstLine) {
          firstLine = false;
          int commentStart = line.indexOf("/**");
          if (commentStart == -1) {
            // Malformed.
            return false;
          }
          line = line.substring(commentStart + 3);
        }

        String[] tokens = PATTERN_WHITESPACE.split(line);
        boolean canIgnoreStar = true;
        for (int i = 0; i < tokens.length; i++) {
          String token = tokens[i];

          // Check for the end.
          //
          if (token.endsWith("*/")) {
            token = token.substring(0, token.length() - 2);
          }

          // Check for an ignored leading star.
          //
          if (canIgnoreStar && token.startsWith("*")) {
            token = token.substring(1);
            canIgnoreStar = false;
          }

          // Decide what to do with whatever is left.
          //
          if (token.length() > 0) {
            canIgnoreStar = false;
            if (token.startsWith("@")) {
              // A new tag has been introduced.
              // Subsequent tokens will attach to it.
              // Make sure we finish the previously active tag before moving on.
              //
              if (activeTag != null) {
                finishTag(hasMetaData, activeTag, tagValues);
              }
              activeTag = token.substring(1);
            } else if (activeTag != null) {
              // Attach this item to the active tag.
              //
              tagValues.add(token);
            } else {
              // Just ignore it.
              //
            }
          }
        }

        line = reader.readLine();
      }
    } catch (IOException e) {
      return false;
    }

    // To catch the last batch of values, if any.
    //
    finishTag(hasMetaData, activeTag, tagValues);
    return true;
  }

  private static void finishTag(HasMetaData hasMetaData, String tagName,
      List<String> tagValues) {
    // Add the values even if the list is empty, because the presence of the
    // tag itself might be important.
    // 
    String[] values = tagValues.toArray(Empty.STRINGS);
    hasMetaData.addMetaData(tagName, values);
    tagValues.clear();
  }

  /**
   * Returns the value associated with a JDT constant.
   */
  private static Object getConstantValue(Constant constant) {
    switch (constant.typeID()) {
      case TypeIds.T_char:
        return constant.charValue();
      case TypeIds.T_byte:
        return constant.byteValue();
      case TypeIds.T_short:
        return constant.shortValue();
      case TypeIds.T_boolean:
        return constant.booleanValue();
      case TypeIds.T_long:
        return constant.longValue();
      case TypeIds.T_double:
        return constant.doubleValue();
      case TypeIds.T_float:
        return constant.floatValue();
      case TypeIds.T_int:
        return constant.intValue();
      case TypeIds.T_JavaLangString:
        return constant.stringValue();
      case TypeIds.T_null:
        return null;
      default:
        break;
    }

    assert false : "Unknown constant type";
    return null;
  }

  private static String getMethodName(JClassType enclosingType,
      AbstractMethodDeclaration jmethod) {
    if (jmethod.isConstructor()) {
      return String.valueOf(enclosingType.getSimpleSourceName());
    } else {
      return String.valueOf(jmethod.binding.selector);
    }
  }

  private static boolean isAnnotation(TypeDeclaration typeDecl) {
    if (TypeDeclaration.kind(typeDecl.modifiers) == TypeDeclaration.ANNOTATION_TYPE_DECL) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Returns <code>true</code> if this name is the special package-info type
   * name.
   * 
   * @return <code>true</code> if this name is the special package-info type
   *         name
   */
  private static boolean isPackageInfoTypeName(String qname) {
    return "package-info".equals(qname);
  }

  private static boolean maybeGeneric(TypeDeclaration typeDecl,
      JClassType enclosingType) {

    if (enclosingType != null && enclosingType.isGenericType() != null) {
      if (!typeDecl.binding.isStatic()) {
        /*
         * This non-static, inner class can reference the type parameters of its
         * enclosing generic type, so we will treat it as a generic type.
         */
        return true;
      }
    }

    if (typeDecl.binding.isLocalType()) {
      LocalTypeBinding localTypeBinding = (LocalTypeBinding) typeDecl.binding;
      MethodBinding enclosingMethod = localTypeBinding.enclosingMethod;
      if (enclosingMethod != null) {
        if (enclosingMethod.typeVariables != null
            && enclosingMethod.typeVariables.length != 0) {
          /*
           * This local type can reference the type parameters of its enclosing
           * method, so we will treat is as a generic type.
           */
          return true;
        }
      }
    }

    if (typeDecl.typeParameters != null) {
      // Definitely generic since it has type parameters.
      return true;
    }

    return false;
  }

  // TODO: move into the Annotations class or create an AnnotationsUtil class?
  private static HashMap<Class<? extends java.lang.annotation.Annotation>, java.lang.annotation.Annotation> newAnnotationMap() {
    return new HashMap<Class<? extends java.lang.annotation.Annotation>, java.lang.annotation.Annotation>();
  }

  private static void removeInfectedUnits(final TreeLogger logger,
      final Map<String, CompilationUnitDeclaration> cudsByFileName) {

    final Set<String> pendingRemovals = new HashSet<String>();
    TypeRefVisitor trv = new TypeRefVisitor() {
      @Override
      protected void onTypeRef(SourceTypeBinding referencedType,
          CompilationUnitDeclaration unitOfReferrer) {
        // If the referenced type belongs to a compilation unit that is
        // not in the list of valid units, then the unit in which it
        // is referenced must also be removed.
        //
        String referencedFn = String.valueOf(referencedType.getFileName());
        CompilationUnitDeclaration referencedCud = cudsByFileName.get(referencedFn);
        if (referencedCud == null) {
          // This is a referenced to a bad or non-existent unit.
          // So, remove the referrer's unit if it hasn't been already.
          //
          String referrerFn = String.valueOf(unitOfReferrer.getFileName());
          if (cudsByFileName.containsKey(referrerFn)
              && !pendingRemovals.contains(referrerFn)) {
            TreeLogger branch = logger.branch(TreeLogger.TRACE,
                "Cascaded removal of compilation unit '" + referrerFn + "'",
                null);
            final String badTypeName = CharOperation.toString(referencedType.compoundName);
            branch.branch(TreeLogger.TRACE,
                "Due to reference to unavailable type: " + badTypeName, null);
            pendingRemovals.add(referrerFn);
          }
        }
      }
    };

    do {
      // Perform any pending removals.
      //
      for (Iterator<String> iter = pendingRemovals.iterator(); iter.hasNext();) {
        String fnToRemove = iter.next();
        Object removed = cudsByFileName.remove(fnToRemove);
        assert (removed != null);
      }

      // Start fresh for this iteration.
      //
      pendingRemovals.clear();

      // Find references to type in units that aren't valid.
      //
      for (Iterator<CompilationUnitDeclaration> iter = cudsByFileName.values().iterator(); iter.hasNext();) {
        CompilationUnitDeclaration cud = iter.next();
        cud.traverse(trv, cud.scope);
      }
    } while (!pendingRemovals.isEmpty());
  }

  private static void removeUnitsWithErrors(TreeLogger logger,
      Map<String, CompilationUnitDeclaration> cudsByFileName) {
    // Start by removing units with a known problem.
    //
    boolean anyRemoved = false;
    for (Iterator<CompilationUnitDeclaration> iter = cudsByFileName.values().iterator(); iter.hasNext();) {
      CompilationUnitDeclaration cud = iter.next();
      CompilationResult result = cud.compilationResult;
      IProblem[] errors = result.getErrors();
      if (errors != null && errors.length > 0) {
        anyRemoved = true;
        iter.remove();

        String fileName = CharOperation.charToString(cud.getFileName());
        char[] source = cud.compilationResult.compilationUnit.getContents();
        Util.maybeDumpSource(logger, fileName, source,
            String.valueOf(cud.getMainTypeName()));
        logger.log(TreeLogger.TRACE, "Removing problematic compilation unit '"
            + fileName + "'", null);
      }
    }

    if (anyRemoved) {
      // Then removing anything else that won't compile as a result.
      //
      removeInfectedUnits(logger, cudsByFileName);
    }
  }

  private final CacheManager cacheManager;

  /**
   * Constructs a default instance, with a default cacheManager. This is not to
   * be used in Hosted Mode, as caching will then not work.
   */
  public TypeOracleBuilder() {
    cacheManager = new CacheManager();
  }

  /**
   * Constructs an instance from the supplied cacheManager, using the
   * <code>TypeOracle</code> contained therein. This is to be used in Hosted
   * Mode, so that caching will work, assuming the cacheManager has a cache
   * directory.
   */
  public TypeOracleBuilder(CacheManager cacheManager) {
    this.cacheManager = cacheManager;
  }

  /**
   * Constructs an instance from the supplied typeOracle, with a cacheManager
   * using the same typeOracle. This is not to be used in Hosted Mode, as
   * caching will then not work.
   */
  public TypeOracleBuilder(TypeOracle typeOracle) {
    cacheManager = new CacheManager(typeOracle);
  }

  /**
   * Includes the specified logical compilation unit into the set of units this
   * builder will parse and analyze. If a previous compilation unit was
   * specified in the same location, it will be replaced if it is older.
   */
  public void addCompilationUnit(CompilationUnitProvider cup)
      throws UnableToCompleteException {
    cacheManager.addCompilationUnit(cup);
  }

  public TypeOracle build(final TreeLogger logger)
      throws UnableToCompleteException {
    PerfLogger.start("TypeOracleBuilder.build");

    Set<CompilationUnitProvider> addedCups = cacheManager.getAddedCups();
    TypeOracle oracle = cacheManager.getTypeOracle();
    // Make a copy that we can sort.
    //
    for (Iterator<CompilationUnitProvider> iter = addedCups.iterator(); iter.hasNext();) {
      CompilationUnitProvider cup = iter.next();
      String location = cup.getLocation();
      if (!((location.indexOf("http://") != -1) || (location.indexOf("ftp://") != -1))) {
        location = Util.findFileName(location);
        if (!(new File(location).exists() || cup.isTransient())) {
          iter.remove();
          logger.log(
              TreeLogger.TRACE,
              "The file "
                  + location
                  + " was removed by the user.  All types therein are now unavailable.",
              null);
        }
      }
    }
    CompilationUnitProvider[] cups = Util.toArray(
        CompilationUnitProvider.class, addedCups);
    Arrays.sort(cups, CompilationUnitProvider.LOCATION_COMPARATOR);

    // Make sure we can find the java.lang.Object compilation unit.
    //
    boolean foundJavaLangPackage = oracle.findPackage("java.lang") != null;

    // Adapt to JDT idioms.
    //
    ICompilationUnit[] units = new ICompilationUnit[cups.length];
    for (int i = 0; i < cups.length; i++) {
      if (!foundJavaLangPackage && cups[i].getPackageName().equals("java.lang")) {
        foundJavaLangPackage = true;
      }
      units[i] = cacheManager.findUnitForCup(cups[i]);
    }

    // Error if no java.lang.
    if (!foundJavaLangPackage) {
      Util.logMissingTypeErrorWithHints(logger, "java.lang.Object");
      throw new UnableToCompleteException();
    }

    PerfLogger.start("TypeOracleBuilder.build (compile)");
    CompilationUnitDeclaration[] cuds = cacheManager.getAstCompiler().getChangedCompilationUnitDeclarations(
        logger, units);
    PerfLogger.end();

    // Build a list that makes it easy to remove problems.
    //
    final Map<String, CompilationUnitDeclaration> cudsByFileName = new TreeMap<String, CompilationUnitDeclaration>();
    for (int i = 0; i < cuds.length; i++) {
      CompilationUnitDeclaration cud = cuds[i];
      char[] location = cud.getFileName();
      cudsByFileName.put(String.valueOf(location), cud);
    }
    cacheManager.getCudsByFileName().putAll(cudsByFileName);

    // Remove bad cuds and all the other cuds that are affected.
    //
    removeUnitsWithErrors(logger, cudsByFileName);

    // Perform a shallow pass to establish identity for new types.
    //
    final CacheManager.Mapper identityMapper = cacheManager.getIdentityMapper();
    for (Iterator<CompilationUnitDeclaration> iter = cudsByFileName.values().iterator(); iter.hasNext();) {
      CompilationUnitDeclaration cud = iter.next();

      cud.traverse(new ASTVisitor() {
        @Override
        public boolean visit(TypeDeclaration typeDecl, BlockScope scope) {
          JClassType enclosingType = identityMapper.get(typeDecl.binding.enclosingType());
          processType(typeDecl, enclosingType, true);
          return true;
        }

        @Override
        public boolean visit(TypeDeclaration typeDecl, ClassScope scope) {
          JClassType enclosingType = identityMapper.get(typeDecl.binding.enclosingType());
          processType(typeDecl, enclosingType, false);
          return true;
        }

        @Override
        public boolean visit(TypeDeclaration typeDecl,
            CompilationUnitScope scope) {
          processType(typeDecl, null, false);
          return true;
        }

      }, cud.scope);
    }

    // Perform a deep pass to resolve all types in terms of our types.
    //
    for (Iterator<CompilationUnitDeclaration> iter = cudsByFileName.values().iterator(); iter.hasNext();) {
      CompilationUnitDeclaration cud = iter.next();
      String loc = String.valueOf(cud.getFileName());
      String processing = "Processing types in compilation unit: " + loc;
      final TreeLogger cudLogger = logger.branch(TreeLogger.SPAM, processing,
          null);
      final char[] source = cud.compilationResult.compilationUnit.getContents();

      cud.traverse(new ASTVisitor() {
        @Override
        public boolean visit(TypeDeclaration typeDecl, BlockScope scope) {
          if (!resolveTypeDeclaration(cudLogger, source, typeDecl)) {
            String name = String.valueOf(typeDecl.binding.readableName());
            String msg = "Unexpectedly unable to fully resolve type " + name;
            logger.log(TreeLogger.WARN, msg, null);
          }
          return true;
        }

        @Override
        public boolean visit(TypeDeclaration typeDecl, ClassScope scope) {
          if (!resolveTypeDeclaration(cudLogger, source, typeDecl)) {
            String name = String.valueOf(typeDecl.binding.readableName());
            String msg = "Unexpectedly unable to fully resolve type " + name;
            logger.log(TreeLogger.WARN, msg, null);
          }
          return true;
        }

        @Override
        public boolean visit(TypeDeclaration typeDecl,
            CompilationUnitScope scope) {
          if (!resolveTypeDeclaration(cudLogger, source, typeDecl)) {
            String name = String.valueOf(typeDecl.binding.readableName());
            String msg = "Unexpectedly unable to fully resolve type " + name;
            logger.log(TreeLogger.WARN, msg, null);
          }
          return true;
        }
      }, cud.scope);
    }
    Util.invokeInaccessableMethod(TypeOracle.class, "refresh",
        new Class[] {TreeLogger.class}, oracle, new Object[] {logger});

    PerfLogger.end();

    return oracle;
  }

  /**
   * Used for testing purposes only.
   */
  final CacheManager getCacheManager() {
    return cacheManager;
  }

  private Object createAnnotationInstance(TreeLogger logger,
      Expression expression) {
    Annotation annotation = (Annotation) expression;

    // Determine the annotation class
    TypeBinding resolvedType = annotation.resolvedType;
    Class<?> classLiteral = getClassLiteral(logger, resolvedType);
    if (classLiteral == null) {
      return null;
    }

    Class<? extends java.lang.annotation.Annotation> clazz = classLiteral.asSubclass(java.lang.annotation.Annotation.class);

    // Build the map of identifiers to values.
    Map<String, Object> identifierToValue = new HashMap<String, Object>();
    for (MemberValuePair mvp : annotation.memberValuePairs()) {
      // Method name
      String identifier = String.valueOf(mvp.name);

      // Value
      Expression expressionValue = mvp.value;
      TypeBinding expectedElementValueType = mvp.binding.returnType;
      Object elementValue = getAnnotationElementValue(logger,
          expectedElementValueType, expressionValue);
      if (elementValue == null) {
        return null;
      }

      /*
       * If the expected value is supposed to be an array then the element value
       * had better be an array.
       */
      assert (expectedElementValueType.isArrayType() == false || expectedElementValueType.isArrayType()
          && elementValue.getClass().isArray());

      identifierToValue.put(identifier, elementValue);
    }

    // Create the Annotation proxy
    JClassType annotationType = (JClassType) resolveType(logger, resolvedType);
    if (annotationType == null) {
      return null;
    }

    return AnnotationProxyFactory.create(clazz, annotationType,
        identifierToValue);
  }

  private JClassType[] createTypeParameterBounds(TreeLogger logger,
      TypeVariableBinding tvBinding) {
    TypeBinding firstBound = tvBinding.firstBound;
    if (firstBound == null) {
      // No bounds were specified, so we default to Object. We assume that the
      // superclass field of a TypeVariableBinding object is a Binding
      // for a java.lang.Object, and we use this binding to find the
      // JClassType for java.lang.Object. To be sure that our assumption
      // about the superclass field is valid, we perform a runtime check
      // against the name of the resolved type.

      // You may wonder why we have to go this roundabout way to find a
      // JClassType for java.lang.Object. The reason is that the TypeOracle
      // has not been constructed as yet, so we cannot simply call
      // TypeOracle.getJavaLangObject().
      JClassType jimplicitUpperBound = (JClassType) resolveType(logger,
          tvBinding.superclass);
      if (jimplicitUpperBound != null) {
        assert (Object.class.getName().equals(jimplicitUpperBound.getQualifiedSourceName()));
        return new JClassType[] {jimplicitUpperBound};
      }

      // Failed to resolve the implicit upper bound.
      return null;
    }

    List<JClassType> bounds = new ArrayList<JClassType>();
    JClassType jfirstBound = (JClassType) resolveType(logger, firstBound);
    if (jfirstBound == null) {
      return null;
    }

    bounds.add(jfirstBound);

    ReferenceBinding[] superInterfaces = tvBinding.superInterfaces();
    for (ReferenceBinding superInterface : superInterfaces) {
      if (superInterface != firstBound) {
        JClassType jsuperInterface = (JClassType) resolveType(logger,
            superInterface);
        if (jsuperInterface == null) {
          return null;
        }
        bounds.add(jsuperInterface);
      } else {
        /*
         * If the first bound was an interface JDT will still include it in the
         * set of superinterfaces. So, we ignore it since we have already added
         * it to the bounds.
         */
      }
    }

    return bounds.toArray(NO_JCLASSES);
  }

  /**
   * Declares TypeParameters declared on a JGenericType or a JAbstractMethod by
   * mapping the TypeParameters into JTypeParameters. <p/> This mapping has to
   * be done on the first pass through the AST in order to handle declarations
   * of the form: <<C exends GenericClass<T>, T extends SimpleClass> <p/> JDT
   * already knows that GenericClass<T> is a parameterized type with a type
   * argument of <T extends SimpleClass>. Therefore, in order to resolve
   * GenericClass<T>, we must have knowledge of <T extends SimpleClass>. <p/>
   * By calling this method on the first pass through the AST, a JTypeParameter
   * for <T extends SimpleClass> will be created.
   */
  private JTypeParameter[] declareTypeParameters(TypeParameter[] typeParameters) {
    if (typeParameters == null || typeParameters.length == 0) {
      return null;
    }

    JTypeParameter[] jtypeParamArray = new JTypeParameter[typeParameters.length];
    Mapper identityMapper = cacheManager.getIdentityMapper();

    for (int i = 0; i < typeParameters.length; i++) {
      TypeParameter typeParam = typeParameters[i];
      jtypeParamArray[i] = new JTypeParameter(String.valueOf(typeParam.name), i);
      identityMapper.put(typeParam.binding, jtypeParamArray[i]);
    }

    return jtypeParamArray;
  }

  /**
   * Returns an annotation element value as defined in JLS 3.0 section 9.7.
   * 
   * @param logger
   * @param expectedElementValueType the expected element value type
   * @param elementValueExpression the expression that defines the element value
   * 
   * @return annotation element value as defined in JLS 3.0 section 9.7
   */
  private Object getAnnotationElementValue(TreeLogger logger,
      TypeBinding expectedElementValueType, Expression elementValueExpression) {

    Object elementValue = null;

    if (elementValueExpression.constant != null
        && elementValueExpression.constant != Constant.NotAConstant) {
      /*
       * Rely on JDT's computed constant value to deal with an
       * AnnotationElementValue expression whose resolved type is a primitive or
       * a string.
       */
      Constant constant = elementValueExpression.constant;
      int expectedTypeId = expectedElementValueType.id;

      if (expectedElementValueType.isArrayType()) {
        /*
         * This can happen when an element value is an array with a single
         * element. In this case JLS 3.0 section 9.7 allows for the
         * ArrayInitializer expression to be implicit. Since, element values can
         * only be single dimensional arrays, we take the leaf type of the
         * expected array type as our resultant element value type.
         */
        assert (!elementValueExpression.resolvedType.isArrayType() && expectedElementValueType.dimensions() == 1);

        expectedTypeId = expectedElementValueType.leafComponentType().id;
      }

      if (elementValueExpression.resolvedType.id != expectedTypeId) {
        /*
         * Narrowing and widening conversions are handled by the following
         * Constant.castTo call. JDT wants the upper four bits of this mask to
         * be the target type id and the lower four bits to be the source type
         * id. See Constant.castTo for more details.
         */
        constant = constant.castTo((expectedTypeId << 4)
            + elementValueExpression.resolvedType.id);
      }

      elementValue = getConstantValue(constant);
    } else if (elementValueExpression instanceof ClassLiteralAccess) {
      ClassLiteralAccess classLiteral = (ClassLiteralAccess) elementValueExpression;
      elementValue = getClassLiteral(logger, classLiteral.targetType);
    } else if (elementValueExpression instanceof ArrayInitializer) {
      elementValue = getAnnotationElementValueArray(logger,
          (ArrayInitializer) elementValueExpression);
    } else if (elementValueExpression instanceof NameReference) {
      /*
       * Any primitive types, conditionals, strings, arrays and name references
       * to constant fields will have all been handled by the constant
       * expression block above. This name reference can only be for an
       * enumerated type.
       */
      NameReference nameRef = (NameReference) elementValueExpression;

      assert (nameRef.constant == null || nameRef.constant == Constant.NotAConstant);
      assert (nameRef.actualReceiverType.isEnum());

      Class<?> clazz = getClassLiteral(logger, nameRef.actualReceiverType);
      Class<? extends Enum> enumClass = clazz.asSubclass(Enum.class);

      String enumName = String.valueOf(nameRef.fieldBinding().name);
      elementValue = Enum.valueOf(enumClass, enumName);
    } else if (elementValueExpression instanceof Annotation) {
      elementValue = createAnnotationInstance(logger, elementValueExpression);
    } else {
      assert (false);
      return null;
    }

    assert (elementValue != null);

    if (expectedElementValueType.isArrayType()
        && !elementValue.getClass().isArray()) {
      /*
       * Handle single element arrays where no explicit array initializer was
       * given.
       */
      Object array = Array.newInstance(elementValue.getClass(), 1);
      Array.set(array, 0, elementValue);
      elementValue = array;
    }

    return elementValue;
  }

  /**
   * Returns an annotation element value array. These arrays can only have a
   * single dimension.
   */
  private Object getAnnotationElementValueArray(TreeLogger logger,
      ArrayInitializer arrayInitializer) {
    assert (arrayInitializer.binding.dimensions == 1);

    Class<?> leafComponentClass = getClassLiteral(logger,
        arrayInitializer.binding.leafComponentType);
    if (leafComponentClass == null) {
      return null;
    }

    Expression[] initExpressions = arrayInitializer.expressions;
    int arrayLength = initExpressions != null ? initExpressions.length : 0;

    Object array = Array.newInstance(leafComponentClass, arrayLength);
    boolean failed = false;
    for (int i = 0; i < arrayLength; ++i) {
      Expression arrayInitExp = initExpressions[i];
      Object value = getAnnotationElementValue(logger,
          arrayInitializer.binding.leafComponentType, arrayInitExp);
      if (value != null) {
        Array.set(array, i, value);
      } else {
        failed = true;
        break;
      }
    }

    if (!failed) {
      return array;
    }

    return null;
  }

  private Class<?> getClassLiteral(TreeLogger logger, TypeBinding resolvedType) {
    JType type = resolveType(logger, resolvedType);
    if (type == null) {
      return null;
    }

    if (type.isPrimitive() != null) {
      return getClassLiteralForPrimitive(type.isPrimitive());
    } else {
      try {
        String className = computeBinaryClassName(type);
        Class<?> clazz = Class.forName(className);
        return clazz;
      } catch (ClassNotFoundException e) {
        logger.log(TreeLogger.ERROR, "", e);
        return null;
      }
    }
  }

  private Class<?> getClassLiteralForPrimitive(JPrimitiveType type) {
    if (type == JPrimitiveType.BOOLEAN) {
      return boolean.class;
    } else if (type == JPrimitiveType.BYTE) {
      return byte.class;
    } else if (type == JPrimitiveType.CHAR) {
      return char.class;
    } else if (type == JPrimitiveType.DOUBLE) {
      return double.class;
    } else if (type == JPrimitiveType.FLOAT) {
      return float.class;
    } else if (type == JPrimitiveType.INT) {
      return int.class;
    } else if (type == JPrimitiveType.LONG) {
      return long.class;
    } else if (type == JPrimitiveType.SHORT) {
      return short.class;
    }

    assert (false);
    return null;
  }

  private CompilationUnitProvider getCup(TypeDeclaration typeDecl) {
    ICompilationUnit icu = typeDecl.compilationResult.compilationUnit;
    ICompilationUnitAdapter icua = (ICompilationUnitAdapter) icu;
    return icua.getCompilationUnitProvider();
  }

  private String getPackage(TypeDeclaration typeDecl) {
    final char[][] pkgParts = typeDecl.compilationResult.compilationUnit.getPackageName();
    return String.valueOf(CharOperation.concatWith(pkgParts, '.'));
  }

  /**
   * Returns the qualified name of the binding, excluding any type parameter
   * information.
   */
  private String getQualifiedName(ReferenceBinding binding) {
    String qualifiedName = CharOperation.toString(binding.compoundName);
    if (binding instanceof LocalTypeBinding) {
      // The real name of a local type is its constant pool name.
      qualifiedName = CharOperation.charToString(binding.constantPoolName());
      qualifiedName = qualifiedName.replace('/', '.');
    } else {
      /*
       * All other types have their fully qualified name as part of its compound
       * name.
       */
      qualifiedName = CharOperation.toString(binding.compoundName);
    }

    qualifiedName = qualifiedName.replace('$', '.');

    return qualifiedName;
  }

  private String getSimpleName(TypeDeclaration typeDecl) {
    return String.valueOf(typeDecl.name);
  }

  private boolean isInterface(TypeDeclaration typeDecl) {
    if (TypeDeclaration.kind(typeDecl.modifiers) == TypeDeclaration.INTERFACE_DECL) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Maps a TypeDeclaration into a JClassType. If the TypeDeclaration has
   * TypeParameters (i.e, it is a generic type or method), then the
   * TypeParameters are mapped into JTypeParameters by
   * {@link TypeOracleBuilder#declareTypeParameters(org.eclipse.jdt.internal.compiler.ast.TypeParameter[])}
   */
  private void processType(TypeDeclaration typeDecl, JClassType jenclosingType,
      boolean isLocalType) {
    TypeOracle oracle = cacheManager.getTypeOracle();

    // Create our version of the type structure unless it already exists in the
    // type oracle.
    //
    SourceTypeBinding binding = typeDecl.binding;
    if (binding.constantPoolName() == null) {
      /*
       * Weird case: if JDT determines that this local class is totally
       * uninstantiable, it won't bother allocating a local name.
       */
      return;
    }

    String qname = getQualifiedName(binding);
    String className;
    if (binding instanceof LocalTypeBinding) {
      className = qname.substring(qname.lastIndexOf('.') + 1);
    } else {
      className = getSimpleName(typeDecl);
    }

    if (oracle.findType(qname) != null) {
      return;
    }

    String jpkgName = getPackage(typeDecl);
    JPackage pkg = oracle.getOrCreatePackage(jpkgName);
    final boolean jclassIsIntf = isInterface(typeDecl);
    boolean jclassIsAnnonation = isAnnotation(typeDecl);
    CompilationUnitProvider cup = getCup(typeDecl);

    int declStart = typeDecl.declarationSourceStart;
    int declEnd = typeDecl.declarationSourceEnd;
    int bodyStart = typeDecl.bodyStart;
    int bodyEnd = typeDecl.bodyEnd;

    JRealClassType jrealClassType;
    if (jclassIsAnnonation) {
      jrealClassType = new JAnnotationType(oracle, cup, pkg, jenclosingType,
          isLocalType, className, declStart, declEnd, bodyStart, bodyEnd,
          jclassIsIntf);
    } else if (maybeGeneric(typeDecl, jenclosingType)) {
      // Go through and create declarations for each of the type parameters on
      // the generic class or method
      JTypeParameter[] jtypeParameters = declareTypeParameters(typeDecl.typeParameters);

      JGenericType jgenericType = new JGenericType(oracle, cup, pkg,
          jenclosingType, isLocalType, className, declStart, declEnd,
          bodyStart, bodyEnd, jclassIsIntf, jtypeParameters);

      jrealClassType = jgenericType;
    } else if (binding.isEnum()) {
      jrealClassType = new JEnumType(oracle, cup, pkg, jenclosingType,
          isLocalType, className, declStart, declEnd, bodyStart, bodyEnd,
          jclassIsIntf);
    } else {
      jrealClassType = new JRealClassType(oracle, cup, pkg, jenclosingType,
          isLocalType, className, declStart, declEnd, bodyStart, bodyEnd,
          jclassIsIntf);
    }

    /*
     * Add modifiers since these are needed for
     * TypeOracle.getParameterizedType's error checking code.
     */
    jrealClassType.addModifierBits(Shared.bindingToModifierBits(binding));

    cacheManager.setTypeForBinding(binding, jrealClassType);
  }

  private boolean resolveAnnotation(
      TreeLogger logger,
      Annotation jannotation,
      Map<Class<? extends java.lang.annotation.Annotation>, java.lang.annotation.Annotation> declaredAnnotations) {

    logger = logger.branch(TreeLogger.SPAM, "Resolving annotation '"
        + jannotation.printExpression(0, new StringBuffer()).toString() + "'",
        null);

    // Determine the annotation class
    TypeBinding resolvedType = jannotation.resolvedType;
    Class<? extends java.lang.annotation.Annotation> clazz = (Class<? extends java.lang.annotation.Annotation>) getClassLiteral(
        logger, resolvedType);
    if (clazz == null) {
      return false;
    }

    java.lang.annotation.Annotation annotation = (java.lang.annotation.Annotation) createAnnotationInstance(
        logger, jannotation);
    if (annotation == null) {
      return false;
    }

    declaredAnnotations.put(clazz, annotation);
    return true;
  }

  private boolean resolveAnnotations(
      TreeLogger logger,
      Annotation[] annotations,
      Map<Class<? extends java.lang.annotation.Annotation>, java.lang.annotation.Annotation> declaredAnnotations) {
    boolean succeeded = true;
    if (annotations != null) {
      for (Annotation annotation : annotations) {
        succeeded &= resolveAnnotation(logger, annotation, declaredAnnotations);
      }
    }
    return succeeded;
  }

  private boolean resolveBoundForTypeParameter(TreeLogger logger,
      HasTypeParameters genericElement, TypeParameter typeParameter, int ordinal) {
    JClassType[] jbounds = createTypeParameterBounds(logger,
        typeParameter.binding);
    if (jbounds == null) {
      return false;
    }

    genericElement.getTypeParameters()[ordinal].setBounds(jbounds);

    return true;
  }

  private boolean resolveBoundsForTypeParameters(TreeLogger logger,
      HasTypeParameters genericElement, TypeParameter[] typeParameters) {
    if (typeParameters != null) {
      for (int i = 0; i < typeParameters.length; ++i) {
        if (!resolveBoundForTypeParameter(logger, genericElement,
            typeParameters[i], i)) {
          return false;
        }
      }
    }
    return true;
  }

  private boolean resolveField(TreeLogger logger, char[] unitSource,
      JClassType enclosingType, FieldDeclaration jfield) {

    if (jfield instanceof Initializer) {
      // Pretend we didn't see this.
      //
      return true;
    }

    // Try to resolve annotations, ignore any that fail.
    Map<Class<? extends java.lang.annotation.Annotation>, java.lang.annotation.Annotation> declaredAnnotations = newAnnotationMap();
    resolveAnnotations(logger, jfield.annotations, declaredAnnotations);

    String name = String.valueOf(jfield.name);
    JField field;
    if (jfield.getKind() == AbstractVariableDeclaration.ENUM_CONSTANT) {
      assert (enclosingType.isEnum() != null);
      field = new JEnumConstant(enclosingType, name, declaredAnnotations,
          jfield.binding.original().id);
    } else {
      field = new JField(enclosingType, name, declaredAnnotations);
    }

    // Get modifiers.
    //
    field.addModifierBits(Shared.bindingToModifierBits(jfield.binding));

    // Set the field type.
    //
    TypeBinding jfieldType = jfield.binding.type;

    JType fieldType = resolveType(logger, jfieldType);
    if (fieldType == null) {
      // Unresolved type.
      //
      return false;
    }
    field.setType(fieldType);

    // Get tags.
    //
    if (jfield.javadoc != null) {
      if (!parseMetaDataTags(unitSource, field, jfield.javadoc)) {
        return false;
      }
    }

    return true;
  }

  private boolean resolveFields(TreeLogger logger, char[] unitSource,
      JClassType type, FieldDeclaration[] jfields) {
    if (jfields != null) {
      for (int i = 0; i < jfields.length; i++) {
        FieldDeclaration jfield = jfields[i];
        if (!resolveField(logger, unitSource, type, jfield)) {
          return false;
        }
      }
    }
    return true;
  }

  private boolean resolveMethod(TreeLogger logger, char[] unitSource,
      JClassType enclosingType, AbstractMethodDeclaration jmethod) {

    if (jmethod instanceof Clinit) {
      // Pretend we didn't see this.
      //
      return true;
    }

    int declStart = jmethod.declarationSourceStart;
    int declEnd = jmethod.declarationSourceEnd;
    int bodyStart = jmethod.bodyStart;
    int bodyEnd = jmethod.bodyEnd;
    String name = getMethodName(enclosingType, jmethod);

    // Try to resolve annotations, ignore any that fail.
    Map<Class<? extends java.lang.annotation.Annotation>, java.lang.annotation.Annotation> declaredAnnotations = newAnnotationMap();
    resolveAnnotations(logger, jmethod.annotations, declaredAnnotations);

    JAbstractMethod method;

    // Declare the type parameters. We will pass them into the constructors for
    // JConstructor/JMethod/JAnnotatedMethod. Then, we'll do a second pass to
    // resolve the bounds on each JTypeParameter object.
    JTypeParameter[] jtypeParameters = declareTypeParameters(jmethod.typeParameters());

    if (jmethod.isConstructor()) {
      method = new JConstructor(enclosingType, name, declStart, declEnd,
          bodyStart, bodyEnd, declaredAnnotations, jtypeParameters);
      // Do a second pass to resolve the bounds on each JTypeParameter.
      if (!resolveBoundsForTypeParameters(logger, method,
          jmethod.typeParameters())) {
        return false;
      }
    } else {
      if (jmethod.isAnnotationMethod()) {
        AnnotationMethodDeclaration annotationMethod = (AnnotationMethodDeclaration) jmethod;
        Object defaultValue = null;
        if (annotationMethod.defaultValue != null) {
          defaultValue = getAnnotationElementValue(logger,
              annotationMethod.returnType.resolvedType,
              annotationMethod.defaultValue);
        }
        method = new JAnnotationMethod(enclosingType, name, declStart, declEnd,
            bodyStart, bodyEnd, defaultValue, declaredAnnotations);
      } else {
        method = new JMethod(enclosingType, name, declStart, declEnd,
            bodyStart, bodyEnd, declaredAnnotations, jtypeParameters);
      }

      // Do a second pass to resolve the bounds on each JTypeParameter.
      // The type parameters must be resolved at this point, because they may
      // be used in the resolution of the method's return type.
      if (!resolveBoundsForTypeParameters(logger, method,
          jmethod.typeParameters())) {
        return false;
      }

      TypeBinding jreturnType = ((MethodDeclaration) jmethod).returnType.resolvedType;
      JType returnType = resolveType(logger, jreturnType);
      if (returnType == null) {
        // Unresolved type.
        //
        return false;
      }
      ((JMethod) method).setReturnType(returnType);
    }

    // Parse modifiers.
    //
    method.addModifierBits(Shared.bindingToModifierBits(jmethod.binding));
    if (enclosingType.isInterface() != null) {
      // Always add implicit modifiers on interface methods.
      //
      method.addModifierBits(Shared.MOD_PUBLIC | Shared.MOD_ABSTRACT);
    }

    // Add the parameters.
    //
    Argument[] jparams = jmethod.arguments;
    if (!resolveParameters(logger, method, jparams)) {
      return false;
    }

    // Add throws.
    //
    TypeReference[] jthrows = jmethod.thrownExceptions;
    if (!resolveThrownTypes(logger, method, jthrows)) {
      return false;
    }

    // Get tags.
    //
    if (jmethod.javadoc != null) {
      if (!parseMetaDataTags(unitSource, method, jmethod.javadoc)) {
        return false;
      }
    }

    return true;
  }

  private boolean resolveMethods(TreeLogger logger, char[] unitSource,
      JClassType type, AbstractMethodDeclaration[] jmethods) {
    if (jmethods != null) {
      for (int i = 0; i < jmethods.length; i++) {
        AbstractMethodDeclaration jmethod = jmethods[i];
        if (!resolveMethod(logger, unitSource, type, jmethod)) {
          return false;
        }
      }
    }
    return true;
  }

  private boolean resolvePackage(TreeLogger logger, TypeDeclaration jclass) {
    SourceTypeBinding binding = jclass.binding;

    TypeOracle oracle = cacheManager.getTypeOracle();
    String packageName = String.valueOf(binding.fPackage.readableName());
    JPackage pkg = oracle.getOrCreatePackage(packageName);
    assert (pkg != null);

    CompilationUnitScope cus = (CompilationUnitScope) jclass.scope.parent;
    assert (cus != null);

    // Try to resolve annotations, ignore any that fail.
    Map<Class<? extends java.lang.annotation.Annotation>, java.lang.annotation.Annotation> declaredAnnotations = newAnnotationMap();
    resolveAnnotations(logger, cus.referenceContext.currentPackage.annotations,
        declaredAnnotations);

    pkg.addAnnotations(declaredAnnotations);

    return true;
  }

  private boolean resolveParameter(TreeLogger logger, JAbstractMethod method,
      Argument jparam) {
    TypeBinding jtype = jparam.binding.type;
    JType type = resolveType(logger, jtype);
    if (type == null) {
      // Unresolved.
      //
      return false;
    }

    // Try to resolve annotations, ignore any that fail.
    Map<Class<? extends java.lang.annotation.Annotation>, java.lang.annotation.Annotation> declaredAnnotations = newAnnotationMap();
    resolveAnnotations(logger, jparam.annotations, declaredAnnotations);

    String name = String.valueOf(jparam.name);
    new JParameter(method, type, name, declaredAnnotations);
    if (jparam.isVarArgs()) {
      method.setVarArgs();
    }
    return true;
  }

  private boolean resolveParameters(TreeLogger logger, JAbstractMethod method,
      Argument[] jparams) {
    if (jparams != null) {
      for (int i = 0; i < jparams.length; i++) {
        Argument jparam = jparams[i];
        if (!resolveParameter(logger, method, jparam)) {
          return false;
        }
      }
    }
    return true;
  }

  private boolean resolveThrownType(TreeLogger logger, JAbstractMethod method,
      TypeReference jthrown) {

    JType type = resolveType(logger, jthrown.resolvedType);
    if (type == null) {
      // Not resolved.
      //
      return false;
    }

    method.addThrows(type);

    return true;
  }

  private boolean resolveThrownTypes(TreeLogger logger, JAbstractMethod method,
      TypeReference[] jthrows) {
    if (jthrows != null) {
      for (int i = 0; i < jthrows.length; i++) {
        TypeReference jthrown = jthrows[i];
        if (!resolveThrownType(logger, method, jthrown)) {
          return false;
        }
      }
    }
    return true;
  }

  private JType resolveType(TreeLogger logger, TypeBinding binding) {
    TypeOracle oracle = cacheManager.getTypeOracle();
    // Check for primitives.
    //
    if (binding instanceof BaseTypeBinding) {
      switch (binding.id) {
        case TypeIds.T_boolean:
          return JPrimitiveType.BOOLEAN;
        case TypeIds.T_byte:
          return JPrimitiveType.BYTE;
        case TypeIds.T_char:
          return JPrimitiveType.CHAR;
        case TypeIds.T_short:
          return JPrimitiveType.SHORT;
        case TypeIds.T_int:
          return JPrimitiveType.INT;
        case TypeIds.T_long:
          return JPrimitiveType.LONG;
        case TypeIds.T_float:
          return JPrimitiveType.FLOAT;
        case TypeIds.T_double:
          return JPrimitiveType.DOUBLE;
        case TypeIds.T_void:
          return JPrimitiveType.VOID;
        default:
          assert false : "Unexpected base type id " + binding.id;
      }
    }

    /*
     * Check for a user-defined type, which may be either a SourceTypeBinding or
     * a RawTypeBinding. Both of these are subclasses of ReferenceBinding and
     * all the functionality we need is on ReferenceBinding, so we cast it to
     * that and deal with them in common code.
     * 
     * TODO: do we need to do anything more with raw types?
     */
    if (binding instanceof SourceTypeBinding
        || binding instanceof RawTypeBinding) {
      ReferenceBinding referenceBinding = (ReferenceBinding) binding;

      // First check the type oracle to prefer type identity with the type
      // oracle we're assimilating into.
      //
      String typeName = getQualifiedName(referenceBinding);
      JType resolvedType = oracle.findType(typeName);
      if (resolvedType == null) {
        // Otherwise, it should be something we've mapped during this build.
        resolvedType = cacheManager.getTypeForBinding(referenceBinding);
      }

      if (resolvedType != null) {
        if (binding instanceof RawTypeBinding) {
          // Use the raw type instead of the generic type.
          JGenericType genericType = (JGenericType) resolvedType;
          resolvedType = genericType.getRawType();
        }
        return resolvedType;
      }
    }

    // Check for an array.
    //
    if (binding instanceof ArrayBinding) {
      ArrayBinding arrayBinding = (ArrayBinding) binding;

      // Start by resolving the leaf type.
      //
      TypeBinding leafBinding = arrayBinding.leafComponentType;
      JType resolvedType = resolveType(logger, leafBinding);
      if (resolvedType != null) {
        int dims = arrayBinding.dimensions;
        for (int i = 0; i < dims; ++i) {
          // By using the oracle to intern, we guarantee correct identity
          // mapping of lazily-created array types.
          //
          resolvedType = oracle.getArrayType(resolvedType);
        }
        return resolvedType;
      } else {
        // Fall-through to failure.
        //
      }
    }

    // Check for parameterized.
    if (binding instanceof ParameterizedTypeBinding) {
      ParameterizedTypeBinding ptBinding = (ParameterizedTypeBinding) binding;

      /*
       * NOTE: it is possible for ParameterizedTypeBinding.arguments to be null.
       * This can happen if a generic class has a non-static, non-generic, inner
       * class that references a TypeParameter from its enclosing generic type.
       * You would think that typeVariables() would do the right thing but it
       * does not.
       */
      TypeBinding[] arguments = ptBinding.arguments;
      int nArguments = arguments != null ? arguments.length : 0;
      JClassType[] typeArguments = new JClassType[nArguments];
      boolean failed = false;
      for (int i = 0; i < typeArguments.length; ++i) {
        typeArguments[i] = (JClassType) resolveType(logger, arguments[i]);
        if (typeArguments[i] == null) {
          failed = true;
        }
      }

      JClassType enclosingType = null;
      if (ptBinding.enclosingType() != null) {
        enclosingType = (JClassType) resolveType(logger,
            ptBinding.enclosingType());
        if (enclosingType == null) {
          failed = true;
        }
      }

      /*
       * NOTE: In the case where a generic type has a nested, non-static,
       * non-generic type. The type for the binding will not be a generic type.
       */
      JType resolveType = resolveType(logger, ptBinding.genericType());
      if (resolveType == null) {
        failed = true;
      }

      if (!failed) {
        if (resolveType.isGenericType() != null) {
          return oracle.getParameterizedType(resolveType.isGenericType(),
              enclosingType, typeArguments);
        } else {
          /*
           * A static type (enum or class) that does not declare any type
           * parameters that is nested within a generic type might be referenced
           * via a parameterized type by JDT. In this case we just return the
           * type and don't treat it as a parameterized.
           */
          return resolveType;
        }
      } else {
        // Fall-through to failure
      }
    }

    if (binding instanceof TypeVariableBinding) {
      TypeVariableBinding tvBinding = (TypeVariableBinding) binding;
      JTypeParameter typeParameter = (JTypeParameter) cacheManager.getTypeForBinding(tvBinding);
      if (typeParameter != null) {
        return typeParameter;
      }
      
      // Fall-through to failure
    }

    if (binding instanceof WildcardBinding) {
      WildcardBinding wcBinding = (WildcardBinding) binding;

      assert (wcBinding.otherBounds == null);

      BoundType boundType;
      JClassType typeBound;

      switch (wcBinding.boundKind) {
        case Wildcard.EXTENDS: {
          assert (wcBinding.bound != null);
          boundType = BoundType.EXTENDS;
          typeBound = (JClassType) resolveType(logger, wcBinding.bound);
        }
          break;
        case Wildcard.SUPER: {
          assert (wcBinding.bound != null);
          boundType = BoundType.SUPER;
          typeBound = (JClassType) resolveType(logger, wcBinding.bound);
        }
          break;
        case Wildcard.UNBOUND: {
          boundType = BoundType.UNBOUND;
          typeBound = (JClassType) resolveType(logger, wcBinding.erasure());
        }
          break;
        default:
          assert false : "WildcardBinding of unknown boundKind???";
          return null;
      }

      if (boundType != null) {
        return oracle.getWildcardType(boundType, typeBound);
      }
      
      // Fall-through to failure
    }

    // Log other cases we know about that don't make sense.
    //
    String name = String.valueOf(binding.readableName());
    logger = logger.branch(TreeLogger.WARN, "Unable to resolve type: " + name
        + " binding: " + binding.getClass().getCanonicalName(), null);

    if (binding instanceof BinaryTypeBinding) {
      logger.log(TreeLogger.WARN,
          "Source not available for this type, so it cannot be resolved", null);
    }

    return null;
  }

  private boolean resolveTypeDeclaration(TreeLogger logger, char[] unitSource,
      TypeDeclaration clazz) {
    SourceTypeBinding binding = clazz.binding;
    if (binding.constantPoolName() == null) {
      /*
       * Weird case: if JDT determines that this local class is totally
       * uninstantiable, it won't bother allocating a local name.
       */
      return true;
    }

    String qname = String.valueOf(binding.qualifiedSourceName());
    logger = logger.branch(TreeLogger.SPAM, "Found type '" + qname + "'", null);

    // Handle package-info classes.
    if (isPackageInfoTypeName(qname)) {
      return resolvePackage(logger, clazz);
    }

    // Just resolve the type.
    JRealClassType jtype = (JRealClassType) resolveType(logger, binding);
    if (jtype == null) {
      // Failed to resolve.
      //
      return false;
    }

    /*
     * Modifiers were added during processType since getParameterizedType
     * depends on them being set.
     */

    // Try to resolve annotations, ignore any that fail.
    Map<Class<? extends java.lang.annotation.Annotation>, java.lang.annotation.Annotation> declaredAnnotations = newAnnotationMap();
    resolveAnnotations(logger, clazz.annotations, declaredAnnotations);
    jtype.addAnnotations(declaredAnnotations);

    // Resolve bounds for type parameters on generic types. Note that this
    // step does not apply to type parameters on generic methods; that
    // occurs during the method resolution stage.
    JGenericType jGenericType = jtype.isGenericType();
    if (jGenericType != null
        && !resolveBoundsForTypeParameters(logger, jtype.isGenericType(),
            clazz.typeParameters)) {
      // Failed to resolve
      return false;
    }

    // Resolve superclass (for classes only).
    //
    if (jtype.isInterface() == null) {
      ReferenceBinding superclassRef = binding.superclass;
      if (superclassRef != null) {
        JClassType jsuperClass = (JClassType) resolveType(logger, superclassRef);
        if (jsuperClass == null) {
          return false;
        }
        jtype.setSuperclass(jsuperClass);
      }
    }

    // Resolve superinterfaces.
    //
    ReferenceBinding[] superintfRefs = binding.superInterfaces;
    for (int i = 0; i < superintfRefs.length; i++) {
      ReferenceBinding superintfRef = superintfRefs[i];
      JClassType jinterface = (JClassType) resolveType(logger, superintfRef);
      if (jinterface == null) {
        // Failed to resolve.
        //
        return false;
      }
      jtype.addImplementedInterface(jinterface);
    }

    // Resolve fields.
    //
    FieldDeclaration[] fields = clazz.fields;
    if (!resolveFields(logger, unitSource, jtype, fields)) {
      return false;
    }

    // Resolve methods. This also involves the declaration of type
    // variables on generic methods, and the resolution of the bounds
    // on these type variables.

    // One would think that it would be better to perform the declaration
    // of type variables on methods at the time when we are processing
    // all of the classes. Unfortunately, when we are processing classes,
    // we do not have enough information about their methods to analyze
    // their type variables. Hence, the type variable declaration and
    // bounds resolution for generic methods must happen after the resolution
    // of methods is complete.
    AbstractMethodDeclaration[] methods = clazz.methods;
    if (!resolveMethods(logger, unitSource, jtype, methods)) {
      return false;
    }

    // Get tags.
    //
    if (clazz.javadoc != null) {
      if (!parseMetaDataTags(unitSource, jtype, clazz.javadoc)) {
        return false;
      }
    }

    return true;
  }
}
