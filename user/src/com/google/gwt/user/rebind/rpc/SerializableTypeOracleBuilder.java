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
package com.google.gwt.user.rebind.rpc;

import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.typeinfo.JArrayType;
import com.google.gwt.core.ext.typeinfo.JBound;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JPackage;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.JWildcardType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.user.client.rpc.IncompatibleRemoteServiceException;
import com.google.gwt.user.client.rpc.IsSerializable;
import com.google.gwt.user.client.rpc.SerializationStreamReader;
import com.google.gwt.user.client.rpc.SerializationStreamWriter;

import java.io.Serializable;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;

/**
 * Builds a {@link SerializableTypeOracle} for a given
 * {@link com.google.gwt.user.client.rpc.RemoteService RemoteService} interface.
 * 
 * <h4>Background</h4>
 * </p>
 * There are two goals for this builder. First, discover the set serializable
 * types that can be exchanged between client and server code over a given
 * {@link com.google.gwt.user.client.rpc.RemoteService RemoteService} interface.
 * Second, to make sure that all types which qualify for serializability
 * actually adhere to the constraints for the particular type of serializability
 * which applies to them.
 * 
 * </p>
 * This builder starts from the set of methods that are declared or inherited by
 * the RemoteService interface. It then traverses the type hierarchy of each of
 * the types in these method signatures in order to discover additional types
 * which it might need to include. For the purposes of this explanation we
 * define a root type to be any type which appears in the RemoteService method
 * signatures or the type of any non-final, instance field which is part of a
 * type that qualifies for serialization. The builder will fail if a root is not
 * serializable and it has no subtypes that are.
 * 
 * </p>
 * A type qualifies for serialization if it is automatically or manually
 * serializable. Automatic serialization is selected if the type is assignable
 * to {@link IsSerializable} or {@link Serializable} or if the type is a
 * primitive type such as int, boolean, etc. Manual serialization is selected if
 * there exists another type with the same fully qualified name concatenated
 * with "_CustomFieldSerializer". If a type qualifies for both manual and
 * automatic serialization, manual serialization is preferred.
 * 
 * </p>
 * If any of the checks described in the following sections fail, the build
 * process will fail.
 * 
 * <h4>Root Types</h4>
 * <ul>
 * <li>If not parameterized and it is assignable to Map or Collection, emit a
 * warning and check all serializable subtypes of object.</li>
 * <li>If parameterized check all subtypes of the type arguments.</li>
 * <li>Check all subtypes of the raw type</li>
 * <li>If not parameterized and not assignable to Map or Collection, check the
 * root type and all of its subtypes</li>
 * </ul>
 * 
 * <h4>Classes</h4>
 * <ul>
 * <li>Does not qualify for serialization</li>
 * <ul>
 * <li>If asked to check subtypes, check all subclasses; must have one
 * serializable subtype</li>
 * </ul>
 * <li>Qualifies for Auto Serialization
 * <ul>
 * <li>If superclass qualifies for serialization check it</li>
 * <li>Check the type of every non-final instance field</li>
 * <li>If class is local or nested and not static ignore it</li>
 * <li>If class is not abstract and not default instantiable fail</li>
 * <li>If class is not part of the JRE, warn if it uses native methods</li>
 * <li>If asked to check subtypes, check all subclasses</li>
 * </ul>
 * </li>
 * <li>Qualifies for Manual Serialization
 * <ul>
 * <li>If superclass qualifies for serialization check it</li>
 * <li>Check the type of every non-final instance field</li>
 * <li>Check that the CustomFieldSerializer meets the following criteria:
 * <ul>
 * <li>A deserialize method whose signature is: 'public static void
 * deserialize({@link SerializationStreamReader}, &lt;T&gt; instance)'</li>
 * <li>A serialize method whose signature is: 'public static void serialize({@link SerializationStreamWriter},
 * &lt;T&gt; instance)'</li>
 * <li>It the class is not abstract, not default instantiable the custom field
 * serializer must implement an instantiate method whose signature is: 'public
 * static &lt;T&gt; instantiate(SerializationStreamReader)'</li>
 * </ul>
 * </ul>
 * </li>
 * </ul>
 * 
 * <h4>Arrays</h4>
 * <ul>
 * <li>Check the leaf type of the array; it must be serializable or have a
 * subtype that is.</li>
 * <li>All covariant array types are included.</li>
 * </ul>
 */
public class SerializableTypeOracleBuilder {

  private class TypeInfo {
    /**
     * <code>true</code> if the type is assignable to {@link IsSerializable}
     * or {@link java.io.Serializable Serializable}.
     */
    private final boolean autoSerializable;

    /**
     * <code>true</code> if the this type directly implements one of the
     * marker interfaces.
     */
    private final boolean directlyImplementsMarker;

    /**
     * Custom field serializer or <code>null</code> if there isn't one.
     */
    private final JClassType manualSerializer;

    TypeInfo(JClassType type) {
      autoSerializable = type.isAssignableTo(isSerializableClass)
          || type.isAssignableTo(serializableClass);
      manualSerializer = findCustomFieldSerializer(typeOracle, type);
      directlyImplementsMarker = directlyImplementsInterface(type,
          isSerializableClass)
          || directlyImplementsInterface(type, serializableClass);
    }

    public JClassType getManualSerializer() {
      return manualSerializer;
    }

    public boolean isAutoSerializable() {
      return autoSerializable;
    }

    public boolean isDeclaredSerializable() {
      return autoSerializable || isManuallySerializable();
    }

    public boolean isDirectlySerializable() {
      return directlyImplementsMarker || isManuallySerializable();
    }

    public boolean isManuallySerializable() {
      return manualSerializer != null;
    }
  }

  private static class TypeInfoComputed {
    /**
     * An issue that prevents a type from being serializable.
     */
    private static class SerializationIssue implements
        Comparable<SerializationIssue> {
      public final boolean isSpeculative;
      public final String issueMessage;

      SerializationIssue(boolean isSpeculative, String issueMessage) {
        this.isSpeculative = isSpeculative;
        this.issueMessage = issueMessage;
      }

      public int compareTo(SerializationIssue other) {
        if (isSpeculative == other.isSpeculative) {
          return issueMessage.compareTo(other.issueMessage);
        }

        if (isSpeculative && !other.isSpeculative) {
          return -1;
        }

        return 1;
      }
    }

    /**
     * Represents the state of a type while we are determining the set of
     * serializable types.
     */
    private static final class TypeState {
      private final String state;

      protected TypeState(String state) {
        this.state = state;
      }

      @Override
      public String toString() {
        return state;
      }
    }

    /**
     * The instantiability of a type has been determined.
     */
    static final TypeState CHECK_DONE = new TypeState("Check succeeded");

    /**
     * The instantiability of a type is being checked.
     */
    static final TypeState CHECK_IN_PROGRESS = new TypeState(
        "Check in progress");

    /**
     * The instantiability of a type has not been checked.
     */
    static final TypeState NOT_CHECKED = new TypeState("Not checked");

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
     * List of serialization warnings or errors that prevent this type from
     * being serializable.
     */
    private Set<SerializationIssue> serializationIssues = new TreeSet<SerializationIssue>();

    /**
     * The state that this type is currently in.
     */
    private TypeState state = NOT_CHECKED;

    /**
     * {@link JClassType} associated with this metadata.
     */
    private final JClassType type;

    public TypeInfoComputed(JClassType type) {
      this.type = type;
    }

    public void addSerializationIssue(boolean isSpeculative, String issueMessage) {
      serializationIssues.add(new SerializationIssue(isSpeculative,
          issueMessage));
    }

    public JClassType getType() {
      return type;
    }

    public boolean isDone() {
      return state == CHECK_DONE;
    }

    public boolean isFieldSerializable() {
      return fieldSerializable;
    }

    public boolean isInstantiable() {
      return instantiable;
    }

    public boolean isPendingInstantiable() {
      return state == CHECK_IN_PROGRESS;
    }

    public void logReasonsForUnserializability(TreeLogger logger) {
      for (SerializationIssue serializationIssue : serializationIssues) {
        logger.branch(getLogLevel(serializationIssue.isSpeculative),
            serializationIssue.issueMessage, null);
      }
    }

    public void setFieldSerializable() {
      fieldSerializable = true;
    }

    public void setInstantiable(boolean instantiable) {
      this.instantiable = instantiable;
      if (instantiable) {
        fieldSerializable = true;
      }
      state = CHECK_DONE;
    }

    public void setPendingInstantiable() {
      state = CHECK_IN_PROGRESS;
    }
  }

  /**
   * Compares {@link JClassType}s according to their qualified source names.
   */
  private static final Comparator<JClassType> JCLASS_TYPE_COMPARATOR = new Comparator<JClassType>() {
    public int compare(JClassType t1, JClassType t2) {
      return t1.getQualifiedSourceName().compareTo(
          t2.getQualifiedSourceName());
    }
  };

  /**
   * Finds the custom field serializer for a given type.
   * 
   * @param typeOracle
   * @param type
   * @return the custom field serializer for a type or <code>null</code> if
   *         there is not one
   */
  static JClassType findCustomFieldSerializer(TypeOracle typeOracle, JType type) {
    JClassType classOrInterface = type.isClassOrInterface();
    if (classOrInterface == null) {
      return null;
    }

    String customFieldSerializerName = type.getQualifiedSourceName()
        + "_CustomFieldSerializer";
    JClassType customSerializer = typeOracle.findType(customFieldSerializerName);
    if (customSerializer == null) {
      // If the type is in the java.lang or java.util packages then it will be
      // mapped into com.google.gwt.user.client.rpc.core package
      customSerializer = typeOracle.findType("com.google.gwt.user.client.rpc.core."
          + customFieldSerializerName);
    }

    return customSerializer;
  }

  private static void addEdge(Map<JClassType, List<JClassType>> adjList,
      JClassType subclass, JClassType clazz) {
    List<JClassType> edges = adjList.get(subclass);
    if (edges == null) {
      edges = new ArrayList<JClassType>();
      adjList.put(subclass, edges);
    }

    edges.add(clazz);
  }

  private static void depthFirstSearch(Set<JClassType> seen,
      Map<JClassType, List<JClassType>> adjList, JClassType type) {
    if (seen.contains(type)) {
      return;
    }
    seen.add(type);

    List<JClassType> children = adjList.get(type);
    if (children != null) {
      for (JClassType child : children) {
        if (!seen.contains(child)) {
          depthFirstSearch(seen, adjList, child);
        }
      }
    }
  }

  /**
   * Returns <code>true</code> if the type directly implements the specified
   * interface.
   * 
   * @param type type to check
   * @param intf interface to look for
   * @return <code>true</code> if the type directly implements the specified
   *         interface
   */
  private static boolean directlyImplementsInterface(JClassType type,
      JClassType intf) {
    return directlyImplementsInterfaceRecursive(new HashSet<JClassType>(),
        type, intf);
  }

  private static boolean directlyImplementsInterfaceRecursive(
      Set<JClassType> seen, JClassType clazz, JClassType intf) {

    if (clazz == intf) {
      return true;
    }

    JClassType[] intfImpls = clazz.getImplementedInterfaces();

    for (JClassType intfImpl : intfImpls) {
      if (!seen.contains(intfImpl)) {
        seen.add(intfImpl);

        if (directlyImplementsInterfaceRecursive(seen, intfImpl, intf)) {
          return true;
        }
      }
    }

    return false;
  }

  /**
   * Returns all types on the path from the root type to the serializable
   * leaves.
   * 
   * @param root the root type
   * @param leaves the set of serializable leaf types
   * @return all types on the path from the root type to the serializable leaves
   */
  private static List<JClassType> getAllTypesBetweenRootTypeAndLeaves(
      JClassType root, List<JClassType> leaves) {
    Map<JClassType, List<JClassType>> adjList = getInvertedTypeHierarchy(root);
    Set<JClassType> types = new HashSet<JClassType>();

    for (JClassType type : leaves) {
      depthFirstSearch(types, adjList, type);
    }

    return Arrays.asList(types.toArray(new JClassType[0]));
  }

  private static JArrayType getArrayType(TypeOracle typeOracle, int rank,
      JType component) {
    assert (rank > 0);

    JArrayType array = null;
    JType currentComponent = component;
    for (int i = 0; i < rank; ++i) {
      array = typeOracle.getArrayType(currentComponent);
      currentComponent = array;
    }

    return array;
  }

  /**
   * Given a root type return an adjacency list that is the inverted type
   * hierarchy.
   */
  private static Map<JClassType, List<JClassType>> getInvertedTypeHierarchy(
      JClassType root) {
    Map<JClassType, List<JClassType>> adjList = new HashMap<JClassType, List<JClassType>>();
    Set<JClassType> seen = new HashSet<JClassType>();
    Stack<JClassType> queue = new Stack<JClassType>();
    queue.push(root);
    while (!queue.isEmpty()) {
      JClassType clazz = queue.pop();
      JClassType[] subclasses = clazz.getSubtypes();

      if (seen.contains(clazz)) {
        continue;
      }
      seen.add(clazz);

      for (JClassType subclass : subclasses) {
        if (clazz.isInterface() != null) {
          if (directlyImplementsInterface(subclass, clazz)) {
            addEdge(adjList, subclass, clazz);
            queue.push(subclass);
          }
        } else {
          if (subclass.getSuperclass() == clazz) {
            addEdge(adjList, subclass, clazz);
            queue.push(subclass);
          }
        }
      }
    }

    return adjList;
  }

  private static Type getLogLevel(boolean isSpeculative) {
    return isSpeculative ? TreeLogger.WARN : TreeLogger.ERROR;
  }

  private static void logSerializableTypes(TreeLogger logger,
      Set<JClassType> fieldSerializableTypes) {
    TreeLogger localLogger = logger.branch(TreeLogger.DEBUG, "Identified "
        + fieldSerializableTypes.size() + " serializable type"
        + ((fieldSerializableTypes.size() == 1) ? "" : "s"), null);

    for (JClassType fieldSerializableType : fieldSerializableTypes) {
      localLogger.branch(TreeLogger.DEBUG,
          fieldSerializableType.getParameterizedQualifiedSourceName(), null);
    }
  }

  private boolean alreadyCheckedObject;

  /**
   * Cache of the {@link JClassType} for {@link Collection}.
   */
  private final JClassType collectionClass;

  /**
   * Cache of the {@link JClassType} for {@link Exception}.
   */
  private final JClassType exceptionClass;

  /**
   * Cache of the {@link JClassType} for {@link IsSerializable}.
   */
  private final JClassType isSerializableClass;

  /**
   * Cache of the {@link JClassType} for {@link Map}.
   */
  private final JClassType mapClass;

  private final TreeLogger rootLogger;

  /**
   * Cache of the {@link JClassType} for
   * {@link java.io.Serializable Serializable}.
   */
  private final JClassType serializableClass;

  /**
   * Cache of the {@link JClassType} for {@link SerializationStreamReader}.
   */
  private final JClassType streamReaderClass;

  /**
   * Cache of the {@link JClassType} for {@link SerializationStreamWriter}.
   */
  private final JClassType streamWriterClass;

  private final TypeOracle typeOracle;

  /**
   * Map of {@link JClassType} to {@link TypeInfo}.
   */
  private final Map<JClassType, TypeInfo> typeToTypeInfo = new HashMap<JClassType, TypeInfo>();

  /**
   * Map of {@link JType} to {@link TypeInfoComputed}.
   */
  private final Map<JType, TypeInfoComputed> typeToTypeInfoComputed = new HashMap<JType, TypeInfoComputed>();

  /**
   * Constructs a builder.
   * 
   * @param rootLogger
   * @param typeOracle
   * @throws UnableToCompleteException if we fail to find one of our special
   *           types
   */
  public SerializableTypeOracleBuilder(TreeLogger rootLogger,
      TypeOracle typeOracle) throws UnableToCompleteException {
    this.rootLogger = rootLogger;
    this.typeOracle = typeOracle;

    try {
      collectionClass = typeOracle.getType(Collection.class.getName());
      exceptionClass = typeOracle.getType(Exception.class.getName());
      isSerializableClass = typeOracle.getType(IsSerializable.class.getName());
      mapClass = typeOracle.getType(Map.class.getName());
      serializableClass = typeOracle.getType(Serializable.class.getName());
      streamReaderClass = typeOracle.getType(SerializationStreamReader.class.getName());
      streamWriterClass = typeOracle.getType(SerializationStreamWriter.class.getName());
    } catch (NotFoundException e) {
      rootLogger.log(TreeLogger.ERROR, null, e);
      throw new UnableToCompleteException();
    }
  }

  /**
   * Builds a {@link SerializableTypeOracle} for a give
   * {@link com.google.gwt.user.client.rpc.RemoteService} interface.
   * 
   * @param propertyOracle property oracle used for initializing properties
   * @param remoteService
   *          {@link com.google.gwt.user.client.rpc.RemoteService RemoteService}
   *          interface to build the oracle for
   * @return a {@link SerializableTypeOracle} for the specified
   *         {@link com.google.gwt.user.client.rpc.RemoteService RemoteService}
   *         interface
   * 
   * @throws UnableToCompleteException if the the remote service is considered
   *           invalid due to serialization problem or a missing or ill formed
   *           remote service asynchronous interface
   */
  public SerializableTypeOracle build(PropertyOracle propertyOracle,
      JClassType remoteService) throws UnableToCompleteException {

    try {
      // String is always instantiable.
      JClassType stringType = typeOracle.getType(String.class.getName());
      if (!checkTypeInstantiable(rootLogger, stringType, false)) {
        throw new UnableToCompleteException();
      }
      // IncompatibleRemoteServiceException is always serializable
      JClassType icseType = typeOracle.getType(IncompatibleRemoteServiceException.class.getName());
      if (!checkTypeInstantiable(rootLogger, icseType, false)) {
        throw new UnableToCompleteException();
      }
    } catch (NotFoundException e) {
      rootLogger.log(TreeLogger.ERROR, null, e);
      throw new UnableToCompleteException();
    }

    TreeLogger logger = rootLogger.branch(TreeLogger.DEBUG, "Analyzing '"
        + remoteService.getParameterizedQualifiedSourceName()
        + "' for serializable types", null);

    alreadyCheckedObject = false;

    validateRemoteService(logger, remoteService);

    // Compute covariant arrays.
    // Cache a list to prevent comodification.
    List<TypeInfoComputed> typeInfoComputed = new ArrayList<TypeInfoComputed>(
        typeToTypeInfoComputed.values());
    for (TypeInfoComputed tic : typeInfoComputed) {
      if (tic.isInstantiable()) {
        JArrayType arrayType = tic.getType().isArray();
        if (arrayType != null) {
          JType leafType = arrayType.getLeafType();
          int rank = arrayType.getRank();
          JClassType classType = leafType.isClassOrInterface();
          if (classType != null) {
            List<JClassType> instantiableSubTypes = new ArrayList<JClassType>();
            JClassType[] subTypes = classType.getSubtypes();
            for (int i = 0; i < subTypes.length; ++i) {
              if (getTypeInfoComputed(subTypes[i]).isInstantiable()) {
                instantiableSubTypes.add(subTypes[i]);
              }
            }
            List<JClassType> covariantTypes = getAllTypesBetweenRootTypeAndLeaves(
                classType, instantiableSubTypes);
            for (int i = 0, c = covariantTypes.size(); i < c; ++i) {
              JArrayType covariantArray = getArrayType(typeOracle, rank,
                  covariantTypes.get(i));
              getTypeInfoComputed(covariantArray).setInstantiable(true);
            }
          }
        }
      }
    }

    Set<JClassType> possiblyInstantiatedTypes = new TreeSet<JClassType>(
        JCLASS_TYPE_COMPARATOR);

    Set<JClassType> fieldSerializableTypes = new TreeSet<JClassType>(
        JCLASS_TYPE_COMPARATOR);

    for (TypeInfoComputed tic : typeToTypeInfoComputed.values()) {
      JClassType type = tic.getType();

      if (type.isTypeParameter() != null || type.isWildcard() != null) {
        /*
         * Wildcard and type parameters types are ignored here. Their
         * corresponding subtypes will be part of the typeToTypeInfoComputed
         * set. TypeParameters can reach here if there are methods on the
         * RemoteService that declare their own type parameters.
         */
        continue;
      }

      // Only record real types
      if (type.isParameterized() != null) {
        type = type.isParameterized().getRawType();
      } else if (type.isGenericType() != null) {
        type = type.isGenericType().getRawType();
        assert (type == type.getErasedType());
      }

      if (tic.isInstantiable()) {
        possiblyInstantiatedTypes.add(type);
      }

      if (tic.isFieldSerializable()) {
        fieldSerializableTypes.add(type);
      }
    }

    logSerializableTypes(logger, fieldSerializableTypes);

    return new SerializableTypeOracleImpl(typeOracle, fieldSerializableTypes,
        possiblyInstantiatedTypes);
  }

  /**
   * Consider any subtype of java.lang.Object which qualifies for serialization.
   * 
   * @param logger
   */
  private void checkAllSubtypesOfObject(TreeLogger logger) {
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
    TreeLogger localLogger = logger.branch(TreeLogger.WARN,
        "Checking all subtypes of Object which qualify for serialization", null);
    JClassType[] allTypes = typeOracle.getJavaLangObject().getSubtypes();
    for (JClassType cls : allTypes) {
      if (getTypeInfo(cls).isDeclaredSerializable()) {
        checkTypeInstantiable(localLogger, cls, true);
      }
    }
  }

  private boolean checkArrayInstantiable(TreeLogger logger,
      JArrayType arrayType, TypeInfoComputed tic, boolean isSpeculative) {
    TreeLogger branch = logger.branch(TreeLogger.DEBUG,
        "Analyzing component type:", null);
    boolean success = checkTypeInstantiable(branch,
        arrayType.getComponentType(), isSpeculative);
    tic.setInstantiable(success);
    return success;
  }

  private boolean checkClassOrInterfaceInstantiable(TreeLogger logger,
      JClassType type, boolean isSpeculative) {

    TypeInfo typeInfo = getTypeInfo(type);
    TypeInfoComputed tic = getTypeInfoComputed(type);

    if (!typeInfo.isDeclaredSerializable()) {
      logger.branch(TreeLogger.DEBUG, "Type '"
          + type.getParameterizedQualifiedSourceName()
          + "' is not assignable to '" + IsSerializable.class.getName()
          + "' or '" + Serializable.class.getName()
          + "' nor does it have a custom field serializer", null);
      return false;
    }

    if (typeInfo.isManuallySerializable()) {
      List<String> failures = CustomFieldSerializerValidator.validate(
          streamReaderClass, streamWriterClass, typeInfo.getManualSerializer(),
          type);
      if (!failures.isEmpty()) {
        markAsUninstantiableAndLog(logger, isSpeculative, failures, tic);
        return false;
      }
    } else {
      assert (typeInfo.isAutoSerializable());

      if (type.isEnum() != null) {
        if (type.isLocalType()) {
          /*
           * Quietly ignore local enum types.
           */
          tic.setInstantiable(false);
          return false;
        } else {
          /*
           * Enumerated types are serializable by default, but they do not have 
           * their state automatically or manually serialized.  So, consider it 
           * serializable but do not check its fields.
           */
          return true;
        }
      }
      
      if (type.isPrivate()) {
        /*
         * Quietly ignore private types since these cannot be instantiated
         * from the generated field serializers.
         */
        tic.setInstantiable(false);
        return false;
      }
      
      if (type.isLocalType()) {
        markAsUninstantiableAndLog(
            logger,
            isSpeculative,
            type.getParameterizedQualifiedSourceName()
                + " is a local type, it will be excluded from the set of serializable types",
            tic);
        return false;
      }

      if (type.isMemberType() && !type.isStatic()) {
        markAsUninstantiableAndLog(
            logger,
            isSpeculative,
            type.getParameterizedQualifiedSourceName()
                + " is nested but not static, it will be excluded from the set of serializable types",
            tic);
        return false;
      }

      if (type.isInterface() != null || type.isAbstract()) {
        // Quietly return false.
        return false;
      }

      if (!Shared.isDefaultInstantiable(type)) {
        // Warn and return false.
        logger.log(
            TreeLogger.WARN,
            "Was not default instantiable (it must have a zero-argument, non-private constructor or no constructors at all)",
            null);
        return false;
      }
    }

    if (!checkFields(logger, type, isSpeculative)) {
      return false;
    }

    checkMethods(logger, type);
    return true;
  }

  private boolean checkFields(TreeLogger logger, JClassType classOrInterface,
      boolean isSpeculative) {
    TypeInfo typeInfo = getTypeInfo(classOrInterface);

    // Check all super type fields first (recursively).
    JClassType superType = classOrInterface.getSuperclass();
    if (superType != null && getTypeInfo(superType).isDeclaredSerializable()) {
      boolean superTypeOk = checkFields(logger, superType, isSpeculative);
      /*
       * If my super type did not check out, then I am not instantiable and we
       * should error out... UNLESS I am *directly* serializable myself, in
       * which case it's ok for me to be the root of a new instantiable
       * hierarchy.
       */
      if (!superTypeOk && !typeInfo.isDirectlySerializable()) {
        return false;
      }
    }

    if (typeInfo.isManuallySerializable()) {
      // All fields on a manual serializable are considered speculative.
      isSpeculative = true;
    }

    boolean allSucceeded = true;
    JField[] fields = classOrInterface.getFields();
    if (fields.length > 0) {
      TreeLogger localLogger = logger.branch(TreeLogger.DEBUG,
          "Analyzing Fields:", null);

      for (JField field : fields) {
        if (field.isStatic() || field.isTransient()) {
          continue;
        }

        if (field.isFinal()) {
          localLogger.branch(TreeLogger.DEBUG, "Field '" + field.toString()
              + "' will not be serialized because it is final", null);
          continue;
        }

        TreeLogger fieldLogger = localLogger.branch(TreeLogger.DEBUG,
            field.toString(), null);
        JType fieldType = field.getType();

        if (typeInfo.isManuallySerializable()
            && fieldType.getLeafType() == typeOracle.getJavaLangObject()) {
          checkAllSubtypesOfObject(fieldLogger.branch(TreeLogger.WARN,
              "Object was reached from a manually serializable type", null));
        } else {
          allSucceeded &= checkTypeInstantiable(fieldLogger, fieldType,
              isSpeculative);
        }
      }
    }

    boolean succeeded = allSucceeded || typeInfo.isManuallySerializable();
    if (succeeded) {
      getTypeInfoComputed(classOrInterface).setFieldSerializable();
    }
    return succeeded;
  }

  private void checkMethods(TreeLogger logger, JClassType classOrInterface) {
    if (isDefinedInJREEmulation(classOrInterface)) {
      // JRE emulation classes are never used on the server; skip the check
      return;
    }

    // TODO: consider looking up type hierarchy.
    JMethod[] methods = classOrInterface.getMethods();
    for (JMethod method : methods) {
      if (method.isNative()) {
        logger.branch(
            TreeLogger.WARN,
            MessageFormat.format(
                "Method ''{0}'' is native, calling this method in server side code will result in an UnsatisfiedLinkError",
                method.toString()), null);
      }
    }
  }

  /**
   * Returns <code>true</code> if all of the type arguments of the
   * parameterized type are themselves instantiable.
   */
  private boolean checkTypeArgumentsInstantiable(TreeLogger logger,
      JParameterizedType parameterizedType, boolean isSpeculative) {
    TreeLogger branch = logger.branch(TreeLogger.DEBUG, "Analyzing type args",
        null);
    JClassType[] typeArgs = parameterizedType.getTypeArgs();
    boolean allSucceeded = true;
    for (JClassType typeArg : typeArgs) {
      allSucceeded &= checkTypeInstantiable(branch, typeArg, isSpeculative);
    }

    return allSucceeded;
  }

  private boolean checkTypeInstantiable(TreeLogger logger, JType type,
      boolean isSpeculative) {
    assert (type != null);
    if (type.isPrimitive() != null) {
      return true;
    }

    assert (type instanceof JClassType);

    JClassType classType = (JClassType) type;

    TreeLogger localLogger = logger.branch(TreeLogger.DEBUG,
        classType.getParameterizedQualifiedSourceName(), null);

    TypeInfoComputed tic = getTypeInfoComputed(classType);
    if (tic.isPendingInstantiable()) {
      // just early out and pretend we will succeed
      return true;
    } else if (tic.isDone()) {
      return tic.isInstantiable();
    }

    tic.setPendingInstantiable();

    if (classType.getLeafType() == typeOracle.getJavaLangObject()) {
      markAsUninstantiableAndLog(
          logger,
          isSpeculative,
          "In order to produce smaller client-side code, 'Object' is not allowed; consider using a more specific type",
          tic);
      return false;
    }

    if (classType.isArray() != null) {
      return checkArrayInstantiable(logger, classType.isArray(), tic,
          isSpeculative);
    } else if (classType.isWildcard() != null) {
      return checkWildcardInstantiable(logger, classType.isWildcard(), tic,
          isSpeculative);
    } else if (classType.isClassOrInterface() != null) {
      TypeInfo typeInfo = getTypeInfo(classType);
      if (isSpeculative && typeInfo.isDirectlySerializable()) {
        isSpeculative = false;
      }

      boolean anySubtypes = false;
      if (checkClassOrInterfaceInstantiable(localLogger, classType,
          isSpeculative)) {
        tic.setInstantiable(true);
        anySubtypes = true;
      }

      if (classType.isParameterized() != null) {
        /*
         * Backwards compatibility. The number of parameterization arguments
         * specified via gwt.typeArgs could exceed the number of formal
         * parameters declared on the generic type. Therefore have to explicitly
         * visit them here and they must all be instantiable.
         */
        JParameterizedType parameterizedType = classType.isParameterized();
        if (!checkTypeArgumentsInstantiable(localLogger, parameterizedType,
            isSpeculative)) {
          return false;
        }
      } else if (classType.isRawType() != null) {
        TreeLogger rawTypeLogger = logger.branch(
            TreeLogger.WARN,
            "Type '"
                + classType.getQualifiedSourceName()
                + "' should be parameterized to help the compiler produce the smallest code size possible for your module.",
            null);

        if (classType.isAssignableTo(collectionClass)
            || classType.isAssignableTo(mapClass)) {
          /*
           * Backwards compatibility. Raw collections or maps force all object
           * subtypes to be considered. Fall through to the normal class
           * handling.
           */
          checkAllSubtypesOfObject(rawTypeLogger);
        }
      }

      // Speculatively check all subtypes.
      JClassType[] subtypes = classType.getSubtypes();
      if (subtypes.length > 0) {
        TreeLogger subLogger = localLogger.branch(TreeLogger.DEBUG,
            "Analyzing subclasses:", null);

        for (JClassType subType : subtypes) {
          if (checkClassOrInterfaceInstantiable(subLogger.branch(
              TreeLogger.DEBUG, subType.getParameterizedQualifiedSourceName(),
              null), subType, true)) {
            getTypeInfoComputed(subType).setInstantiable(true);
            anySubtypes = true;
          }
        }
      }

      if (!anySubtypes && !isSpeculative) {
        // No instantiable types were found
        markAsUninstantiableAndLog(
            logger,
            isSpeculative,
            "Type '"
                + classType.getParameterizedQualifiedSourceName()
                + "' was not serializable and has no concrete serializable subtypes",
            tic);
      }

      return anySubtypes;
    } else {
      assert (false);
      return false;
    }
  }

  private boolean checkWildcardInstantiable(TreeLogger logger,
      JWildcardType wildcard, TypeInfoComputed tic, boolean isSpeculative) {
    JBound bounds = wildcard.getBounds();
    boolean success;
    if (bounds.isLowerBound() != null) {
      // Fail since ? super T for any T implies object also
      markAsUninstantiableAndLog(logger, isSpeculative,
          "In order to produce smaller client-side code, 'Object' is not allowed; '"
              + wildcard.getQualifiedSourceName() + "' includes Object.", tic);

      success = false;
    } else {
      JClassType firstBound = bounds.getFirstBound();
      success = checkTypeInstantiable(logger, firstBound, isSpeculative);
    }

    tic.setInstantiable(success);
    return success;
  }

  private TypeInfo getTypeInfo(JClassType type) {
    TypeInfo ti = typeToTypeInfo.get(type);
    if (ti == null) {
      ti = new TypeInfo(type);
      typeToTypeInfo.put(type, ti);
    }
    return ti;
  }

  private TypeInfoComputed getTypeInfoComputed(JClassType type) {
    TypeInfoComputed tic = typeToTypeInfoComputed.get(type);
    if (tic == null) {
      tic = new TypeInfoComputed(type);
      typeToTypeInfoComputed.put(type, tic);
    }
    return tic;
  }

  /**
   * Returns <code>true</code> if the type is defined by the JRE.
   */
  private boolean isDefinedInJREEmulation(JClassType type) {
    JPackage pkg = type.getPackage();
    if (pkg != null) {
      return pkg.getName().startsWith("java.");
    }

    return false;
  }

  private void markAsUninstantiableAndLog(TreeLogger logger,
      boolean isSpeculative, List<String> es, TypeInfoComputed tic) {
    for (String s : es) {
      markAsUninstantiableAndLog(logger, isSpeculative, s, tic);
    }
  }

  private void markAsUninstantiableAndLog(TreeLogger logger,
      boolean isSpeculative, String logMessage, TypeInfoComputed tic) {
    tic.setInstantiable(false);
    tic.addSerializationIssue(isSpeculative, logMessage);
    logger.branch(getLogLevel(isSpeculative), logMessage, null);
  }

  private void validateRemoteService(TreeLogger logger, JClassType remoteService)
      throws UnableToCompleteException {
    JMethod[] methods = remoteService.getOverridableMethods();

    TreeLogger validationLogger = logger.branch(TreeLogger.DEBUG,
        "Analyzing methods:", null);

    boolean allSucceeded = true;
    for (JMethod method : methods) {
      TreeLogger methodLogger = validationLogger.branch(TreeLogger.DEBUG,
          method.toString(), null);
      JType returnType = method.getReturnType();
      if (returnType != JPrimitiveType.VOID) {
        TreeLogger returnTypeLogger = methodLogger.branch(TreeLogger.DEBUG,
            "Return type: " + returnType.getParameterizedQualifiedSourceName(),
            null);
        allSucceeded &= checkTypeInstantiable(returnTypeLogger, returnType,
            false);
      }

      JParameter[] params = method.getParameters();
      for (JParameter param : params) {
        TreeLogger paramLogger = methodLogger.branch(TreeLogger.DEBUG,
            "Parameter: " + param.toString(), null);
        JType paramType = param.getType();
        allSucceeded &= checkTypeInstantiable(paramLogger, paramType, false);
      }

      JType[] exs = method.getThrows();
      if (exs.length > 0) {
        TreeLogger throwsLogger = methodLogger.branch(TreeLogger.DEBUG,
            "Throws:", null);
        for (JType ex : exs) {
          if (!exceptionClass.isAssignableFrom(ex.isClass())) {
            throwsLogger = throwsLogger.branch(
                TreeLogger.WARN,
                "'"
                    + ex.getQualifiedSourceName()
                    + "' is not a checked exception; only checked exceptions may be used",
                null);
          }

          allSucceeded &= checkTypeInstantiable(throwsLogger, ex, false);
        }
      }
    }

    if (!allSucceeded) {
      // the validation code has already logged why
      throw new UnableToCompleteException();
    }
  }
}
