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
  private static class MetaTypeInfo {
    /**
     * An issue that prevents a type from being serializable.
     */
    private static class SerializationIssue {
      final String issueMessage;

      final TreeLogger.Type issueType;

      SerializationIssue(Type issueType, String issueMessage) {
        this.issueType = issueType;
        this.issueMessage = issueMessage;
      }
    }

    /**
     * <code>true</code> if the type is assignable to {@link IsSerializable}
     * or {@link java.io.Serializable Serializable}.
     */
    private boolean autoSerializable;

    /**
     * <code>true</code> if the type was checked from a custom field
     * serializer.
     */
    private boolean checkedInManualContext;

    /**
     * <code>true</code> if the subtypes of this type were also checked.
     */
    private boolean checkedSubtypes;

    /**
     * Custom field serializer or <code>null</code> if there isn't one.
     */
    private JClassType manualSerializer;

    /**
     * <code>true</code> if the type is automatically or manually serializable
     * and the corresponding checks succeed.
     */
    private boolean serializable;

    /**
     * List of serialization warnings or errors that prevent this type from
     * being serializable.
     */
    private Set /* <SerializationIssue> */serializationIssues;

    /**
     * The state that this type is currently in.
     */
    private TypeState state = SerializableTypeOracleBuilder.NOT_CHECKED;

    /**
     * {@link JType} associated with this metadata.
     */
    private final JType type;

    MetaTypeInfo(JType type, boolean autoSerializable,
        JClassType manualSerializer) {
      this.type = type;
      this.autoSerializable = autoSerializable;
      this.manualSerializer = manualSerializer;
    }

    public void addSerializationIssue(TreeLogger.Type issueType,
        String issueMessage) {
      if (serializationIssues == null) {
        serializationIssues = new TreeSet/* <SerializationIssue> */();
      }

      serializationIssues.add(new SerializationIssue(issueType, issueMessage));
    }

    public void clearSerializationIssues() {
      if (serializationIssues != null) {
        serializationIssues.clear();
      }
    }

    public boolean getCheckedInManualContext() {
      return checkedInManualContext;
    }

    public boolean getCheckedSubtypes() {
      return checkedSubtypes;
    }

    public JClassType getManualSerializer() {
      return manualSerializer;
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
     * Returns <code>true</code> if this type needs to be rechecked. This
     * happens if we are asked to check the subtypes and we did not previously
     * check them. Or if this type is not serializable and we previously checked
     * this type from a type which used manual serialization.
     * 
     * @param checkSubtypes <code>true</code> if we need to check the subtypes
     * @param inManualContext <code>true</code> if we are checking this type
     *          from a manually serializable type
     * @return <code>true</code> if this type needs to be rechecked
     */
    public boolean needToRecheck(boolean checkSubtypes, boolean inManualContext) {
      assert (state == SerializableTypeOracleBuilder.CHECK_SUCCEEDED);

      if (!qualifiesForSerialization() && !inManualContext
          && getCheckedInManualContext()) {
        return true;
      }

      if (checkSubtypes && !getCheckedSubtypes()) {
        return true;
      }

      return false;
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

    public void setCheckedInManualContext(boolean checkedInManualContext) {
      this.checkedInManualContext = checkedInManualContext;
    }

    public void setCheckedSubtypes(boolean checkedSubtypes) {
      this.checkedSubtypes = checkedSubtypes;
    }

    public void setSerializable(boolean serializable) {
      this.serializable = serializable;
    }

    public void setState(TypeState newState) {
      state = newState;
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
   * If <code>true</code> it is not an error if all of the subtypes of an
   * automatically serializable type are not themselves serializable.
   */
  private boolean allowUnserializableSubtypesOfAutoSerializableTypes;

  /**
   * Cache of the {@link JClassType} for {@link Collection}.
   */
  private final JClassType collectionClass;

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
      isSerializableClass = typeOracle.getType(IsSerializable.class.getName());
      mapClass = typeOracle.getType(Map.class.getName());
      serializableClass = typeOracle.getType(Serializable.class.getName());
      stringClass = typeOracle.getType(String.class.getName());
      streamReaderClass = typeOracle.getType(SerializationStreamReader.class.getName());
      streamWriterClass = typeOracle.getType(SerializationStreamWriter.class.getName());

      // String is always serializable
      MetaTypeInfo stringMti = getMetaTypeInfo(stringClass);
      stringMti.setSerializable(true);

      // IncompatibleRemoteServiceException is always serializable
      MetaTypeInfo incompatibleRemoteServiceExceptionMti = getMetaTypeInfo(typeOracle.getType(IncompatibleRemoteServiceException.class.getName()));
      incompatibleRemoteServiceExceptionMti.setSerializable(true);
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

    List serializableTypesList = new ArrayList();
    Iterator iterTypes = typeToMetaTypeInfo.values().iterator();
    while (iterTypes.hasNext()) {
      MetaTypeInfo mti = (MetaTypeInfo) iterTypes.next();
      JType type = mti.getType();

      if (mti.isSerializable() && type.isInterface() == null) {
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

    return new SerializableTypeOracleImpl(typeOracle, serializableTypes);
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

  /**
   * The component type of an array must be serializable.
   */
  private void checkArray(TreeLogger logger, JArrayType array) {
    checkType(
        logger.branch(TreeLogger.DEBUG, "Analyzing component type:", null),
        array.getComponentType(), true);

    JType leafType = array.getLeafType();
    JClassType classOrInterface = leafType.isClassOrInterface();
    if (leafType.isPrimitive() != null) {
      getMetaTypeInfo(array).setSerializable(true);
    } else if (classOrInterface != null) {
      MetaTypeInfo mti = getMetaTypeInfo(classOrInterface);
      if (mti.isSerializable()) {
        getMetaTypeInfo(array).setSerializable(true);
      }

      JClassType[] subtypes = classOrInterface.getSubtypes();
      for (int i = 0; i < subtypes.length; ++i) {
        JClassType component = subtypes[i];
        MetaTypeInfo cmti = getMetaTypeInfo(component);
        if (cmti.isSerializable()) {
          JArrayType covariantArray = getArrayType(typeOracle, array.getRank(),
              component);

          logger.branch(TreeLogger.DEBUG,
              covariantArray.getParameterizedQualifiedSourceName(), null);

          getMetaTypeInfo(covariantArray).setSerializable(true);
        }
      }
    }
  }

  private void checkClassOrInterface(TreeLogger logger, JClassType type,
      boolean checkSubtypes) {
    if (type == stringClass) {
      // we know that it is serializable
      return;
    }

    MetaTypeInfo mti = getMetaTypeInfo(type);
    if (type == typeOracle.getJavaLangObject()) {
      if (inManualSerializationContext()) {
        TreeLogger branch = logger.branch(
            TreeLogger.WARN,
            "Object was reached from a manually serializable type; all subtypes of Object which qualify for serialization will be considered",
            null);
        checkAllSubtypesOfObject(branch);
      } else {
        setUnserializableAndLog(
            logger,
            TreeLogger.ERROR,
            "In order to produce smaller client-side code, 'Object' is not allowed; consider using a more specific type",
            mti);
      }

      return;
    }

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

    if (mti.qualifiesForSerialization()) {
      if (mti.qualifiesForManualSerialization()) {
        List failures = CustomFieldSerializerValidator.validate(
            streamReaderClass, streamWriterClass, mti.getManualSerializer(),
            type);
        if (!failures.isEmpty()) {
          setUnserializableAndLog(logger, TreeLogger.ERROR, failures, mti);
          return;
        }
      } else {
        if (type.isLocalType()) {
          setUnserializableAndLog(
              logger,
              TreeLogger.WARN,
              "Is a local type, it will be excluded from the set of serializable types",
              mti);
          return;
        }

        if (type.isMemberType() && !type.isStatic()) {
          setUnserializableAndLog(
              logger,
              TreeLogger.WARN,
              "Is nested but not static, it will be excluded from the set of serializable types",
              mti);
          return;
        }

        if (type.isClass() != null && !type.isAbstract()
            && !type.isDefaultInstantiable()) {
          setUnserializableAndLog(
              logger,
              TreeLogger.ERROR,
              "Was not default instantiable (it must have a zero-argument public constructor or no constructors at all)",
              mti);
          return;
        }
      }

      mti.setSerializable(true);

      checkFields(logger, type);

      checkMethods(logger, type);
    }

    if (checkSubtypes) {
      int nSubtypes = 0;
      int nSerializableSubtypes = 0;

      JClassType[] subtypes = type.getSubtypes();
      if (subtypes.length > 0) {
        TreeLogger localLogger = logger.branch(TreeLogger.DEBUG,
            "Analyzing subclasses:", null);

        for (int i = 0; i < subtypes.length; ++i) {
          JClassType subtype = subtypes[i];
          MetaTypeInfo smti = getMetaTypeInfo(subtype);
          if (smti.qualifiesForSerialization()) {
            checkType(localLogger, subtype, false);

            ++nSubtypes;

            if (smti.isSerializable()) {
              ++nSerializableSubtypes;
            } else {
              localLogger.branch(TreeLogger.DEBUG,
                  subtype.getParameterizedQualifiedSourceName()
                      + " is not serializable", null);

              if (subtype.isLocalType() || subtype.isMemberType()
                  && !subtype.isStatic()) {
                --nSubtypes;
              }
            }
          } else {
            localLogger.branch(TreeLogger.DEBUG, "Not analyzing subclass '"
                + subtype.getParameterizedQualifiedSourceName()
                + "' because it is not assignable to '"
                + IsSerializable.class.getName() + "' or '"
                + Serializable.class.getName()
                + "' nor does it have a custom field serializer", null);
          }
        }
      }

      if (mti.qualifiesForAutoSerialization()) {
        if (nSerializableSubtypes < nSubtypes) {
          setUnserializableAndLog(logger,
              allowUnserializableSubtypesOfAutoSerializableTypes
                  ? TreeLogger.WARN : TreeLogger.ERROR,
              "Not all subtypes of the automatically serializable type '"
                  + type.getQualifiedSourceName()
                  + "' are themselves automatically serializable", mti);
        }
      } else if (!mti.qualifiesForManualSerialization()
          && nSerializableSubtypes == 0) {
        /*
         * The type does not qualify for either serialization and it has no
         * serializable subtypes; this is only an error if we are not in the
         * context of a custom field serializer
         */
        String message = MessageFormat.format(
            "Type ''{0}'' is not assignable to IsSerializable or java.io.Serializable, it does not have a custom field serializer and it does not have any serializable subtypes",
            new String[] {type.getParameterizedQualifiedSourceName()});
        setUnserializableAndLog(logger, inManualSerializationContext()
            ? TreeLogger.WARN : TreeLogger.ERROR, message, mti);
      }
    }
  }

  private void checkFields(TreeLogger logger, JClassType classOrInterface) {
    TreeLogger localLogger = logger;
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
        checkForUnparameterizedType(fieldLogger, fieldType);
        checkType(fieldLogger, fieldType, true);
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
  private void checkType(TreeLogger logger, JType type, boolean checkSubtypes) {
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
      if (!mti.needToRecheck(checkSubtypes, inManualSerializationContext())) {
        localLogger.branch(TreeLogger.DEBUG, "Type has already been analyzed",
            null);
        return;
      }

      mti.clearSerializationIssues();
    }

    mti.setState(SerializableTypeOracleBuilder.CHECK_IN_PROGRESS);

    if (type.isParameterized() != null) {
      JParameterizedType parameterized = type.isParameterized();
      checkType(
          localLogger.branch(TreeLogger.DEBUG, "Analyzing raw type", null),
          parameterized.getRawType(), true);

      checkTypes(localLogger.branch(TreeLogger.DEBUG, "Analyzing type args",
          null), parameterized.getTypeArgs());
    } else if (type.isArray() != null) {
      checkArray(localLogger, type.isArray());
    } else if (type.isClassOrInterface() != null) {
      checkClassOrInterface(localLogger, type.isClassOrInterface(),
          checkSubtypes);
    }

    if (mti.getState() != SerializableTypeOracleBuilder.CHECK_FAILED) {
      mti.setState(SerializableTypeOracleBuilder.CHECK_SUCCEEDED);
    }

    mti.setCheckedSubtypes(checkSubtypes);
    mti.setCheckedInManualContext(inManualSerializationContext());
  }

  private void checkTypes(TreeLogger logger, JType[] types) {
    for (int i = 0; i < types.length; ++i) {
      checkType(logger, types[i], true);
    }
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

  private void initializeProperties(TreeLogger logger,
      PropertyOracle propertyOracle) {
    // Assume subtype warnings unless user explicitly turns it off.
    allowUnserializableSubtypesOfAutoSerializableTypes = false;

    try {
      String propVal = propertyOracle.getPropertyValue(logger,
          "gwt.allowUnserializableSubtypesOfAutoSerializableTypes");
      if (propVal.equals("true")) {
        allowUnserializableSubtypesOfAutoSerializableTypes = true;
      }
    } catch (BadPropertyValueException e) {
      // Purposely ignored, because we do not want to allow subtypes that are
      // not serializable by default.
    }

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

  private void setUnserializableAndLog(TreeLogger logger,
      TreeLogger.Type logType, List failures, MetaTypeInfo mti) {
    Iterator iter = failures.iterator();
    while (iter.hasNext()) {
      setUnserializableAndLog(logger, logType, (String) iter.next(), mti);
    }
  }

  private void setUnserializableAndLog(TreeLogger logger,
      TreeLogger.Type logType, String logMessage, MetaTypeInfo mti) {
    mti.setState(SerializableTypeOracleBuilder.CHECK_FAILED);
    mti.setSerializable(false);
    mti.addSerializationIssue(logType, logMessage);

    if (logType == TreeLogger.ERROR) {
      validationFailed = true;
    }

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
      TreeLogger returnTypeLogger = methodLogger.branch(TreeLogger.DEBUG,
          "Return type: " + returnType.getParameterizedQualifiedSourceName(),
          null);
      checkForUnparameterizedType(returnTypeLogger, returnType);
      checkType(returnTypeLogger, returnType, true);

      JParameter[] params = method.getParameters();
      for (int j = 0; j < params.length; ++j) {
        JParameter param = params[j];
        TreeLogger paramLogger = methodLogger.branch(TreeLogger.DEBUG,
            "Parameter: " + param.toString(), null);
        JType paramType = param.getType();
        checkForUnparameterizedType(paramLogger, paramType);
        checkType(paramLogger, paramType, true);
      }

      JType[] exs = method.getThrows();
      if (exs.length > 0) {
        checkTypes(methodLogger.branch(TreeLogger.DEBUG, "Throws:", null),
            method.getThrows());
      }
    }
  }
}
