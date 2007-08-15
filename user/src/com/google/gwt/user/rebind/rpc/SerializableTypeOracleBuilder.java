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

import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.TreeLogger.Type;
import com.google.gwt.core.ext.typeinfo.JArrayType;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JConstructor;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JPackage;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
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
import java.util.Iterator;
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
    private class SerializationIssue implements Comparable {
      public final boolean isSpeculative;
      public final String issueMessage;

      SerializationIssue(boolean isSpeculative, String issueMessage) {
        this.isSpeculative = isSpeculative;
        this.issueMessage = issueMessage;
      }

      public int compareTo(Object obj) {
        SerializationIssue other = (SerializationIssue) obj;
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
    private Set /* <SerializationIssue> */serializationIssues = new TreeSet/* <SerializationIssue> */();

    /**
     * The state that this type is currently in.
     */
    private TypeState state = NOT_CHECKED;

    /**
     * {@link JType} associated with this metadata.
     */
    private final JType type;

    public TypeInfoComputed(JType type) {
      this.type = type;
    }

    public void addSerializationIssue(boolean isSpeculative, String issueMessage) {
      serializationIssues.add(new SerializationIssue(isSpeculative,
          issueMessage));
    }

    public JType getType() {
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
      Iterator iter = serializationIssues.iterator();
      while (iter.hasNext()) {
        SerializationIssue serializationIssue = (SerializationIssue) iter.next();
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

  private static void addEdge(Map adjList, JClassType subclass, JClassType clazz) {
    List edges = (List) adjList.get(subclass);
    if (edges == null) {
      edges = new ArrayList();
      adjList.put(subclass, edges);
    }

    edges.add(clazz);
  }

  private static void depthFirstSearch(Set seen, Map adjList, JClassType type) {
    if (seen.contains(type)) {
      return;
    }
    seen.add(type);

    List children = (List) adjList.get(type);
    if (children != null) {
      Iterator it = children.iterator();
      while (it.hasNext()) {
        JClassType child = (JClassType) it.next();

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
    return directlyImplementsInterfaceRecursive(new HashSet(), type, intf);
  }

  private static boolean directlyImplementsInterfaceRecursive(Set seen,
      JClassType clazz, JClassType intf) {

    if (clazz == intf) {
      return true;
    }

    JClassType[] intfImpls = clazz.getImplementedInterfaces();

    for (int i = 0; i < intfImpls.length; ++i) {
      JClassType intfImpl = intfImpls[i];

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
  private static List getAllTypesBetweenRootTypeAndLeaves(JClassType root,
      List leaves) {
    Map adjList = getInvertedTypeHierarchy(root);
    Set types = new HashSet();

    Iterator it = leaves.iterator();
    while (it.hasNext()) {
      JClassType type = (JClassType) it.next();

      depthFirstSearch(types, adjList, type);
    }

    return Arrays.asList(types.toArray());
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
  private static Map getInvertedTypeHierarchy(JClassType root) {
    Map adjList = new HashMap/* <JClassType, List<JClassType>> */();
    Set seen = new HashSet /* <JClassType> */();
    Stack queue = new Stack /* <JClassType> */();
    queue.push(root);
    while (!queue.isEmpty()) {
      JClassType clazz = (JClassType) queue.pop();
      JClassType[] subclasses = clazz.getSubtypes();

      if (seen.contains(clazz)) {
        continue;
      }
      seen.add(clazz);

      for (int i = 0; i < subclasses.length; ++i) {
        JClassType subclass = subclasses[i];

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

  private static void logSerializableTypes(TreeLogger logger, JType[] types) {
    TreeLogger localLogger = logger.branch(TreeLogger.DEBUG, "Identified "
        + types.length + " serializable type"
        + ((types.length == 1) ? "" : "s"), null);

    for (int i = 0; i < types.length; ++i) {
      localLogger.branch(TreeLogger.DEBUG,
          types[i].getParameterizedQualifiedSourceName(), null);
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

  /**
   * If <code>true</code> we will not warn if a serializable type contains a
   * non-static final field. We warn because these fields are not serialized.
   */
  private boolean suppressNonStaticFinalFieldWarnings;

  private final TypeOracle typeOracle;

  /**
   * Map of {@link JClassType} to {@link TypeInfo}.
   */
  private final Map /* <JClassType, TypeInfo> */typeToTypeInfo = new HashMap();

  /**
   * Map of {@link JType} to {@link TypeInfoComputed}.
   */
  private final Map /* <JType, TypeInfoComputed> */typeToTypeInfoComputed = new HashMap();

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

    initializeProperties(rootLogger, propertyOracle);

    try {
      // String is always instantiable.
      JClassType stringType = typeOracle.getType(String.class.getName());
      if (!checkTypeInstantiable(rootLogger, stringType, false, false)) {
        throw new UnableToCompleteException();
      }
      // IncompatibleRemoteServiceException is always serializable
      JClassType icseType = typeOracle.getType(IncompatibleRemoteServiceException.class.getName());
      if (!checkTypeInstantiable(rootLogger, icseType, false, false)) {
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
    List typeInfoComputed = new ArrayList(typeToTypeInfoComputed.values());
    Iterator iterTypes = typeInfoComputed.iterator();
    while (iterTypes.hasNext()) {
      TypeInfoComputed tic = (TypeInfoComputed) iterTypes.next();
      if (tic.isInstantiable()) {
        JArrayType arrayType = tic.getType().isArray();
        if (arrayType != null) {
          JType leafType = arrayType.getLeafType();
          int rank = arrayType.getRank();
          JClassType classType = leafType.isClassOrInterface();
          if (classType != null) {
            List instantiableSubTypes = new ArrayList();
            JClassType[] subTypes = classType.getSubtypes();
            for (int i = 0; i < subTypes.length; ++i) {
              if (getTypeInfoComputed(subTypes[i]).isInstantiable()) {
                instantiableSubTypes.add(subTypes[i]);
              }
            }
            List covariantTypes = getAllTypesBetweenRootTypeAndLeaves(
                classType, instantiableSubTypes);
            for (int i = 0, c = covariantTypes.size(); i < c; ++i) {
              JArrayType covariantArray = getArrayType(typeOracle, rank,
                  (JType) covariantTypes.get(i));
              getTypeInfoComputed(covariantArray).setInstantiable(true);
            }
          }
        }
      }
    }

    Set possiblyInstantiatedTypes = new HashSet();
    List serializableTypesList = new ArrayList();
    iterTypes = typeToTypeInfoComputed.values().iterator();
    while (iterTypes.hasNext()) {
      TypeInfoComputed tic = (TypeInfoComputed) iterTypes.next();
      JType type = tic.getType();
      // Only record real types
      if (type.isParameterized() == null) {
        if (tic.isInstantiable()) {
          possiblyInstantiatedTypes.add(type);
        }
        if (tic.isFieldSerializable()) {
          serializableTypesList.add(type);
        }
      }
    }

    JType[] serializableTypes = new JType[serializableTypesList.size()];
    serializableTypesList.toArray(serializableTypes);

    Arrays.sort(serializableTypes, new Comparator() {
      public int compare(Object o1, Object o2) {
        String n1 = ((JType) o1).getQualifiedSourceName();
        String n2 = ((JType) o2).getQualifiedSourceName();
        return n1.compareTo(n2);
      }
    });

    logSerializableTypes(logger, serializableTypes);

    return new SerializableTypeOracleImpl(typeOracle, serializableTypes,
        possiblyInstantiatedTypes);
  }

  /**
   * Consider any subtype of java.lang.Object which qualifies for serialization.
   * 
   * @param localLogger
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
    for (int i = 0; i < allTypes.length; ++i) {
      JClassType cls = allTypes[i];
      if (getTypeInfo(cls).isDeclaredSerializable()) {
        checkTypeInstantiable(localLogger, cls, true, true);
      }
    }
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
      List failures = CustomFieldSerializerValidator.validate(
          streamReaderClass, streamWriterClass, typeInfo.getManualSerializer(),
          type);
      if (!failures.isEmpty()) {
        markAsUninstantiableAndLog(logger, isSpeculative, failures, tic);
        return false;
      }
    } else {
      assert (typeInfo.isAutoSerializable());

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

      boolean isDefaultInstantiable = false;
      if (type.getConstructors().length == 0) {
        isDefaultInstantiable = true;
      } else {
        JConstructor ctor = type.findConstructor(new JType[0]);
        if (ctor != null && !ctor.isPrivate()) {
          isDefaultInstantiable = true;
        }
      }

      if (!isDefaultInstantiable) {
        // Warn and return false.
        logger.log(
            TreeLogger.WARN,
            "Was not default instantiable (it must have a zero-argument, non-private constructor or no constructors at all)",
            null);
        return false;
      }
    }

    // Check all fields, including inherited fields
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

      for (int i = 0; i < fields.length; ++i) {
        JField field = fields[i];

        if (field.isStatic() || field.isTransient()) {
          continue;
        }

        if (field.isFinal()) {
          if (!suppressNonStaticFinalFieldWarnings) {
            localLogger.branch(TreeLogger.WARN, "Field '" + field.toString()
                + "' will not be serialized because it is final", null);
          }
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
              isSpeculative, false);
        }
      }
    }

    boolean succeeded = allSucceeded || typeInfo.isManuallySerializable();
    if (succeeded) {
      getTypeInfoComputed(classOrInterface).setFieldSerializable();
    }
    return succeeded;
  }

  private void checkForUnparameterizedType(TreeLogger logger, JType type) {
    if (type.isParameterized() != null) {
      return;
    }

    JClassType classOrInterface = type.isClassOrInterface();
    if (classOrInterface != null) {
      if (classOrInterface.isAssignableTo(collectionClass)
          || classOrInterface.isAssignableTo(mapClass)) {
        TreeLogger localLogger = logger.branch(
            TreeLogger.WARN,
            "Type '"
                + type.getQualifiedSourceName()
                + "' should be parameterized to help the compiler produce the smallest code size possible for your module. Since the gwt.typeArgs javadoc annotation is missing, all subtypes of Object will be analyzed for serializability even if they are not directly or indirectly used",
            null);

        checkAllSubtypesOfObject(localLogger);
      }
    }
  }

  private void checkMethods(TreeLogger logger, JClassType classOrInterface) {
    if (isDefinedInJREEmulation(classOrInterface)) {
      // JRE emulation classes are never used on the server; skip the check
      return;
    }

    // TODO: consider looking up type hierarchy.
    JMethod[] methods = classOrInterface.getMethods();
    for (int i = 0; i < methods.length; ++i) {
      JMethod method = methods[i];
      if (method.isNative()) {
        logger.branch(
            TreeLogger.WARN,
            MessageFormat.format(
                "Method ''{0}'' is native, calling this method in server side code will result in an UnsatisfiedLinkError",
                new String[] {method.toString()}), null);
      }
    }
  }

  private boolean checkTypeInstantiable(TreeLogger logger, JType type,
      boolean isSpeculative, boolean rawTypeOk) {
    assert (type != null);
    if (type.isPrimitive() != null) {
      return true;
    }

    TreeLogger localLogger = logger.branch(TreeLogger.DEBUG,
        type.getParameterizedQualifiedSourceName(), null);

    TypeInfoComputed tic = getTypeInfoComputed(type);
    if (tic.isPendingInstantiable()) {
      // just early out and pretend we will succeed
      return true;
    } else if (tic.isDone()) {
      return tic.isInstantiable();
    }

    tic.setPendingInstantiable();

    if (type.getLeafType() == typeOracle.getJavaLangObject()) {
      markAsUninstantiableAndLog(
          logger,
          isSpeculative,
          "In order to produce smaller client-side code, 'Object' is not allowed; consider using a more specific type",
          tic);
      return false;
    }

    if (type.isParameterized() != null) {
      JParameterizedType parameterized = type.isParameterized();
      boolean allSucceeded = checkTypeInstantiable(localLogger.branch(
          TreeLogger.DEBUG, "Analyzing raw type", null),
          parameterized.getRawType(), isSpeculative, true);

      TreeLogger branch = localLogger.branch(TreeLogger.DEBUG,
          "Analyzing type args", null);
      JType[] typeArgs = parameterized.getTypeArgs();
      for (int i = 0; i < typeArgs.length; ++i) {
        allSucceeded &= checkTypeInstantiable(branch, typeArgs[i],
            isSpeculative, false);
      }
      tic.setInstantiable(allSucceeded);
      return allSucceeded;
    } else if (type.isArray() != null) {
      TreeLogger branch = localLogger.branch(TreeLogger.DEBUG,
          "Analyzing component type:", null);
      boolean success = checkTypeInstantiable(branch,
          type.isArray().getComponentType(), isSpeculative, false);
      tic.setInstantiable(success);
      return success;
    } else if (type.isClassOrInterface() != null) {
      JClassType classType = type.isClassOrInterface();
      TypeInfo typeInfo = getTypeInfo(classType);
      if (isSpeculative && typeInfo.isDirectlySerializable()) {
        isSpeculative = false;
      }

      boolean anySubtypes = false;
      if (checkClassOrInterfaceInstantiable(localLogger, classType,
          isSpeculative)) {
        tic.setInstantiable(true);
        anySubtypes = true;

        if (!rawTypeOk) {
          checkForUnparameterizedType(logger, classType);
        }
      }

      // Speculatively check all subtypes.
      JClassType[] subtypes = classType.getSubtypes();
      if (subtypes.length > 0) {
        TreeLogger subLogger = localLogger.branch(TreeLogger.DEBUG,
            "Analyzing subclasses:", null);

        for (int i = 0; i < subtypes.length; ++i) {
          JClassType subType = subtypes[i];
          if (checkClassOrInterfaceInstantiable(subLogger, subType, true)) {
            getTypeInfoComputed(subType).setInstantiable(true);
            anySubtypes = true;
          }
        }
      }

      if (!anySubtypes && !isSpeculative) {
        logger.log(
            getLogLevel(isSpeculative),
            "Type '"
                + type.getParameterizedQualifiedSourceName()
                + "' was not serializable and has no concrete serializable subtypes",
            null);
      }
      return anySubtypes;
    } else {
      assert (false);
      return false;
    }
  }

  private TypeInfo getTypeInfo(JClassType type) {
    TypeInfo ti = (TypeInfo) typeToTypeInfo.get(type);
    if (ti == null) {
      ti = new TypeInfo(type);
      typeToTypeInfo.put(type, ti);
    }
    return ti;
  }

  private TypeInfoComputed getTypeInfoComputed(JType type) {
    TypeInfoComputed tic = (TypeInfoComputed) typeToTypeInfoComputed.get(type);
    if (tic == null) {
      tic = new TypeInfoComputed(type);
      typeToTypeInfoComputed.put(type, tic);
    }
    return tic;
  }

  private void initializeProperties(TreeLogger logger,
      PropertyOracle propertyOracle) {
    suppressNonStaticFinalFieldWarnings = false;
    try {
      String propVal = propertyOracle.getPropertyValue(logger,
          "gwt.suppressNonStaticFinalFieldWarnings");
      if (propVal.equals("true")) {
        suppressNonStaticFinalFieldWarnings = true;
      }
    } catch (BadPropertyValueException e) {
      // Purposely ignored, because we do want to warn if non-static, final
      // are part of a serializable type
    }
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
      boolean isSpeculative, List es, TypeInfoComputed tic) {
    Iterator iter = es.iterator();
    while (iter.hasNext()) {
      markAsUninstantiableAndLog(logger, isSpeculative, (String) iter.next(),
          tic);
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
    for (int i = 0; i < methods.length; ++i) {
      JMethod method = methods[i];
      TreeLogger methodLogger = validationLogger.branch(TreeLogger.DEBUG,
          method.toString(), null);
      JType returnType = method.getReturnType();
      if (returnType != JPrimitiveType.VOID) {
        TreeLogger returnTypeLogger = methodLogger.branch(TreeLogger.DEBUG,
            "Return type: " + returnType.getParameterizedQualifiedSourceName(),
            null);
        allSucceeded &= checkTypeInstantiable(returnTypeLogger, returnType,
            false, false);
      }

      JParameter[] params = method.getParameters();
      for (int j = 0; j < params.length; ++j) {
        JParameter param = params[j];
        TreeLogger paramLogger = methodLogger.branch(TreeLogger.DEBUG,
            "Parameter: " + param.toString(), null);
        JType paramType = param.getType();
        allSucceeded &= checkTypeInstantiable(paramLogger, paramType, false,
            false);
      }

      JType[] exs = method.getThrows();
      if (exs.length > 0) {
        TreeLogger throwsLogger = methodLogger.branch(TreeLogger.DEBUG,
            "Throws:", null);
        for (int j = 0; j < exs.length; ++j) {
          JType ex = exs[j];
          if (!exceptionClass.isAssignableFrom(ex.isClass())) {
            throwsLogger = throwsLogger.branch(
                TreeLogger.WARN,
                "'"
                    + ex.getQualifiedSourceName()
                    + "' is not a checked exception; only checked exceptions may be used",
                null);
          }

          allSucceeded &= checkTypeInstantiable(throwsLogger, ex, false, false);
        }
      }
    }

    if (!allSucceeded) {
      // the validation code has already logged why
      throw new UnableToCompleteException();
    }
  }

}
