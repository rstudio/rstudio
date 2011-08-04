/*
 * Copyright 2011 Google Inc.
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
import com.google.gwt.core.ext.typeinfo.JPrimitiveType;
import com.google.gwt.core.ext.typeinfo.JRawType;
import com.google.gwt.core.ext.typeinfo.JRealClassType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * A container for type information, for use with generator result caching.
 */
public class CachedRpcTypeInformation implements Serializable {
  private final Map<String, Long> lastModifiedTimes = new HashMap<String, Long>();
  private final Set<String> instantiableFromBrowser = new HashSet<String>();
  private final Set<String> instantiableToBrowser = new HashSet<String>();
  private final Set<String> serializableFromBrowser = new HashSet<String>();
  private final Set<String> serializableToBrowser = new HashSet<String>();
  private final Set<String> typesNotUsingCustomSerializer = new HashSet<String>();
  private final Set<String> customSerializerTypes = new HashSet<String>();

  public CachedRpcTypeInformation(SerializableTypeOracle typesFromBrowser,
      SerializableTypeOracle typesToBrowser, Set<JType> customSerializersUsed,
      Set<JType> typesNotUsingCustomSerializers) {

    recordTypes(serializableFromBrowser, instantiableFromBrowser, typesFromBrowser);
    recordTypes(serializableToBrowser, instantiableToBrowser, typesToBrowser);

    assert (customSerializersUsed != null);
    for (JType type : customSerializersUsed) {
      addCustomSerializerType(type);
    }

    assert (typesNotUsingCustomSerializers != null);
    for (JType type : typesNotUsingCustomSerializers) {
      addTypeNotUsingCustomSerializer(type);
    }
  }

  public boolean checkLastModifiedTime(JType type) {
    Long cachedTime = lastModifiedTimes.get(type.getQualifiedSourceName());
    if (cachedTime == null) {
      return false;
    }
    return cachedTime == getLastModifiedTime(type);
  }

  public boolean checkTypeInformation(TreeLogger logger, TypeOracle typeOracle,
      SerializableTypeOracle typesFromBrowser, SerializableTypeOracle typesToBrowser) {

    JType[] typesFrom = typesFromBrowser.getSerializableTypes();
    if (typesFrom.length != serializableFromBrowser.size()) {
      if (logger.isLoggable(TreeLogger.TRACE)) {
        logger.log(TreeLogger.TRACE,
            "The number of serializable types sent from the browser has changed");
        logDifferencesBetweenCurrentAndCachedTypes(logger, typesFrom, serializableFromBrowser);
      }
      return false;
    }

    JType[] typesTo = typesToBrowser.getSerializableTypes();
    if (typesTo.length != serializableToBrowser.size()) {
      if (logger.isLoggable(TreeLogger.TRACE)) {
        logger.log(TreeLogger.TRACE,
            "The number of serializable types sent to the browser has changed");
        logDifferencesBetweenCurrentAndCachedTypes(logger, typesTo, serializableToBrowser);
      }
      return false;
    }

    if (!checkTypes(logger, serializableFromBrowser, instantiableFromBrowser, typesFromBrowser)
        || !checkTypes(logger, serializableToBrowser, instantiableToBrowser, typesToBrowser)) {
      return false;
    }

    for (String customSerializerType : customSerializerTypes) {
      JType currType = typeOracle.findType(customSerializerType);
      if (currType == null) {
        logger.log(TreeLogger.TRACE, "Custom serializer no longer available: "
            + customSerializerType);
        return false;
      }
      if (!checkLastModifiedTime(currType)) {
        logger.log(TreeLogger.TRACE, "A change was detected in custom serializer: "
            + customSerializerType);
        return false;
      }
    }

    for (String sourceName : typesNotUsingCustomSerializer) {
      String fieldSerializerName =
          SerializableTypeOracleBuilder.getCustomFieldSerializerName(sourceName);
      if (SerializableTypeOracleBuilder.findCustomFieldSerializer(typeOracle, fieldSerializerName) != null) {
        logger.log(TreeLogger.TRACE, "A new custom serializer is available " + sourceName);
        return false;
      }
    }

    return true;
  }

  public boolean checkTypeNotUsingCustomSerializer(JType type) {
    return typesNotUsingCustomSerializer.contains(type.getQualifiedSourceName());
  }

  private void addCustomSerializerType(JType type) {
    String sourceName = type.getQualifiedSourceName();
    lastModifiedTimes.put(sourceName, getLastModifiedTime(type));
    customSerializerTypes.add(sourceName);
  }

  private void addTypeNotUsingCustomSerializer(JType type) {
    String sourceName = type.getQualifiedSourceName();
    typesNotUsingCustomSerializer.add(sourceName);
  }

  private boolean checkTypes(TreeLogger logger, Set<String> serializable, Set<String> instantiable,
      SerializableTypeOracle sto) {
    for (JType type : sto.getSerializableTypes()) {
      String sourceName = type.getQualifiedSourceName();
      if (sto.isSerializable(type) != serializable.contains(sourceName)
          || sto.maybeInstantiated(type) != instantiable.contains(sourceName)
          || !checkLastModifiedTime(type)) {
        logger.log(TreeLogger.TRACE, "A change was detected in type " + sourceName);
        return false;
      }
    }
    return true;
  }

  /*
   * Finds a last modified time for a type, for testing cache reusability.
   */
  private long getLastModifiedTime(JType type) {
    if (type instanceof JArrayType) {
      return getLastModifiedTime(type.getLeafType());
    } else if (type instanceof JRawType) {
      return getLastModifiedTime(((JRawType) type).getGenericType());
    }

    if (type instanceof JRealClassType) {
      return ((JRealClassType) type).getLastModifiedTime();
    } else {
      // we have a type that is an array with a primitive leafType
      assert type instanceof JPrimitiveType;
      // this type is never out of date
      return Long.MAX_VALUE;
    }
  }

  private void logDifferencesBetweenCurrentAndCachedTypes(TreeLogger logger, JType[] currentTypes,
      Set<String> cachedTypes) {

    Set<String> remainingCachedTypes = new HashSet<String>(cachedTypes);
    for (JType currentType : currentTypes) {
      String sourceName = currentType.getQualifiedSourceName();
      if (!remainingCachedTypes.remove(sourceName)) {
        logger.log(TreeLogger.TRACE, "New type " + sourceName + " not in cached list");
      }
    }

    for (String remainingCachedType : remainingCachedTypes) {
      logger.log(TreeLogger.TRACE, "Cached type " + remainingCachedType + " not in new list");
    }
  }

  private void recordTypes(Set<String> serializable, Set<String> instantiable,
      SerializableTypeOracle sto) {
    assert (sto != null);
    for (JType type : sto.getSerializableTypes()) {
      String sourceName = type.getQualifiedSourceName();
      lastModifiedTimes.put(sourceName, getLastModifiedTime(type));
      serializable.add(sourceName);
      if (sto.maybeInstantiated(type)) {
        instantiable.add(sourceName);
      }
    }
  }
}
