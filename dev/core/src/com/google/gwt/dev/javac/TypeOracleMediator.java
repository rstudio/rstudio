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
import com.google.gwt.core.ext.typeinfo.JRawType;
import com.google.gwt.core.ext.typeinfo.JRealClassType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.JTypeParameter;
import com.google.gwt.core.ext.typeinfo.JWildcardType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.asm.ClassReader;
import com.google.gwt.dev.asm.ClassVisitor;
import com.google.gwt.dev.asm.Opcodes;
import com.google.gwt.dev.asm.Type;
import com.google.gwt.dev.asm.signature.SignatureReader;
import com.google.gwt.dev.asm.util.TraceClassVisitor;
import com.google.gwt.dev.javac.asm.CollectAnnotationData;
import com.google.gwt.dev.javac.asm.CollectClassData;
import com.google.gwt.dev.javac.asm.CollectFieldData;
import com.google.gwt.dev.javac.asm.CollectMethodData;
import com.google.gwt.dev.javac.asm.CollectTypeParams;
import com.google.gwt.dev.javac.asm.ResolveClassSignature;
import com.google.gwt.dev.javac.asm.ResolveMethodSignature;
import com.google.gwt.dev.javac.asm.ResolveTypeSignature;
import com.google.gwt.dev.javac.asm.CollectAnnotationData.AnnotationData;
import com.google.gwt.dev.javac.asm.CollectClassData.AnnotationEnum;
import com.google.gwt.dev.resource.Resource;
import com.google.gwt.dev.util.Name;
import com.google.gwt.dev.util.PerfLogger;
import com.google.gwt.dev.util.Name.InternalName;

import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds or rebuilds a {@link com.google.gwt.core.ext.typeinfo.TypeOracle} from
 * a set of compilation units.
 */
public class TypeOracleMediator {

  /**
   * Pairs of bits to convert from ASM Opcodes.* to Shared.* bitfields.
   */
  private static final int[] ASM_TO_SHARED_MODIFIERS = new int[] {
      Opcodes.ACC_PUBLIC, Shared.MOD_PUBLIC, Opcodes.ACC_PRIVATE,
      Shared.MOD_PRIVATE, Opcodes.ACC_PROTECTED, Shared.MOD_PROTECTED,
      Opcodes.ACC_STATIC, Shared.MOD_STATIC, Opcodes.ACC_FINAL,
      Shared.MOD_FINAL, Opcodes.ACC_ABSTRACT, Shared.MOD_ABSTRACT,
      Opcodes.ACC_VOLATILE, Shared.MOD_VOLATILE, Opcodes.ACC_TRANSIENT,
      Shared.MOD_TRANSIENT,};

  private static final JTypeParameter[] NO_TYPE_PARAMETERS = new JTypeParameter[0];

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

  private static JTypeParameter[] collectTypeParams(String signature) {
    if (signature != null) {
      List<JTypeParameter> params = new ArrayList<JTypeParameter>();
      SignatureReader reader = new SignatureReader(signature);
      reader.accept(new CollectTypeParams(params));
      return params.toArray(new JTypeParameter[params.size()]);
    }
    return NO_TYPE_PARAMETERS;
  }

  private static JTypeParameter[] getTypeParametersForClass(
      CollectClassData classData) {
    JTypeParameter[] typeParams = null;
    if (classData.getSignature() != null) {
      // TODO(jat): do we need to consider generic types w/ method type
      // params for local classes?
      typeParams = collectTypeParams(classData.getSignature());
    }
    return typeParams;
  }

  private static Class<?> getWrapperClass(Class<?> primitiveClass) {
    assert primitiveClass.isPrimitive();
    if (primitiveClass.equals(Integer.TYPE)) {
      return Integer.class;
    } else if (primitiveClass.equals(Boolean.TYPE)) {
      return Boolean.class;
    } else if (primitiveClass.equals(Byte.TYPE)) {
      return Byte.class;
    } else if (primitiveClass.equals(Character.TYPE)) {
      return Character.class;
    } else if (primitiveClass.equals(Short.TYPE)) {
      return Short.class;
    } else if (primitiveClass.equals(Long.TYPE)) {
      return Long.class;
    } else if (primitiveClass.equals(Float.TYPE)) {
      return Float.class;
    } else if (primitiveClass.equals(Double.TYPE)) {
      return Double.class;
    } else {
      throw new IllegalArgumentException(primitiveClass.toString()
          + " not a primitive class");
    }
  }

  /**
   * @return <code>true</code> if this name is the special package-info type
   *         name.
   */
  private static boolean isPackageInfoTypeName(String qname) {
    return "package-info".equals(qname);
  }

  /**
   * Returns true if this class is a non-static class inside a generic class.
   * 
   * TODO(jat): do we need to consider the entire hierarchy?
   * 
   * @param classData
   * @param enclosingClassData
   * @return true if this class is a non-static class inside a generic class
   */
  private static boolean nonStaticInsideGeneric(CollectClassData classData,
      CollectClassData enclosingClassData) {
    if (enclosingClassData == null
        || (classData.getAccess() & Opcodes.ACC_STATIC) != 0) {
      return false;
    }
    return getTypeParametersForClass(enclosingClassData) != null;
  }

  /**
   * Substitute the raw type if the supplied type is generic.
   * 
   * @param type
   * @return original type or its raw type if it is generic
   */
  private static JType possiblySubstituteRawType(JType type) {
    if (type != null) {
      JGenericType genericType = type.isGenericType();
      if (genericType != null) {
        type = genericType.getRawType();
      }
    }
    return type;
  }

  // map of internal names to classes
  final Map<String, JRealClassType> binaryMapper = new HashMap<String, JRealClassType>();

  final TypeOracle typeOracle;

  // map of internal names to class visitors
  // transient since it is not retained across calls to addNewUnits
  private transient Map<String, CollectClassData> classMap;

  // transient since it is not retained across calls to addNewUnits
  private transient HashMap<JRealClassType, CollectClassData> classMapType;

  private final Set<JRealClassType> resolved = new HashSet<JRealClassType>();

  private Resolver resolver;

  public TypeOracleMediator() {
    this(null);
  }

  // @VisibleForTesting
  public TypeOracleMediator(TypeOracle typeOracle) {
    if (typeOracle == null) {
      typeOracle = new TypeOracle();
    }
    this.typeOracle = typeOracle;
    resolver = new Resolver() {
      public Map<String, JRealClassType> getBinaryMapper() {
        return TypeOracleMediator.this.binaryMapper;
      }

      public TypeOracle getTypeOracle() {
        return TypeOracleMediator.this.typeOracle;
      }

      public boolean resolveAnnotation(TreeLogger logger,
          CollectAnnotationData annotVisitor,
          Map<Class<? extends Annotation>, Annotation> declaredAnnotations) {
        return TypeOracleMediator.this.resolveAnnotation(logger, annotVisitor,
            declaredAnnotations);
      }

      public boolean resolveAnnotations(TreeLogger logger,
          List<CollectAnnotationData> annotations,
          Map<Class<? extends Annotation>, Annotation> declaredAnnotations) {
        return TypeOracleMediator.this.resolveAnnotations(logger, annotations,
            declaredAnnotations);
      }

      public boolean resolveClass(TreeLogger logger, JRealClassType type) {
        return TypeOracleMediator.this.resolveClass(logger, type);
      }
    };
  }

  /**
   * Adds new units to an existing TypeOracle.
   */
  public void addNewUnits(TreeLogger logger, Collection<CompilationUnit> units) {
    PerfLogger.start("TypeOracleMediator.addNewUnits");
    // First collect all class data.
    classMap = new HashMap<String, CollectClassData>();
    for (CompilationUnit unit : units) {
      if (!unit.isCompiled()) {
        continue;
      }
      Collection<CompiledClass> compiledClasses = unit.getCompiledClasses();
      for (CompiledClass compiledClass : compiledClasses) {
        CollectClassData cv = processClass(compiledClass);
        // skip any classes that can't be referenced by name outside of
        // their local scope, such as anonymous classes and method-local classes
        if (!cv.hasNoExternalName()) {
          classMap.put(compiledClass.getInternalName(), cv);
        }
      }
    }

    // Perform a shallow pass to establish identity for new and old types.
    classMapType = new HashMap<JRealClassType, CollectClassData>();
    Set<JRealClassType> unresolvedTypes = new HashSet<JRealClassType>();
    for (CompilationUnit unit : units) {
      if (!unit.isCompiled()) {
        continue;
      }
      Collection<CompiledClass> compiledClasses = unit.getCompiledClasses();
      for (CompiledClass compiledClass : compiledClasses) {
        String internalName = compiledClass.getInternalName();
        CollectClassData cv = classMap.get(internalName);
        if (cv == null) {
          // ignore classes that were skipped earlier
          continue;
        }
        JRealClassType type = createType(compiledClass, unresolvedTypes);
        if (type != null) {
          if (unit instanceof SourceFileCompilationUnit) {
            SourceFileCompilationUnit sourceUnit = (SourceFileCompilationUnit) unit;
            Resource sourceFile = sourceUnit.getSourceFile();
            typeOracle.addSourceReference(type, sourceFile);
          }
          binaryMapper.put(internalName, type);
          classMapType.put(type, cv);
        }
      }
    }

    // Hook up enclosing types
    TreeLogger branch = logger.branch(TreeLogger.SPAM,
        "Resolving enclosing classes");
    for (Iterator<JRealClassType> it = unresolvedTypes.iterator(); it.hasNext();) {
      JRealClassType type = it.next();
      if (!resolveEnclosingClass(branch, type)) {
        // already logged why it failed, don't try and use it further
        it.remove();
      }
    }

    // Resolve unresolved types.
    for (JRealClassType type : unresolvedTypes) {
      branch = logger.branch(TreeLogger.SPAM, "Resolving "
          + type.getQualifiedSourceName());
      if (!resolveClass(branch, type)) {
        // already logged why it failed
        // TODO: should we do anything else here?
      }
    }

    typeOracle.finish();

    // no longer needed
    classMap = null;
    classMapType = null;
    PerfLogger.end();
  }

  public Map<String, JRealClassType> getBinaryMapper() {
    return binaryMapper;
  }

  public TypeOracle getTypeOracle() {
    return typeOracle;
  }

  private Annotation createAnnotation(TreeLogger logger,
      Class<? extends Annotation> annotationClass, AnnotationData annotData) {
    Map<String, Object> values = annotData.getValues();
    for (Map.Entry<String, Object> entry : values.entrySet()) {
      Method method = null;
      Throwable caught = null;
      try {
        method = annotationClass.getMethod(entry.getKey());
      } catch (SecurityException e) {
        caught = e;
      } catch (NoSuchMethodException e) {
        caught = e;
      }
      if (caught != null) {
        logger.log(TreeLogger.WARN, "Exception resolving "
            + annotationClass.getCanonicalName() + "." + entry.getKey(), caught);
        return null;
      }
      entry.setValue(resolveAnnotationValue(logger, method.getReturnType(),
          entry.getValue()));
    }
    return AnnotationProxyFactory.create(annotationClass, values);
  }

  private JRealClassType createType(CompiledClass compiledClass,
      CollectClassData classData, CollectClassData enclosingClassData) {
    int access = classData.getAccess();
    String qname = compiledClass.getSourceName();
    String className = Shared.getShortName(qname);
    JRealClassType resultType = null;
    String jpkgName = compiledClass.getPackageName();
    JPackage pkg = typeOracle.getOrCreatePackage(jpkgName);
    boolean isIntf = (access & Opcodes.ACC_INTERFACE) != 0;
    boolean isLocalType = classData.isLocal();
    String enclosingTypeName = null;
    if (enclosingClassData != null) {
      enclosingTypeName = InternalName.toSourceName(InternalName.getClassName(enclosingClassData.getName()));
    }
    if ((access & Opcodes.ACC_ANNOTATION) != 0) {
      resultType = new JAnnotationType(typeOracle, pkg, enclosingTypeName,
          false, className, true);
    } else if ((access & Opcodes.ACC_ENUM) != 0) {
      resultType = new JEnumType(typeOracle, pkg, enclosingTypeName,
          isLocalType, className, isIntf);
    } else {
      JTypeParameter[] typeParams = getTypeParametersForClass(classData);
      if ((typeParams != null && typeParams.length > 0)
          || nonStaticInsideGeneric(classData, enclosingClassData)) {
        resultType = new JGenericType(typeOracle, pkg, enclosingTypeName,
            isLocalType, className, isIntf, typeParams);
      } else {
        resultType = new JRealClassType(typeOracle, pkg, enclosingTypeName,
            isLocalType, className, isIntf);
      }
    }

    /*
     * Declare type parameters for all methods; we must do this during the first
     * pass.
     */
    // if (typeDecl.methods != null) {
    // for (AbstractMethodDeclaration method : typeDecl.methods) {
    // declareTypeParameters(method.typeParameters());
    // }
    // }
    /*
     * Add modifiers since these are needed for
     * TypeOracle.getParameterizedType's error checking code.
     */
    resultType.addModifierBits(mapBits(ASM_TO_SHARED_MODIFIERS, access));
    if (isIntf) {
      // Always add implicit modifiers on interfaces.
      resultType.addModifierBits(Shared.MOD_STATIC | Shared.MOD_ABSTRACT);
    }

    return resultType;
  }

  private JRealClassType createType(CompiledClass compiledClass,
      Set<JRealClassType> unresolvedTypes) {
    CollectClassData classData = classMap.get(compiledClass.getInternalName());
    String outerClassName = classData.getOuterClass();
    CollectClassData enclosingClassData = null;
    if (outerClassName != null) {
      enclosingClassData = classMap.get(outerClassName);
      if (enclosingClassData == null) {
        // if our enclosing class was skipped, skip this one too
        return null;
      }
    }
    JRealClassType realClassType = createType(compiledClass, classData,
        enclosingClassData);
    unresolvedTypes.add(realClassType);
    return realClassType;
  }

  private Class<? extends Annotation> getAnnotationClass(TreeLogger logger,
      AnnotationData annotData) {
    Type type = Type.getType(annotData.getDesc());
    try {
      Class<?> clazz = Class.forName(type.getClassName(), false,
          Thread.currentThread().getContextClassLoader());
      if (!Annotation.class.isAssignableFrom(clazz)) {
        logger.log(TreeLogger.ERROR, "Type " + type.getClassName()
            + " is not an annotation");
        return null;
      }
      return clazz.asSubclass(Annotation.class);
    } catch (ClassNotFoundException e) {
      logger.log(TreeLogger.WARN, "Ignoring unresolvable annotation type "
          + type.getClassName(), e);
      return null;
    }
  }

  @SuppressWarnings("unused")
  private Class<?> getClassLiteralForPrimitive(Type type) {
    switch (type.getSort()) {
      case Type.BOOLEAN:
        return Boolean.TYPE;
      case Type.BYTE:
        return Byte.TYPE;
      case Type.CHAR:
        return Character.TYPE;
      case Type.SHORT:
        return Short.TYPE;
      case Type.INT:
        return Integer.TYPE;
      case Type.LONG:
        return Long.TYPE;
      case Type.FLOAT:
        return Float.TYPE;
      case Type.DOUBLE:
        return Double.TYPE;
      case Type.VOID:
        return Void.TYPE;
      default:
        assert false : "Unexpected primitive type " + type;
        return null;
    }
  }

  /**
   * Map a bitset onto a different bitset.
   * 
   * @param mapping int array containing a sequence of from/to pairs, each from
   *          entry should have exactly one bit set
   * @param input bitset to map
   * @return mapped bitset
   */
  private int mapBits(int[] mapping, int input) {
    int output = 0;
    for (int i = 0; i < mapping.length; i += 2) {
      if ((input & mapping[i]) != 0) {
        output |= mapping[i + 1];
      }
    }
    return output;
  }


  /**
   * Collects data about a class which only needs the bytecode and no TypeOracle
   * data structures. This is used to make the initial shallow identity pass for
   * creating JRealClassType/JGenericType objects.
   */
  private CollectClassData processClass(CompiledClass compiledClass) {
    byte[] classBytes = compiledClass.getBytes();
    ClassReader reader = new ClassReader(classBytes);
    CollectClassData mcv = new CollectClassData(classBytes);
    ClassVisitor cv = mcv;
    if (false) {
      cv = new TraceClassVisitor(cv, new PrintWriter(System.out));
    }
    reader.accept(cv, 0);
    return mcv;
  }

  private boolean resolveAnnotation(TreeLogger logger,
      CollectAnnotationData annotVisitor,
      Map<Class<? extends Annotation>, Annotation> declaredAnnotations) {
    AnnotationData annotData = annotVisitor.getAnnotation();
    Class<? extends Annotation> annotationClass = getAnnotationClass(logger,
        annotData);
    if (annotationClass == null) {
      return false;
    }
    Annotation annotInstance = createAnnotation(logger, annotationClass,
        annotData);
    if (annotInstance == null) {
      return false;
    }
    declaredAnnotations.put(annotationClass, annotInstance);
    return true;
  }

  private boolean resolveAnnotations(TreeLogger logger,
      List<CollectAnnotationData> annotations,
      Map<Class<? extends Annotation>, Annotation> declaredAnnotations) {
    boolean succeeded = true;
    if (annotations != null) {
      for (CollectAnnotationData annotation : annotations) {
        succeeded &= resolveAnnotation(logger, annotation, declaredAnnotations);
      }
    }
    return succeeded;
  }

  @SuppressWarnings("unchecked")
  private Object resolveAnnotationValue(TreeLogger logger,
      Class<?> expectedType, Object value) {
    if (expectedType.isArray()) {
      Class<?> componentType = expectedType.getComponentType();
      if (!value.getClass().isArray()) {
        logger.log(TreeLogger.WARN, "Annotation error: expected array of "
            + componentType.getCanonicalName() + ", got "
            + value.getClass().getCanonicalName());
        return null;
      }
      // resolve each element in the array
      int n = Array.getLength(value);
      Object newArray = Array.newInstance(componentType, n);
      for (int i = 0; i < n; ++i) {
        Object valueElement = Array.get(value, i);
        Object resolvedValue = resolveAnnotationValue(logger, componentType,
            valueElement);
        if (resolvedValue == null
            || !componentType.isAssignableFrom(resolvedValue.getClass())) {
          logger.log(TreeLogger.ERROR, "Annotation error: expected "
              + componentType + ", got " + resolvedValue);
        } else {
          Array.set(newArray, i, resolvedValue);
        }
      }
      return newArray;
    } else if (expectedType.isEnum()) {
      if (!(value instanceof AnnotationEnum)) {
        logger.log(TreeLogger.ERROR,
            "Annotation error: expected an enum value," + " but got " + value);
        return null;
      }
      AnnotationEnum annotEnum = (AnnotationEnum) value;
      Class<? extends Enum> enumType = expectedType.asSubclass(Enum.class);
      try {
        return Enum.valueOf(enumType, annotEnum.getValue());
      } catch (IllegalArgumentException e) {
        logger.log(TreeLogger.WARN, "Unable to resolve annotation value '"
            + annotEnum.getValue() + "' within enum type '"
            + enumType.getName() + "'");
        return null;
      }
    } else if (Annotation.class.isAssignableFrom(expectedType)) {
      if (!(value instanceof AnnotationData)) {
        logger.log(TreeLogger.WARN,
            "Annotation error: expected annotation type "
                + expectedType.getCanonicalName() + ", got "
                + value.getClass().getCanonicalName());
        return null;
      }
      AnnotationData annotData = (AnnotationData) value;
      Class<? extends Annotation> annotationClass = getAnnotationClass(logger,
          annotData);
      if (!expectedType.isAssignableFrom(annotationClass)) {
        logger.log(TreeLogger.WARN, "Annotation error: expected "
            + expectedType.getCanonicalName() + ", got "
            + annotationClass.getCanonicalName());
        return null;
      }
      return createAnnotation(logger, annotationClass, annotData);
    } else if (expectedType.isPrimitive()) {
      Class<?> wrapper = getWrapperClass(expectedType);
      return wrapper.cast(value);
    } else {
      if (expectedType.isAssignableFrom(value.getClass())) {
        return value;
      }
      if (Class.class.equals(expectedType)) {
        if (!(value instanceof Type)) {
          logger.log(TreeLogger.WARN, "Annotation error: expected a class "
              + "literal, but received " + value);
          return null;
        }
        Type valueType = (Type) value;
        // See if we can use a binary only class here
        try {
          return Class.forName(valueType.getClassName(), false,
              Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
          logger.log(TreeLogger.ERROR, "Annotation error: cannot resolve "
              + valueType.getClassName(), e);
          return null;
        }
      }
      // TODO(jat) asserts about other acceptable types
      return value;
    }
  }

  private JType resolveArray(Type type) {
    assert type.getSort() == Type.ARRAY;
    JType resolvedType = resolveType(type.getElementType());
    int dims = type.getDimensions();
    for (int i = 0; i < dims; ++i) {
      resolvedType = typeOracle.getArrayType(resolvedType);
    }
    return resolvedType;
  }

  private boolean resolveClass(TreeLogger logger, JRealClassType type) {
    assert type != null;
    // Avoid cycles and useless computation.
    if (resolved.contains(type)) {
      return true;
    }
    resolved.add(type);

    // Make sure our enclosing type is resolved first.
    if (type.getEnclosingType() != null
        && !resolveClass(logger, type.getEnclosingType())) {
      return false;
    }

    // Build a search list for type parameters to find their definition,
    // resolving enclosing classes as we go up.
    TypeParameterLookup typeParamLookup = new TypeParameterLookup();
    typeParamLookup.pushEnclosingScopes(type);

    CollectClassData classData = classMapType.get(type);
    assert classData != null;
    int access = classData.getAccess();

    assert (!classData.getClassType().isLocal());

    logger = logger.branch(TreeLogger.SPAM, "Found type '"
        + type.getQualifiedSourceName() + "'", null);

    // Handle package-info classes.
    if (isPackageInfoTypeName(type.getSimpleSourceName())) {
      return resolvePackage(logger, type, classData.getAnnotations());
    }

    // Resolve annotations
    Map<Class<? extends Annotation>, Annotation> declaredAnnotations = new HashMap<Class<? extends Annotation>, Annotation>();
    resolveAnnotations(logger, classData.getAnnotations(), declaredAnnotations);
    type.addAnnotations(declaredAnnotations);

    String signature = classData.getSignature();
    if (signature != null) {
      // If we have a signature, use it for superclass and interfaces
      SignatureReader reader = new SignatureReader(signature);
      ResolveClassSignature classResolver = new ResolveClassSignature(
          resolver, binaryMapper, logger, type, typeParamLookup);
      reader.accept(classResolver);
      classResolver.finish();
    } else {
      // Set the super type for non-interfaces
      if ((access & Opcodes.ACC_INTERFACE) == 0) {
        String superName = classData.getSuperName();
        if (superName != null) {
          JClassType superType = binaryMapper.get(superName);
          if (superType == null || !resolveClass(logger, superType)) {
            logger.log(TreeLogger.WARN, "Unable to resolve supertype "
                + superName);
            return false;
          }
          type.setSuperclass((JClassType) possiblySubstituteRawType(superType));
        }
      }

      // Set interfaces
      for (String intfName : classData.getInterfaces()) {
        JClassType intf = binaryMapper.get(intfName);
        if (intf == null || !resolveClass(logger, intf)) {
          logger.log(TreeLogger.WARN, "Unable to resolve interface " + intfName);
          return false;
        }
        type.addImplementedInterface((JClassType) possiblySubstituteRawType(intf));
      }
    }
    if (((access & Opcodes.ACC_INTERFACE) == 0) && type.getSuperclass() == null) {
      // Only Object or interfaces should not have a superclass
      assert "java/lang/Object".equals(classData.getName());
    }

    // Process methods
    for (CollectMethodData method : classData.getMethods()) {
      if (!resolveMethod(logger, type, method, typeParamLookup)) {
        logger.log(TreeLogger.WARN, "Unable to resolve method " + method);
        return false;
      }
    }

    // Process fields
    // Track the next enum ordinal across resolveField calls.
    int[] nextEnumOrdinal = new int[] {0};
    for (CollectFieldData field : classData.getFields()) {
      if (!resolveField(logger, type, field, typeParamLookup, nextEnumOrdinal)) {
        logger.log(TreeLogger.WARN, "Unable to resolve field " + field);
        return false;
      }
    }

    return true;
  }

  private boolean resolveClass(TreeLogger logger, JType type) {
    if (!(type instanceof JClassType)) {
      // non-classes are already resolved
      return true;
    }
    if (type instanceof JRealClassType) {
      return resolveClass(logger, (JRealClassType) type);
    }
    if (type instanceof JArrayType) {
      return resolveClass(logger, ((JArrayType) type).getComponentType());
    }
    if (type instanceof JParameterizedType) {
      return resolveClass(logger, ((JParameterizedType) type).getBaseType());
    }
    if (type instanceof JRawType) {
      return resolveClass(logger, ((JRawType) type).getBaseType());
    }
    if (type instanceof JTypeParameter) {
      JTypeParameter typeParam = (JTypeParameter) type;
      if (!resolveClass(logger, typeParam.getDeclaringClass())) {
        return false;
      }
      for (JClassType bound : typeParam.getBounds()) {
        if (!resolveClass(logger, bound)) {
          return false;
        }
      }
      return true;
    }
    if (type instanceof JWildcardType) {
      JWildcardType wildcard = (JWildcardType) type;
      for (JClassType bound : wildcard.getUpperBounds()) {
        if (!resolveClass(logger, bound)) {
          return false;
        }
      }
      for (JClassType bound : wildcard.getLowerBounds()) {
        if (!resolveClass(logger, bound)) {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  private boolean resolveEnclosingClass(TreeLogger logger, JRealClassType type) {
    assert type != null;
    if (type.getEnclosingType() != null) {
      return true;
    }
    // Find our enclosing class and set it
    CollectClassData classData = classMapType.get(type);
    assert classData != null;
    String outerClass = classData.getOuterClass();
    JRealClassType enclosingType = null;
    if (outerClass != null) {
      enclosingType = binaryMapper.get(outerClass);
      // Ensure enclosing classes are resolved
      if (enclosingType != null) {
        if (!resolveEnclosingClass(logger, enclosingType)) {
          return false;
        }
        if (enclosingType.isGenericType() != null
            && (classData.getAccess() & (Opcodes.ACC_STATIC | Opcodes.ACC_INTERFACE)) != 0) {
          // If the inner class doesn't have access to it's enclosing type's
          // type variables, the enclosign type must be the raw type instead
          // of the generic type.
          JGenericType genericType = enclosingType.isGenericType();
          type.setEnclosingType(genericType.getRawType());
        } else {
          type.setEnclosingType(enclosingType);
        }
      }
    }
    return true;
  }

  private boolean resolveField(TreeLogger logger, JRealClassType type,
      CollectFieldData field, TypeParameterLookup typeParamLookup,
      int[] nextEnumOrdinal) {
    Map<Class<? extends Annotation>, Annotation> declaredAnnotations = new HashMap<Class<? extends Annotation>, Annotation>();
    resolveAnnotations(logger, field.getAnnotations(), declaredAnnotations);
    String name = field.getName();
    JField jfield;
    if ((field.getAccess() & Opcodes.ACC_ENUM) != 0) {
      assert (type.isEnum() != null);
      jfield = new JEnumConstant(type, name, declaredAnnotations,
          nextEnumOrdinal[0]++);
    } else {
      jfield = new JField(type, name, declaredAnnotations);
    }

    // Get modifiers.
    //
    jfield.addModifierBits(mapBits(ASM_TO_SHARED_MODIFIERS, field.getAccess()));

    String signature = field.getSignature();
    JType fieldType;
    if (signature != null) {
      SignatureReader reader = new SignatureReader(signature);
      JType[] fieldTypeRef = new JType[1];
      reader.acceptType(new ResolveTypeSignature(resolver, binaryMapper,
          logger, fieldTypeRef, typeParamLookup, null));
      fieldType = fieldTypeRef[0];
      // TraceSignatureVisitor trace = new TraceSignatureVisitor(
      // methodData.getAccess());
      // reader.acceptType(trace);
      // System.err.println("Field " + name + ": " + trace.getDeclaration());

    } else {
      fieldType = resolveType(Type.getType(field.getDesc()));
    }
    if (fieldType == null) {
      return false;
    }
    jfield.setType(fieldType);
    return true;
  }

  private boolean resolveMethod(TreeLogger logger, JRealClassType type,
      CollectMethodData methodData, TypeParameterLookup typeParamLookup) {
    Map<Class<? extends Annotation>, Annotation> declaredAnnotations = new HashMap<Class<? extends Annotation>, Annotation>();
    resolveAnnotations(logger, methodData.getAnnotations(), declaredAnnotations);
    String name = methodData.getName();

    if ("<clinit>".equals(name)
        || (methodData.getAccess() & Opcodes.ACC_SYNTHETIC) != 0) {
      // Ignore the following and leave them out of TypeOracle:
      // - static initializers
      // - synthetic methods
      return true;
    }

    if (type.isEnum() != null && "<init>".equals(name)) {
      // Leave enum constructors out of TypeOracle
      return true;
    }

    JAbstractMethod method;

    // Declare the type parameters. We will pass them into the constructors for
    // JConstructor/JMethod/JAnnotatedMethod. Then, we'll do a second pass to
    // resolve the bounds on each JTypeParameter object.
    JTypeParameter[] typeParams = collectTypeParams(methodData.getSignature());

    typeParamLookup.pushScope(typeParams);
    boolean hasReturnType = true;
    if ("<init>".equals(name)) {
      name = type.getSimpleSourceName();
      method = new JConstructor(type, name, declaredAnnotations, typeParams);
      hasReturnType = false;
    } else {
      if (type.isAnnotation() != null) {
        // TODO(jat): !! anything else to do here?
        method = new JAnnotationMethod(type, name, typeParams,
            declaredAnnotations);
      } else {
        method = new JMethod(type, name, declaredAnnotations, typeParams);
      }
    }

    // Copy modifier flags
    method.addModifierBits(mapBits(ASM_TO_SHARED_MODIFIERS,
        methodData.getAccess()));
    if (type.isInterface() != null) {
      // Always add implicit modifiers on interface methods.
      method.addModifierBits(Shared.MOD_PUBLIC | Shared.MOD_ABSTRACT);
    }

    if ((methodData.getAccess() & Opcodes.ACC_VARARGS) != 0) {
      method.setVarArgs();
    }

    String signature = methodData.getSignature();
    Type[] argTypes = methodData.getArgTypes();
    String[] argNames = methodData.getArgNames();
    if (signature != null) {
      // If we have a signature, use it for superclass and interfaces
      SignatureReader reader = new SignatureReader(signature);
      ResolveMethodSignature methodResolver = new ResolveMethodSignature(
          resolver, logger, method, typeParamLookup, hasReturnType, methodData,
          argTypes, argNames);
      // TraceSignatureVisitor trace = new TraceSignatureVisitor(
      // methodData.getAccess());
      // reader.accept(trace);
      // System.err.println("Method " + name + ": " + trace.getDeclaration());
      reader.accept(methodResolver);
      if (!methodResolver.finish()) {
        return false;
      }
    } else {
      if (hasReturnType) {
        Type returnType = Type.getReturnType(methodData.getDesc());
        JType returnJType = resolveType(returnType);
        if (returnJType == null) {
          return false;
        }
        ((JMethod) method).setReturnType(returnJType);
      }

      if (!resolveParameters(logger, method, methodData)) {
        return false;
      }
    }
    // The signature might not actually include the exceptions if they don't
    // include a type variable, so resolveThrows is always used (it does
    // nothing if there are already exceptions defined)
    if (!resolveThrows(method, methodData)) {
      return false;
    }
    typeParamLookup.popScope();
    return true;
  }

  private JRealClassType resolveObject(Type type) {
    assert type.getSort() == Type.OBJECT;
    String className = type.getInternalName();
    assert Name.isInternalName(className);
    JRealClassType classType = binaryMapper.get(className);
    return classType;
  }

  private boolean resolvePackage(TreeLogger logger, JRealClassType type,
      List<CollectAnnotationData> annotations) {
    Map<Class<? extends Annotation>, Annotation> declaredAnnotations = new HashMap<Class<? extends Annotation>, Annotation>();
    resolveAnnotations(logger, annotations, declaredAnnotations);
    type.getPackage().addAnnotations(declaredAnnotations);
    return true;
  }

  private boolean resolveParameters(TreeLogger logger, JAbstractMethod method,
      CollectMethodData methodData) {
    Type[] argTypes = methodData.getArgTypes();
    String[] argNames = methodData.getArgNames();
    boolean argNamesAreReal = methodData.hasActualArgNames();
    List<CollectAnnotationData>[] paramAnnot = methodData.getArgAnnotations();
    for (int i = 0; i < argTypes.length; ++i) {
      JType argType = resolveType(argTypes[i]);
      if (argType == null) {
        return false;
      }
      // Try to resolve annotations, ignore any that fail.
      Map<Class<? extends Annotation>, Annotation> declaredAnnotations = new HashMap<Class<? extends Annotation>, Annotation>();
      resolveAnnotations(logger, paramAnnot[i], declaredAnnotations);

      // JParameter adds itself to the method
      new JParameter(method, argType, argNames[i], declaredAnnotations,
          argNamesAreReal);
    }
    return true;
  }

  private boolean resolveThrows(JAbstractMethod method,
      CollectMethodData methodData) {
    if (method.getThrows().length == 0) {
      for (String excName : methodData.getExceptions()) {
        JType exc = resolveType(Type.getObjectType(excName));
        if (exc == null) {
          return false;
        }
        method.addThrows(exc);
      }
    }
    return true;
  }

  /**
   * Returns a primitive, an array, or a JRealClassType.
   */
  private JType resolveType(Type type) {
    // Check for primitives.
    switch (type.getSort()) {
      case Type.BOOLEAN:
        return JPrimitiveType.BOOLEAN;
      case Type.BYTE:
        return JPrimitiveType.BYTE;
      case Type.CHAR:
        return JPrimitiveType.CHAR;
      case Type.SHORT:
        return JPrimitiveType.SHORT;
      case Type.INT:
        return JPrimitiveType.INT;
      case Type.LONG:
        return JPrimitiveType.LONG;
      case Type.FLOAT:
        return JPrimitiveType.FLOAT;
      case Type.DOUBLE:
        return JPrimitiveType.DOUBLE;
      case Type.VOID:
        return JPrimitiveType.VOID;
      case Type.ARRAY:
        return resolveArray(type);
      case Type.OBJECT:
        JRealClassType resolvedType = resolveObject(type);
        return possiblySubstituteRawType(resolvedType);
      default:
        assert false : "Unexpected type " + type;
        return null;
    }
  }
}
