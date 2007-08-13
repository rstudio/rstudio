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
  /**
   * Represents additional information about a type with regards to its
   * serializability.
   */
  private class MetaTypeInfo {
    /**
     * An issue that prevents a type from being serializable.
     */
    private class SerializationIssue implements Comparable {
      final String issueMessage;

      final TreeLogger.Type issueType;

      SerializationIssue(Type issueType, String issueMessage) {
        this.issueType = issueType;
        this.issueMessage = issueMessage;
      }

      public int compareTo(Object obj) {
        SerializationIssue other = (SerializationIssue) obj;
        if (issueType == other.issueType) {
          return issueMessage.compareTo(other.issueMessage);
        }

        if (issueType.isLowerPriorityThan(other.issueType)) {
          return -1;
        }

        return 1;
      }
    }

    /**
     * <code>true</code> if the type is assignable to {@link IsSerializable}
     * or {@link java.io.Serializable Serializable}.
     */
    private final boolean autoSerializable;

    /**
     * <code>true</code> if the this type directly implements one of the
     * marker interfaces.
     */
    private Boolean directlyImplementsMarker;

    /**
     * Custom field serializer or <code>null</code> if there isn't one.
     */
    private final JClassType manualSerializer;

    /**
     * <code>true</code> if this type might be instantiated.
     */
    private boolean maybeInstantiated;

    /**
     * <code>true</code> if the type is automatically or manually serializable
     * and the corresponding checks succeed.
     */
    private boolean serializable;

    /**
     * List of serializable types that are assignable to this one.
     */
    private List serializableTypesAssignableToMe;

    /**
     * List of serialization warnings or errors that prevent this type from
     * being serializable.
     */
    private Set /* <SerializationIssue> */serializationIssues = new TreeSet/* <SerializationIssue> */();

    /**
     * The state that this type is currently in.
     */
    private TypeState state = SerializableTypeOracleBuilder.NOT_CHECKED;

    /**
     * {@link JType} associated with this metadata.
     */
    private final JType type;

    /**
     * <code>true</code> if the subtypes of this type were also checked.
     */
    private boolean typeRoot;

    MetaTypeInfo(JType type, boolean autoSerializable,
        JClassType manualSerializer) {
      this.type = type;
      this.autoSerializable = autoSerializable;
      this.manualSerializer = manualSerializer;
    }

    public void addSerializationIssue(TreeLogger.Type issueType,
        String issueMessage) {
      serializationIssues.add(new SerializationIssue(issueType, issueMessage));
    }

    /**
     * Returns <code>true</code> if this type directly implements one of the
     * marker interfaces.
     * 
     * @return <code>true</code> if this type directly implements one of the
     *         marker interfaces.
     */
    public boolean directlyImplementsMarkerInterface() {
      JClassType isClassOrInterface = type.isClassOrInterface();
      if (isClassOrInterface == null) {
        return false;
      }

      if (directlyImplementsMarker == null) {
        if (directlyImplementsInterface(isClassOrInterface, isSerializableClass)
            || directlyImplementsInterface(isClassOrInterface,
                serializableClass)) {
          directlyImplementsMarker = Boolean.TRUE;
        } else {
          directlyImplementsMarker = Boolean.FALSE;
        }
      }

      return directlyImplementsMarker.booleanValue();
    }

    public JClassType getManualSerializer() {
      return manualSerializer;
    }

    public List getSerializableTypesAssignableToMe() {
      return serializableTypesAssignableToMe;
    }

    public Set /* <SerializationIssue> */getSerializationIssues() {
      return serializationIssues;
    }

    public TypeState getState() {
      return state;
    }

    public JType getType() {
      return type;
    }

    public boolean isSerializable() {
      if (state == SerializableTypeOracleBuilder.CHECK_IN_PROGRESS) {
        // Assume that we are serializable if we are currently checking the
        // type
        return true;
      }

      return serializable;
    }

    /**
     * Returns <code>true</code> if this type was seen as part of a method
     * signature or as a field type.
     */
    public boolean isTypeRoot() {
      return typeRoot;
    }

    /**
     * Returns <code>true</code> if this type might be instantiated.
     * 
     * @return <code>true</code> if this type might be instantiated
     */
    public boolean maybeInstantiated() {
      return maybeInstantiated;
    }

    /**
     * Returns <code>true</code> if this type needs to be rechecked. Only type
     * roots types have their subtypes analyzed. However it is possible that
     * this type was previously checked as the super type of a root type in
     * which case not all of its subtypes were analyzed.
     * 
     * @param asTypeRoot <code>true</code> if we are being asked to check this
     *          type as a type root
     * @return <code>true</code> if this type needs to be rechecked
     */
    public boolean needToRecheck(boolean asTypeRoot) {
      assert (state == SerializableTypeOracleBuilder.CHECK_SUCCEEDED);

      return asTypeRoot && !isTypeRoot();
    }

    /**
     * Returns <code>true</code> if the type is assignable to
     * {@link IsSerializable} or {@link java.io.Serializable Serializable}.
     * 
     * @return <code>true</code> if the type is assignable to
     *         {@link IsSerializable} or
     *         {@link java.io.Serializable Serializable}
     */
    public boolean qualifiesForAutoSerialization() {
      return autoSerializable && manualSerializer == null;
    }

    /**
     * Returns <code>true</code> if the type has a custom field serializer.
     * 
     * @return <code>true</code> if the type has a custom field serializer
     */
    public boolean qualifiesForManualSerialization() {
      return manualSerializer != null;
    }

    public boolean qualifiesForSerialization() {
      return qualifiesForAutoSerialization()
          || qualifiesForManualSerialization();
    }

    /**
     * Reset any state cached by this type.
     */
    public void reset() {
      serializationIssues.clear();

      serializableTypesAssignableToMe = null;

      state = NOT_CHECKED;

      serializable = false;

      directlyImplementsMarker = null;
    }

    public void setMaybeInstantiated(boolean maybeInstantiated) {
      this.maybeInstantiated = maybeInstantiated;
    }

    public void setSerializable(boolean serializable) {
      this.serializable = serializable;
    }

    public void setSerializableTypesAssignableToMe(List serializableTypes) {
      serializableTypesAssignableToMe = serializableTypes;
    }

    public void setState(TypeState newState) {
      state = newState;
    }

    public void setTypeRoot(boolean checkedSubtypes) {
      this.typeRoot = checkedSubtypes;
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
   * A serializability problem was discovered with the type.
   */
  static final TypeState CHECK_FAILED = new TypeState("Check failed");

  /**
   * The serializability of a type is being checked.
   */
  static final TypeState CHECK_IN_PROGRESS = new TypeState("Check in progress");

  /**
   * The serializability of a type has been determined and there were no errors.
   */
  static final TypeState CHECK_SUCCEEDED = new TypeState("Check succeeded");

  /**
   * The serializability of a type has not been checked.
   */
  static final TypeState NOT_CHECKED = new TypeState("Not checked");

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

  /**
   * Returns <code>true</code> if the list of serializable types contains at
   * least one concrete serializable type.
   */
  private static boolean containsConcreteSerializableTypes(
      List serializableTypes) {
    Iterator it = serializableTypes.iterator();
    while (it.hasNext()) {
      JType type = (JType) it.next();
      JType leafType = type.getLeafType();

      JClassType clazz = leafType.isClass();
      if (clazz != null && !clazz.isAbstract()) {
        return true;
      } else if (leafType.isPrimitive() != null) {
        return true;
      }
    }

    return false;
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
  private static List getAllTypesBetweenRootTypeAndSerializableLeaves(
      JClassType root, List leaves) {
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

  private static void logSerializableTypes(TreeLogger logger, JType[] types) {
    TreeLogger localLogger = logger.branch(TreeLogger.DEBUG, "Identified "
        + types.length + " serializable type"
        + ((types.length == 1) ? "" : "s"), null);

    for (int i = 0; i < types.length; ++i) {
      localLogger.branch(TreeLogger.DEBUG,
          types[i].getParameterizedQualifiedSourceName(), null);
    }
  }

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
   * Cache of the {@link JClassType} for {@link String}.
   */
  private final JClassType stringClass;

  /**
   * If <code>true</code> we will not warn if a serializable type contains a
   * non-static final field. We warn because these fields are not serialized.
   */
  private boolean suppressNonStaticFinalFieldWarnings;

  private final TypeOracle typeOracle;

  /**
   * A stack of types whose fields we are currently checking.
   */
  private final Stack /* <JType> */typesBeingAnalyzed = new Stack();

  /**
   * Map of {@link JType} to {@link MetaTypeInfo}.
   */
  private final Map /* <JType, MetaTypeInfo> */typeToMetaTypeInfo = new HashMap();

  /**
   * <code>true</code> if we encountered a violation of either automatic or
   * manual serialization which should result in an error.
   */
  private boolean validationFailed;

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
      stringClass = typeOracle.getType(String.class.getName());
      streamReaderClass = typeOracle.getType(SerializationStreamReader.class.getName());
      streamWriterClass = typeOracle.getType(SerializationStreamWriter.class.getName());

      // String is always serializable
      MetaTypeInfo stringMti = getMetaTypeInfo(stringClass);
      stringMti.setSerializable(true);
      stringMti.setMaybeInstantiated(true);

      // IncompatibleRemoteServiceException is always serializable
      MetaTypeInfo incompatibleRemoteServiceExceptionMti = getMetaTypeInfo(typeOracle.getType(IncompatibleRemoteServiceException.class.getName()));
      incompatibleRemoteServiceExceptionMti.setSerializable(true);
      incompatibleRemoteServiceExceptionMti.setMaybeInstantiated(true);
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

    TreeLogger logger = rootLogger.branch(TreeLogger.DEBUG, "Analyzing '"
        + remoteService.getParameterizedQualifiedSourceName()
        + "' for serializable types", null);

    validateRemoteService(logger, remoteService);

    if (validationFailed) {
      // the validation code has already logged why
      throw new UnableToCompleteException();
    }

    Set possiblyInstantiatedTypes = new HashSet();
    List serializableTypesList = new ArrayList();
    Iterator iterTypes = typeToMetaTypeInfo.values().iterator();
    while (iterTypes.hasNext()) {
      MetaTypeInfo mti = (MetaTypeInfo) iterTypes.next();
      JType type = mti.getType();

      if (mti.isSerializable() && type.isInterface() == null) {
        if (mti.maybeInstantiated()) {
          possiblyInstantiatedTypes.add(type);
        }
        serializableTypesList.add(type);
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
  private void checkAllSubtypesOfObject(TreeLogger localLogger) {
    /*
     * This will pull in the world and the set of serializable types will be
     * larger than it needs to be. We exclude types that do not qualify for
     * serialization to avoid generating false errors due to types that do not
     * qualify for serialization and have no serializable subtypes.
     */
    JClassType[] allTypes = typeOracle.getJavaLangObject().getSubtypes();
    for (int i = 0; i < allTypes.length; ++i) {
      JClassType cls = allTypes[i];
      MetaTypeInfo mti = getMetaTypeInfo(cls);
      if (mti.qualifiesForSerialization()) {
        checkType(localLogger, cls, true);
      }
    }
  }

  private void checkClassOrInterface(TreeLogger logger, JClassType type,
      boolean checkSubtypes) {

    JClassType superclass = type.getSuperclass();
    if (superclass != null) {
      MetaTypeInfo superMti = getMetaTypeInfo(superclass);
      if (superMti.qualifiesForSerialization()) {
        checkType(
            logger.branch(TreeLogger.DEBUG, "Analyzing superclass:", null),
            superclass, false);
      } else {
        logger.branch(TreeLogger.DEBUG, "Not analyzing superclass '"
            + superclass.getParameterizedQualifiedSourceName()
            + "' because it is not assignable to '"
            + IsSerializable.class.getName() + "' or '"
            + Serializable.class.getName()
            + "' nor does it have a custom field serializer", null);
      }
    }

    MetaTypeInfo mti = getMetaTypeInfo(type);
    if (mti.qualifiesForSerialization()) {
      if (mti.qualifiesForManualSerialization()) {
        List failures = CustomFieldSerializerValidator.validate(
            streamReaderClass, streamWriterClass, mti.getManualSerializer(),
            type);
        if (!failures.isEmpty()) {
          validationFailed = true;
          markAsUnserializableAndLog(logger, TreeLogger.ERROR, failures, mti);
          return;
        }
      } else {
        if (!mti.directlyImplementsMarkerInterface()) {
          if (superclass != null
              && !getMetaTypeInfo(superclass).isSerializable()) {
            markAsUnserializableAndLog(logger, TreeLogger.WARN, "Superclass '"
                + superclass.getQualifiedSourceName()
                + "' is not serializable and this type does not implement '"
                + IsSerializable.class.getName() + "' or '"
                + Serializable.class.getName() + "'; see previous log entries",
                mti);
            return;
          }
        }

        if (type.isLocalType()) {
          markAsUnserializableAndLog(
              logger,
              TreeLogger.WARN,
              "Is a local type, it will be excluded from the set of serializable types",
              mti);
          return;
        }

        if (type.isMemberType() && !type.isStatic()) {
          markAsUnserializableAndLog(
              logger,
              TreeLogger.WARN,
              "Is nested but not static, it will be excluded from the set of serializable types",
              mti);
          return;
        }

        // TODO: revisit this check; we probably want to field serialize a type
        // that is not default constructable, for the sake of subclasses.
        if (type.isClass() != null && !type.isAbstract()
            && !type.isDefaultInstantiable()) {
          markAsUnserializableAndLog(
              logger,
              TreeLogger.WARN,
              "Was not default instantiable (it must have a zero-argument, public constructor or no constructors at all)",
              mti);
          return;
        }
      }

      mti.setSerializable(true);

      checkFields(logger, type);

      checkMethods(logger, type);
    } else {
      logger.branch(TreeLogger.DEBUG, "Type '"
          + type.getParameterizedQualifiedSourceName()
          + "' is not assignable to '" + IsSerializable.class.getName()
          + "' or '" + Serializable.class.getName()
          + "' nor does it have a custom field serializer", null);
    }

    if (checkSubtypes) {
      if (!type.isAbstract()
          && (type.isDefaultInstantiable() || mti.qualifiesForManualSerialization())) {
        mti.setMaybeInstantiated(true);
      }

      JClassType[] subtypes = type.getSubtypes();
      if (subtypes.length > 0) {
        TreeLogger localLogger = logger.branch(TreeLogger.DEBUG,
            "Analyzing subclasses:", null);

        for (int i = 0; i < subtypes.length; ++i) {
          JClassType subType = subtypes[i];
          MetaTypeInfo smti = getMetaTypeInfo(subType);
          if (smti.qualifiesForSerialization()) {
            checkType(localLogger, subType, false);
            if (!subType.isAbstract()
                && (subType.isDefaultInstantiable() || smti.qualifiesForManualSerialization())) {
              smti.setMaybeInstantiated(true);
            }
          } else {
            localLogger.branch(TreeLogger.DEBUG, "Not analyzing subclass '"
                + subType.getParameterizedQualifiedSourceName()
                + "' because it is not assignable to '"
                + IsSerializable.class.getName() + "' or '"
                + Serializable.class.getName()
                + "' nor does it have a custom field serializer", null);
          }
        }
      }
    }
  }

  private void checkFields(TreeLogger logger, JClassType classOrInterface) {
    TreeLogger localLogger = logger;
    MetaTypeInfo mti = getMetaTypeInfo(classOrInterface);
    JField[] fields = classOrInterface.getFields();
    if (fields.length > 0) {
      localLogger = localLogger.branch(TreeLogger.DEBUG, "Analyzing Fields:",
          null);

      typesBeingAnalyzed.push(classOrInterface);

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

        if (!checkTypeRoot(fieldLogger, fieldType, false)) {
          if (mti.qualifiesForAutoSerialization()) {
            mti.setSerializable(false);
          }
        }
      }

      typesBeingAnalyzed.pop();

    } else {
      localLogger.branch(TreeLogger.DEBUG, "No fields to analyze", null);
    }
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

  /**
   * Checks that a type which qualifies as either automatically serializable or
   * manually serializable actually is.
   */
  private void checkType(TreeLogger logger, JType type, boolean isRootType) {
    if (type == null || type.isPrimitive() != null) {
      return;
    }

    TreeLogger localLogger = logger.branch(TreeLogger.DEBUG,
        type.getParameterizedQualifiedSourceName(), null);

    MetaTypeInfo mti = getMetaTypeInfo(type);
    TypeState state = mti.getState();

    if (state == SerializableTypeOracleBuilder.CHECK_FAILED) {
      logReasonsForUnserializability(localLogger, mti);
      return;
    } else if (state == SerializableTypeOracleBuilder.CHECK_IN_PROGRESS) {
      localLogger.branch(TreeLogger.DEBUG, "'"
          + type.getParameterizedQualifiedSourceName()
          + "' is being analyzed; skipping", null);
      return;
    } else if (state == SerializableTypeOracleBuilder.CHECK_SUCCEEDED) {
      if (!mti.needToRecheck(isRootType)) {
        localLogger.branch(TreeLogger.DEBUG, "Type has already been analyzed",
            null);
        return;
      }

      mti.reset();
    }

    mti.setState(SerializableTypeOracleBuilder.CHECK_IN_PROGRESS);

    if (type.isParameterized() != null) {
      JParameterizedType parameterized = type.isParameterized();
      checkType(
          localLogger.branch(TreeLogger.DEBUG, "Analyzing raw type", null),
          parameterized.getRawType(), true);

      TreeLogger branch = localLogger.branch(TreeLogger.DEBUG,
          "Analyzing type args", null);
      JType[] typeArgs = parameterized.getTypeArgs();
      for (int i = 0; i < typeArgs.length; ++i) {
        checkType(branch, typeArgs[i], true);
      }
    } else if (type.isArray() != null) {
      checkType(localLogger.branch(TreeLogger.DEBUG,
          "Analyzing component type:", null),
          type.isArray().getComponentType(), true);
    } else if (type.isClassOrInterface() != null) {
      checkClassOrInterface(localLogger, type.isClassOrInterface(), isRootType);
    }

    if (mti.getState() != SerializableTypeOracleBuilder.CHECK_FAILED) {
      mti.setState(SerializableTypeOracleBuilder.CHECK_SUCCEEDED);
    }

    mti.setTypeRoot(isRootType);
  }

  /**
   * Check any type that is used in the method signature of a
   * {@link com.google.gwt.user.client.rpc.RemoteService RemoteService} method
   * or as the field type of a type reachable from the method signature.
   */
  private boolean checkTypeRoot(TreeLogger logger, JType type,
      boolean errorOnNoSerializableSubtypes) {

    if (type.getLeafType() == typeOracle.getJavaLangObject()) {
      if (!inManualSerializationContext()) {
        markAsUnserializableAndLog(
            logger,
            TreeLogger.WARN,
            "In order to produce smaller client-side code, 'Object' is not allowed; consider using a more specific type",
            getMetaTypeInfo(type));

        if (errorOnNoSerializableSubtypes) {
          validationFailed = true;
        }

        return false;
      }

      logger.branch(
          TreeLogger.WARN,
          "Object was reached from a manually serializable type; all subtypes of Object which qualify for serialization will be considered",
          null);
    }

    checkForUnparameterizedType(logger, type);
    checkType(logger, type, true);

    List serializableTypes = getSerializableTypesAssignableTo(type);
    if (serializableTypes.size() == 0) {
      markAsUnserializableAndLog(
          logger,
          errorOnNoSerializableSubtypes ? TreeLogger.ERROR : TreeLogger.WARN,
          "Type '"
              + type.getParameterizedQualifiedSourceName()
              + "' was not serializable and none of its subtypes were either; see previous log entries",
          getMetaTypeInfo(type));

      if (errorOnNoSerializableSubtypes) {
        validationFailed = true;
      }

      return false;
    } else if (!containsConcreteSerializableTypes(serializableTypes)) {
      logger.branch(TreeLogger.WARN,
          "There are only abstract serializable types assignable to '"
              + type.getParameterizedQualifiedSourceName() + "'", null);
    }

    return true;
  }

  private MetaTypeInfo getMetaTypeInfo(JType type) {
    MetaTypeInfo mti = (MetaTypeInfo) typeToMetaTypeInfo.get(type);
    if (mti == null) {
      boolean autoSerializable = false;
      JClassType manualSerializer = null;
      JClassType classOrInterface = type.isClassOrInterface();
      if (classOrInterface != null) {
        autoSerializable = classOrInterface.isAssignableTo(isSerializableClass)
            || classOrInterface.isAssignableTo(serializableClass);
        manualSerializer = findCustomFieldSerializer(typeOracle,
            classOrInterface);
      }

      mti = new MetaTypeInfo(type, autoSerializable, manualSerializer);
      typeToMetaTypeInfo.put(type, mti);
    }

    return mti;
  }

  /**
   * Returns the list of serializable types that can be assigned to the
   * specified type. If the list was empty then there were no types which
   * qualified.
   */
  private List getSerializableTypesAssignableTo(JType type) {
    MetaTypeInfo mti = getMetaTypeInfo(type);
    List serializableTypes = mti.getSerializableTypesAssignableToMe();
    if (serializableTypes == null) {
      JArrayType isArray = type.isArray();
      JClassType isClassOrInterface = type.isClassOrInterface();
      JParameterizedType isParameterized = type.isParameterized();
      JPrimitiveType isPrimitive = type.isPrimitive();

      serializableTypes = new ArrayList();
      if (isArray != null) {
        // array
        JType leafType = isArray.getLeafType();
        if (leafType.isPrimitive() != null) {
          serializableTypes.add(isArray);
          mti.setSerializable(true);
          mti.setMaybeInstantiated(true);
        } else {
          List leafTypes = getSerializableTypesAssignableTo(leafType);
          List covariantLeafTypes = getAllTypesBetweenRootTypeAndSerializableLeaves(
              leafType.isClassOrInterface(), leafTypes);

          Iterator iter = covariantLeafTypes.iterator();
          while (iter.hasNext()) {
            JClassType clazz = (JClassType) iter.next();
            JArrayType covariantArray = getArrayType(typeOracle,
                isArray.getRank(), clazz);
            serializableTypes.add(covariantArray);

            MetaTypeInfo cmti = getMetaTypeInfo(covariantArray);
            cmti.setSerializable(true);
            cmti.setMaybeInstantiated(true);
          }
        }
      } else if (isParameterized != null) {
        // parameterized type
        JType[] typeArgs = isParameterized.getTypeArgs();
        boolean failed = false;
        for (int i = 0; i < typeArgs.length && !failed; ++i) {
          JType typeArg = typeArgs[i];
          failed = (getSerializableTypesAssignableTo(typeArg).size() == 0);
        }

        if (!failed) {
          serializableTypes = getSerializableTypesAssignableTo(isParameterized.getRawType());
        }
      } else if (isClassOrInterface != null) {
        // class or interface
        if (getMetaTypeInfo(type).isSerializable()) {
          serializableTypes.add(type);
        }

        JClassType[] subtypes = isClassOrInterface.getSubtypes();
        for (int i = 0; i < subtypes.length; ++i) {
          JClassType subtype = subtypes[i];
          if (getMetaTypeInfo(subtype).isSerializable()) {
            serializableTypes.add(subtype);
          }
        }

      } else {
        assert (isPrimitive != null && isPrimitive != JPrimitiveType.VOID);
        serializableTypes.add(type);
      }

      mti.setSerializableTypesAssignableToMe(serializableTypes);
    }

    return serializableTypes;
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
   * Returns <code>true</code> if the type which caused us to analyze the
   * current type uses manual serialization.
   * 
   * @return <code>true</code> if the type which caused us to analyze the
   *         current type uses manual serialization
   */
  private boolean inManualSerializationContext() {
    if (typesBeingAnalyzed.isEmpty()) {
      return false;
    }

    JType parent = (JType) typesBeingAnalyzed.peek();
    JClassType parentClass = parent.isClassOrInterface();

    if (parentClass != null) {
      return getMetaTypeInfo(parentClass).qualifiesForManualSerialization();
    }

    return false;
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

  private void logReasonsForUnserializability(TreeLogger logger,
      MetaTypeInfo mti) {
    Set /* <SerializationIssue> */serializationIssues = mti.getSerializationIssues();
    Iterator iter = serializationIssues.iterator();
    while (iter.hasNext()) {
      MetaTypeInfo.SerializationIssue serializationIssue = (MetaTypeInfo.SerializationIssue) iter.next();
      logger.branch(serializationIssue.issueType,
          serializationIssue.issueMessage, null);
    }
  }

  private void markAsUnserializableAndLog(TreeLogger logger,
      TreeLogger.Type logType, List failures, MetaTypeInfo mti) {
    Iterator iter = failures.iterator();
    while (iter.hasNext()) {
      markAsUnserializableAndLog(logger, logType, (String) iter.next(), mti);
    }
  }

  private void markAsUnserializableAndLog(TreeLogger logger,
      TreeLogger.Type logType, String logMessage, MetaTypeInfo mti) {
    mti.setState(SerializableTypeOracleBuilder.CHECK_FAILED);
    mti.setSerializable(false);
    mti.addSerializationIssue(logType, logMessage);
    logger.branch(logType, logMessage, null);
  }

  private void validateRemoteService(TreeLogger logger, JClassType remoteService) {
    JMethod[] methods = remoteService.getOverridableMethods();

    TreeLogger validationLogger = logger.branch(TreeLogger.DEBUG,
        "Analyzing methods:", null);

    for (int i = 0; i < methods.length; ++i) {
      JMethod method = methods[i];
      TreeLogger methodLogger = validationLogger.branch(TreeLogger.DEBUG,
          method.toString(), null);
      JType returnType = method.getReturnType();
      if (returnType != JPrimitiveType.VOID) {
        TreeLogger returnTypeLogger = methodLogger.branch(TreeLogger.DEBUG,
            "Return type: " + returnType.getParameterizedQualifiedSourceName(),
            null);
        checkTypeRoot(returnTypeLogger, returnType, true);
      }

      JParameter[] params = method.getParameters();
      for (int j = 0; j < params.length; ++j) {
        JParameter param = params[j];
        TreeLogger paramLogger = methodLogger.branch(TreeLogger.DEBUG,
            "Parameter: " + param.toString(), null);
        JType paramType = param.getType();
        checkTypeRoot(paramLogger, paramType, true);
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

          checkTypeRoot(throwsLogger, ex, true);
        }
      }
    }
  }
}
