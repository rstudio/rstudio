/*
 * Copyright 2006 Google Inc.
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

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.typeinfo.JArrayType;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JConstructor;
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.user.client.rpc.IsSerializable;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

/**
 * This class is responsible for building an oracle can answer questions about
 * the set of serializable types that are reachable from an interface that
 * extends the
 * {@link com.google.gwt.user.client.rpc.RemoteService RemoteService} interface.
 * 
 * <p>
 * A type is serializable if:
 * <ol>
 * <li>It is a primitive type
 * <li>It is java.lang.String
 * <li>It is an array type whose component type is also serializable
 * <li>Has a custom field serializer
 * <li>Implements or inherits IsSerializable and all fields are of serializable
 * types
 * <li> Has at least one serializable concrete subtype (This type not strictly
 * serializable but it does not prevent a containing type from being
 * serializable nor does it prevent a service interface from being valid)
 * <li>Is default constructable (arrays are an exception since arrays cannot be
 * default constructed since the length must be specified)
 * </ol>
 * 
 * <p>
 * Reachable Types Algorithm:
 * <ol>
 * <li>Start with the service interface
 * <li>For every type, paramter, return and throws, listed in the method
 * signature, add it.
 * <li>For every type added, if the type inherits IsSerializable or it has a
 * custom field serializer mark it as serializable. Then, recusively add all of
 * its subtypes and the types of every field that is not <code>static</code>,
 * <code>final</code>, or <code>transient</code>.
 * </ol>
 * 
 * <p>
 * Serializability Algorithm:
 * <ol>
 * <li>Set done to true;
 * <li>For every serializable type, perform the following checks:
 * <ol>
 * <li>Has a custom serializer then skip it
 * <li>Inherits IsSerializable, check if superclass is still serializable. If
 * not, then set serializable to false, and set done to false. If it is, then
 * proceed as in c.
 * <li>Implements IsSerializable, check all fields to make sure that all of the
 * field types are serializable. If a field type is not serializable but it does
 * have a serializable subtype then continue. Otherwise, mark the type as not
 * being serializable set done to false.
 * <li>If the type does not implement or inherit IsSerializable and it does not
 * have a custom serializer, then mark it as not being serializableand set done
 * equal to false.
 * </ol>
 * <li>If done == false go back to step 1.
 * </ol>
 */
public class SerializableTypeOracleBuilder {

  private static final String CUSTOM_FIELD_SERIALIZER_SUFFIX = "_CustomFieldSerializer";
  private static final String HAS_NO_SERIALIZABLE_SUBTYPES = "Has no serializable subtypes";
  private static final String IS_LOCAL = "Is local";
  private static final String IS_NON_STATIC_NESTED = "Is nested but not static";
  private static final String IS_NOT_DEFAULT_INSTANTIABLE = "Was not default instantiable (it must have a zero-argument public constructor or no constructors at all)";
  private static final String NO_SERIALIZABLE_COMPONENT_TYPE = "component type ''{0}'' is not of a serializable type nor does it have any serializable subtypes";
  private static final String NOT_SERIALIZABLE = "Does not implement the marker interface IsSerializable, nor does it have a custom field serializer";
  private static final String RAW_TYPE_IS_NOT_SERIALIZABLE = "rawType ''{0}'' is not serializable";
  private static final String SUPERCLASS_IS_NOT_SERIALIZABLE = "Superclass ''{0}'' is not serializable";
  private static final String UNSERIALIZABLE_FIELD = "Field ''{0}'' is not of a serializable type nor does that type have serializable subtypes";
  private static final String UNSERIALIZABLE_TYPE_ARG = "typeArg ''{0}'' is not of a serializable type, nor does that type have serializable subtypes";

  /*
   * A map of type to CustomSerializer entry that handles contains information
   * about the custom serializer that handles it. If there is no entry then the
   * type does not have a custom serializer.
   */
  private Map customSerializers;

  private final JClassType objectType;

  /*
   * Set of types that are reachable from a service interface
   */
  private Map reachableTypes;

  private final Map reasonsForUnserializability = new HashMap();

  private final TreeLogger rootLogger;

  /*
   * Cache for the JClassType instance associated with the IsSerializable
   * interface
   */
  private final JClassType serializationMarkerIntf;

  private final JClassType stringType;

  private final TypeOracle typeOracle;

  public SerializableTypeOracleBuilder(TreeLogger rootLogger,
      TypeOracle typeOracle) throws NotFoundException {
    assert (rootLogger != null);
    this.rootLogger = rootLogger;

    assert (typeOracle != null);
    this.typeOracle = typeOracle;

    // Get key types up front.
    //
    objectType = typeOracle.getType("java.lang", "Object");
    stringType = typeOracle.getType("java.lang", "String");
    serializationMarkerIntf = typeOracle.getType(IsSerializable.class.getName());
  }

  /**
   * Build a {@link SerializableTypeOracle} from a array of types.
   */
  public SerializableTypeOracle build(JType[] types) throws NotFoundException {
    reachableTypes = new IdentityHashMap();

    TreeLogger logger = rootLogger.branch(TreeLogger.DEBUG,
        "Analyzing serializability for " + types.length + " types", null);

    customSerializers = initializeCustomSerializers(types);
    if (customSerializers == null) {
      return null;
    }

    initializeSerializability(types);

    updateSerializability();

    logTypes(logger);

    return new SerializableTypeOracleImpl(typeOracle, reachableTypes);
  }

  boolean implementsMarkerInterface(JClassType classOrInterface) {
    if (classOrInterface == getMarkerInterface()) {
      return true;
    }

    JClassType[] intfs = classOrInterface.getImplementedInterfaces();
    for (int i = 0; i < intfs.length; ++i) {
      JClassType intf = intfs[i];
      assert (intf != null);

      if (implementsMarkerInterface(intf)) {
        return true;
      }
    }

    return false;
  }

  private void addReasonForUnserializability(JType type, String reason) {

    JClassType coit = type.getLeafType().isClassOrInterface();
    if (coit != null && coit.isAssignableTo(getMarkerInterface())) {
      // Only report for classes that aret compatible with IsSerializable.
      // This prevents a lot of spam that won't have an obvious reason to
      // be there from the developer's perspective.
      //
      if (!reasonsForUnserializability.containsKey(type)) {
        // Keep only the first reason
        //
        reasonsForUnserializability.put(type, reason);
      }
    }
  }

  private JClassType findCustomFieldSerializer(JType type) {
    String qualifiedTypeName = type.getQualifiedSourceName();

    JClassType customSerializer = typeOracle.findType(qualifiedTypeName
        + CUSTOM_FIELD_SERIALIZER_SUFFIX);
    if (customSerializer != null) {
      return customSerializer;
    }

    // Try with the regular name
    String simpleSerializerName = qualifiedTypeName
        + CUSTOM_FIELD_SERIALIZER_SUFFIX;
    String[] packagePaths = getPackagePaths();
    for (int i = 0; i < packagePaths.length; ++i) {
      customSerializer = typeOracle.findType(packagePaths[i] + "."
          + simpleSerializerName);
      if (customSerializer != null) {
        return customSerializer;
      }
    }

    return null;
  }

  private JClassType getMarkerInterface() {
    return serializationMarkerIntf;
  }

  private JClassType getObjectType() {
    return objectType;
  }

  private String[] getPackagePaths() {
    return new String[] {"com.google.gwt.user.client.rpc.core"};
  }

  private String getReasonForUnserializability(JType type) {
    return (String) reasonsForUnserializability.get(type);
  }

  private JType[] getSortedArray(ArrayList typeNames) {
    JType[] sortedTypes = (JType[]) typeNames.toArray(new JType[typeNames.size()]);
    Arrays.sort(sortedTypes, new Comparator() {
      public int compare(Object o1, Object o2) {
        JType t1 = (JType) o1;
        JType t2 = (JType) o2;
        return t1.getParameterizedQualifiedSourceName().compareTo(
            t2.getParameterizedQualifiedSourceName());
      }
    });

    return sortedTypes;
  }

  private JClassType getStringType() {
    return stringType;
  }

  /*
   * Returns true if the type has a custom serializer.
   * 
   */
  private boolean hasCustomFieldSerializer(JType type) {
    SerializableType reachableType = (SerializableType) reachableTypes.get(type);

    if (reachableType == null) {
      return false;
    }

    return reachableType.getCustomSerializer() != null;
  }

  /**
   * Given a type that is assumed to be serializable, see if all of it's
   * declared fields are serializable.
   */
  private boolean hasSerializableFields(JType type) {
    assert (isSerializable(type));

    JClassType classOrInterface = type.isClassOrInterface();
    if (classOrInterface == null) {
      return true;
    }

    JField[] fields = classOrInterface.getFields();
    for (int index = 0; index < fields.length; ++index) {
      JField field = fields[index];

      // TODO(mmendez): this test will be replicated in a couple of other
      // places, refactor and join
      //
      if (field.isStatic() || field.isTransient() || field.isFinal()) {
        continue;
      }

      JType fieldType = field.getType();
      if (isSerializable(fieldType)) {
        continue;
      }

      if (!hasSerializableSubtypes(fieldType)) {
        addReasonForUnserializability(type, MessageFormat.format(
            UNSERIALIZABLE_FIELD, new String[] {field.toString()}));
        return false;
      }
    }

    return true;
  }

  /**
   * See if a type has any subtypes that are serializable.
   */
  private boolean hasSerializableSubtypes(JType type) {
    assert (type.isPrimitive() == null);

    if (isObject(type)) {
      return false;
    }

    JClassType classOrInterface = type.isClassOrInterface();
    if (classOrInterface == null) {
      return false;
    }

    JClassType[] subTypes = classOrInterface.getSubtypes();
    for (int index = 0; index < subTypes.length; ++index) {
      JClassType subType = subTypes[index];
      if (isSerializable(subType)) {
        return true;
      }
    }

    addReasonForUnserializability(type, HAS_NO_SERIALIZABLE_SUBTYPES);

    return false;
  }

  /*
   * If this type is or does not implement IsSerializable then check the super
   * type.
   */
  private boolean inheritedMarkerInterface(JClassType classOrInterface) {
    assert (classOrInterface != null);
    assert (((SerializableType) reachableTypes.get(classOrInterface)).isSerializable());

    JClassType intfs[] = classOrInterface.getImplementedInterfaces();
    for (int index = 0; index < intfs.length; ++index) {
      JClassType intf = intfs[index];
      assert (intf != null);

      if (intf.isAssignableTo(getMarkerInterface())) {
        return false;
      }
    }

    JClassType superClass = classOrInterface.getSuperclass();
    if (superClass != null) {
      return superClass.isAssignableTo(getMarkerInterface());
    }

    return false;
  }

  /**
   * Identify the custom field serializers that are available.
   */
  private Map initializeCustomSerializers(JType[] types) {
    ArrayList customSerializersAccum = new ArrayList();
    for (int i = 0; i < types.length; ++i) {
      JClassType serializer = findCustomFieldSerializer(types[i]);
      if (serializer != null) {
        customSerializersAccum.add(serializer);
      }
    }

    JClassType[] serializers = (JClassType[]) customSerializersAccum.toArray(new JClassType[customSerializersAccum.size()]);

    // Validate the list of potential custom field serializers
    //
    return CustomFieldSerializerValidator.validateCustomFieldSerializers(
        rootLogger, typeOracle, serializers);
  }

  private void initializeSerializability(JType[] types) {
    assert (types != null);

    for (int index = 0; index < types.length; ++index) {
      JType type = types[index];

      assert (!reachableTypes.containsKey(type));

      // Assume that a type is serializable and let the dynamic determine
      // whether it is or not
      //
      CustomSerializerInfo customSerializerInfo = (CustomSerializerInfo) customSerializers.get(type);
      reachableTypes.put(type, new SerializableType(type, true,
          customSerializerInfo));
    }
  }

  /*
   * Returns true if the class can be instantiated via a public parameterless
   * constructor.
   */
  private boolean isDefaultInstantiable(JClassType cls) {
    if (!cls.isDefaultInstantiable()) {
      return false;
    }

    try {
      JConstructor defaultCtor = cls.getConstructor(new JType[0]);
      if (!defaultCtor.isPublic()) {
        return false;
      }
    } catch (NotFoundException e) {
      // Purposely ignore the exception
    }

    return true;
  }

  /*
   * Returns true if this type is object.
   */
  private boolean isObject(JType type) {
    if (type == getObjectType()) {
      return true;
    }

    return false;
  }

  /**
   * Returns true if the type is serializable.
   * 
   * @param type
   * @return true if the type is serializable and false if we do not know about
   *         it or we know that it is not serializable.
   */
  private boolean isSerializable(JType type) {
    if (isObject(type)) {
      return false;
    }

    if (isString(type)) {
      return true;
    }

    SerializableType reachableType = (SerializableType) reachableTypes.get(type);

    if (reachableType == null) {
      return false;
    }

    return reachableType.isSerializable();
  }

  /*
   * Return true if this type is java.lang.String
   */
  private boolean isString(JType type) {
    if (type == getStringType()) {
      return true;
    }

    return false;
  }

  private void logTypes(TreeLogger logger) {
    Set entrySet = reachableTypes.entrySet();
    Iterator entrySetIter = entrySet.iterator();
    ArrayList serializableTypesList = new ArrayList();
    ArrayList unserializableTypesList = new ArrayList();
    while (entrySetIter.hasNext()) {
      Entry entry = (Entry) entrySetIter.next();
      SerializableType reachableType = (SerializableType) entry.getValue();

      ArrayList targetList;
      if (reachableType.isSerializable()) {
        targetList = serializableTypesList;
      } else {
        targetList = unserializableTypesList;
      }

      targetList.add(reachableType.getType());
    }

    JType[] serializableTypes = getSortedArray(serializableTypesList);
    if (serializableTypes.length > 0) {
      TreeLogger localLogger = logger.branch(TreeLogger.DEBUG,
          "The following types were determined to be serializable:", null);
      for (int i = 0; i < serializableTypes.length; ++i) {
        localLogger.log(TreeLogger.DEBUG,
            serializableTypes[i].getParameterizedQualifiedSourceName(), null);
      }
    }

    JType[] unserializableTypes = getSortedArray(unserializableTypesList);
    if (unserializableTypes.length > 0) {
      // It's a DEBUG message unless one of the unserializable types has a
      // specific reported reason for unserializability that the developer
      // can actually do something about, in which case the child logger
      // will WARN, which will upgrade this branch.
      //
      TreeLogger localLogger = logger.branch(TreeLogger.DEBUG,
          "The following types were determined to be unserializable:", null);

      for (int i = 0; i < unserializableTypes.length; ++i) {
        JType unserializableType = unserializableTypes[i];
        String reason = getReasonForUnserializability(unserializableType);
        if (reason != null) {
          TreeLogger branch = localLogger.branch(TreeLogger.INFO,
              unserializableType.getParameterizedQualifiedSourceName(), null);
          branch.log(TreeLogger.INFO, reason, null);
        } else {
          localLogger.log(TreeLogger.DEBUG,
              unserializableType.getParameterizedQualifiedSourceName(), null);
        }
      }
    }
  }

  /*
   * Enumerate over the serializable types checking serializability until there
   * are no more changes.
   * 
   * At this point we have a set of types that contains some serializable types.
   * The serializable types meet the following criteria 1) Have a custom
   * serializer 2) Implement IsSerializable directly 3) Inherit IsSerializable
   * from a base type 4) Contain serializable fields, these are either of a
   * serializable type or of a type that has a serializable subtype
   */
  private void updateSerializability() {
    Set entrySet = reachableTypes.entrySet();
    boolean done = false;

    while (!done) {
      done = true;
      Iterator entrySetIter = entrySet.iterator();
      while (entrySetIter.hasNext()) {
        Entry entry = (Entry) entrySetIter.next();
        SerializableType reachableType = (SerializableType) entry.getValue();

        if (!reachableType.isSerializable()) {
          continue;
        }

        boolean changed = updateSerializability(reachableType);
        if (changed) {
          reachableType.setSerializable(false);
          done = false;
        }
      }
    }
  }

  /**
   * Performs a series of checks on a reachable type to determine if it is
   * serializable. Returns true if the type was thought to have been
   * serializable and now we have determined that it is not. Once a type is
   * deemed to not be serializable then it can never become serializable for
   * this compilation.
   */
  private boolean updateSerializability(SerializableType reachableType) {
    if (reachableType.hasCustomSerializer()) {
      return false;
    }

    if (!reachableType.isSerializable()) {
      return false;
    }

    JType type = reachableType.getType();
    if (isString(type)) {
      return false;
    }

    JPrimitiveType isPrimitive = type.isPrimitive();
    if (isPrimitive != null) {
      return false;
    }

    JArrayType isArray = type.isArray();
    if (isArray != null) {
      return updateSerializabilityForArray(isArray);
    }

    JParameterizedType parameterizedType = type.isParameterized();
    if (parameterizedType != null) {
      return updateSerializabilityForParameterizedType(parameterizedType);
    }

    JClassType isInterface = type.isInterface();
    if (isInterface != null) {
      return updateSerializabilityForInterface(isInterface);
    }

    return updateSerializabilityForClass(type);
  }

  private boolean updateSerializabilityForArray(JArrayType isArray) {
    assert (isSerializable(isArray));

    JType compType = isArray.getComponentType();
    assert (compType != null);

    if (!isSerializable(compType) && !hasSerializableSubtypes(compType)) {
      addReasonForUnserializability(isArray, MessageFormat.format(
          NO_SERIALIZABLE_COMPONENT_TYPE,
          new String[] {compType.getQualifiedSourceName()}));
      return true;
    }

    return false;
  }

  /*
   * A class is serializable if:
   * 
   * It is assignable to IsSerializable
   * 
   * All of it's field types are serializable
   * 
   * If it inherited serializability then it's superclass must still be
   * serializable
   * 
   */
  private boolean updateSerializabilityForClass(JType type) {
    assert (!hasCustomFieldSerializer(type));

    JClassType isClass = type.isClass();
    assert (type.isClass() != null);

    // If the class is a local class, it is not-serializable.
    //
    if (isClass.isLocalType()) {
      addReasonForUnserializability(type, IS_LOCAL);
      return true;
    }

    // If the class is non-static and nested, it is not-serializable.
    // We might be able to support it in the future, but not in this version.
    //
    if (isClass.getEnclosingType() != null) {
      if (!isClass.isStatic()) {
        addReasonForUnserializability(type, IS_NON_STATIC_NESTED);
        return true;
      }
    }

    // If the class is not default instantiable then it cannot be serialized.
    // if it has a custom serializaer then it should never reach here
    //
    if (!isDefaultInstantiable(isClass)) {
      addReasonForUnserializability(type, IS_NOT_DEFAULT_INSTANTIABLE);
      return true;
    }

    // If the class implements the marker interface, then the parent type does
    // not matter. so just check the fields
    //
    if (implementsMarkerInterface(isClass)) {
      return !hasSerializableFields(isClass);
    }

    // If the type inherited the marker interface AND its supertype is still
    // considered serializable AND all of this type's fields are serializable
    // then it is still serializable.
    //
    if (inheritedMarkerInterface(isClass)) {
      JClassType superClass = isClass.getSuperclass();
      if (isSerializable(superClass)) {
        if (hasSerializableFields(type)) {
          return false;
        }
      } else {
        addReasonForUnserializability(type, MessageFormat.format(
            SUPERCLASS_IS_NOT_SERIALIZABLE,
            new String[] {superClass.getQualifiedSourceName()}));
      }
    } else {
      addReasonForUnserializability(type, NOT_SERIALIZABLE);
    }

    return true;
  }

  private boolean updateSerializabilityForInterface(JClassType isInterface) {
    assert (isInterface != null);
    assert (isSerializable(isInterface));

    if (isInterface.isAssignableTo(getMarkerInterface())) {
      return false;
    }

    addReasonForUnserializability(isInterface, NOT_SERIALIZABLE);
    return true;
  }

  private boolean updateSerializabilityForParameterizedType(
      JParameterizedType parameterizedType) {
    assert (parameterizedType != null);
    assert (isSerializable(parameterizedType));

    JType rawType = parameterizedType.getRawType();
    if (!isSerializable(rawType)) {
      addReasonForUnserializability(parameterizedType, MessageFormat.format(
          RAW_TYPE_IS_NOT_SERIALIZABLE,
          new String[] {rawType.getQualifiedSourceName()}));
      return true;
    }

    JType[] typeArgs = parameterizedType.getTypeArgs();
    for (int index = 0; index < typeArgs.length; ++index) {
      JType type = typeArgs[index];
      if (!isSerializable(type) && !hasSerializableSubtypes(type)) {
        addReasonForUnserializability(parameterizedType, MessageFormat.format(
            UNSERIALIZABLE_TYPE_ARG,
            new String[] {type.getParameterizedQualifiedSourceName()}));
        return true;
      }
    }

    return false;
  }
}
