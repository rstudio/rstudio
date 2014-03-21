/*
 * Copyright 2008 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
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
import com.google.gwt.dev.javac.typemodel.TypeOracleUpdater;
import com.google.gwt.dev.util.Name;
import com.google.gwt.dev.util.log.speedtracer.CompilerEventType;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger;
import com.google.gwt.dev.util.log.speedtracer.SpeedTracerLogger.Event;
import com.google.gwt.thirdparty.guava.common.collect.Sets;

import java.io.PrintWriter;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Builds or rebuilds a {@link com.google.gwt.core.ext.typeinfo.TypeOracle} from a set of
 * compilation units.
 */
public class CompilationUnitTypeOracleUpdater extends TypeOracleUpdater {

  /**
   * A container to hold all the information we need to add one type to the TypeOracle.
   */
  static class TypeData {

    private final byte[] byteCode;
    private CollectClassData classData;
    private final String internalName;

    /**
     * A timestamp as returned from {@link System#currentTimeMillis()}
     */
    private final long lastModifiedTime;
    private final String packageName;
    private final String sourceName;

    protected TypeData(String packageName, String sourceName, String internalName, byte[] byteCode,
        long lastModifiedTime) {
      this.packageName = packageName;
      this.sourceName = sourceName;
      this.internalName = internalName;
      this.byteCode = byteCode;
      this.lastModifiedTime = lastModifiedTime;
    }

    /**
     * Collects data about a class which only needs the bytecode and no TypeOracle data structures.
     * This is used to make the initial shallow identity pass for creating
     * JRealClassType/JGenericType objects.
     */
    synchronized CollectClassData getCollectClassData() {
      if (classData == null) {
        ClassReader reader = new ClassReader(byteCode);
        classData = new CollectClassData();
        ClassVisitor classVisitor = classData;
        if (TRACE_CLASSES) {
          classVisitor = new TraceClassVisitor(classVisitor, new PrintWriter(System.out));
        }
        reader.accept(classVisitor, 0);
      }
      return classData;
    }
  }

  private class CompilationUnitTypeOracleResolver implements Resolver {

    private final TypeOracleBuildContext context;

    public CompilationUnitTypeOracleResolver(TypeOracleBuildContext context) {
      this.context = context;
    }

    @Override
    public void addImplementedInterface(JRealClassType type, JClassType intf) {
      CompilationUnitTypeOracleUpdater.this.addImplementedInterface(type, intf);
    }

    @Override
    public void addThrows(JAbstractMethod method, JClassType exception) {
      CompilationUnitTypeOracleUpdater.this.addThrows(method, exception);
    }

    @Override
    public JRealClassType findByInternalName(String internalName) {
      return CompilationUnitTypeOracleUpdater.this.findByInternalName(internalName);
    }

    @Override
    public TypeOracle getTypeOracle() {
      return CompilationUnitTypeOracleUpdater.this.typeOracle;
    }

    @Override
    public JMethod newMethod(JClassType type, String name,
        Map<Class<? extends Annotation>, Annotation> declaredAnnotations,
        JTypeParameter[] typeParams) {
      return CompilationUnitTypeOracleUpdater.this.newMethod(
          type, name, declaredAnnotations, typeParams);
    }

    @Override
    public void newParameter(JAbstractMethod method, JType argType, String argName,
        Map<Class<? extends Annotation>, Annotation> declaredAnnotations, boolean argNamesAreReal) {
      CompilationUnitTypeOracleUpdater.this.newParameter(
          method, argType, argName, declaredAnnotations, argNamesAreReal);
    }

    @Override
    public JRealClassType newRealClassType(JPackage pkg, String enclosingTypeName,
        boolean isLocalType, String simpleSourceName, boolean isInterface) {
      return CompilationUnitTypeOracleUpdater.this.newRealClassType(
          pkg, enclosingTypeName, simpleSourceName, isInterface);
    }

    @Override
    public boolean resolveAnnotation(TreeLogger logger, CollectAnnotationData annotVisitor,
        Map<Class<? extends Annotation>, Annotation> declaredAnnotations) {
      return CompilationUnitTypeOracleUpdater.this.resolveAnnotation(
          logger, annotVisitor, declaredAnnotations);
    }

    @Override
    public boolean resolveAnnotations(TreeLogger logger, List<CollectAnnotationData> annotations,
        Map<Class<? extends Annotation>, Annotation> declaredAnnotations) {
      return CompilationUnitTypeOracleUpdater.this.resolveAnnotations(
          logger, annotations, declaredAnnotations);
    }

    @Override
    public boolean resolveClass(TreeLogger logger, JRealClassType type) {
      return CompilationUnitTypeOracleUpdater.this.resolveClass(logger, type, context);
    }

    @Override
    public void setReturnType(JAbstractMethod method, JType returnType) {
      CompilationUnitTypeOracleUpdater.this.setReturnType(method, returnType);
    }

    @Override
    public void setSuperClass(JRealClassType type, JClassType superType) {
      CompilationUnitTypeOracleUpdater.this.setSuperClass(type, superType);
    }
  }

  /**
   * This context keeps common data so we don't have to pass it around between methods for one pass
   * of {@link CompilationUnitTypeOracleUpdater#addNewTypesDontIndex(TreeLogger, Collection,
   * MethodArgNamesLookup)} .
   */
  protected class TypeOracleBuildContext {
    protected final MethodArgNamesLookup allMethodArgs;

    private final Map<String, CollectClassData> classDataByInternalName =
        new HashMap<String, CollectClassData>();

    private final Map<JRealClassType, CollectClassData> classDataByType =
        new HashMap<JRealClassType, CollectClassData>();

    private final Resolver resolver = new CompilationUnitTypeOracleResolver(this);

    protected TypeOracleBuildContext(MethodArgNamesLookup allMethodArgs) {
      this.allMethodArgs = allMethodArgs;
    }
  }

  /**
   * Pairs of bits to convert from ASM Opcodes.* to Shared.* bitfields.
   */
  private static final int[] ASM_TO_SHARED_MODIFIERS =
      new int[] {Opcodes.ACC_PUBLIC, Shared.MOD_PUBLIC, //
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

  private static JTypeParameter[] getTypeParametersForClass(CollectClassData classData) {
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
      throw new IllegalArgumentException(primitiveClass.toString() + " not a primitive class");
    }
  }

  /**
   * Returns whether this name is the special package-info type name.
   */
  private static boolean isPackageInfoTypeName(String simpleSourceName) {
    return "package-info".equals(simpleSourceName);
  }

  /**
   * Returns true if this class is a non-static class inside a generic class.
   */
  // TODO(jat): do we need to consider the entire hierarchy?
  private static boolean nonStaticInsideGeneric(
      CollectClassData classData, CollectClassData enclosingClassData) {
    if (enclosingClassData == null || (classData.getAccess() & Opcodes.ACC_STATIC) != 0) {
      return false;
    }
    return getTypeParametersForClass(enclosingClassData) != null;
  }

  /**
   * Returns the original type or its raw type if it is generic
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

  private final Set<String> resolvedTypeSourceNames = Sets.newHashSet();
  private final Map<String, JRealClassType> typesByInternalName =
      new HashMap<String, JRealClassType>();

  public CompilationUnitTypeOracleUpdater(TypeOracle typeOracle) {
    super(typeOracle);
  }

  /**
   * Adds new units to an existing TypeOracle but does not yet index their type hierarchy.<br />
   *
   * It is ok for this function to recursive since no repeated or invalid type indexing will result.
   *
   * @param logger logger to use
   * @param typeDataList collection of data need to build types. (Doesn't retain references to
   *          TypeData instances.)
   * @param argsLookup Allows the caller to pass the method argument names which are not normally
   *          available in bytecode.
   */
  void addNewTypesDontIndex(
      TreeLogger logger, Collection<TypeData> typeDataList, MethodArgNamesLookup argsLookup) {
    Event typeOracleUpdaterEvent = SpeedTracerLogger.start(CompilerEventType.TYPE_ORACLE_UPDATER);

    // First collect all class data.
    Event visitClassFileEvent = SpeedTracerLogger.start(
        CompilerEventType.TYPE_ORACLE_UPDATER, "phase", "Visit Class Files");
    TypeOracleBuildContext context = getContext(argsLookup);

    for (TypeData typeData : typeDataList) {
      CollectClassData classData = typeData.getCollectClassData();
      // skip any classes that can't be referenced by name outside of
      // their local scope, such as anonymous classes and method-local classes
      if (classData.hasNoExternalName()) {
        continue;
      }
      // skip classes that have been previously added
      if (typesByInternalName.containsKey(classData.getInternalName())) {
        continue;
      }
      context.classDataByInternalName.put(typeData.internalName, classData);
    }
    visitClassFileEvent.end();

    Event identityEvent = SpeedTracerLogger.start(
        CompilerEventType.TYPE_ORACLE_UPDATER, "phase", "Establish Identity");
    // Perform a shallow pass to establish identity for new and old types.
    Set<JRealClassType> unresolvedTypes = new LinkedHashSet<JRealClassType>();
    for (TypeData typeData : typeDataList) {
      CollectClassData classData = context.classDataByInternalName.get(typeData.internalName);
      if (classData == null) {
        // ignore classes that were skipped earlier
        continue;
      }
      if (typesByInternalName.containsKey(classData.getInternalName())) {
        // skip classes that have been previously added
        continue;
      }
      JRealClassType type = createType(typeData, unresolvedTypes, context);
      if (type != null) {
        assert Name.isInternalName(typeData.internalName);
        typesByInternalName.put(typeData.internalName, type);
        context.classDataByType.put(type, classData);
      }
    }
    identityEvent.end();

    Event resolveEnclosingEvent = SpeedTracerLogger.start(
        CompilerEventType.TYPE_ORACLE_UPDATER, "phase", "Resolve Enclosing Classes");
    // Hook up enclosing types
    TreeLogger branch = logger.branch(TreeLogger.SPAM, "Resolving enclosing classes");
    for (Iterator<JRealClassType> unresolvedTypesIterator = unresolvedTypes.iterator();
        unresolvedTypesIterator.hasNext();) {
      JRealClassType unresolvedType = unresolvedTypesIterator.next();
      if (!resolveEnclosingClass(branch, unresolvedType, context)) {
        // already logged why it failed, don't try and use it further
        unresolvedTypesIterator.remove();
      }
    }
    resolveEnclosingEvent.end();

    Event resolveUnresolvedEvent = SpeedTracerLogger.start(
        CompilerEventType.TYPE_ORACLE_UPDATER, "phase", "Resolve Unresolved Types");
    // Resolve unresolved types.
    for (JRealClassType unresolvedType : unresolvedTypes) {
      branch =
          logger.branch(TreeLogger.SPAM, "Resolving " + unresolvedType.getQualifiedSourceName());
      if (!resolveClass(branch, unresolvedType, context)) {
        // already logged why it failed.
        // TODO: should we do anything else here?
      }
    }
    resolveUnresolvedEvent.end();

    // no longer needed
    context = null;
    typeOracleUpdaterEvent.end();
  }

  /**
   * Adds new units to an existing TypeOracle and indexes their type hierarchy.
   */
  public void addNewUnits(TreeLogger logger, Collection<CompilationUnit> compilationUnits) {
    addNewTypesDontIndex(logger, compilationUnits);
    indexTypes();
  }

  // VisibleForTesting
  void indexTypes() {
    Event finishEvent =
        SpeedTracerLogger.start(CompilerEventType.TYPE_ORACLE_UPDATER, "phase", "Finish");
    super.finish();
    finishEvent.end();
  }

  protected void addNewTypesDontIndex(TreeLogger logger,
      Collection<CompilationUnit> compilationUnits) {
    Collection<TypeData> typeDataList = new ArrayList<TypeData>();

    // Create method args data for types to add
    MethodArgNamesLookup argsLookup = new MethodArgNamesLookup();
    for (CompilationUnit compilationUnit : compilationUnits) {
      argsLookup.mergeFrom(compilationUnit.getMethodArgs());
    }

    // Create list including byte code for each type to add
    for (CompilationUnit compilationUnit : compilationUnits) {
      for (CompiledClass compiledClass : compilationUnit.getCompiledClasses()) {
        TypeData typeData = new TypeData(compiledClass.getPackageName(),
            compiledClass.getSourceName(), compiledClass.getInternalName(),
            compiledClass.getBytes(), compiledClass.getUnit().getLastModified());
        typeDataList.add(typeData);
      }
    }

    // Add the new types to the type oracle build in progress.
    addNewTypesDontIndex(logger, typeDataList, argsLookup);
  }

  // VisibleForTesting
  public Resolver getMockResolver() {
    return new CompilationUnitTypeOracleResolver(
        new TypeOracleBuildContext(new MethodArgNamesLookup()));
  }

  public TypeOracle getTypeOracle() {
    return typeOracle;
  }

  // VisibleForTesting
  public Map<String, JRealClassType> getTypesByInternalName() {
    return typesByInternalName;
  }

  /**
   * Returns the type corresponding to the given internal name.<br />
   *
   * Implementations are free to service requests eagerly or lazily.
   */
  protected JRealClassType findByInternalName(String internalName) {
    assert Name.isInternalName(internalName);
    return typesByInternalName.get(internalName);
  }

  private Annotation createAnnotation(TreeLogger logger,
      Class<? extends Annotation> annotationClass, AnnotationData annotationData) {
    // Make a copy before we mutate the collection.
    Map<String, Object> values = new HashMap<String, Object>(annotationData.getValues());
    for (Map.Entry<String, Object> entry : values.entrySet()) {
      Method method = null;
      Throwable caught = null;
      try {
        method = annotationClass.getMethod(entry.getKey());
        entry.setValue(resolveAnnotationValue(logger, method.getReturnType(), entry.getValue()));
      } catch (SecurityException e) {
        caught = e;
      } catch (NoSuchMethodException e) {
        caught = e;
      }
      if (caught != null) {
        logger.log(TreeLogger.WARN,
            "Exception resolving " + annotationClass.getCanonicalName() + "." + entry.getKey(),
            caught);
        return null;
      }
    }
    return AnnotationProxyFactory.create(annotationClass, values);
  }

  /**
   * Doesn't retain a reference to the TypeData.
   */
  private JRealClassType createType(
      TypeData typeData, CollectClassData collectClassData, CollectClassData enclosingClassData) {
    int access = collectClassData.getAccess();
    String simpleName = Shared.getShortName(typeData.sourceName);
    JRealClassType type = null;
    String packageName = typeData.packageName;
    JPackage pkg = typeOracle.getOrCreatePackage(packageName);
    boolean isInterface = (access & Opcodes.ACC_INTERFACE) != 0;
    assert !collectClassData.hasNoExternalName();
    String enclosingSimpleName = null;
    if (enclosingClassData != null) {
      enclosingSimpleName = enclosingClassData.getNestedSourceName();
    }
    if ((access & Opcodes.ACC_ANNOTATION) != 0) {
      type = newAnnotationType(pkg, enclosingSimpleName, simpleName);
    } else if ((access & Opcodes.ACC_ENUM) != 0) {
      type = newEnumType(pkg, enclosingSimpleName, simpleName);
    } else {
      JTypeParameter[] typeParams = getTypeParametersForClass(collectClassData);
      if ((typeParams != null && typeParams.length > 0)
          || nonStaticInsideGeneric(collectClassData, enclosingClassData)) {
        type = new JGenericType(
            typeOracle, pkg, enclosingSimpleName, simpleName, isInterface, typeParams);
      } else {
        type = newRealClassType(pkg, enclosingSimpleName, simpleName, isInterface);
      }
    }

    type.addModifierBits(mapBits(ASM_TO_SHARED_MODIFIERS, access));
    if (isInterface) {
      // Always add implicit modifiers on interfaces.
      type.addModifierBits(Shared.MOD_STATIC | Shared.MOD_ABSTRACT);
    }
    type.addLastModifiedTime(typeData.lastModifiedTime);

    return type;
  }

  /**
   * Doesn't retain a reference to the TypeData.
   */
  private JRealClassType createType(
      TypeData typeData, Set<JRealClassType> unresolvedTypes, TypeOracleBuildContext context) {
    CollectClassData classData = context.classDataByInternalName.get(typeData.internalName);
    String enclosingClassInternalName = classData.getEnclosingInternalName();
    CollectClassData enclosingClassData = null;
    if (enclosingClassInternalName != null) {
      enclosingClassData = context.classDataByInternalName.get(enclosingClassInternalName);
      if (enclosingClassData == null) {
        // if our enclosing class was skipped, skip this one too
        return null;
      }
    }
    JRealClassType realClassType = createType(typeData, classData, enclosingClassData);
    unresolvedTypes.add(realClassType);
    return realClassType;
  }

  private Class<? extends Annotation> getAnnotationClass(
      TreeLogger logger, AnnotationData annotationData) {
    Type type = Type.getType(annotationData.getDesc());
    String binaryName = type.getClassName();
    try {
      Class<?> clazz =
          Class.forName(binaryName, false, Thread.currentThread().getContextClassLoader());
      if (!Annotation.class.isAssignableFrom(clazz)) {
        logger.log(TreeLogger.ERROR, "Type " + binaryName + " is not an annotation");
        return null;
      }
      return clazz.asSubclass(Annotation.class);
    } catch (ClassNotFoundException e) {
      TreeLogger.Type level = TreeLogger.WARN;
      if (shouldSuppressUnresolvableAnnotation(logger, binaryName)) {
        level = TreeLogger.DEBUG;
      }
      logger.log(level, "Ignoring unresolvable annotation type " + binaryName);
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
   * Returns a new build context to use for the duration of one addNewTypesDontIndex() invocation.
   */
  protected TypeOracleBuildContext getContext(MethodArgNamesLookup argsLookup) {
    return new TypeOracleBuildContext(argsLookup);
  }

  /**
   * Map a bitset onto a different bitset.
   *
   * @param mapping int array containing a sequence of from/to pairs, each from entry should have
   *          exactly one bit set
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

  private boolean resolveAnnotation(TreeLogger logger, CollectAnnotationData annotationVisitor,
      Map<Class<? extends Annotation>, Annotation> declaredAnnotations) {
    AnnotationData annotationData = annotationVisitor.getAnnotation();
    Class<? extends Annotation> annotationClass = getAnnotationClass(logger, annotationData);
    if (annotationClass == null) {
      return false;
    }
    Annotation annotation = createAnnotation(logger, annotationClass, annotationData);
    if (annotation == null) {
      return false;
    }
    declaredAnnotations.put(annotationClass, annotation);
    return true;
  }

  private boolean resolveAnnotations(TreeLogger logger,
      List<CollectAnnotationData> annotationVisitors,
      Map<Class<? extends Annotation>, Annotation> declaredAnnotations) {
    boolean succeeded = true;
    if (annotationVisitors != null) {
      for (CollectAnnotationData annotationVisitor : annotationVisitors) {
        succeeded &= resolveAnnotation(logger, annotationVisitor, declaredAnnotations);
      }
    }
    return succeeded;
  }

  @SuppressWarnings("unchecked")
  private Object resolveAnnotationValue(TreeLogger logger, Class<?> expectedType, Object value) {
    if (expectedType.isArray()) {
      Class<?> componentType = expectedType.getComponentType();
      if (!value.getClass().isArray()) {
        logger.log(TreeLogger.WARN, "Annotation error: expected array of "
            + componentType.getCanonicalName() + ", got " + value.getClass().getCanonicalName());
        return null;
      }
      if (componentType.isPrimitive()) {
        // primitive arrays are already resolvedTypes
        return value;
      }
      // resolve each element in the array
      int n = Array.getLength(value);
      Object newArray = Array.newInstance(componentType, n);
      for (int i = 0; i < n; ++i) {
        Object valueElement = Array.get(value, i);
        Object resolvedValue = resolveAnnotationValue(logger, componentType, valueElement);
        if (resolvedValue == null || !componentType.isAssignableFrom(resolvedValue.getClass())) {
          logger.log(TreeLogger.ERROR,
              "Annotation error: expected " + componentType + ", got " + resolvedValue);
        } else {
          Array.set(newArray, i, resolvedValue);
        }
      }
      return newArray;
    } else if (expectedType.isEnum()) {
      if (!(value instanceof AnnotationEnum)) {
        logger.log(
            TreeLogger.ERROR, "Annotation error: expected an enum value," + " but got " + value);
        return null;
      }
      AnnotationEnum annotEnum = (AnnotationEnum) value;
      Class<? extends Enum> enumType = expectedType.asSubclass(Enum.class);
      try {
        return Enum.valueOf(enumType, annotEnum.getValue());
      } catch (IllegalArgumentException e) {
        logger.log(TreeLogger.WARN, "Unable to resolve annotation value '" + annotEnum.getValue()
            + "' within enum type '" + enumType.getName() + "'");
        return null;
      }
    } else if (Annotation.class.isAssignableFrom(expectedType)) {
      if (!(value instanceof AnnotationData)) {
        logger.log(TreeLogger.WARN, "Annotation error: expected annotation type "
            + expectedType.getCanonicalName() + ", got " + value.getClass().getCanonicalName());
        return null;
      }
      AnnotationData annotData = (AnnotationData) value;
      Class<? extends Annotation> annotationClass = getAnnotationClass(logger, annotData);
      if (!expectedType.isAssignableFrom(annotationClass)) {
        logger.log(TreeLogger.WARN, "Annotation error: expected " + expectedType.getCanonicalName()
            + ", got " + annotationClass.getCanonicalName());
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
          logger.log(TreeLogger.WARN,
              "Annotation error: expected a class " + "literal, but received " + value);
          return null;
        }
        Type valueType = (Type) value;
        // See if we can use a binary only class here
        try {
          return Class.forName(
              valueType.getClassName(), false, Thread.currentThread().getContextClassLoader());
        } catch (ClassNotFoundException e) {
          logger.log(
              TreeLogger.ERROR, "Annotation error: cannot resolve " + valueType.getClassName(), e);
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
    int dimensions = type.getDimensions();
    for (int i = 0; i < dimensions; ++i) {
      resolvedType = typeOracle.getArrayType(resolvedType);
    }
    return resolvedType;
  }

  private boolean resolveClass(
      TreeLogger logger, JRealClassType unresolvedType, TypeOracleBuildContext context) {
    assert unresolvedType != null;
    // Avoid cycles and useless computation.
    if (resolvedTypeSourceNames.contains(unresolvedType.getQualifiedSourceName())) {
      return true;
    }
    resolvedTypeSourceNames.add(unresolvedType.getQualifiedSourceName());

    // Make sure our enclosing type is resolved first.
    if (unresolvedType.getEnclosingType() != null
        && !resolveClass(logger, unresolvedType.getEnclosingType(), context)) {
      return false;
    }

    // Build a search list for type parameters to find their definition,
    // resolving enclosing classes as we go up.
    TypeParameterLookup typeParamLookup = new TypeParameterLookup();
    typeParamLookup.pushEnclosingScopes(unresolvedType);

    CollectClassData classData = context.classDataByType.get(unresolvedType);
    assert classData != null;
    int access = classData.getAccess();

    assert (!classData.getClassType().hasNoExternalName());

    logger = logger.branch(
        TreeLogger.SPAM, "Found type '" + unresolvedType.getQualifiedSourceName() + "'", null);

    // Handle package-info classes.
    if (isPackageInfoTypeName(unresolvedType.getSimpleSourceName())) {
      return resolvePackage(logger, unresolvedType, classData.getAnnotations());
    }

    // Resolve annotations
    Map<Class<? extends Annotation>, Annotation> declaredAnnotations =
        new HashMap<Class<? extends Annotation>, Annotation>();
    resolveAnnotations(logger, classData.getAnnotations(), declaredAnnotations);
    addAnnotations(unresolvedType, declaredAnnotations);

    String signature = classData.getSignature();

    /*
     * Note: Byte code from the OpenJDK compiler doesn't contain a type signature for non-static
     * inner classes of parameterized types that do not contain new parameters (but JDT compiled
     * byte code does). That can cause some differences in the way generic types are represented in
     * the type oracle.
     *
     * These differences also show up when using java.lang.reflect to look at types.
     */
    if (signature != null) {
      // If we have a signature, use it for superclass and interfaces
      SignatureReader reader = new SignatureReader(signature);
      ResolveClassSignature classResolver = new ResolveClassSignature(
          context.resolver, logger, unresolvedType, typeParamLookup);
      reader.accept(classResolver);
      classResolver.finish();

      if (unresolvedType.getSuperclass() != null
          && !resolveClass(logger, unresolvedType.getSuperclass(), context)) {
        logger.log(TreeLogger.WARN,
            "Unable to resolve supertype " + unresolvedType.getSuperclass().getName());
        return false;
      }
    } else {
      // Set the super type for non-interfaces
      if ((access & Opcodes.ACC_INTERFACE) == 0) {
        String superInternalName = classData.getSuperInternalName();
        assert Name.isInternalName(superInternalName);
        if (superInternalName != null) {
          JClassType superType = findByInternalName(superInternalName);
          if (superType == null || !resolveClass(logger, superType, context)) {
            logger.log(TreeLogger.WARN, "Unable to resolve supertype " + superInternalName);
            return false;
          }
          setSuperClass(unresolvedType, (JClassType) possiblySubstituteRawType(superType));
        }
      }

      // Set interfaces
      for (String interfaceInternalName : classData.getInterfaceInternalNames()) {
        JClassType interfaceType = findByInternalName(interfaceInternalName);
        if (interfaceType == null || !resolveClass(logger, interfaceType, context)) {
          logger.log(TreeLogger.WARN, "Unable to resolve interface " + interfaceInternalName);
          return false;
        }
        addImplementedInterface(
            unresolvedType, (JClassType) possiblySubstituteRawType(interfaceType));
      }
    }
    if (((access & Opcodes.ACC_INTERFACE) == 0) && unresolvedType.getSuperclass() == null) {
      // Only Object or interfaces should not have a superclass
      assert "java/lang/Object".equals(classData.getInternalName());
    }

    // Process methods
    for (CollectMethodData method : classData.getMethods()) {
      if (!resolveMethod(logger, unresolvedType, method, typeParamLookup, context)) {
        logger.log(TreeLogger.WARN, "Unable to resolve method " + method);
        return false;
      }
    }

    // Process fields
    // Track the next enum ordinal across resolveField calls.
    int[] nextEnumOrdinal = new int[] {0};
    for (CollectFieldData field : classData.getFields()) {
      if (!resolveField(logger, unresolvedType, field, typeParamLookup, nextEnumOrdinal, context)) {
        logger.log(TreeLogger.WARN, "Unable to resolve field " + field);
        return false;
      }
    }

    return true;
  }

  private boolean resolveClass(
      TreeLogger logger, JType unresolvedType, TypeOracleBuildContext context) {
    if (!(unresolvedType instanceof JClassType)) {
      // non-classes are already resolvedTypes
      return true;
    }
    if (unresolvedType instanceof JRealClassType) {
      return resolveClass(logger, (JRealClassType) unresolvedType, context);
    }
    if (unresolvedType instanceof JArrayType) {
      return resolveClass(logger, ((JArrayType) unresolvedType).getComponentType(), context);
    }
    if (unresolvedType instanceof JParameterizedType) {
      return resolveClass(logger, ((JParameterizedType) unresolvedType).getBaseType(), context);
    }
    if (unresolvedType instanceof JRawType) {
      return resolveClass(logger, ((JRawType) unresolvedType).getBaseType(), context);
    }
    if (unresolvedType instanceof JTypeParameter) {
      JTypeParameter typeParam = (JTypeParameter) unresolvedType;
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
    if (unresolvedType instanceof JWildcardType) {
      JWildcardType wildcard = (JWildcardType) unresolvedType;
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

  private boolean resolveEnclosingClass(
      TreeLogger logger, JRealClassType unresolvedType, TypeOracleBuildContext context) {
    assert unresolvedType != null;
    if (unresolvedType.getEnclosingType() != null) {
      return true;
    }

    // Find our enclosing class and set it
    CollectClassData classData = context.classDataByType.get(unresolvedType);
    assert classData != null;
    String enclosingClassInternalName = classData.getEnclosingInternalName();
    JRealClassType enclosingType = null;
    if (enclosingClassInternalName != null) {
      enclosingType = findByInternalName(enclosingClassInternalName);
      // Ensure enclosing classes are resolved
      if (enclosingType != null) {
        if (!resolveEnclosingClass(logger, enclosingType, context)) {
          return false;
        }
        if (enclosingType.isGenericType() != null
            && (classData.getAccess() & (Opcodes.ACC_STATIC | Opcodes.ACC_INTERFACE)) != 0) {
          // If the inner class doesn't have access to it's enclosing type's
          // type variables, the enclosing type must be the raw type instead
          // of the generic type.
          JGenericType genericType = enclosingType.isGenericType();
          setEnclosingType(unresolvedType, genericType.getRawType());
        } else {
          setEnclosingType(unresolvedType, enclosingType);
        }
      }
    }
    return true;
  }

  private boolean resolveField(TreeLogger logger, JRealClassType unresolvedType,
      CollectFieldData field, TypeParameterLookup typeParamLookup, int[] nextEnumOrdinal,
      TypeOracleBuildContext context) {
    Map<Class<? extends Annotation>, Annotation> declaredAnnotations =
        new HashMap<Class<? extends Annotation>, Annotation>();
    resolveAnnotations(logger, field.getAnnotations(), declaredAnnotations);
    String name = field.getName();
    JField jfield;
    if ((field.getAccess() & Opcodes.ACC_ENUM) != 0) {
      assert (unresolvedType.isEnum() != null);
      jfield = newEnumConstant(unresolvedType, name, declaredAnnotations, nextEnumOrdinal[0]++);
    } else {
      JField newField = newField(unresolvedType, name, declaredAnnotations);
      jfield = newField;
    }

    // Get modifiers.
    addModifierBits(jfield, mapBits(ASM_TO_SHARED_MODIFIERS, field.getAccess()));

    String signature = field.getSignature();
    JType fieldType;
    if (signature != null) {
      SignatureReader reader = new SignatureReader(signature);
      JType[] fieldTypeRef = new JType[1];
      reader.acceptType(new ResolveTypeSignature(
          context.resolver, logger, fieldTypeRef, typeParamLookup, null));
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

  private boolean resolveMethod(TreeLogger logger, JRealClassType unresolvedType,
      CollectMethodData methodData, TypeParameterLookup typeParamLookup,
      TypeOracleBuildContext context) {
    Map<Class<? extends Annotation>, Annotation> declaredAnnotations =
        new HashMap<Class<? extends Annotation>, Annotation>();
    resolveAnnotations(logger, methodData.getAnnotations(), declaredAnnotations);
    String name = methodData.getName();

    if ("<clinit>".equals(name) || (methodData.getAccess() & Opcodes.ACC_SYNTHETIC) != 0) {
      // Ignore the following and leave them out of TypeOracle:
      // - static initializers
      // - synthetic methods
      return true;
    }

    if (unresolvedType.isEnum() != null && "<init>".equals(name)) {
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
      name = unresolvedType.getSimpleSourceName();
      method = newConstructor(unresolvedType, name, declaredAnnotations, typeParams);
      hasReturnType = false;
    } else {
      if (unresolvedType.isAnnotation() != null) {
        // TODO(jat): actually resolve the default annotation value.
        method = newAnnotationMethod(unresolvedType, name, declaredAnnotations, typeParams, null);
      } else {
        method = newMethod(unresolvedType, name, declaredAnnotations, typeParams);
      }
    }

    addModifierBits(method, mapBits(ASM_TO_SHARED_MODIFIERS, methodData.getAccess()));
    if (unresolvedType.isInterface() != null) {
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
      ResolveMethodSignature methodResolver = new ResolveMethodSignature(context.resolver, logger,
          method, typeParamLookup, hasReturnType, methodData, methodData.getArgTypes(),
          methodData.getArgNames(), methodData.hasActualArgNames(), context.allMethodArgs);
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
    String internalName = type.getInternalName();
    assert Name.isInternalName(internalName);
    JRealClassType classType = findByInternalName(internalName);
    return classType;
  }

  private boolean resolvePackage(TreeLogger logger, JRealClassType unresolvedType,
      List<CollectAnnotationData> annotationVisitors) {
    Map<Class<? extends Annotation>, Annotation> declaredAnnotations =
        new HashMap<Class<? extends Annotation>, Annotation>();
    resolveAnnotations(logger, annotationVisitors, declaredAnnotations);
    addAnnotations(unresolvedType.getPackage(), declaredAnnotations);
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
      Map<Class<? extends Annotation>, Annotation> declaredAnnotations =
          new HashMap<Class<? extends Annotation>, Annotation>();
      resolveAnnotations(logger, paramAnnot[i], declaredAnnotations);

      newParameter(method, argType, argNames[i], declaredAnnotations, argNamesAreReal);
    }
    return true;
  }

  private boolean resolveThrows(JAbstractMethod method, CollectMethodData methodData) {
    if (method.getThrows().length == 0) {
      for (String exceptionName : methodData.getExceptions()) {
        JType exceptionType = resolveType(Type.getObjectType(exceptionName));
        if (exceptionType == null) {
          return false;
        }
        addThrows(method, (JClassType) exceptionType);
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
   */
  // TODO(zundel): Can be removed when javax.validation is included in the JRE
  private boolean shouldSuppressUnresolvableAnnotation(TreeLogger logger, String sourceName) {
    if (sourceName.startsWith("javax.validation.")
        || sourceName.startsWith("com.google.gwt.validation.")) {
      if (!warnedMissingValidationJar) {
        warnedMissingValidationJar = true;
        logger.log(TreeLogger.WARN, "Detected warnings related to '" + sourceName + "'. "
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
