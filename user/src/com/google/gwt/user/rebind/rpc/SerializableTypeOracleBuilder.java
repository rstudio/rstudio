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

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JArrayType;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JGenericType;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JRealClassType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.JTypeParameter;
import com.google.gwt.core.ext.typeinfo.JWildcardType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.util.log.PrintWriterTreeLogger;
import com.google.gwt.user.client.rpc.GwtTransient;
import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.rebind.rpc.ProblemReport.Priority;
import com.google.gwt.user.rebind.rpc.TypeParameterExposureComputer.TypeParameterFlowInfo;
import com.google.gwt.user.rebind.rpc.TypePaths.TypePath;

import java.io.PrintWriter;
import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

/**
 * Builds a {@link SerializableTypeOracle} for a given set of root types.
 *
 * <p>
 * There are two goals for this builder. First, discover the set of serializable
 * types that can be serialized if you serialize one of the root types. Second,
 * to make sure that all root types can actually be serialized by GWT.
 * </p>
 *
 * <p>
 * To find the serializable types, it includes the root types, and then it
 * iteratively traverses the type hierarchy and the fields of any type already
 * deemed serializable. To improve the accuracy of the traversal there is a
 * computations of the exposure of type parameters. When the traversal reaches a
 * parameterized type, these exposure values are used to determine how to treat
 * the arguments.
 * </p>
 *
 * <p>
 * A type qualifies for serialization if it or one of its subtypes is
 * automatically or manually serializable. Automatic serialization is selected
 * if the type is assignable to {@link IsSerializable} or {@link Serializable}
 * or if the type is a primitive type such as int, boolean, etc. Manual
 * serialization is selected if there exists another type with the same fully
 * qualified name concatenated with "_CustomFieldSerializer". If a type
 * qualifies for both manual and automatic serialization, manual serialization
 * is preferred.
 * </p>
 *
 * <p>
 * Some types may be marked as "enhanced," either automatically by the presence
 * of a JDO <code>@PersistenceCapable</code> or JPA <code>@Entity</code> tag on
 * the class definition, or manually by extending the 'rpc.enhancedClasses'
 * configuration property in the GWT module XML file. For example, to manually
 * mark the class com.google.myapp.MyPersistentClass as enhanced, use:
 *
 * <pre>
 * <extend-configuration-property name='rpc.enhancedClasses'
 *     value='com.google.myapp.MyPersistentClass'/>
 * </pre>
 *
 * <p>
 * Enhanced classes are checked for the presence of additional serializable
 * fields on the server that were not defined in client code as seen by the GWT
 * compiler. If it is possible for an instance of such a class to be transmitted
 * bidrectionally between server and client, a special RPC rule is used. The
 * server-only fields are serialized using standard Java serialization and sent
 * between the client and server as a blob of opaque base-64 encoded binary
 * data. When an instance is sent from client to server, the server instance is
 * populated by invoking setter methods where possible rather than by setting
 * fields directly. This allows APIs such as JDO the opportunity to update the
 * object state properly to take into account changes that may have occurred to
 * the object's state while resident on the client.
 * </p>
 */
public class SerializableTypeOracleBuilder {

  static class TypeInfoComputed {

    /**
     * <code>true</code> if the type is automatically or manually serializable
     * and the corresponding checks succeed.
     */
    private boolean fieldSerializable = false;

    /**
     * <code>true</code> if this type might be instantiated.
     */
    private boolean instantiable = false;

    /**
     * <code>true</code> if there are instantiable subtypes assignable to this
     * one.
     */
    private boolean instantiableSubtypes;

    /**
     * All instantiable types found when this type was queried, including the
     * type itself. (Null until calculated.)
     */
    private Set<JClassType> instantiableTypes;

    /**
     * Custom field serializer or <code>null</code> if there isn't one.
     */
    private final JClassType manualSerializer;

    /**
     * <code>true</code> if this class might be enhanced on the server to
     * contain extra fields.
     */
    private final boolean maybeEnhanced;

    /**
     * Path used to discover this type.
     */
    private final TypePath path;

    /**
     * The state that this type is currently in.
     */
    private TypeState state = TypeState.NOT_CHECKED;

    /**
     * {@link JClassType} associated with this metadata.
     */
    private final JType type;

    private TypeInfoComputed(JType type, TypePath path, TypeOracle typeOracle) {
      this.type = type;
      this.path = path;
      if (type instanceof JClassType) {
        JClassType typeClass = (JClassType) type;
        manualSerializer = findCustomFieldSerializer(typeOracle, typeClass);
        maybeEnhanced = hasJdoAnnotation(typeClass) || hasJpaAnnotation(typeClass);
      } else {
        manualSerializer = null;
        maybeEnhanced = false;
      }
    }

    public TypePath getPath() {
      return path;
    }

    public JType getType() {
      return type;
    }

    public boolean hasInstantiableSubtypes() {
      return instantiable || instantiableSubtypes || state == TypeState.CHECK_IN_PROGRESS;
    }

    public boolean isDone() {
      return state == TypeState.CHECK_DONE;
    }

    public boolean isFieldSerializable() {
      return fieldSerializable;
    }

    public boolean isInstantiable() {
      return instantiable;
    }

    public boolean isManuallySerializable() {
      return manualSerializer != null;
    }

    public boolean isPendingInstantiable() {
      return state == TypeState.CHECK_IN_PROGRESS;
    }

    public boolean maybeEnhanced() {
      return maybeEnhanced;
    }

    public void setFieldSerializable() {
      fieldSerializable = true;
    }

    public void setInstantiable(boolean instantiable) {
      this.instantiable = instantiable;
      if (instantiable) {
        fieldSerializable = true;
      }
      state = TypeState.CHECK_DONE;
    }

    public void setInstantiableSubtypes(boolean instantiableSubtypes) {
      this.instantiableSubtypes = instantiableSubtypes;
    }

    public void setPendingInstantiable() {
      state = TypeState.CHECK_IN_PROGRESS;
    }
  }

  private enum TypeState {
    /**
     * The instantiability of a type has been determined.
     */
    CHECK_DONE("Check succeeded"),
    /**
     * The instantiability of a type is being checked.
     */
    CHECK_IN_PROGRESS("Check in progress"),
    /**
     * The instantiability of a type has not been checked.
     */
    NOT_CHECKED("Not checked");

    private final String message;

    TypeState(String message) {
      this.message = message;
    }

    @Override
    public String toString() {
      return message;
    }
  }

  /**
   * Compares {@link JType}s according to their qualified source names.
   */
  static final Comparator<JType> JTYPE_COMPARATOR = new Comparator<JType>() {
    @Override
    public int compare(JType t1, JType t2) {
      return t1.getQualifiedSourceName().compareTo(t2.getQualifiedSourceName());
    }
  };

  /**
   * No type filtering by default..
   */
  private static final TypeFilter DEFAULT_TYPE_FILTER = new TypeFilter() {
    @Override
    public String getName() {
      return "Default";
    }

    @Override
    public boolean isAllowed(JClassType type) {
      return true;
    }
  };

  /**
   * A reference to the annotation class
   * javax.jdo.annotations.PersistenceCapable used by the JDO API. May be null
   * if JDO is not present in the runtime environment.
   */
  private static Class<? extends Annotation> JDO_PERSISTENCE_CAPABLE_ANNOTATION = null;

  /**
   * A reference to the method 'String
   * javax.jdo.annotations.PersistenceCapable.detachable()'.
   */
  private static Method JDO_PERSISTENCE_CAPABLE_DETACHABLE_METHOD;

  /**
   * A reference to the annotation class javax.persistence.Entity used by the
   * JPA API. May be null if JPA is not present in the runtime environment.
   */
  private static Class<? extends Annotation> JPA_ENTITY_ANNOTATION = null;

  static {
    try {
      JDO_PERSISTENCE_CAPABLE_ANNOTATION =
          Class.forName("javax.jdo.annotations.PersistenceCapable").asSubclass(Annotation.class);
      JDO_PERSISTENCE_CAPABLE_DETACHABLE_METHOD =
          JDO_PERSISTENCE_CAPABLE_ANNOTATION.getDeclaredMethod("detachable", (Class[]) null);
    } catch (ClassNotFoundException e) {
      // Ignore, JDO_PERSISTENCE_CAPABLE_ANNOTATION will be null
    } catch (NoSuchMethodException e) {
      JDO_PERSISTENCE_CAPABLE_ANNOTATION = null;
    }

    try {
      JPA_ENTITY_ANNOTATION =
          Class.forName("javax.persistence.Entity").asSubclass(Annotation.class);
    } catch (ClassNotFoundException e) {
      // Ignore, JPA_ENTITY_CAPABLE_ANNOTATION will be null
    }
  }

  static boolean canBeInstantiated(JClassType type, ProblemReport problems) {
    if (type.isEnum() == null) {
      if (type.isAbstract()) {
        // Abstract types will be picked up if there is an instantiable
        // subtype.
        return false;
      }

      if (!type.isDefaultInstantiable() && !isManuallySerializable(type)) {
        // Warn and return false.
        problems.add(type, type.getParameterizedQualifiedSourceName()
            + " is not default instantiable (it must have a zero-argument "
            + "constructor or no constructors at all) and has no custom " + "serializer.",
            Priority.DEFAULT);
        return false;
      }
    } else {
      /*
       * Enums are always instantiable regardless of abstract or default
       * instantiability.
       */
    }

    return true;
  }

  /**
   * Finds the custom field serializer for a given type.
   *
   * @param typeOracle
   * @param type
   * @return the custom field serializer for a type or <code>null</code> if
   *         there is not one
   */
  public static JClassType findCustomFieldSerializer(TypeOracle typeOracle, JType type) {
    JClassType classOrInterface = type.isClassOrInterface();
    if (classOrInterface == null) {
      return null;
    }

    String customFieldSerializerName = getCustomFieldSerializerName(type.getQualifiedSourceName());
    return findCustomFieldSerializer(typeOracle, customFieldSerializerName);
  }

  /**
   * Finds the custom field serializer for a given qualified source name.
   *
   * @param typeOracle
   * @param customFieldSerializerName
   * @return the custom field serializer for a type of <code>null</code> if
   *         there is not one
   */
  public static JClassType findCustomFieldSerializer(TypeOracle typeOracle,
      String customFieldSerializerName) {
    JClassType customSerializer = typeOracle.findType(customFieldSerializerName);
    if (customSerializer == null) {
      // If the type is in the java.lang or java.util packages then it will be
      // mapped into com.google.gwt.user.client.rpc.core package
      customSerializer =
          typeOracle.findType("com.google.gwt.user.client.rpc.core." + customFieldSerializerName);
    }

    return customSerializer;
  }

  /**
   * Returns the name for a custom field serializer, given a source name.
   *
   * @param sourceName
   * @return the custom field serializer type name for a given source name.
   */
  public static String getCustomFieldSerializerName(String sourceName) {
    return sourceName + "_CustomFieldSerializer";
  }

  static JRealClassType getBaseType(JClassType type) {
    if (type.isParameterized() != null) {
      return type.isParameterized().getBaseType();
    } else if (type.isRawType() != null) {
      return type.isRawType().getBaseType();
    }

    return (JRealClassType) type;
  }

  static boolean hasGwtTransientAnnotation(JField field) {
    for (Annotation a : field.getAnnotations()) {
      if (a.annotationType().getSimpleName().equals(GwtTransient.class.getSimpleName())) {
        return true;
      }
    }
    return false;
  }

  /**
   * @param type the type to query
   * @return true if the type is annotated with @PersistenceCapable(...,
   *         detachable="true")
   */
  static boolean hasJdoAnnotation(JClassType type) {
    if (JDO_PERSISTENCE_CAPABLE_ANNOTATION == null) {
      return false;
    }
    Annotation annotation = type.getAnnotation(JDO_PERSISTENCE_CAPABLE_ANNOTATION);
    if (annotation == null) {
      return false;
    }
    try {
      Object value = JDO_PERSISTENCE_CAPABLE_DETACHABLE_METHOD.invoke(annotation, (Object[]) null);
      if (value instanceof String) {
        return "true".equalsIgnoreCase((String) value);
      } else {
        return false;
      }
    } catch (IllegalAccessException e) {
      // will return false
    } catch (InvocationTargetException e) {
      // will return false
    }

    return false;
  }

  /**
   * @param type the type to query
   * @return true if the type is annotated with @Entity
   */
  static boolean hasJpaAnnotation(JClassType type) {
    if (JPA_ENTITY_ANNOTATION == null) {
      return false;
    }
    Annotation annotation = type.getAnnotation(JPA_ENTITY_ANNOTATION);
    return annotation != null;
  }

  static boolean isAutoSerializable(JClassType type) {
    try {
      JClassType isSerializable = getIsSerializableMarkerInterface(type);
      JClassType serializable = getSerializableMarkerInterface(type);
      return type.isAssignableTo(isSerializable) || type.isAssignableTo(serializable);
    } catch (NotFoundException e) {
      return false;
    }
  }

  /**
   * Returns <code>true</code> if this type is part of the standard java
   * packages.
   */
  static boolean isInStandardJavaPackage(String className) {
    if (className.startsWith("java.")) {
      return true;
    }

    if (className.startsWith("javax.")) {
      return true;
    }

    return false;
  }

  static void recordTypeParametersIn(JType type, Set<JTypeParameter> params) {
    JTypeParameter isTypeParameter = type.isTypeParameter();
    if (isTypeParameter != null) {
      params.add(isTypeParameter);
    }

    JArrayType isArray = type.isArray();
    if (isArray != null) {
      recordTypeParametersIn(isArray.getComponentType(), params);
    }

    JWildcardType isWildcard = type.isWildcard();
    if (isWildcard != null) {
      for (JClassType bound : isWildcard.getUpperBounds()) {
        recordTypeParametersIn(bound, params);
      }
    }

    JParameterizedType isParameterized = type.isParameterized();
    if (isParameterized != null) {
      for (JClassType arg : isParameterized.getTypeArgs()) {
        recordTypeParametersIn(arg, params);
      }
    }
  }

  /**
   * Return <code>true</code> if a class's fields should be considered for
   * serialization. If it returns <code>false</code> then none of the fields of
   * this class should be serialized.
   */
  static boolean shouldConsiderFieldsForSerialization(JClassType type, TypeFilter filter,
      ProblemReport problems) {
    if (!isAllowedByFilter(filter, type, problems)) {
      return false;
    }

    if (!isDeclaredSerializable(type)) {
      problems.add(type, type.getParameterizedQualifiedSourceName() + " is not assignable to '"
          + IsSerializable.class.getName() + "' or '" + Serializable.class.getName()
          + "' nor does it have a custom field serializer", Priority.DEFAULT);
      return false;
    }

    if (isManuallySerializable(type)) {
      JClassType manualSerializer = findCustomFieldSerializer(type.getOracle(), type);
      assert (manualSerializer != null);

      List<String> fieldProblems = CustomFieldSerializerValidator.validate(manualSerializer, type);
      if (!fieldProblems.isEmpty()) {
        for (String problem : fieldProblems) {
          problems.add(type, problem, Priority.FATAL);
        }
        return false;
      }
    } else {
      assert (isAutoSerializable(type));

      if (!isAccessibleToSerializer(type)) {
        // Class is not visible to a serializer class in the same package
        problems.add(type, type.getParameterizedQualifiedSourceName()
            + " is not accessible from a class in its same package; it "
            + "will be excluded from the set of serializable types", Priority.DEFAULT);
        return false;
      }

      if (type.isMemberType() && !type.isStatic()) {
        // Non-static member types cannot be serialized
        problems.add(type, type.getParameterizedQualifiedSourceName() + " is nested but "
            + "not static; it will be excluded from the set of serializable " + "types",
            Priority.DEFAULT);
        return false;
      }
    }

    return true;
  }

  /**
   * Returns <code>true</code> if the field qualifies for serialization without
   * considering its type.
   */
  static boolean shouldConsiderForSerialization(TreeLogger logger,
      boolean suppressNonStaticFinalFieldWarnings, JField field) {
    if (field.isStatic() || field.isTransient()) {
      return false;
    }

    if (hasGwtTransientAnnotation(field)) {
      return false;
    }

    if (field.isFinal()) {
      TreeLogger.Type logLevel;
      if (isManuallySerializable(field.getEnclosingType())) {
        /*
         * If the type has a custom serializer, assume the programmer knows
         * best.
         */
        logLevel = TreeLogger.DEBUG;
      } else {
        logLevel = TreeLogger.WARN;
      }
      logger.branch(suppressNonStaticFinalFieldWarnings ? TreeLogger.DEBUG : logLevel, "Field '"
          + field.toString() + "' will not be serialized because it is final", null);
      return false;
    }

    return true;
  }

  private static boolean directlyImplementsMarkerInterface(JClassType type) {
    try {
      return TypeHierarchyUtils.directlyImplementsInterface(type,
          getIsSerializableMarkerInterface(type))
          || TypeHierarchyUtils.directlyImplementsInterface(type,
              getSerializableMarkerInterface(type));
    } catch (NotFoundException e) {
      return false;
    }
  }

  private static JArrayType getArrayType(TypeOracle typeOracle, int rank, JType component) {
    assert (rank > 0);

    JArrayType array = null;
    JType currentComponent = component;
    for (int i = 0; i < rank; ++i) {
      array = typeOracle.getArrayType(currentComponent);
      currentComponent = array;
    }

    return array;
  }

  private static JClassType getIsSerializableMarkerInterface(JClassType type)
      throws NotFoundException {
    return type.getOracle().getType(IsSerializable.class.getName());
  }

  private static JClassType getSerializableMarkerInterface(JClassType type)
      throws NotFoundException {
    return type.getOracle().getType(Serializable.class.getName());
  }

  /**
   * Returns <code>true</code> if a serializer class could access this type.
   */
  private static boolean isAccessibleToSerializer(JClassType type) {
    if (type.isPrivate()) {
      return false;
    }

    if (isInStandardJavaPackage(type.getQualifiedSourceName())) {
      if (!type.isPublic()) {
        return false;
      }
    }

    if (type.isMemberType()) {
      return isAccessibleToSerializer(type.getEnclosingType());
    }

    return true;
  }

  private static boolean isAllowedByFilter(TypeFilter filter, JClassType classType,
      ProblemReport problems) {
    if (!filter.isAllowed(classType)) {
      problems.add(classType, classType.getParameterizedQualifiedSourceName()
          + " is excluded by type filter ", Priority.AUXILIARY);
      return false;
    }

    return true;
  }

  private static boolean isDeclaredSerializable(JClassType type) {
    return isAutoSerializable(type) || isManuallySerializable(type);
  }

  private static boolean isDirectlySerializable(JClassType type) {
    return directlyImplementsMarkerInterface(type) || isManuallySerializable(type);
  }

  private static boolean isManuallySerializable(JClassType type) {
    return findCustomFieldSerializer(type.getOracle(), type) != null;
  }

  private static void logSerializableTypes(TreeLogger logger, Set<JClassType> fieldSerializableTypes) {
    if (!logger.isLoggable(TreeLogger.DEBUG)) {
      return;
    }
    TreeLogger localLogger =
        logger.branch(TreeLogger.DEBUG, "Identified " + fieldSerializableTypes.size()
            + " serializable type" + ((fieldSerializableTypes.size() == 1) ? "" : "s"), null);

    for (JClassType fieldSerializableType : fieldSerializableTypes) {
      localLogger.branch(TreeLogger.DEBUG, fieldSerializableType
          .getParameterizedQualifiedSourceName(), null);
    }
  }

  private boolean alreadyCheckedObject;

  /**
   * Cache of the {@link JClassType} for {@link Collection}.
   */
  private final JGenericType collectionClass;

  private final GeneratorContext context;

  private Set<String> enhancedClasses = null;

  private PrintWriter logOutputWriter;

  /**
   * Cache of the {@link JClassType} for {@link Map}.
   */
  private final JGenericType mapClass;

  private final Map<JClassType, TreeLogger> rootTypes = new LinkedHashMap<JClassType, TreeLogger>();

  /**
   * If <code>true</code> we will not warn if a serializable type contains a
   * non-static final field. We warn because these fields are not serialized.
   */
  private final boolean suppressNonStaticFinalFieldWarnings;

  private final TypeConstrainer typeConstrainer;
  private TypeFilter typeFilter = DEFAULT_TYPE_FILTER;

  private final TypeOracle typeOracle;

  private final TypeParameterExposureComputer typeParameterExposureComputer =
      new TypeParameterExposureComputer(typeFilter);

  /**
   * The set of type parameters that appear in one of the root types.
   * TODO(spoon): It would be cleaner to delete this field, and instead to have
   * {@link #addRootType(TreeLogger, JType)} replace parameters with wildcard
   * types. Then the root types would not contain any parameters.
   */
  private Set<JTypeParameter> typeParametersInRootTypes = new HashSet<JTypeParameter>();

  /**
   * Map of {@link JType} to {@link TypeInfoComputed}.
   */
  private final Map<JType, TypeInfoComputed> typeToTypeInfoComputed =
      new HashMap<JType, TypeInfoComputed>();

  /**
   * Constructs a builder.
   *
   * @param logger
   * @param propertyOracle
   * @param context
   *
   * @throws UnableToCompleteException if we fail to find one of our special
   *           types
   */
  public SerializableTypeOracleBuilder(TreeLogger logger, PropertyOracle propertyOracle,
      GeneratorContext context) throws UnableToCompleteException {
    this.context = context;
    this.typeOracle = context.getTypeOracle();
    typeConstrainer = new TypeConstrainer(typeOracle);

    try {
      collectionClass = typeOracle.getType(Collection.class.getName()).isGenericType();
      mapClass = typeOracle.getType(Map.class.getName()).isGenericType();
    } catch (NotFoundException e) {
      logger.log(TreeLogger.ERROR, null, e);
      throw new UnableToCompleteException();
    }

    suppressNonStaticFinalFieldWarnings =
        Shared.shouldSuppressNonStaticFinalFieldWarnings(logger, propertyOracle);
    enhancedClasses = Shared.getEnhancedTypes(propertyOracle);
  }

  public void addRootType(TreeLogger logger, JType type) {
    if (type.isPrimitive() != null) {
      return;
    }

    JClassType clazz = (JClassType) type;
    if (!rootTypes.containsKey(clazz)) {
      recordTypeParametersIn(type, typeParametersInRootTypes);

      rootTypes.put(clazz, logger);
    } else {
      if (logger.isLoggable(TreeLogger.TRACE)) {
        logger.log(TreeLogger.TRACE, clazz.getParameterizedQualifiedSourceName()
            + " is already a root type.");
      }
    }
  }

  /**
   * Builds a {@link SerializableTypeOracle} for a given set of root types.
   *
   * @param logger
   * @return a {@link SerializableTypeOracle} for the specified set of root
   *         types
   *
   * @throws UnableToCompleteException if there was not at least one
   *           instantiable type assignable to each of the specified root types
   */
  public SerializableTypeOracle build(TreeLogger logger) throws UnableToCompleteException {
    alreadyCheckedObject = false;

    boolean allSucceeded = true;

    for (Entry<JClassType, TreeLogger> entry : rootTypes.entrySet()) {
      ProblemReport problems = new ProblemReport();
      problems.setContextType(entry.getKey());
      boolean entrySucceeded =
          computeTypeInstantiability(entry.getValue(), entry.getKey(),
              TypePaths.createRootPath(entry.getKey()), problems).hasInstantiableSubtypes();
      if (!entrySucceeded) {
        problems.report(logger, TreeLogger.ERROR, TreeLogger.INFO);
      } else {
        maybeReport(logger, problems);
      }
      allSucceeded &= entrySucceeded & !problems.hasFatalProblems();
    }

    if (!allSucceeded) {
      throw new UnableToCompleteException();
    }
    assertNothingPending();

    // Add covariant arrays in a separate pass. We want to ensure that nothing is pending
    // so that the leaf type's instantiableTypes variable is ready (if it's computed at all)
    // and all of the leaf's subtypes are ready.
    // (Copy values to avoid concurrent modification.)
    List<TypeInfoComputed> ticsToCheck = new ArrayList<TypeInfoComputed>();
    ticsToCheck.addAll(typeToTypeInfoComputed.values());
    for (TypeInfoComputed tic : ticsToCheck) {
      JArrayType type = tic.getType().isArray();
      if (type != null && tic.instantiable) {
        ProblemReport problems = new ProblemReport();
        problems.setContextType(type);

        markArrayTypes(logger, type, tic.getPath(), problems);

        maybeReport(logger, problems);
        allSucceeded &= !problems.hasFatalProblems();
      }
    }

    if (!allSucceeded) {
      throw new UnableToCompleteException();
    }
    assertNothingPending();

    pruneUnreachableTypes();

    logReachableTypes(logger);

    Set<JClassType> possiblyInstantiatedTypes = new TreeSet<JClassType>(JTYPE_COMPARATOR);

    Set<JClassType> fieldSerializableTypes = new TreeSet<JClassType>(JTYPE_COMPARATOR);

    for (TypeInfoComputed tic : typeToTypeInfoComputed.values()) {
      if (!(tic.getType() instanceof JClassType)) {
        continue;
      }
      JClassType type = (JClassType) tic.getType();

      type = type.getErasedType();

      if (tic.isInstantiable()) {
        assert (!type.isAbstract() || type.isEnum() != null);

        possiblyInstantiatedTypes.add(type);
      }

      if (tic.isFieldSerializable()) {
        assert (type.isInterface() == null);

        fieldSerializableTypes.add(type);
      }

      if (tic.maybeEnhanced()
          || (enhancedClasses != null && enhancedClasses.contains(type.getQualifiedSourceName()))) {
        type.setEnhanced();
      }
    }

    logSerializableTypes(logger, fieldSerializableTypes);

    return new SerializableTypeOracleImpl(fieldSerializableTypes, possiblyInstantiatedTypes);
  }

  /**
   * Set the {@link PrintWriter} which will receive a detailed log of the types
   * which were examined in order to determine serializability.
   */
  public void setLogOutputWriter(PrintWriter logOutputWriter) {
    this.logOutputWriter = logOutputWriter;
  }

  public void setTypeFilter(TypeFilter typeFilter) {
    this.typeFilter = typeFilter;
    typeParameterExposureComputer.setTypeFilter(typeFilter);
  }

  /**
   * This method determines information about serializing a type with GWT. To do
   * so, it must traverse all subtypes as well as all field types of those
   * types, transitively.
   *
   * It returns a {@link TypeInfoComputed} with the information found.
   *
   * As a side effect, all types needed--plus some--to serialize this type are
   * accumulated in {@link #typeToTypeInfoComputed}. In particular, there will
   * be an entry for any type that has been validated by this method, as a
   * shortcircuit to avoid recomputation.
   *
   * The method is exposed using default access to enable testing.
   */
  TypeInfoComputed computeTypeInstantiability(TreeLogger logger, JType type, TypePath path,
      ProblemReport problems) {
    assert (type != null);
    if (type.isPrimitive() != null) {
      TypeInfoComputed tic = ensureTypeInfoComputed(type, path);
      tic.setInstantiableSubtypes(true);
      tic.setInstantiable(false);
      return tic;
    }

    assert (type instanceof JClassType);

    JClassType classType = (JClassType) type;

    TypeInfoComputed tic = typeToTypeInfoComputed.get(classType);
    if (tic != null && tic.isDone()) {
      // we have an answer already; use it.
      return tic;
    }

    TreeLogger localLogger =
        logger.branch(TreeLogger.DEBUG, classType.getParameterizedQualifiedSourceName(), null);

    JTypeParameter isTypeParameter = classType.isTypeParameter();
    if (isTypeParameter != null) {
      if (typeParametersInRootTypes.contains(isTypeParameter)) {
        return computeTypeInstantiability(localLogger, isTypeParameter.getFirstBound(), TypePaths
            .createTypeParameterInRootPath(path, isTypeParameter), problems);
      }

      /*
       * This type parameter was not in a root type and therefore it is the
       * caller's responsibility to deal with it. We assume that it is
       * indirectly instantiable here.
       */
      tic = ensureTypeInfoComputed(classType, path);
      tic.setInstantiableSubtypes(true);
      tic.setInstantiable(false);
      return tic;
    }

    JWildcardType isWildcard = classType.isWildcard();
    if (isWildcard != null) {
      boolean success = true;
      for (JClassType bound : isWildcard.getUpperBounds()) {
        success &=
            computeTypeInstantiability(localLogger, bound, path, problems)
                .hasInstantiableSubtypes();
      }
      tic = ensureTypeInfoComputed(classType, path);
      tic.setInstantiableSubtypes(success);
      tic.setInstantiable(false);
      return tic;
    }

    JArrayType isArray = classType.isArray();
    if (isArray != null) {
      TypeInfoComputed arrayTic = checkArrayInstantiable(localLogger, isArray, path, problems);
      assert typeToTypeInfoComputed.get(classType) != null;
      return arrayTic;
    }

    if (classType == typeOracle.getJavaLangObject()) {
      /*
       * Report an error if the type is or erases to Object since this violates
       * our restrictions on RPC. Should be fatal, but I worry users may have
       * Object-using code they can't readily get out of the class hierarchy.
       */
      problems.add(classType, "In order to produce smaller client-side code, 'Object' is not "
          + "allowed; please use a more specific type", Priority.DEFAULT);
      tic = ensureTypeInfoComputed(classType, path);
      tic.setInstantiable(false);
      return tic;
    }

    if (classType.isRawType() != null) {
      localLogger
          .log(
              TreeLogger.DEBUG,
              "Type '"
                  + classType.getQualifiedSourceName()
                  + "' should be parameterized to help the compiler produce the smallest code size possible for your module",
              null);
    }

    JClassType originalType = (JClassType) type;

    // TreeLogger subtypesLogger = localLogger.branch(TreeLogger.DEBUG,
    // "Analyzing subclasses:", null);
    tic = ensureTypeInfoComputed(classType, path);
    Set<JClassType> instantiableTypes = new HashSet<JClassType>();
    boolean anySubtypes =
        checkSubtypes(localLogger, originalType, instantiableTypes, path, problems);
    if (!tic.isDone()) {
      tic.setInstantiableSubtypes(anySubtypes);
      tic.setInstantiable(false);
    }
    // Don't publish this until complete to ensure nobody depends on partial results.
    tic.instantiableTypes = instantiableTypes;
    return tic;
  }

  int getTypeParameterExposure(JGenericType type, int index) {
    return getFlowInfo(type, index).getExposure();
  }

  /**
   * Returns <code>true</code> if the fields of the type should be considered
   * for serialization.
   *
   * Default access to allow for testing.
   */
  boolean shouldConsiderFieldsForSerialization(JClassType type, ProblemReport problems) {
    return shouldConsiderFieldsForSerialization(type, typeFilter, problems);
  }

  private void assertNothingPending() {
    if (getClass().desiredAssertionStatus()) {
      for (TypeInfoComputed tic : typeToTypeInfoComputed.values()) {
        assert (!tic.isPendingInstantiable());
      }
    }
  }

  /**
   * Consider any subtype of java.lang.Object which qualifies for serialization.
   */
  private void checkAllSubtypesOfObject(TreeLogger logger, TypePath parent, ProblemReport problems) {
    if (alreadyCheckedObject) {
      return;
    }
    alreadyCheckedObject = true;

    /*
     * This will pull in the world and the set of serializable types will be
     * larger than it needs to be. We exclude types that do not qualify for
     * serialization to avoid generating false errors due to types that do not
     * qualify for serialization and have no serializable subtypes.
     */
    TreeLogger localLogger =
        logger.branch(TreeLogger.WARN,
            "Checking all subtypes of Object which qualify for serialization", null);
    JClassType[] allTypes = typeOracle.getJavaLangObject().getSubtypes();
    for (JClassType cls : allTypes) {
      if (isDeclaredSerializable(cls)) {
        computeTypeInstantiability(localLogger, cls, TypePaths.createSubtypePath(parent, cls,
            typeOracle.getJavaLangObject()), problems);
      }
    }
  }

  private TypeInfoComputed checkArrayInstantiable(TreeLogger logger, JArrayType array,
      TypePath path, ProblemReport problems) {

    JType leafType = array.getLeafType();
    JWildcardType leafWild = leafType.isWildcard();
    if (leafWild != null) {
      JArrayType arrayType = getArrayType(typeOracle, array.getRank(), leafWild.getUpperBound());
      return checkArrayInstantiable(logger, arrayType, path, problems);
    }

    TypeInfoComputed tic = ensureTypeInfoComputed(array, path);
    if (tic.isDone() || tic.isPendingInstantiable()) {
      return tic;
    }
    tic.setPendingInstantiable();

    JTypeParameter isLeafTypeParameter = leafType.isTypeParameter();
    if (isLeafTypeParameter != null && !typeParametersInRootTypes.contains(isLeafTypeParameter)) {
      // Don't deal with non root type parameters, but make a TIC entry to
      // save time if it recurs. We assume they're indirectly instantiable.
      tic.setInstantiableSubtypes(true);
      tic.setInstantiable(false);
      return tic;
    }

    if (!isAllowedByFilter(array, problems)) {
      // Don't deal with filtered out types either, but make a TIC entry to
      // save time if it recurs. We assume they're not instantiable.
      tic.setInstantiable(false);
      return tic;
    }

    // An array is instantiable provided that any leaf subtype is instantiable.
    // (Ignores the possibility of empty arrays of non-instantiable types.)

    TreeLogger branch = logger.branch(TreeLogger.DEBUG, "Analyzing component type:", null);

    TypeInfoComputed leafTic =
        computeTypeInstantiability(branch, leafType, TypePaths
            .createArrayComponentPath(array, path), problems);
    boolean succeeded = leafTic.hasInstantiableSubtypes();

    tic.setInstantiable(succeeded);
    return tic;
  }

  /**
   * Returns <code>true</code> if the declared fields of this type are all
   * instantiable. As a side-effect it fills in {@link TypeInfoComputed} for all
   * necessary types.
   */
  private boolean checkDeclaredFields(TreeLogger logger, TypeInfoComputed typeInfo,
      TypePath parent, ProblemReport problems) {

    JClassType classOrInterface = (JClassType) typeInfo.getType();
    if (classOrInterface.isEnum() != null) {
      // The fields of an enum are never serialized; they are always okay.
      return true;
    }

    JClassType baseType = getBaseType(classOrInterface);

    boolean allSucceeded = true;
    // TODO: Propagating the constraints will produce better results as long
    // as infinite expansion can be avoided in the process.
    JField[] fields = baseType.getFields();
    if (fields.length > 0) {
      TreeLogger localLogger =
          logger.branch(TreeLogger.DEBUG, "Analyzing the fields of type '"
              + classOrInterface.getParameterizedQualifiedSourceName()
              + "' that qualify for serialization", null);

      for (JField field : fields) {
        if (!shouldConsiderForSerialization(localLogger, suppressNonStaticFinalFieldWarnings, field)) {
          continue;
        }

        TreeLogger fieldLogger = localLogger.branch(TreeLogger.DEBUG, field.toString(), null);
        JType fieldType = field.getType();

        TypePath path = TypePaths.createFieldPath(parent, field);
        if (typeInfo.isManuallySerializable()
            && fieldType.getLeafType() == typeOracle.getJavaLangObject()) {
          checkAllSubtypesOfObject(fieldLogger.branch(TreeLogger.WARN,
              "Object was reached from a manually serializable type", null), path, problems);
        } else {
          allSucceeded &=
              computeTypeInstantiability(fieldLogger, fieldType, path, problems)
                  .hasInstantiableSubtypes();
        }
      }
    }

    boolean succeeded = allSucceeded || typeInfo.isManuallySerializable();
    if (succeeded) {
      typeInfo.setFieldSerializable();
    }

    return succeeded;
  }

  private boolean checkSubtype(TreeLogger logger, JClassType classOrInterface,
      JClassType originalType, TypePath parent, ProblemReport problems) {
    if (classOrInterface.isEnum() != null) {
      // The fields of an enum are never serialized; they are always okay.
      return true;
    }

    JParameterizedType isParameterized = classOrInterface.isParameterized();
    if (isParameterized != null) {
      if (isRawMapOrRawCollection(classOrInterface)) {
        /*
         * Backwards compatibility. Raw collections or maps force all object
         * subtypes to be considered.
         */
        checkAllSubtypesOfObject(logger, parent, problems);
      } else {
        TreeLogger paramsLogger =
            logger.branch(TreeLogger.DEBUG, "Checking parameters of '"
                + isParameterized.getParameterizedQualifiedSourceName() + "'");

        for (JTypeParameter param : isParameterized.getBaseType().getTypeParameters()) {
          if (!checkTypeArgument(paramsLogger, isParameterized.getBaseType(), param.getOrdinal(),
              isParameterized.getTypeArgs()[param.getOrdinal()], parent, problems)) {
            return false;
          }
        }
      }
    }

    // Check all super type fields first (recursively).
    JClassType superType = classOrInterface.getSuperclass();
    if (superType != null && superType.isRawType() != null) {
      superType = superType.isRawType().asParameterizedByWildcards();
    }

    if (superType != null && isDeclaredSerializable(superType)) {
      superType = constrainTypeBy(superType, originalType);
      if (superType == null) {
        return false;
      }

      boolean superTypeOk = false;
      superTypeOk =
          checkSubtype(logger, superType, originalType, TypePaths.createSupertypePath(parent,
              superType, classOrInterface), problems);

      /*
       * If my super type did not check out, then I am not instantiable and we
       * should error out... UNLESS I amdirectly serializable myself, in which
       * case it's ok for me to be the root of a new instantiable hierarchy.
       */
      if (!superTypeOk && !isDirectlySerializable(classOrInterface)) {
        return false;
      }
    }

    TypeInfoComputed tic = ensureTypeInfoComputed(classOrInterface, parent);
    return checkDeclaredFields(logger, tic, parent, problems);
  }

  /**
   * Returns <code>true</code> if this type or one of its subtypes is
   * instantiable relative to a known base type.
   */
  private boolean checkSubtypes(TreeLogger logger, JClassType originalType,
      Set<JClassType> instSubtypes, TypePath path, ProblemReport problems) {
    JRealClassType baseType = getBaseType(originalType);
    TreeLogger computationLogger =
        logger.branch(TreeLogger.DEBUG, "Finding possibly instantiable subtypes");
    List<JClassType> candidates =
        getPossiblyInstantiableSubtypes(computationLogger, baseType, problems);
    boolean anySubtypes = false;

    TreeLogger verificationLogger = logger.branch(TreeLogger.DEBUG, "Verifying instantiability");
    for (JClassType candidate : candidates) {
      if (getBaseType(candidate) == baseType && originalType.isRawType() == null) {
        // Don't rely on the constrainer when we have perfect information.
        candidate = originalType;
      } else {
        candidate = constrainTypeBy(candidate, originalType);
        if (candidate == null) {
          continue;
        }
      }

      if (!isAllowedByFilter(candidate, problems)) {
        continue;
      }

      TypePath subtypePath = TypePaths.createSubtypePath(path, candidate, originalType);
      TypeInfoComputed tic = ensureTypeInfoComputed(candidate, subtypePath);
      if (tic.isDone()) {
        if (tic.isInstantiable()) {
          anySubtypes = true;
          instSubtypes.add(candidate);
        }
        continue;
      } else if (tic.isPendingInstantiable()) {
        anySubtypes = true;
        instSubtypes.add(candidate);
        continue;
      }
      tic.setPendingInstantiable();

      TreeLogger subtypeLogger =
          verificationLogger.branch(TreeLogger.DEBUG, candidate
              .getParameterizedQualifiedSourceName());
      boolean instantiable =
          checkSubtype(subtypeLogger, candidate, originalType, subtypePath, problems);
      anySubtypes |= instantiable;
      tic.setInstantiable(instantiable);

      if (instantiable) {
        subtypeLogger.branch(TreeLogger.DEBUG, "Is instantiable");
      }

      // Note we are leaving hasInstantiableSubtypes() as false which might be
      // wrong but it is only used by arrays and thus it will never be looked at
      // for this tic.
      if (instantiable) {
        instSubtypes.add(candidate);
      }
    }

    return anySubtypes;
  }

  /**
   * Check the argument to a parameterized type to see if it will make the type
   * it is applied to be serializable. As a side effect, populates
   * {@link #typeToTypeInfoComputed} in the same way as
   * {@link #computeTypeInstantiability}.
   *
   * @param baseType - The generic type the parameter is on
   * @param paramIndex - The index of the parameter in the generic type
   * @param typeArg - An upper bound on the actual argument being applied to the
   *          generic type
   *
   * @return Whether the a parameterized type can be serializable if
   *         <code>baseType</code> is the base type and the
   *         <code>paramIndex</code>th type argument is a subtype of
   *         <code>typeArg</code>.
   */
  private boolean checkTypeArgument(TreeLogger logger, JGenericType baseType, int paramIndex,
      JClassType typeArg, TypePath parent, ProblemReport problems) {
    JWildcardType isWildcard = typeArg.isWildcard();
    if (isWildcard != null) {
      return checkTypeArgument(logger, baseType, paramIndex, isWildcard.getUpperBound(), parent,
          problems);
    }

    JArrayType typeArgAsArray = typeArg.isArray();
    if (typeArgAsArray != null) {
      JTypeParameter parameterOfTypeArgArray = typeArgAsArray.getLeafType().isTypeParameter();
      if (parameterOfTypeArgArray != null) {
        JGenericType declaringClass = parameterOfTypeArgArray.getDeclaringClass();
        if (declaringClass != null) {
          TypeParameterFlowInfo flowInfoForArrayParam =
              getFlowInfo(declaringClass, parameterOfTypeArgArray.getOrdinal());
          TypeParameterFlowInfo otherFlowInfo = getFlowInfo(baseType, paramIndex);
          if (otherFlowInfo.getExposure() >= 0
              && otherFlowInfo.isTransitivelyAffectedBy(flowInfoForArrayParam)) {
            problems.add(baseType, "Cannot serialize type '"
                + baseType.getParameterizedQualifiedSourceName()
                + "' when given an argument of type '"
                + typeArg.getParameterizedQualifiedSourceName()
                + "' because it appears to require serializing arrays " + "of unbounded dimension",
                Priority.DEFAULT);
            return false;
          }
        }
      }
    }

    TypePath path = TypePaths.createTypeArgumentPath(parent, baseType, paramIndex, typeArg);
    int exposure = getTypeParameterExposure(baseType, paramIndex);
    switch (exposure) {
      case TypeParameterExposureComputer.EXPOSURE_DIRECT: {
        TreeLogger branch =
            logger.branch(TreeLogger.DEBUG, "Checking type argument " + paramIndex + " of type '"
                + baseType.getParameterizedQualifiedSourceName()
                + "' because it is directly exposed in this type or in one of its subtypes");
        return computeTypeInstantiability(branch, typeArg, path, problems)
            .hasInstantiableSubtypes()
            || mightNotBeExposed(baseType, paramIndex);
      }
      case TypeParameterExposureComputer.EXPOSURE_NONE:
        // Ignore this argument
        logger.log(TreeLogger.DEBUG, "Ignoring type argument " + paramIndex + " of type '"
            + baseType.getParameterizedQualifiedSourceName()
            + "' because it is not exposed in this or any subtype");
        return true;

      default: {
        assert (exposure >= TypeParameterExposureComputer.EXPOSURE_MIN_BOUNDED_ARRAY);
        problems.add(getArrayType(typeOracle, exposure, typeArg), "Checking type argument "
            + paramIndex + " of type '" + baseType.getParameterizedQualifiedSourceName()
            + "' because it is exposed as an array with a maximum dimension of " + exposure
            + " in this type or one of its subtypes", Priority.AUXILIARY);
        return computeTypeInstantiability(logger, getArrayType(typeOracle, exposure, typeArg),
            path, problems).hasInstantiableSubtypes()
            || mightNotBeExposed(baseType, paramIndex);
      }
    }
  }

  /**
   * If <code>type</code>'s base class does not inherit from
   * <code>superType</code>'s base class, then return <code>type</code>
   * unchanged. If it does, then return a subtype of <code>type</code> that
   * includes all values in both <code>type</code> and <code>superType</code>.
   * If there are definitely no such values, return <code>null</code>.
   */
  private JClassType constrainTypeBy(JClassType type, JClassType superType) {
    return typeConstrainer.constrainTypeBy(type, superType);
  }

  private TypeParameterFlowInfo getFlowInfo(JGenericType type, int index) {
    return typeParameterExposureComputer.computeTypeParameterExposure(type, index);
  }

  /**
   * Returns the subtypes of a given base type as parameterized by wildcards.
   */
  private List<JClassType> getPossiblyInstantiableSubtypes(TreeLogger logger,
      JRealClassType baseType, ProblemReport problems) {
    assert (baseType == getBaseType(baseType));

    List<JClassType> possiblyInstantiableTypes = new ArrayList<JClassType>();
    if (baseType == typeOracle.getJavaLangObject()) {
      return possiblyInstantiableTypes;
    }

    List<JClassType> candidates = new ArrayList<JClassType>();
    candidates.add(baseType);
    List<JClassType> subtypes = Arrays.asList(baseType.getSubtypes());
    candidates.addAll(subtypes);

    for (JClassType subtype : candidates) {
      JClassType subtypeBase = getBaseType(subtype);
      if (maybeInstantiable(logger, subtypeBase, problems)) {
        /*
         * Convert the generic type into a parameterization that only includes
         * wildcards.
         */
        JGenericType isGeneric = subtype.isGenericType();
        if (isGeneric != null) {
          subtype = isGeneric.asParameterizedByWildcards();
        } else {
          assert (subtype instanceof JRealClassType);
        }

        possiblyInstantiableTypes.add(subtype);
      }
    }

    if (possiblyInstantiableTypes.size() == 0) {
      String possibilities[] = new String[candidates.size()];
      for (int i = 0; i < possibilities.length; i++) {
        JClassType subtype = candidates.get(i);
        String worstMessage = problems.getWorstMessageForType(subtype);
        if (worstMessage == null) {
          possibilities[i] =
              "   subtype " + subtype.getParameterizedQualifiedSourceName()
                  + " is not instantiable";
        } else {
          // message with have the form "FQCN some-problem-description"
          possibilities[i] = "   subtype " + worstMessage;
        }
      }
      problems.add(baseType, baseType.getParameterizedQualifiedSourceName()
          + " has no available instantiable subtypes.", Priority.DEFAULT, possibilities);
    }
    return possiblyInstantiableTypes;
  }

  private TypeInfoComputed ensureTypeInfoComputed(JType type, TypePath path) {
    TypeInfoComputed tic = typeToTypeInfoComputed.get(type);
    if (tic == null) {
      tic = new TypeInfoComputed(type, path, typeOracle);
      typeToTypeInfoComputed.put(type, tic);
    }
    return tic;
  }

  private boolean isAllowedByFilter(JClassType classType, ProblemReport problems) {
    return isAllowedByFilter(typeFilter, classType, problems);
  }

  /**
   * Returns <code>true</code> if the type is Collection<? extends Object> or
   * Map<? extends Object, ? extends Object>.
   */
  private boolean isRawMapOrRawCollection(JClassType type) {
    if (type.asParameterizationOf(collectionClass) == collectionClass.asParameterizedByWildcards()) {
      return true;
    }

    if (type.asParameterizationOf(mapClass) == mapClass.asParameterizedByWildcards()) {
      return true;
    }

    return false;
  }

  private void logPath(TreeLogger logger, TypePath path) {
    if (path == null) {
      return;
    }

    if (logger.isLoggable(TreeLogger.DEBUG)) {
      logger.log(TreeLogger.DEBUG, path.toString());
    }
    logPath(logger, path.getParent());
  }

  private void logReachableTypes(TreeLogger logger) {
    if (!context.isProdMode() && !logger.isLoggable(TreeLogger.DEBUG)) {
      return;
    }

    if (logOutputWriter != null) {
      // Route the TreeLogger output to an output stream.
      PrintWriterTreeLogger printWriterTreeLogger = new PrintWriterTreeLogger(logOutputWriter);
      printWriterTreeLogger.setMaxDetail(TreeLogger.ALL);
      logger = printWriterTreeLogger;
    }

    if (logger.isLoggable(TreeLogger.DEBUG)) {
      logger.log(TreeLogger.DEBUG, "Reachable types computed on: " + new Date().toString());
    }
    Set<JType> keySet = typeToTypeInfoComputed.keySet();
    JType[] types = keySet.toArray(new JType[0]);
    Arrays.sort(types, JTYPE_COMPARATOR);

    for (JType type : types) {
      TypeInfoComputed tic = typeToTypeInfoComputed.get(type);
      assert (tic != null);

      TreeLogger typeLogger =
          logger.branch(TreeLogger.DEBUG, tic.getType().getParameterizedQualifiedSourceName());
      TreeLogger serializationStatus = typeLogger.branch(TreeLogger.DEBUG, "Serialization status");
      if (tic.isInstantiable()) {
        serializationStatus.branch(TreeLogger.DEBUG, "Instantiable");
      } else {
        if (tic.isFieldSerializable()) {
          serializationStatus.branch(TreeLogger.DEBUG, "Field serializable");
        } else {
          serializationStatus.branch(TreeLogger.DEBUG, "Not serializable");
        }
      }

      TreeLogger pathLogger = typeLogger.branch(TreeLogger.DEBUG, "Path");

      logPath(pathLogger, tic.getPath());
      logger.log(TreeLogger.DEBUG, "");
    }

    if (logOutputWriter != null) {
      logOutputWriter.flush();
    }
  }

  /**
   * Mark arrays of <code>leafType</code> as instantiable, for arrays of
   * dimension up to <code>maxRank</code>.
   */
  private void markArrayTypesInstantiable(JType leafType, int maxRank, TypePath path) {
    for (int rank = 1; rank <= maxRank; ++rank) {
      JArrayType covariantArray = getArrayType(typeOracle, rank, leafType);

      TypeInfoComputed covariantArrayTic = ensureTypeInfoComputed(covariantArray, path);
      covariantArrayTic.setInstantiable(true);
    }
  }

  /**
   * Marks all covariant and lesser-ranked arrays as instantiable for all leaf types between
   * the given array's leaf type and its instantiable subtypes. (Note: this adds O(S * R)
   * array types to the output where S is the number of subtypes and R is the rank.)
   * Prerequisite: The leaf type's tic and its subtypes must already be created.
   * @see #checkArrayInstantiable
   */
  private void markArrayTypes(TreeLogger logger, JArrayType array, TypePath path,
      ProblemReport problems) {
    logger = logger.branch(TreeLogger.DEBUG, "Adding array types for " + array);

    JType leafType = array.getLeafType();
    JTypeParameter isLeafTypeParameter = leafType.isTypeParameter();
    if (isLeafTypeParameter != null) {
      if (typeParametersInRootTypes.contains(isLeafTypeParameter)) {
        leafType = isLeafTypeParameter.getFirstBound(); // to match computeTypeInstantiability
      } else {
        // skip non-root leaf parameters, to match checkArrayInstantiable
        return;
      }
    }

    TypeInfoComputed leafTic = typeToTypeInfoComputed.get(leafType);
    if (leafTic == null) {
      problems.add(array, "internal error: leaf type not computed: " +
          leafType.getQualifiedSourceName(), Priority.FATAL);
      return;
    }

    if (leafType.isClassOrInterface() == null) {
      // Simple case: no covariance, just lower ranks.
      assert leafType.isPrimitive() != null;
      markArrayTypesInstantiable(leafType, array.getRank(), path);
      return;
    }

    JRealClassType baseClass = getBaseType(leafType.isClassOrInterface());

    TreeLogger covariantArrayLogger =
        logger.branch(TreeLogger.DEBUG, "Covariant array types:");

    Set<JClassType> instantiableTypes = leafTic.instantiableTypes;
    if (instantiableTypes == null) {
      // The types are there (due to a supertype) but the Set wasn't computed, so compute it now.
      instantiableTypes = new HashSet<JClassType>();
      List<JClassType> candidates =
          getPossiblyInstantiableSubtypes(logger, baseClass, problems);
      for (JClassType candidate : candidates) {
        TypeInfoComputed tic = typeToTypeInfoComputed.get(candidate);
        if (tic != null && tic.instantiable) {
          instantiableTypes.add(candidate);
        }
      }
    }
    for (JClassType instantiableType : TypeHierarchyUtils.getAllTypesBetweenRootTypeAndLeaves(
        baseClass, instantiableTypes)) {
      if (!isAccessibleToSerializer(instantiableType)) {
        // Skip types that are not accessible from a serializer
        continue;
      }

      if (covariantArrayLogger.isLoggable(TreeLogger.DEBUG)) {
        covariantArrayLogger.branch(TreeLogger.DEBUG, getArrayType(typeOracle, array.getRank(),
            instantiableType).getParameterizedQualifiedSourceName());
      }

      markArrayTypesInstantiable(instantiableType, array.getRank(), path);
    }
  }

  private boolean maybeInstantiable(TreeLogger logger, JClassType type, ProblemReport problems) {
    boolean success =
        canBeInstantiated(type, problems) && shouldConsiderFieldsForSerialization(type, problems);
    if (success) {
      if (logger.isLoggable(TreeLogger.DEBUG)) {
        logger.log(TreeLogger.DEBUG, type.getParameterizedQualifiedSourceName()
            + " might be instantiable");
      }
    }
    return success;
  }

  /**
   * Report problems if they are fatal or we're debugging.
   */
  private void maybeReport(TreeLogger logger, ProblemReport problems) {
    if (problems.hasFatalProblems()) {
      problems.reportFatalProblems(logger, TreeLogger.ERROR);
    }
    // if entrySucceeded, there may still be "warning" problems, but too
    // often they're expected (e.g. non-instantiable subtypes of List), so
    // we log them at DEBUG.
    // TODO(fabbott): we could blacklist or graylist those types here, so
    // instantiation during code generation would flag them for us.
    problems.report(logger, TreeLogger.DEBUG, TreeLogger.DEBUG);
  }

  private boolean mightNotBeExposed(JGenericType baseType, int paramIndex) {
    TypeParameterFlowInfo flowInfo = getFlowInfo(baseType, paramIndex);
    return flowInfo.getMightNotBeExposed() || isManuallySerializable(baseType);
  }

  /**
   * Remove serializable types that were visited due to speculative paths but
   * are not really needed for serialization.
   *
   * NOTE: This is currently much more limited than it should be. For example, a
   * path sensitive prune could remove instantiable types also.
   */
  private void pruneUnreachableTypes() {
    /*
     * Record all supertypes of any instantiable type, whether or not they are
     * field serialziable.
     */
    Set<JType> supersOfInstantiableTypes = new LinkedHashSet<JType>();
    for (TypeInfoComputed tic : typeToTypeInfoComputed.values()) {
      if (tic.isInstantiable() && tic.getType() instanceof JClassType) {
        JClassType sup = (JClassType) tic.getType().getErasedType();
        while (sup != null) {
          supersOfInstantiableTypes.add(sup.getErasedType());
          sup = sup.getErasedType().getSuperclass();
        }
      }
    }

    /*
     * Record any field serializable type that is not in the supers of any
     * instantiable type.
     */
    Set<JType> toKill = new LinkedHashSet<JType>();
    for (TypeInfoComputed tic : typeToTypeInfoComputed.values()) {
      if (tic.isFieldSerializable()
          && !supersOfInstantiableTypes.contains(tic.getType().getErasedType())) {
        toKill.add(tic.getType());
      }
    }

    /*
     * Remove any field serializable supers that cannot be reached from an
     * instantiable type.
     */
    for (JType type : toKill) {
      typeToTypeInfoComputed.remove(type);
    }
  }
}
