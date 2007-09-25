/*
 * Copyright 2007 Google Inc.
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
import com.google.gwt.core.ext.typeinfo.JAbstractMethod;
import com.google.gwt.core.ext.typeinfo.JAnnotationMethod;
import com.google.gwt.core.ext.typeinfo.JAnnotationType;
import com.google.gwt.core.ext.typeinfo.JArrayType;
import com.google.gwt.core.ext.typeinfo.JBound;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JConstructor;
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
import com.google.gwt.core.ext.typeinfo.JWildcardType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.util.Empty;
import com.google.gwt.dev.util.Util;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.core.compiler.IProblem;
import org.eclipse.jdt.internal.compiler.ASTVisitor;
import org.eclipse.jdt.internal.compiler.CompilationResult;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.AnnotationMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.ArrayInitializer;
import org.eclipse.jdt.internal.compiler.ast.CharLiteral;
import org.eclipse.jdt.internal.compiler.ast.ClassLiteralAccess;
import org.eclipse.jdt.internal.compiler.ast.Clinit;
import org.eclipse.jdt.internal.compiler.ast.CompilationUnitDeclaration;
import org.eclipse.jdt.internal.compiler.ast.DoubleLiteral;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.FalseLiteral;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.FloatLiteral;
import org.eclipse.jdt.internal.compiler.ast.Initializer;
import org.eclipse.jdt.internal.compiler.ast.IntLiteral;
import org.eclipse.jdt.internal.compiler.ast.Javadoc;
import org.eclipse.jdt.internal.compiler.ast.LongLiteral;
import org.eclipse.jdt.internal.compiler.ast.MagicLiteral;
import org.eclipse.jdt.internal.compiler.ast.MemberValuePair;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.NullLiteral;
import org.eclipse.jdt.internal.compiler.ast.NumberLiteral;
import org.eclipse.jdt.internal.compiler.ast.ParameterizedQualifiedTypeReference;
import org.eclipse.jdt.internal.compiler.ast.ParameterizedSingleTypeReference;
import org.eclipse.jdt.internal.compiler.ast.QualifiedNameReference;
import org.eclipse.jdt.internal.compiler.ast.StringLiteral;
import org.eclipse.jdt.internal.compiler.ast.TrueLiteral;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeParameter;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.ast.Wildcard;
import org.eclipse.jdt.internal.compiler.env.ICompilationUnit;
import org.eclipse.jdt.internal.compiler.env.IGenericType;
import org.eclipse.jdt.internal.compiler.lookup.ArrayBinding;
import org.eclipse.jdt.internal.compiler.lookup.BaseTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.BinaryTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.BlockScope;
import org.eclipse.jdt.internal.compiler.lookup.ClassScope;
import org.eclipse.jdt.internal.compiler.lookup.CompilationUnitScope;
import org.eclipse.jdt.internal.compiler.lookup.FieldBinding;
import org.eclipse.jdt.internal.compiler.lookup.LocalTypeBinding;
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
import java.util.regex.Pattern;

/**
 * Builds a {@link com.google.gwt.dev.typeinfo.TypeOracle} from a set of
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

  private static String getMethodName(JClassType enclosingType,
      AbstractMethodDeclaration jmethod) {
    if (jmethod.isConstructor()) {
      return String.valueOf(enclosingType.getSimpleSourceName());
    } else {
      return String.valueOf(jmethod.binding.selector);
    }
  }

  private static boolean isAnnotation(TypeDeclaration typeDecl) {
    if (typeDecl.kind() == IGenericType.ANNOTATION_TYPE_DECL) {
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
        Util.maybeDumpSource(logger, fileName, source, null);
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
    cacheManager.invalidateOnRefresh(oracle);
    CompilationUnitDeclaration[] cuds = cacheManager.getAstCompiler().getCompilationUnitDeclarations(
        logger, units);

    // Build a list that makes it easy to remove problems.
    //
    final Map<String, CompilationUnitDeclaration> cudsByFileName = new HashMap<String, CompilationUnitDeclaration>();
    for (int i = 0; i < cuds.length; i++) {
      CompilationUnitDeclaration cud = cuds[i];
      char[] location = cud.getFileName();
      cudsByFileName.put(String.valueOf(location), cud);
    }
    cacheManager.getCudsByFileName().putAll(cudsByFileName);

    // Remove bad cuds and all the other cuds that are affected.
    //
    removeUnitsWithErrors(logger, cudsByFileName);

    // Also remove any compilation units that we've seen before.
    //
    for (Iterator<CompilationUnitDeclaration> iter = cudsByFileName.values().iterator(); iter.hasNext();) {
      CompilationUnitDeclaration cud = iter.next();
      // If we've seen this compilation unit before, the type oracle will
      // tell us about it and so we don't assimilate it again.
      //
      ICompilationUnit unit = cud.compilationResult.compilationUnit;
      ICompilationUnitAdapter adapter = ((ICompilationUnitAdapter) unit);
      CompilationUnitProvider cup = adapter.getCompilationUnitProvider();
      JClassType[] seen = oracle.getTypesInCompilationUnit(cup);
      if (seen.length > 0) {
        // This compilation unit has already been assimilated.
        //
        iter.remove();
      }
    }

    // Perform a shallow pass to establish identity for new types.
    //
    final CacheManager.Mapper identityMapper = cacheManager.getIdentityMapper();
    for (Iterator<CompilationUnitDeclaration> iter = cudsByFileName.values().iterator(); iter.hasNext();) {
      CompilationUnitDeclaration cud = iter.next();

      cud.traverse(new ASTVisitor() {
        @Override
        public boolean visit(
            ParameterizedQualifiedTypeReference parameterizedQualifiedTypeReference,
            BlockScope scope) {
          ParameterizedTypeBinding jparameterizedType = (ParameterizedTypeBinding) parameterizedQualifiedTypeReference.resolvedType;
          processParameterizedType(jparameterizedType);
          return true; // do nothing by default, keep traversing
        }

        @Override
        public boolean visit(
            ParameterizedQualifiedTypeReference parameterizedQualifiedTypeReference,
            ClassScope scope) {
          ParameterizedTypeBinding jparameterizedType = (ParameterizedTypeBinding) parameterizedQualifiedTypeReference.resolvedType;
          processParameterizedType(jparameterizedType);
          return true; // do nothing by default, keep traversing
        }

        @Override
        public boolean visit(
            ParameterizedSingleTypeReference parameterizedSingleTypeReference,
            BlockScope scope) {
          if (parameterizedSingleTypeReference.resolvedType instanceof ParameterizedTypeBinding) {
            ParameterizedTypeBinding jparameterizedType = (ParameterizedTypeBinding) parameterizedSingleTypeReference.resolvedType;
            processParameterizedType(jparameterizedType);
          } else if (parameterizedSingleTypeReference.resolvedType instanceof SourceTypeBinding) {
            // nothing to do
          } else {
            assert false;
          }
          return true; // do nothing by default, keep traversing
        }

        @Override
        public boolean visit(
            ParameterizedSingleTypeReference parameterizedSingleTypeReference,
            ClassScope scope) {
          return true; // do nothing by default, keep traversing
        }

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
    return oracle;
  }

  private JBound createTypeVariableBounds(TreeLogger logger,
      TypeVariableBinding tvBinding) {
    TypeBinding firstBound = tvBinding.firstBound;
    if (firstBound == null) {
      firstBound = tvBinding.superclass;
    }
    JClassType firstBoundType = (JClassType) resolveType(logger, firstBound);
    JBound bounds = new JBound(firstBoundType);
    for (ReferenceBinding ref : tvBinding.superInterfaces()) {
      JClassType bound = (JClassType) resolveType(logger, ref);
      assert (bound.isInterface() != null);
      bounds.addUpperBound(bound);
    }
    return bounds;
  }

  private Object evaluateAnnotationExpression(TreeLogger logger,
      Expression expression) {
    Annotation annotation = (Annotation) expression;

    // Determine the annotation class
    TypeBinding resolvedType = annotation.resolvedType;
    Class<? extends java.lang.annotation.Annotation> clazz = (Class<? extends java.lang.annotation.Annotation>) getClassLiteral(
        logger, resolvedType);
    if (clazz == null) {
      return null;
    }

    // Build the map of identifiers to values.
    Map<String, Object> identifierToValue = new HashMap<String, Object>();
    for (MemberValuePair mvp : annotation.memberValuePairs()) {
      // Method name
      String identifier = String.valueOf(mvp.name);

      // Value
      Expression expressionValue = mvp.value;
      Object value = evaluateExpression(logger, expressionValue);
      if (value == null) {
        return null;
      }

      identifierToValue.put(identifier, value);
    }

    // Create the Annotation proxy
    JClassType annotationType = (JClassType) resolveType(logger, resolvedType);
    if (annotationType == null) {
      return null;
    }

    return AnnotationProxyFactory.create(clazz, annotationType,
        identifierToValue);
  }

  private Object evaluateArrayInitializerExpression(TreeLogger logger,
      Expression expression) {
    ArrayInitializer arrayInitializer = (ArrayInitializer) expression;
    Class<?> leafComponentClass = getClassLiteral(logger,
        arrayInitializer.binding.leafComponentType);
    int[] dimensions = new int[arrayInitializer.binding.dimensions];

    Expression[] initExpressions = arrayInitializer.expressions;
    if (initExpressions != null) {
      dimensions[0] = initExpressions.length;
    }

    Object array = Array.newInstance(leafComponentClass, dimensions);
    boolean failed = false;
    if (initExpressions != null) {
      for (int i = 0; i < initExpressions.length; ++i) {
        Expression arrayInitExp = initExpressions[i];
        Object value = evaluateExpression(logger, arrayInitExp);
        if (value != null) {
          Array.set(array, i, value);
        } else {
          failed = true;
          break;
        }
      }
    }

    if (!failed) {
      return array;
    }

    return null;
  }

  private Object evaluateExpression(TreeLogger logger, Expression expression) {
    if (expression instanceof MagicLiteral) {
      if (expression instanceof FalseLiteral) {
        return Boolean.FALSE;
      } else if (expression instanceof NullLiteral) {
        // null is not a valid annotation value; JLS Third Ed. Section 9.7
      } else if (expression instanceof TrueLiteral) {
        return Boolean.TRUE;
      }
    } else if (expression instanceof NumberLiteral) {
      Object value = evaluateNumericExpression(expression);
      if (value != null) {
        return value;
      }
    } else if (expression instanceof StringLiteral) {
      StringLiteral stringLiteral = (StringLiteral) expression;
      return stringLiteral.constant.stringValue();
    } else if (expression instanceof ClassLiteralAccess) {
      ClassLiteralAccess classLiteral = (ClassLiteralAccess) expression;
      Class<?> clazz = getClassLiteral(logger, classLiteral.resolvedType);
      if (clazz != null) {
        return clazz;
      }
    } else if (expression instanceof ArrayInitializer) {
      Object value = evaluateArrayInitializerExpression(logger, expression);
      if (value != null) {
        return value;
      }
    } else if (expression instanceof QualifiedNameReference) {
      QualifiedNameReference qualifiedNameRef = (QualifiedNameReference) expression;
      Class clazz = getClassLiteral(logger, qualifiedNameRef.actualReceiverType);
      assert (clazz.isEnum());
      if (clazz != null) {
        FieldBinding fieldBinding = qualifiedNameRef.fieldBinding();
        String enumName = String.valueOf(fieldBinding.name);
        return Enum.valueOf(clazz, enumName);
      }
    } else if (expression instanceof Annotation) {
      Object annotationInstance = evaluateAnnotationExpression(logger,
          expression);
      if (annotationInstance != null) {
        return annotationInstance;
      }
    }

    assert (false);
    return null;
  }

  private Object evaluateNumericExpression(Expression expression) {
    if (expression instanceof CharLiteral) {
      CharLiteral charLiteral = (CharLiteral) expression;
      return Character.valueOf(charLiteral.constant.charValue());
    } else if (expression instanceof DoubleLiteral) {
      DoubleLiteral doubleLiteral = (DoubleLiteral) expression;
      return Double.valueOf(doubleLiteral.constant.doubleValue());
    } else if (expression instanceof FloatLiteral) {
      FloatLiteral floatLiteral = (FloatLiteral) expression;
      return Float.valueOf(floatLiteral.constant.floatValue());
    } else if (expression instanceof IntLiteral) {
      IntLiteral intLiteral = (IntLiteral) expression;
      return Integer.valueOf(intLiteral.constant.intValue());
    } else if (expression instanceof LongLiteral) {
      LongLiteral longLiteral = (LongLiteral) expression;
      return Long.valueOf(longLiteral.constant.longValue());
    }

    return null;
  }

  private Class<?> getClassLiteral(TreeLogger logger, TypeBinding resolvedType) {
    JClassType annotationType = (JClassType) resolveType(logger, resolvedType);
    if (annotationType == null) {
      return null;
    }

    String className = computeBinaryClassName(annotationType);
    try {
      Class<?> clazz = Class.forName(className);
      return clazz;
    } catch (ClassNotFoundException e) {
      logger.log(TreeLogger.ERROR, "", e);
      // TODO(mmendez): how should we deal with this error?
      return null;
    }
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

  private String getQualifiedName(ReferenceBinding binding) {
    return CharOperation.toString(binding.compoundName);
  }

  private String getSimpleName(TypeDeclaration typeDecl) {
    return String.valueOf(typeDecl.name);
  }

  private boolean isInterface(TypeDeclaration typeDecl) {
    if (typeDecl.kind() == IGenericType.INTERFACE_DECL) {
      return true;
    } else {
      return false;
    }
  }

  /**
   * Maps a ParameterizedTypeBinding into a JParameterizedType.
   */
  private void processParameterizedType(ParameterizedTypeBinding binding) {
    TypeOracle oracle = cacheManager.getTypeOracle();
    // TODO Get the JParameterized type for the current binding, fill it out
    // as you would a JClassType.
  }

  /**
   * Maps a TypeDeclaration into a JClassType.
   */
  private void processType(TypeDeclaration typeDecl, JClassType enclosingType,
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

    String qname;
    String jclassName;
    if (binding instanceof LocalTypeBinding) {
      char[] localName = binding.constantPoolName();
      for (int i = 0, c = localName.length; i < c; ++i) {
        if (localName[i] == '/' || localName[i] == '$') {
          localName[i] = '.';
        }
      }
      qname = String.valueOf(localName);
      jclassName = qname.substring(qname.lastIndexOf('.') + 1);
    } else {
      qname = getQualifiedName(binding);
      jclassName = getSimpleName(typeDecl);
    }
    if (oracle.findType(qname) != null) {
      // The oracle already knew about this type.
      // Don't re-add it.
      //
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

    JRealClassType type;
    if (jclassIsAnnonation) {
      type = new JAnnotationType(oracle, cup, pkg, enclosingType, isLocalType,
          jclassName, declStart, declEnd, bodyStart, bodyEnd, jclassIsIntf);
    } else if (typeDecl.typeParameters != null
        && typeDecl.typeParameters.length > 0) {
      type = new JGenericType(oracle, cup, pkg, enclosingType, isLocalType,
          jclassName, declStart, declEnd, bodyStart, bodyEnd, jclassIsIntf);
      for (TypeParameter jtypeParameter : typeDecl.typeParameters) {
        JTypeParameter typeParameter = new JTypeParameter(
            String.valueOf(jtypeParameter.name), (JGenericType) type);
        cacheManager.setTypeForBinding(jtypeParameter.binding, typeParameter);
      }
    } else {
      type = new JRealClassType(oracle, cup, pkg, enclosingType, isLocalType,
          jclassName, declStart, declEnd, bodyStart, bodyEnd, jclassIsIntf);
    }

    // TODO: enums?

    cacheManager.setTypeForBinding(binding, type);
  }

  private boolean resolveAnnotation(
      TreeLogger logger,
      Annotation jannotation,
      Map<Class<? extends java.lang.annotation.Annotation>, java.lang.annotation.Annotation> declaredAnnotations) {
    // Determine the annotation class
    TypeBinding resolvedType = jannotation.resolvedType;
    Class<? extends java.lang.annotation.Annotation> clazz = (Class<? extends java.lang.annotation.Annotation>) getClassLiteral(
        logger, resolvedType);
    if (clazz == null) {
      return false;
    }

    java.lang.annotation.Annotation annotation = (java.lang.annotation.Annotation) evaluateExpression(
        logger, jannotation);
    declaredAnnotations.put(clazz, annotation);
    return (annotation != null) ? true : false;
  }

  private boolean resolveAnnotations(
      TreeLogger logger,
      Annotation[] annotations,
      Map<Class<? extends java.lang.annotation.Annotation>, java.lang.annotation.Annotation> declaredAnnotations) {
    if (annotations != null) {
      for (Annotation annotation : annotations) {
        if (!resolveAnnotation(logger, annotation, declaredAnnotations)) {
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

    // Resolve annotations
    Map<Class<? extends java.lang.annotation.Annotation>, java.lang.annotation.Annotation> declaredAnnotations = newAnnotationMap();
    if (!resolveAnnotations(logger, jfield.annotations, declaredAnnotations)) {
      // Failed to resolve.
      //
      return false;
    }

    String name = String.valueOf(jfield.name);
    JField field = new JField(enclosingType, name, declaredAnnotations);

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

    // Resolve annotations
    Map<Class<? extends java.lang.annotation.Annotation>, java.lang.annotation.Annotation> declaredAnnotations = newAnnotationMap();
    if (!resolveAnnotations(logger, jmethod.annotations, declaredAnnotations)) {
      // Failed to resolve.
      //
      return false;
    }

    JAbstractMethod method;
    if (jmethod.isConstructor()) {
      method = new JConstructor(enclosingType, name, declStart, declEnd,
          bodyStart, bodyEnd, declaredAnnotations);
    } else {
      if (jmethod.isAnnotationMethod()) {
        AnnotationMethodDeclaration annotationMethod = (AnnotationMethodDeclaration) jmethod;
        Object defaultValue = null;
        if (annotationMethod.defaultValue != null) {
          defaultValue = evaluateExpression(logger,
              annotationMethod.defaultValue);
        }
        method = new JAnnotationMethod(enclosingType, name, declStart, declEnd,
            bodyStart, bodyEnd, defaultValue, declaredAnnotations);
      } else {
        method = new JMethod(enclosingType, name, declStart, declEnd,
            bodyStart, bodyEnd, declaredAnnotations);
      }

      // Add declared type parameters.
      if (jmethod.typeParameters() != null) {
        for (TypeParameter jtypeParameter : jmethod.typeParameters()) {
          JTypeParameter typeParameter = new JTypeParameter(
              String.valueOf(jtypeParameter.name), method);
          cacheManager.getIdentityMapper().put(jtypeParameter.binding,
              typeParameter);
        }
        JTypeParameter[] typeParameters = method.getTypeParameters();
        for (TypeParameter jtypeParameter : jmethod.typeParameters()) {
          JTypeParameter typeParameter = (JTypeParameter) cacheManager.getIdentityMapper().get(
              jtypeParameter.binding);
          JBound bounds = createTypeVariableBounds(logger,
              jtypeParameter.binding);
          typeParameter.setBounds(bounds);
        }
      }

      // Add the return type if necessary.
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

    // Resolve annotations
    Map<Class<? extends java.lang.annotation.Annotation>, java.lang.annotation.Annotation> declaredAnnotations = newAnnotationMap();
    if (!resolveAnnotations(logger,
        cus.referenceContext.currentPackage.annotations, declaredAnnotations)) {
      // Failed to resolve.
      //
      return false;
    }

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

    // Resolve annotations
    Map<Class<? extends java.lang.annotation.Annotation>, java.lang.annotation.Annotation> declaredAnnotations = newAnnotationMap();
    if (!resolveAnnotations(logger, jparam.annotations, declaredAnnotations)) {
      // Failed to resolve.
      //
      return false;
    }

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
      String typeName = String.valueOf(referenceBinding.readableName());
      JType resolvedType = oracle.findType(typeName);
      if (resolvedType != null) {
        return resolvedType;
      }

      // Otherwise, it should be something we've mapped during this build.
      //
      resolvedType = cacheManager.getTypeForBinding(referenceBinding);
      if (resolvedType != null) {
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
      JClassType[] typeArguments = new JClassType[ptBinding.arguments.length];
      for (int i = 0; i < typeArguments.length; ++i) {
        typeArguments[i] = (JClassType) resolveType(logger,
            ptBinding.arguments[i]);
      }

      JGenericType baseType = (JGenericType) resolveType(logger, ptBinding.type);
      return oracle.getParameterizedType(baseType, typeArguments);
    }

    if (binding instanceof TypeVariableBinding) {
      TypeVariableBinding tvBinding = (TypeVariableBinding) binding;
      JTypeParameter typeParameter = (JTypeParameter) cacheManager.getTypeForBinding(tvBinding);
      assert (typeParameter != null);

      if (typeParameter.getBounds() == null) {
        // Setup a dummy bound to prevent recursion
        typeParameter.setBounds(new JBound(null));
        JBound bounds = createTypeVariableBounds(logger, tvBinding);
        typeParameter.setBounds(bounds);
      }
      return typeParameter;
    }

    if (binding instanceof WildcardBinding) {
      WildcardBinding wcBinding = (WildcardBinding) binding;
      JBound bounds;
      switch (wcBinding.boundKind) {
        case Wildcard.EXTENDS: {
          JClassType firstBoundType = (JClassType) resolveType(logger,
              wcBinding.bound);
          bounds = new JBound(firstBoundType);
          if (wcBinding.otherBounds != null) {
            for (TypeBinding bound : wcBinding.otherBounds) {
              JClassType boundType = (JClassType) resolveType(logger, bound);
              bounds.addUpperBound(boundType);
            }
          }
        }
          break;
        case Wildcard.SUPER: {
          // TODO: verify
          JClassType firstBoundType = (JClassType) resolveType(logger,
              wcBinding.erasure());
          assert (firstBoundType.getQualifiedSourceName().equals("java.lang.Object"));
          bounds = new JBound(firstBoundType);

          assert (wcBinding.bound != null);
          JClassType boundType = (JClassType) resolveType(logger,
              wcBinding.bound);
          bounds.addLowerBound(boundType);
          if (wcBinding.otherBounds != null) {
            for (TypeBinding bound : wcBinding.otherBounds) {
              boundType = (JClassType) resolveType(logger, bound);
              bounds.addLowerBound(boundType);
            }
          }
        }
          break;
        case Wildcard.UNBOUND: {
          JClassType firstBoundType = (JClassType) resolveType(logger,
              wcBinding.erasure());
          assert (firstBoundType.getQualifiedSourceName().equals("java.lang.Object"));
          bounds = new JBound(firstBoundType);
        }
          break;
        default:
          assert false : "WildcardBinding of unknown boundKind???";
          return null;
      }
      JWildcardType wcType = new JWildcardType(bounds);
      return wcType;
    }

    // Log other cases we know about that don't make sense.
    //
    if (binding instanceof BinaryTypeBinding) {
      logger.log(TreeLogger.WARN,
          "Source not available for this type, so it cannot be resolved", null);
    }

    String name = String.valueOf(binding.readableName());
    logger.log(TreeLogger.WARN, "Unable to resolve type: " + name
        + " binding: " + binding.getClass().getCanonicalName(), null);
    return null;
  }

  private boolean resolveTypeDeclaration(TreeLogger logger, char[] unitSource,
      TypeDeclaration jclass) {
    SourceTypeBinding binding = jclass.binding;
    if (binding.constantPoolName() == null) {
      /*
       * Weird case: if JDT determines that this local class is totally
       * uninstantiable, it won't bother allocating a local name.
       */
      return true;
    }

    String qname = String.valueOf(binding.qualifiedSourceName());
    logger.log(TreeLogger.SPAM, "Found type '" + qname + "'", null);

    // Handle package-info classes.
    if (isPackageInfoTypeName(qname)) {
      return resolvePackage(logger, jclass);
    }

    // Just resolve the type.
    JRealClassType type = (JRealClassType) resolveType(logger, binding);
    if (type == null) {
      // Failed to resolve.
      //
      return false;
    }

    // Add modifiers.
    //
    type.addModifierBits(Shared.bindingToModifierBits(jclass.binding));

    // Resolve annotations
    Map<Class<? extends java.lang.annotation.Annotation>, java.lang.annotation.Annotation> declaredAnnotations = newAnnotationMap();
    if (!resolveAnnotations(logger, jclass.annotations, declaredAnnotations)) {
      // Failed to resolve.
      //
      return false;
    }
    type.addAnnotations(declaredAnnotations);

    // Resolve type parameters
    List<JTypeParameter> typeParameters = new ArrayList<JTypeParameter>();
    if (!resolveTypeParameters(logger, jclass.typeParameters, typeParameters)) {
      // Failed to resolve
      return false;
    }

    // Resolve superclass (for classes only).
    //
    if (type.isInterface() == null) {
      ReferenceBinding superclassRef = binding.superclass;
      if (superclassRef != null) {
        JClassType superclass = (JClassType) resolveType(logger, superclassRef);
        if (superclass == null) {
          return false;
        }
        type.setSuperclass(superclass);
      }
    }

    // Resolve superinterfaces.
    //
    ReferenceBinding[] superintfRefs = binding.superInterfaces;
    for (int i = 0; i < superintfRefs.length; i++) {
      ReferenceBinding superintfRef = superintfRefs[i];
      JClassType intf = (JClassType) resolveType(logger, superintfRef);
      if (intf == null) {
        // Failed to resolve.
        //
        return false;
      }
      type.addImplementedInterface(intf);
    }

    // Resolve fields.
    //
    FieldDeclaration[] jfields = jclass.fields;
    if (!resolveFields(logger, unitSource, type, jfields)) {
      return false;
    }

    // Resolve methods.
    //
    AbstractMethodDeclaration[] jmethods = jclass.methods;
    if (!resolveMethods(logger, unitSource, type, jmethods)) {
      return false;
    }

    // Get tags.
    //
    if (jclass.javadoc != null) {
      if (!parseMetaDataTags(unitSource, type, jclass.javadoc)) {
        return false;
      }
    }

    return true;
  }

  private boolean resolveTypeParameter(TreeLogger logger,
      TypeParameter jtypeParameter, List<JTypeParameter> typeParameters) {
    JTypeParameter typeParameter = (JTypeParameter) cacheManager.getIdentityMapper().get(
        jtypeParameter.binding);
    typeParameters.add(typeParameter);
    return true;
  }

  private boolean resolveTypeParameters(TreeLogger logger,
      TypeParameter[] jtypeParameters, List<JTypeParameter> typeParameters) {
    if (jtypeParameters != null) {
      for (int i = 0; i < jtypeParameters.length; ++i) {
        if (!resolveTypeParameter(logger, jtypeParameters[i], typeParameters)) {
          return false;
        }
      }
    }
    return true;
  }
}
