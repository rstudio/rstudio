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
package com.google.gwt.dev.javac;

import com.google.gwt.core.ext.TreeLogger;
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
import com.google.gwt.dev.javac.CompilationUnit.State;
import com.google.gwt.dev.javac.impl.Shared;
import com.google.gwt.dev.util.collect.HashMap;
import com.google.gwt.dev.util.collect.HashSet;
import com.google.gwt.dev.util.collect.IdentityHashMap;
import com.google.gwt.dev.util.collect.Maps;

import org.eclipse.jdt.core.compiler.CharOperation;
import org.eclipse.jdt.internal.compiler.ast.AbstractMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.AbstractVariableDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Annotation;
import org.eclipse.jdt.internal.compiler.ast.AnnotationMethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Argument;
import org.eclipse.jdt.internal.compiler.ast.ArrayInitializer;
import org.eclipse.jdt.internal.compiler.ast.ClassLiteralAccess;
import org.eclipse.jdt.internal.compiler.ast.Clinit;
import org.eclipse.jdt.internal.compiler.ast.Expression;
import org.eclipse.jdt.internal.compiler.ast.FieldDeclaration;
import org.eclipse.jdt.internal.compiler.ast.Initializer;
import org.eclipse.jdt.internal.compiler.ast.MemberValuePair;
import org.eclipse.jdt.internal.compiler.ast.MethodDeclaration;
import org.eclipse.jdt.internal.compiler.ast.NameReference;
import org.eclipse.jdt.internal.compiler.ast.TypeDeclaration;
import org.eclipse.jdt.internal.compiler.ast.TypeParameter;
import org.eclipse.jdt.internal.compiler.ast.TypeReference;
import org.eclipse.jdt.internal.compiler.ast.Wildcard;
import org.eclipse.jdt.internal.compiler.impl.Constant;
import org.eclipse.jdt.internal.compiler.lookup.ArrayBinding;
import org.eclipse.jdt.internal.compiler.lookup.BaseTypeBinding;
import org.eclipse.jdt.internal.compiler.lookup.BinaryTypeBinding;
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

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds or rebuilds a {@link com.google.gwt.core.ext.typeinfo.TypeOracle} from
 * a set of compilation units.
 */
public class TypeOracleMediator {

  private static final JClassType[] NO_JCLASSES = new JClassType[0];

  /**
   * Returns the binary name of a type. This is the same name that would be
   * returned by {@link Class#getName()} for this type.
   */
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

  private static RetentionPolicy getRetentionPolicy(
      Class<? extends java.lang.annotation.Annotation> clazz) {
    // Default retention policy is CLASS, see @Retention.
    RetentionPolicy retentionPolicy = RetentionPolicy.CLASS;
    Retention retentionAnnotation = clazz.getAnnotation(Retention.class);
    if (retentionAnnotation != null) {
      retentionPolicy = retentionAnnotation.value();
    }
    return retentionPolicy;
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

    if (typeDecl.typeParameters != null) {
      // Definitely generic since it has type parameters.
      return true;
    }

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

    return false;
  }

  // TODO: move into the Annotations class or create an AnnotationsUtil class?
  private static HashMap<Class<? extends java.lang.annotation.Annotation>, java.lang.annotation.Annotation> newAnnotationMap() {
    return new HashMap<Class<? extends java.lang.annotation.Annotation>, java.lang.annotation.Annotation>();
  }

  private final Map<String, JRealClassType> binaryMapper = new HashMap<String, JRealClassType>();

  /**
   * Mapping of source type bindings; transient because freshly-compiled units
   * are processed in chunks, so only references between types in the same chunk
   * are source bindings; the rest are going to be binary type bindings.
   */
  private final transient Map<SourceTypeBinding, JRealClassType> sourceMapper = new IdentityHashMap<SourceTypeBinding, JRealClassType>();

  /**
   * Mapping of type variable bindings; transient because compilation units are
   * processed monolithically, and a tv binding is only valid within a single
   * unit.
   */
  private final transient Map<TypeVariableBinding, JTypeParameter> tvMapper = new IdentityHashMap<TypeVariableBinding, JTypeParameter>();

  private final TypeOracle typeOracle = new TypeOracle();

  /**
   * Set of unresolved types to process, generated from the first phase of type
   * oracle resolution. Transient because they all get resolved in the second
   * phase.
   */
  private final transient Set<JRealClassType> unresolvedTypes = new HashSet<JRealClassType>();

  /**
   * Adds new units to an existing TypeOracle.
   */
  public void addNewUnits(TreeLogger logger, Collection<CompilationUnit> units) {
    // Perform a shallow pass to establish identity for new and old types.
    for (CompilationUnit unit : units) {
      switch (unit.getState()) {
        case GRAVEYARD:
          for (CompiledClass compiledClass : unit.getCompiledClasses()) {
            JRealClassType type = compiledClass.getRealClassType();
            assert (type != null);
            type.resurrect();
            binaryMapper.put(compiledClass.getInternalName(), type);
          }
          break;
        case COMPILED:
          for (CompiledClass compiledClass : unit.getCompiledClasses()) {
            JRealClassType type = createType(compiledClass);
            binaryMapper.put(compiledClass.getInternalName(), type);
          }
          break;
        case CHECKED:
          for (CompiledClass compiledClass : unit.getCompiledClasses()) {
            JRealClassType type = compiledClass.getRealClassType();
            assert (type != null);
            binaryMapper.put(compiledClass.getInternalName(), type);
          }
          break;
      }
    }

    // Perform a deep pass to resolve all new types in terms of our types.
    for (CompilationUnit unit : units) {
      if (unit.getState() != State.COMPILED) {
        continue;
      }
      TreeLogger cudLogger = logger.branch(TreeLogger.SPAM,
          "Processing types in compilation unit: " + unit.getDisplayLocation());
      Set<CompiledClass> compiledClasses = unit.getCompiledClasses();
      for (CompiledClass compiledClass : compiledClasses) {
        if (unresolvedTypes.remove(compiledClass.getRealClassType())) {
          TypeDeclaration typeDeclaration = compiledClass.getTypeDeclaration();
          if (!resolveTypeDeclaration(cudLogger, typeDeclaration)) {
            logger.log(TreeLogger.WARN,
                "Unexpectedly unable to fully resolve type "
                    + compiledClass.getSourceName());
          }
        }
      }
    }
    // Clean transient state.
    assert unresolvedTypes.size() == 0;
    sourceMapper.clear();
    tvMapper.clear();

    typeOracle.finish();
  }

  public TypeOracle getTypeOracle() {
    return typeOracle;
  }

  /**
   * Full refresh based on new units.
   */
  public void refresh(TreeLogger logger, Collection<CompilationUnit> units) {
    logger = logger.branch(TreeLogger.DEBUG, "Refreshing TypeOracle");
    binaryMapper.clear();
    typeOracle.reset();
    addNewUnits(logger, units);
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

    return AnnotationProxyFactory.create(clazz,
        Maps.normalize(identifierToValue));
  }

  private JRealClassType createType(CompiledClass compiledClass) {
    JRealClassType realClassType = compiledClass.getRealClassType();
    if (realClassType == null) {
      JRealClassType enclosingType = null;
      CompiledClass enclosingClass = compiledClass.getEnclosingClass();
      if (enclosingClass != null) {
        enclosingType = enclosingClass.getRealClassType();
        if (enclosingType == null) {
          enclosingType = createType(enclosingClass);
        }
      }
      realClassType = createType(compiledClass, enclosingType);
      if (realClassType != null) {
        unresolvedTypes.add(realClassType);
        sourceMapper.put(compiledClass.getTypeDeclaration().binding,
            realClassType);
        compiledClass.setRealClassType(realClassType);
      }
    }
    return realClassType;
  }

  /**
   * Maps a TypeDeclaration into a JRealClassType. If the TypeDeclaration has
   * TypeParameters (i.e, it is a generic type or method), then the
   * TypeParameters are mapped into JTypeParameters.
   */
  private JRealClassType createType(CompiledClass compiledClass,
      JRealClassType enclosingType) {
    TypeDeclaration typeDecl = compiledClass.getTypeDeclaration();
    SourceTypeBinding binding = typeDecl.binding;
    assert (binding.constantPoolName() != null);

    String qname = compiledClass.getSourceName();
    String className = Shared.getShortName(qname);
    String jpkgName = compiledClass.getPackageName();
    JPackage pkg = typeOracle.getOrCreatePackage(jpkgName);
    boolean isLocalType = binding instanceof LocalTypeBinding;
    boolean isIntf = TypeDeclaration.kind(typeDecl.modifiers) == TypeDeclaration.INTERFACE_DECL;
    boolean isAnnotation = TypeDeclaration.kind(typeDecl.modifiers) == TypeDeclaration.ANNOTATION_TYPE_DECL;

    JRealClassType resultType;
    if (isAnnotation) {
      resultType = new JAnnotationType(typeOracle, pkg, enclosingType,
          isLocalType, className, isIntf);
    } else if (maybeGeneric(typeDecl, enclosingType)) {
      // Go through and create declarations for each of the type parameters on
      // the generic class or method
      JTypeParameter[] jtypeParameters = declareTypeParameters(typeDecl.typeParameters);

      JGenericType jgenericType = new JGenericType(typeOracle, pkg,
          enclosingType, isLocalType, className, isIntf, jtypeParameters);

      resultType = jgenericType;
    } else if (binding.isEnum()) {
      resultType = new JEnumType(typeOracle, pkg, enclosingType, isLocalType,
          className, isIntf);
    } else {
      resultType = new JRealClassType(typeOracle, pkg, enclosingType,
          isLocalType, className, isIntf);
    }

    /*
     * Declare type parameters for all methods; we must do this during the first
     * pass.
     */
    if (typeDecl.methods != null) {
      for (AbstractMethodDeclaration method : typeDecl.methods) {
        declareTypeParameters(method.typeParameters());
      }
    }

    /*
     * Add modifiers since these are needed for
     * TypeOracle.getParameterizedType's error checking code.
     */
    resultType.addModifierBits(Shared.bindingToModifierBits(binding));
    return resultType;
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
    for (int i = 0; i < typeParameters.length; ++i) {
      TypeParameter typeParam = typeParameters[i];
      jtypeParamArray[i] = new JTypeParameter(String.valueOf(typeParam.name), i);
      tvMapper.put(typeParam.binding, jtypeParamArray[i]);
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
  @SuppressWarnings("unchecked")
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
    if (resolvedType instanceof BaseTypeBinding) {
      return getClassLiteralForPrimitive((BaseTypeBinding) resolvedType);
    } else {
      try {
        String className = String.valueOf(resolvedType.constantPoolName());
        className = className.replace('/', '.');
        return Class.forName(className, false,
            Thread.currentThread().getContextClassLoader());
      } catch (ClassNotFoundException e) {
        logger.log(TreeLogger.ERROR, "", e);
        return null;
      }
    }
  }

  private Class<?> getClassLiteralForPrimitive(BaseTypeBinding type) {
    switch (type.id) {
      case TypeIds.T_boolean:
        return Boolean.TYPE;
      case TypeIds.T_byte:
        return Byte.TYPE;
      case TypeIds.T_char:
        return Character.TYPE;
      case TypeIds.T_short:
        return Short.TYPE;
      case TypeIds.T_int:
        return Integer.TYPE;
      case TypeIds.T_long:
        return Long.TYPE;
      case TypeIds.T_float:
        return Float.TYPE;
      case TypeIds.T_double:
        return Double.TYPE;
      case TypeIds.T_void:
        return Void.TYPE;
      default:
        assert false : "Unexpected base type id " + type.id;
        return null;
    }
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

  private boolean resolveAnnotation(
      TreeLogger logger,
      Annotation jannotation,
      Map<Class<? extends java.lang.annotation.Annotation>, java.lang.annotation.Annotation> declaredAnnotations) {

    logger = logger.branch(TreeLogger.SPAM, "Resolving annotation '"
        + jannotation.printExpression(0, new StringBuffer()).toString() + "'",
        null);

    // Determine the annotation class
    TypeBinding resolvedType = jannotation.resolvedType;
    Class<?> classLiteral = getClassLiteral(logger, resolvedType);
    if (classLiteral == null) {
      return false;
    }

    java.lang.annotation.Annotation annotation = (java.lang.annotation.Annotation) createAnnotationInstance(
        logger, jannotation);
    if (annotation == null) {
      return false;
    }

    Class<? extends java.lang.annotation.Annotation> clazz = classLiteral.asSubclass(java.lang.annotation.Annotation.class);
    // Do not reflect source-only annotations.
    if (getRetentionPolicy(clazz) != RetentionPolicy.SOURCE) {
      declaredAnnotations.put(clazz, annotation);
    }
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

  private boolean resolveField(TreeLogger logger, JClassType enclosingType,
      FieldDeclaration jfield) {

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

    return true;
  }

  private boolean resolveFields(TreeLogger logger, JClassType type,
      FieldDeclaration[] jfields) {
    if (jfields != null) {
      for (int i = 0; i < jfields.length; i++) {
        FieldDeclaration jfield = jfields[i];
        if (!resolveField(logger, type, jfield)) {
          return false;
        }
      }
    }
    return true;
  }

  private boolean resolveMethod(TreeLogger logger, JClassType enclosingType,
      AbstractMethodDeclaration jmethod) {

    if (jmethod instanceof Clinit) {
      // Pretend we didn't see this.
      //
      return true;
    }

    String name = getMethodName(enclosingType, jmethod);

    // Try to resolve annotations, ignore any that fail.
    Map<Class<? extends java.lang.annotation.Annotation>, java.lang.annotation.Annotation> declaredAnnotations = newAnnotationMap();
    resolveAnnotations(logger, jmethod.annotations, declaredAnnotations);

    JAbstractMethod method;

    // Declare the type parameters. We will pass them into the constructors for
    // JConstructor/JMethod/JAnnotatedMethod. Then, we'll do a second pass to
    // resolve the bounds on each JTypeParameter object.
    JTypeParameter[] jtypeParameters = resolveTypeParameters(jmethod.typeParameters());

    if (jmethod.isConstructor()) {
      method = new JConstructor(enclosingType, name, declaredAnnotations,
          jtypeParameters);
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
        method = new JAnnotationMethod(enclosingType, name, defaultValue,
            declaredAnnotations);
      } else {
        method = new JMethod(enclosingType, name, declaredAnnotations,
            jtypeParameters);
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

    return true;
  }

  private boolean resolveMethods(TreeLogger logger, JClassType type,
      AbstractMethodDeclaration[] jmethods) {
    if (jmethods != null) {
      for (int i = 0; i < jmethods.length; i++) {
        AbstractMethodDeclaration jmethod = jmethods[i];
        if (!resolveMethod(logger, type, jmethod)) {
          return false;
        }
      }
    }
    return true;
  }

  private boolean resolvePackage(TreeLogger logger, TypeDeclaration jclass) {
    SourceTypeBinding binding = jclass.binding;

    String packageName = String.valueOf(binding.fPackage.readableName());
    JPackage pkg = typeOracle.getOrCreatePackage(packageName);
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
    // Check for primitives.
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
      JType resolvedType = typeOracle.findType(typeName);
      if (resolvedType == null) {
        // Otherwise, it should be something we've mapped during this build.
        resolvedType = sourceMapper.get(referenceBinding);
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

    if (binding instanceof BinaryTypeBinding) {
      // Try a binary lookup.
      String binaryName = String.valueOf(binding.constantPoolName());
      JRealClassType realClassType = binaryMapper.get(binaryName);
      if (realClassType != null) {
        return realClassType;
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
          resolvedType = typeOracle.getArrayType(resolvedType);
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
          return typeOracle.getParameterizedType(resolveType.isGenericType(),
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
      JTypeParameter typeParameter = tvMapper.get(tvBinding);
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
        return typeOracle.getWildcardType(boundType, typeBound);
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

  private boolean resolveTypeDeclaration(TreeLogger logger,
      TypeDeclaration clazz) {
    SourceTypeBinding binding = clazz.binding;
    assert (binding.constantPoolName() != null);

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
      assert superclassRef != null
          || "java.lang.Object".equals(jtype.getQualifiedSourceName());
      if (superclassRef != null) {
        JClassType jsuperClass = (JClassType) resolveType(logger, superclassRef);
        assert jsuperClass != null;
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
    if (!resolveFields(logger, jtype, fields)) {
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
    if (!resolveMethods(logger, jtype, methods)) {
      return false;
    }

    return true;
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
  private JTypeParameter[] resolveTypeParameters(TypeParameter[] typeParameters) {
    if (typeParameters == null || typeParameters.length == 0) {
      return null;
    }

    JTypeParameter[] jtypeParamArray = new JTypeParameter[typeParameters.length];
    for (int i = 0; i < typeParameters.length; ++i) {
      jtypeParamArray[i] = tvMapper.get(typeParameters[i].binding);
      assert jtypeParamArray[i] != null;
    }

    return jtypeParamArray;
  }
}
