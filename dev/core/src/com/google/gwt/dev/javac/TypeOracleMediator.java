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
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.dev.asm.ClassReader;
import com.google.gwt.dev.asm.ClassVisitor;
import com.google.gwt.dev.asm.Opcodes;
import com.google.gwt.dev.asm.Type;
import com.google.gwt.dev.asm.signature.SignatureReader;
import com.google.gwt.dev.asm.util.TraceClassVisitor;
import com.google.gwt.dev.javac.asm.CollectAnnotationData;
import com.google.gwt.dev.javac.asm.CollectAnnotationData.AnnotationData;
import com.google.gwt.dev.javac.asm.CollectClassData;
import com.google.gwt.dev.javac.asm.CollectClassData.AnnotationEnum;
import com.google.gwt.dev.javac.asm.CollectFieldData;
import com.google.gwt.dev.javac.asm.CollectMethodData;
import com.google.gwt.dev.javac.asm.CollectTypeParams;
import com.google.gwt.dev.javac.asm.ResolveClassSignature;
import com.google.gwt.dev.javac.asm.ResolveMethodSignature;
import com.google.gwt.dev.javac.asm.ResolveTypeSignature;
import com.google.gwt.dev.javac.typemodel.JAbstractMethod;
import com.google.gwt.dev.javac.typemodel.JArrayType;
import com.google.gwt.dev.javac.typemodel.JClassType;
import com.google.gwt.dev.javac.typemodel.JField;
import com.google.gwt.dev.javac.typemodel.JGenericType;
import com.google.gwt.dev.javac.typemodel.JMethod;
import com.google.gwt.dev.javac.typemodel.JPackage;
import com.google.gwt.dev.javac.typemodel.JParameterizedType;
import com.google.gwt.dev.javac.typemodel.JRawType;
import com.google.gwt.dev.javac.typemodel.JRealClassType;
import com.google.gwt.dev.javac.typemodel.JTypeParameter;
import com.google.gwt.dev.javac.typemodel.JWildcardType;
import com.google.gwt.dev.javac.typemodel.TypeOracle;
import com.google.gwt.dev.javac.typemodel.TypeOracleBuilder;
import com.google.gwt.dev.util.Name;
import com.google.gwt.dev.util.Name.InternalName;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;

import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds or rebuilds a {@link com.google.gwt.core.ext.typeinfo.TypeOracle} from
 * a set of compilation units.
 */
public class TypeOracleMediator extends TypeOracleBuilder {

  /**
   * A container to hold all the information we need to add one type to the
   * TypeOracle.
   */
  public static class TypeData {

    /**
     * Bytecode from compiled Java source.
     */
    private final byte[] byteCode;
    
    /**
     * Prepared information about this class.
     */
    private CollectClassData classData;

    /**
     * See {@link Type#getInternalName()}.
     */
    private final String internalName;

    /**
     * A timestamp as returned from {@link System#currentTimeMillis()}
     *
     * TODO(zundel): currently unused, add to JType later.
     */
    private final long lastModifiedTime;

    /**
     * Package name.
     */
    private final String packageName;

    /**
     * URL to fetch the source file from a class loader. If a method arg name is
     * requested, we may need to go back to this file and compile it.
     *
     * TODO(zundel): currently unused, add to JRealClassType later.
     */
    private final String sourceFileResourceName;

    /**
     * See {@link JType#getQualifiedSourceName()}.
     */
    private final String sourceName;

    protected TypeData(String packageName, String sourceName,
        String internalName, String sourceFileResourceName, byte[] classBytes,
        long lastModifiedTime) {
      this.packageName = packageName;
      this.sourceName = sourceName;
      this.internalName = internalName;
      this.sourceFileResourceName = sourceFileResourceName;
      this.byteCode = classBytes;
      this.lastModifiedTime = lastModifiedTime;
    }
    
    /**
     * Collects data about a class which only needs the bytecode and no TypeOracle
     * data structures. This is used to make the initial shallow identity pass for
     * creating JRealClassType/JGenericType objects.
     */
    synchronized CollectClassData getCollectClassData() {
      if (classData == null) {
        ClassReader reader = new ClassReader(byteCode);
        classData = new CollectClassData();
        ClassVisitor cv = classData;
        if (TRACE_CLASSES) {
          cv = new TraceClassVisitor(cv, new PrintWriter(System.out));
        }
        reader.accept(cv, 0);
      }
      return classData;
    }
  }

  /**
   * This context keeps common data so we don't have to pass it around between
   * methods for one pass of
   * {@link TypeOracleMediator#addNewTypes(TreeLogger, Collection, MethodArgNamesLookup)}
   * .
   */
  private class TypeOracleBuildContext {
    private final MethodArgNamesLookup allMethodArgs;

    // map of internal names to class visitors.
    private final Map<String, CollectClassData> classMap = new HashMap<String, CollectClassData>();
    // map of JRealType instances to lookup class visitors.
    private final HashMap<JRealClassType, CollectClassData> classMapType = new HashMap<JRealClassType, CollectClassData>();

    private final Resolver resolver = new TypeOracleMediatorResolver(this);

    private TypeOracleBuildContext(MethodArgNamesLookup allMethodArgs) {
      this.allMethodArgs = allMethodArgs;
    }
  };

  private class TypeOracleMediatorResolver implements Resolver {
    private final TypeOracleBuildContext context;
    public TypeOracleMediatorResolver(TypeOracleBuildContext context) {
      this.context = context;
    }
    public void addImplementedInterface(JRealClassType type, JClassType intf) {
      TypeOracleMediator.this.addImplementedInterface(type, intf);
    }

    public void addThrows(JAbstractMethod method, JClassType exception) {
      TypeOracleMediator.this.addThrows(method, exception);
    }

    public Map<String, JRealClassType> getBinaryMapper() {
      return TypeOracleMediator.this.binaryMapper;
    }

    public TypeOracle getTypeOracle() {
      return TypeOracleMediator.this.typeOracle;
    }

    public JMethod newMethod(JClassType type, String name,
        Map<Class<? extends Annotation>, Annotation> declaredAnnotations,
        JTypeParameter[] typeParams) {
      return TypeOracleMediator.this.newMethod(type, name,
          declaredAnnotations, typeParams);
    }

    public void newParameter(JAbstractMethod method, JType argType,
        String argName,
        Map<Class<? extends Annotation>, Annotation> declaredAnnotations,
        boolean argNamesAreReal) {
      TypeOracleMediator.this.newParameter(method, argType, argName,
          declaredAnnotations, argNamesAreReal);
    }

    public JRealClassType newRealClassType(JPackage pkg,
        String enclosingTypeName, boolean isLocalType, String className,
        boolean isIntf) {
      return TypeOracleMediator.this.newRealClassType(pkg, enclosingTypeName,
          className, isIntf);
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
      return TypeOracleMediator.this.resolveClass(logger, type, context);
    }

    public void setReturnType(JAbstractMethod method, JType returnType) {
      TypeOracleMediator.this.setReturnType(method, returnType);
    }

    public void setSuperClass(JRealClassType type, JClassType superType) {
      TypeOracleMediator.this.setSuperClass(type, superType);
    }
}

  /**
   * Pairs of bits to convert from ASM Opcodes.* to Shared.* bitfields.
   */
  private static final int[] ASM_TO_SHARED_MODIFIERS = new int[]{
      Opcodes.ACC_PUBLIC, Shared.MOD_PUBLIC, //
      Opcodes.ACC_PRIVATE, Shared.MOD_PRIVATE, //
      Opcodes.ACC_PROTECTED, Shared.MOD_PROTECTED, //
      Opcodes.ACC_STATIC, Shared.MOD_STATIC, //
      Opcodes.ACC_FINAL, Shared.MOD_FINAL, //
      Opcodes.ACC_ABSTRACT, Shared.MOD_ABSTRACT, //
      Opcodes.ACC_VOLATILE, Shared.MOD_VOLATILE, //
      Opcodes.ACC_TRANSIENT, Shared.MOD_TRANSIENT, //
  };

  private static final JTypeParameter[] NO_TYPE_PARAMETERS = new JTypeParameter[0];

  /**
   * Turn on to trace class processing.
   */
  private static final boolean TRACE_CLASSES = false;

  /**
   * Suppress some warnings related to missing valiation.jar on classpath.
   */
  private static boolean warnedMissingValidationJar = false;

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
      JGenericType genericType = (JGenericType) type.isGenericType();
      if (genericType != null) {
        type = genericType.getRawType();
      }
    }
    return type;
  }

  // map of internal names to classes
  final Map<String, JRealClassType> binaryMapper = new HashMap<String, JRealClassType>();

  private final Set<JRealClassType> resolved = new HashSet<JRealClassType>();

  /**
   * Adds new units to an existing TypeOracle.
   *
   * @param logger logger to use
   * @param typeDataList collection of data need to build types
   */
  public void addNewTypes(TreeLogger logger, Collection<TypeData> typeDataList) {
    addNewTypes(logger, typeDataList, new MethodArgNamesLookup());
  }

  /**
   * Adds new units to an existing TypeOracle.
   *
   * @param logger logger to use
   * @param typeDataList collection of data need to build types
   * @param argsLookup Allows the caller to pass the method argument names which
   *          are not normally available in bytecode.
   */
  public void addNewTypes(TreeLogger logger, Collection<TypeData> typeDataList,
      MethodArgNamesLookup argsLookup) {
    Event typeOracleMediatorEvent = SpeedTracerLogger.start(CompilerEventType.TYPE_ORACLE_MEDIATOR);

    // First collect all class data.
    Event visitClassFileEvent = SpeedTracerLogger.start(
        CompilerEventType.TYPE_ORACLE_MEDIATOR, "phase", "Visit Class Files");
    TypeOracleBuildContext context = new TypeOracleBuildContext(argsLookup);

    for (TypeData typeData : typeDataList) {
      CollectClassData cv = typeData.getCollectClassData();
      // skip any classes that can't be referenced by name outside of
      // their local scope, such as anonymous classes and method-local classes
      if (!cv.hasNoExternalName()) {
        context.classMap.put(typeData.internalName, cv);
      }
    }
    visitClassFileEvent.end();

    Event identityEvent = SpeedTracerLogger.start(
        CompilerEventType.TYPE_ORACLE_MEDIATOR, "phase", "Establish Identity");
    // Perform a shallow pass to establish identity for new and old types.
    Set<JRealClassType> unresolvedTypes = new LinkedHashSet<JRealClassType>();
    for (TypeData typeData : typeDataList) {
      CollectClassData cv = context.classMap.get(typeData.internalName);
      if (cv == null) {
        // ignore classes that were skipped earlier
        continue;
      }
      JRealClassType type = createType(typeData, unresolvedTypes, context);
      if (type != null) {
        binaryMapper.put(typeData.internalName, type);
        context.classMapType.put(type, cv);
      }
    }
    identityEvent.end();

    Event resolveEnclosingEvent = SpeedTracerLogger.start(
        CompilerEventType.TYPE_ORACLE_MEDIATOR, "phase",
        "Resolve Enclosing Classes");
    // Hook up enclosing types
    TreeLogger branch = logger.branch(TreeLogger.SPAM,
        "Resolving enclosing classes");
    for (Iterator<JRealClassType> it = unresolvedTypes.iterator(); it.hasNext();) {
      JRealClassType type = it.next();
      if (!resolveEnclosingClass(branch, type, context)) {
        // already logged why it failed, don't try and use it further
        it.remove();
      }
    }
    resolveEnclosingEvent.end();

    Event resolveUnresolvedEvent = SpeedTracerLogger.start(
        CompilerEventType.TYPE_ORACLE_MEDIATOR, "phase",
        "Resolve Unresolved Types");
    // Resolve unresolved types.
    for (JRealClassType type : unresolvedTypes) {
      branch = logger.branch(TreeLogger.SPAM, "Resolving "
          + type.getQualifiedSourceName());
      if (!resolveClass(branch, type, context)) {
        // already logged why it failed.
        // TODO: should we do anything else here?
      }
    }
    resolveUnresolvedEvent.end();

    Event finishEvent = SpeedTracerLogger.start(
        CompilerEventType.TYPE_ORACLE_MEDIATOR, "phase", "Finish");
    super.finish();
    finishEvent.end();

    // no longer needed
    context = null;
    typeOracleMediatorEvent.end();
  }

  /**
   * @return a map from binary class names to JRealClassType.
   */
  public Map<String, JRealClassType> getBinaryMapper() {
    return binaryMapper;
  }

  /**
   * Intended for unit testing only.
   *
   * @return a mocked up version of this mediator's resolver.
   */
  public Resolver getMockResolver() {
    return new TypeOracleMediatorResolver(new TypeOracleBuildContext(new MethodArgNamesLookup()));
  }

  /**
   * @return the TypeOracle managed by the mediator.
   */
  public TypeOracle getTypeOracle() {
    return typeOracle;
  }

  private Annotation createAnnotation(TreeLogger logger,
      Class<? extends Annotation> annotationClass, AnnotationData annotData) {
    // Make a copy before we mutate the collection.
    Map<String, Object> values = new HashMap<String, Object>(annotData.getValues());
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

  private JRealClassType createType(TypeData typeData,
      CollectClassData collectClassData, CollectClassData enclosingClassData) {
    int access = collectClassData.getAccess();
    String qualifiedSourceName = typeData.sourceName;
    String className = Shared.getShortName(qualifiedSourceName);
    JRealClassType resultType = null;
    String jpkgName = typeData.packageName;
    JPackage pkg = typeOracle.getOrCreatePackage(jpkgName);
    boolean isIntf = (access & Opcodes.ACC_INTERFACE) != 0;
    assert !collectClassData.hasNoExternalName();
    String enclosingTypeName = null;
    if (enclosingClassData != null) {
      enclosingTypeName = InternalName.toSourceName(InternalName.getClassName(enclosingClassData.getName()));
    }
    if ((access & Opcodes.ACC_ANNOTATION) != 0) {
      resultType = newAnnotationType(pkg, enclosingTypeName, className);
    } else if ((access & Opcodes.ACC_ENUM) != 0) {
      resultType = newEnumType(pkg, enclosingTypeName, className);
    } else {
      JTypeParameter[] typeParams = getTypeParametersForClass(collectClassData);
      if ((typeParams != null && typeParams.length > 0)
          || nonStaticInsideGeneric(collectClassData, enclosingClassData)) {
        resultType = new JGenericType(typeOracle, pkg, enclosingTypeName,
            className, isIntf, typeParams);
      } else {
        resultType = newRealClassType(pkg, enclosingTypeName, className, isIntf);
      }
    }

    /*
     * Add modifiers since these are needed for
     * TypeOracle.getParameterizedType's error checking code.
     */
    resultType.addModifierBits(mapBits(ASM_TO_SHARED_MODIFIERS, access));
    if (isIntf) {
      // Always add implicit modifiers on interfaces.
      resultType.addModifierBits(Shared.MOD_STATIC | Shared.MOD_ABSTRACT);
    }

    /*
     * Add lastModified time from compilation unit
     */
    resultType.addLastModifiedTime(typeData.lastModifiedTime);

    return resultType;
  }

  private JRealClassType createType(TypeData typeData,
      Set<JRealClassType> unresolvedTypes,
      TypeOracleBuildContext context) {
    CollectClassData collectClassData = context.classMap.get(typeData.internalName);
    String outerClassName = collectClassData.getOuterClass();
    CollectClassData enclosingClassData = null;
    if (outerClassName != null) {
      enclosingClassData = context.classMap.get(outerClassName);
      if (enclosingClassData == null) {
        // if our enclosing class was skipped, skip this one too
        return null;
      }
    }
    JRealClassType realClassType = createType(typeData, collectClassData,
        enclosingClassData);
    unresolvedTypes.add(realClassType);
    return realClassType;
  }

  private Class<? extends Annotation> getAnnotationClass(TreeLogger logger,
      AnnotationData annotData) {
    Type type = Type.getType(annotData.getDesc());
    String typeName = type.getClassName();
    try {
      Class<?> clazz = Class.forName(typeName, false,
          Thread.currentThread().getContextClassLoader());
      if (!Annotation.class.isAssignableFrom(clazz)) {
        logger.log(TreeLogger.ERROR, "Type " + typeName
            + " is not an annotation");
        return null;
      }
      return clazz.asSubclass(Annotation.class);
    } catch (ClassNotFoundException e) {
      TreeLogger.Type level = TreeLogger.WARN;
      if (shouldSuppressUnresolvableAnnotation(logger, typeName)) {
        level = TreeLogger.DEBUG;
      }
      logger.log(level, "Ignoring unresolvable annotation type "
          + typeName);
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
      if (componentType.isPrimitive()) {
        // primitive arrays are already resolved
        return value;
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

  private boolean resolveClass(TreeLogger logger, JRealClassType type,
      TypeOracleBuildContext context) {
    assert type != null;
    // Avoid cycles and useless computation.
    if (resolved.contains(type)) {
      return true;
    }
    resolved.add(type);

    // Make sure our enclosing type is resolved first.
    if (type.getEnclosingType() != null
        && !resolveClass(logger, type.getEnclosingType(), context)) {
      return false;
    }

    // Build a search list for type parameters to find their definition,
    // resolving enclosing classes as we go up.
    TypeParameterLookup typeParamLookup = new TypeParameterLookup();
    typeParamLookup.pushEnclosingScopes(type);

    CollectClassData classData = context.classMapType.get(type);
    assert classData != null;
    int access = classData.getAccess();

    assert (!classData.getClassType().hasNoExternalName());

    logger = logger.branch(TreeLogger.SPAM, "Found type '"
        + type.getQualifiedSourceName() + "'", null);

    // Handle package-info classes.
    if (isPackageInfoTypeName(type.getSimpleSourceName())) {
      return resolvePackage(logger, type, classData.getAnnotations());
    }

    // Resolve annotations
    Map<Class<? extends Annotation>, Annotation> declaredAnnotations = new HashMap<Class<? extends Annotation>, Annotation>();
    resolveAnnotations(logger, classData.getAnnotations(), declaredAnnotations);
    addAnnotations(type, declaredAnnotations);

    String signature = classData.getSignature();

    /*
     * Note: Byte code from the OpenJDK compiler doesn't contain a type signature for non-static
     * inner classes of parameterized types that do not contain new parameters (but JDT compiled
     * byte code does). That can cause some differences in the way generic types are represented
     * in the type oracle.
     *
     * These differences also show up when using java.lang.reflect to look at types.
     */
    if (signature != null) {
      // If we have a signature, use it for superclass and interfaces
      SignatureReader reader = new SignatureReader(signature);
      ResolveClassSignature classResolver = new ResolveClassSignature(context.resolver,
          binaryMapper, logger, type, typeParamLookup);
      reader.accept(classResolver);
      classResolver.finish();
    } else {
      // Set the super type for non-interfaces
      if ((access & Opcodes.ACC_INTERFACE) == 0) {
        String superName = classData.getSuperName();
        if (superName != null) {
          JClassType superType = binaryMapper.get(superName);
          if (superType == null || !resolveClass(logger, superType, context)) {
            logger.log(TreeLogger.WARN, "Unable to resolve supertype "
                + superName);
            return false;
          }
          setSuperClass(type, (JClassType) possiblySubstituteRawType(superType));
        }
      }

      // Set interfaces
      for (String intfName : classData.getInterfaces()) {
        JClassType intf = binaryMapper.get(intfName);
        if (intf == null || !resolveClass(logger, intf, context)) {
          logger.log(TreeLogger.WARN, "Unable to resolve interface " + intfName);
          return false;
        }
        addImplementedInterface(type,
            (JClassType) possiblySubstituteRawType(intf));
      }
    }
    if (((access & Opcodes.ACC_INTERFACE) == 0) && type.getSuperclass() == null) {
      // Only Object or interfaces should not have a superclass
      assert "java/lang/Object".equals(classData.getName());
    }

    // Process methods
    for (CollectMethodData method : classData.getMethods()) {
      if (!resolveMethod(logger, type, method, typeParamLookup, context)) {
        logger.log(TreeLogger.WARN, "Unable to resolve method " + method);
        return false;
      }
    }

    // Process fields
    // Track the next enum ordinal across resolveField calls.
    int[] nextEnumOrdinal = new int[]{0};
    for (CollectFieldData field : classData.getFields()) {
      if (!resolveField(logger, type, field, typeParamLookup, nextEnumOrdinal, context)) {
        logger.log(TreeLogger.WARN, "Unable to resolve field " + field);
        return false;
      }
    }

    return true;
  }

  private boolean resolveClass(TreeLogger logger, JType type, TypeOracleBuildContext context) {
    if (!(type instanceof JClassType)) {
      // non-classes are already resolved
      return true;
    }
    if (type instanceof JRealClassType) {
      return resolveClass(logger, (JRealClassType) type, context);
    }
    if (type instanceof JArrayType) {
      return resolveClass(logger, ((JArrayType) type).getComponentType(), context);
    }
    if (type instanceof JParameterizedType) {
      return resolveClass(logger, ((JParameterizedType) type).getBaseType(), context);
    }
    if (type instanceof JRawType) {
      return resolveClass(logger, ((JRawType) type).getBaseType(), context);
    }
    if (type instanceof JTypeParameter) {
      JTypeParameter typeParam = (JTypeParameter) type;
      if (!resolveClass(logger, typeParam.getDeclaringClass(), context)) {
        return false;
      }
      for (JClassType bound : typeParam.getBounds()) {
        if (!resolveClass(logger, bound, context)) {
          return false;
        }
      }
      return true;
    }
    if (type instanceof JWildcardType) {
      JWildcardType wildcard = (JWildcardType) type;
      for (JClassType bound : wildcard.getUpperBounds()) {
        if (!resolveClass(logger, bound, context)) {
          return false;
        }
      }
      for (JClassType bound : wildcard.getLowerBounds()) {
        if (!resolveClass(logger, bound, context)) {
          return false;
        }
      }
      return true;
    }
    return false;
  }

  private boolean resolveEnclosingClass(TreeLogger logger, JRealClassType type,
      TypeOracleBuildContext context) {
    assert type != null;
    if (type.getEnclosingType() != null) {
      return true;
    }
    // Find our enclosing class and set it
    CollectClassData classData = context.classMapType.get(type);
    assert classData != null;
    String outerClass = classData.getOuterClass();
    JRealClassType enclosingType = null;
    if (outerClass != null) {
      enclosingType = binaryMapper.get(outerClass);
      // Ensure enclosing classes are resolved
      if (enclosingType != null) {
        if (!resolveEnclosingClass(logger, enclosingType, context)) {
          return false;
        }
        if (enclosingType.isGenericType() != null
            && (classData.getAccess() & (Opcodes.ACC_STATIC | Opcodes.ACC_INTERFACE)) != 0) {
          // If the inner class doesn't have access to it's enclosing type's
          // type variables, the enclosign type must be the raw type instead
          // of the generic type.
          JGenericType genericType = enclosingType.isGenericType();
          setEnclosingType(type, genericType.getRawType());
        } else {
          setEnclosingType(type, enclosingType);
        }
      }
    }
    return true;
  }

  private boolean resolveField(TreeLogger logger, JRealClassType type,
      CollectFieldData field, TypeParameterLookup typeParamLookup,
      int[] nextEnumOrdinal, TypeOracleBuildContext context) {
    Map<Class<? extends Annotation>, Annotation> declaredAnnotations = new HashMap<Class<? extends Annotation>, Annotation>();
    resolveAnnotations(logger, field.getAnnotations(), declaredAnnotations);
    String name = field.getName();
    JField jfield;
    if ((field.getAccess() & Opcodes.ACC_ENUM) != 0) {
      assert (type.isEnum() != null);
      jfield = newEnumConstant(type, name, declaredAnnotations,
          nextEnumOrdinal[0]++);
    } else {
      JField newField = newField(type, name, declaredAnnotations);
      jfield = newField;
    }

    // Get modifiers.
    //
    addModifierBits(jfield, mapBits(ASM_TO_SHARED_MODIFIERS, field.getAccess()));

    String signature = field.getSignature();
    JType fieldType;
    if (signature != null) {
      SignatureReader reader = new SignatureReader(signature);
      JType[] fieldTypeRef = new JType[1];
      reader.acceptType(new ResolveTypeSignature(context.resolver, binaryMapper,
          logger, fieldTypeRef, typeParamLookup, null));
      fieldType = fieldTypeRef[0];

    } else {
      fieldType = resolveType(Type.getType(field.getDesc()));
    }
    if (fieldType == null) {
      return false;
    }
    setFieldType(jfield, fieldType);
    return true;
  }

  private boolean resolveMethod(TreeLogger logger, JRealClassType type,
      CollectMethodData methodData, TypeParameterLookup typeParamLookup,
      TypeOracleBuildContext context) {
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
      method = newConstructor(type, name, declaredAnnotations, typeParams);
      hasReturnType = false;
    } else {
      if (type.isAnnotation() != null) {
        // TODO(jat): actually resolve the default annotation value.
        method = newAnnotationMethod(type, name, declaredAnnotations,
            typeParams, null);
      } else {
        method = newMethod(type, name, declaredAnnotations, typeParams);
      }
    }

    addModifierBits(method, mapBits(ASM_TO_SHARED_MODIFIERS,
        methodData.getAccess()));
    if (type.isInterface() != null) {
      // Always add implicit modifiers on interface methods.
      addModifierBits(method, Shared.MOD_PUBLIC | Shared.MOD_ABSTRACT);
    }

    if ((methodData.getAccess() & Opcodes.ACC_VARARGS) != 0) {
      setVarArgs(method);
    }

    String signature = methodData.getSignature();
    if (signature != null) {
      // If we have a signature, use it for superclass and interfaces
      SignatureReader reader = new SignatureReader(signature);
      ResolveMethodSignature methodResolver = new ResolveMethodSignature(
          context.resolver, logger, method, typeParamLookup, hasReturnType, methodData,
          methodData.getArgTypes(), methodData.getArgNames(),
          methodData.hasActualArgNames(), context.allMethodArgs);
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
        setReturnType(method, returnJType);
      }

      if (!resolveParameters(logger, method, methodData, context)) {
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
    addAnnotations(type.getPackage(), declaredAnnotations);
    return true;
  }

  private boolean resolveParameters(TreeLogger logger, JAbstractMethod method,
      CollectMethodData methodData, TypeOracleBuildContext context) {
    Type[] argTypes = methodData.getArgTypes();
    boolean argNamesAreReal = methodData.hasActualArgNames();
    String[] argNames = methodData.getArgNames();
    if (!argNamesAreReal) {
      String[] lookupNames = context.allMethodArgs.lookup(method, methodData);
      if (lookupNames != null) {
        argNames = lookupNames;
        argNamesAreReal = true;
      }
    }
    List<CollectAnnotationData>[] paramAnnot = methodData.getArgAnnotations();
    for (int i = 0; i < argTypes.length; ++i) {
      JType argType = resolveType(argTypes[i]);
      if (argType == null) {
        return false;
      }
      // Try to resolve annotations, ignore any that fail.
      Map<Class<? extends Annotation>, Annotation> declaredAnnotations = new HashMap<Class<? extends Annotation>, Annotation>();
      resolveAnnotations(logger, paramAnnot[i], declaredAnnotations);

      newParameter(method, argType, argNames[i], declaredAnnotations,
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
        addThrows(method, (JClassType) exc);
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

  /**
   * Suppress multiple validation related messages and replace with a hint.  
   *     
   * @param typeName fully qualified type name to check for filtering
   */
  // TODO(zundel): Can be removed when javax.validation is included in the JRE
  private boolean shouldSuppressUnresolvableAnnotation(TreeLogger logger, String typeName) {
    if (typeName.startsWith("javax.validation.")
        || typeName.startsWith("com.google.gwt.validation.")) {
      if (!warnedMissingValidationJar) {
        warnedMissingValidationJar = true;
        logger.log(TreeLogger.WARN, "Detected warnings related to '" + typeName + "'. "
            + "  Is validation-<version>.jar on the classpath?");
        logger.log(TreeLogger.INFO, "Specify -logLevel DEBUG to see all errors.");
        // Show the first error that matches
        return false;
      }
      // Suppress subsequent errors that match
      return true;
    }
    return false;
  }
}
