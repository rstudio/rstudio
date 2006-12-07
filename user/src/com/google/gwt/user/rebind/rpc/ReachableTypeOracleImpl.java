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
import com.google.gwt.core.ext.typeinfo.JField;
import com.google.gwt.core.ext.typeinfo.JMethod;
import com.google.gwt.core.ext.typeinfo.JParameter;
import com.google.gwt.core.ext.typeinfo.JParameterizedType;
import com.google.gwt.core.ext.typeinfo.JType;

import java.util.HashMap;
import java.util.HashSet;

/**
 * This class provides an implementation of the ReachableTypeOracle.
 */
final class ReachableTypeOracleImpl implements ReachableTypeOracle {
  private TreeLogger rootLogger;
  private HashMap reachableTypeCache = new HashMap();

  ReachableTypeOracleImpl(TreeLogger rootLogger) {
    this.rootLogger = rootLogger;
  }

  public JType[] getTypesReachableFromInterface(JClassType intf) {
    if ((intf == null) || (intf.isInterface() == null)) {
      // TODO(mmendez): add the proper error message
      throw new IllegalArgumentException();
    }

    HashSet cachedResult = (HashSet) reachableTypeCache.get(intf);
    if (cachedResult != null) {
      return (JType[]) cachedResult.toArray(new JType[cachedResult.size()]);
    }

    HashSet reachableTypes = new HashSet();

    addTypesReachableFromInterface(rootLogger, reachableTypes, intf);

    reachableTypeCache.put(intf, reachableTypes);

    return (JType[]) reachableTypes.toArray(new JType[reachableTypes.size()]);
  }

  public JType[] getTypesReachableFromMethod(JMethod method) {
    HashSet cachedResult = (HashSet) reachableTypeCache.get(method);
    if (cachedResult != null) {
      return (JType[]) cachedResult.toArray(new JType[cachedResult.size()]);
    }

    HashSet reachableTypes = new HashSet();

    addTypesReachableFromMethod(rootLogger, reachableTypes, method);

    reachableTypeCache.put(method, reachableTypes);

    return (JType[]) reachableTypes.toArray(new JType[reachableTypes.size()]);
  }

  public JType[] getTypesReachableFromType(JType type) {
    if (type.isClass() == null && type.isParameterized() == null
        && type.isArray() == null) {
      // TODO(mmendez): add the proper error message
      throw new IllegalArgumentException();
    }

    HashSet cachedResult = (HashSet) reachableTypeCache.get(type);
    if (cachedResult != null) {
      return (JType[]) cachedResult.toArray(new JType[cachedResult.size()]);
    }

    HashSet reachableTypes = new HashSet();

    addTypesReachableFromType(rootLogger, reachableTypes, type);

    reachableTypeCache.put(type, reachableTypes);

    return (JType[]) reachableTypes.toArray(new JType[reachableTypes.size()]);
  }

  private void addTypeReachableFromParameterizedType(TreeLogger logger,
      HashSet reachableTypes, JParameterizedType parameterizedType) {
    assert (parameterizedType != null);

    addTypesReachableFromType(logger, reachableTypes,
        parameterizedType.getRawType());

    JType typeArgs[] = parameterizedType.getTypeArgs();
    for (int index = 0; index < typeArgs.length; ++index) {
      JType typeArg = typeArgs[index];

      addTypesReachableFromType(logger, reachableTypes, typeArg);
    }
  }

  private void addTypesReachableFromArray(TreeLogger logger,
      HashSet reachableTypes, JArrayType arrayType) {
    assert (arrayType != null);
    JType componentType = arrayType.getComponentType();
    addTypesReachableFromType(logger, reachableTypes, componentType);
  }

  private void addTypesReachableFromClassOrInterface(TreeLogger logger,
      HashSet reachableTypes, JClassType classOrInterface) {
    JField[] fields = classOrInterface.getFields();
    for (int fieldIndex = 0; fieldIndex < fields.length; ++fieldIndex) {
      JField field = fields[fieldIndex];
      if (field.isStatic() || field.isTransient()) {
        continue;
      }

      JType fieldType = field.getType();
      if (fieldType != null) {
        addTypesReachableFromType(logger, reachableTypes, fieldType);
      }
    }

    // Examine the implemented interfaces
    //
    JClassType[] superIntfs = classOrInterface.getImplementedInterfaces();
    for (int superIntfIndex = 0; superIntfIndex < superIntfs.length; ++superIntfIndex) {
      JClassType superIntf = superIntfs[superIntfIndex];

      addTypesReachableFromType(logger, reachableTypes, superIntf);
    }

    // Examine the superclass
    //
    JClassType superclass = classOrInterface.getSuperclass();
    if (superclass != null) {
      addTypesReachableFromType(logger, reachableTypes, superclass);
    }

    // Examine the subtypes
    //
    JClassType[] subTypes = classOrInterface.getSubtypes();
    for (int subTypeIndex = 0; subTypeIndex < subTypes.length; ++subTypeIndex) {
      JClassType subType = subTypes[subTypeIndex];

      addTypesReachableFromType(logger, reachableTypes, subType);
    }
  }

  /*
   * Add all types reachable from this interface.
   */
  private void addTypesReachableFromInterface(TreeLogger logger,
      HashSet reachableTypes, JClassType intf) {
    logger = logger.branch(TreeLogger.DEBUG,
        "Adding types reachable from interface "
            + intf.getQualifiedSourceName(), null);

    JClassType[] intfs = intf.getImplementedInterfaces();
    for (int intfIndex = 0; intfIndex < intfs.length; ++intfIndex) {
      JClassType intfImpl = intfs[intfIndex];
      assert (intfImpl != null);

      addTypesReachableFromInterface(logger, reachableTypes, intfImpl);
    }

    JMethod[] methods = intf.getMethods();
    for (int methodIndex = 0; methodIndex < methods.length; ++methodIndex) {
      JMethod method = methods[methodIndex];
      assert (method != null);

      addTypesReachableFromMethod(logger, reachableTypes, method);
    }
  }

  /*
   * Add all types that are reachable from a method by considering the
   * parameters return type, and exceptions thrown.
   */
  private void addTypesReachableFromMethod(TreeLogger logger,
      HashSet reachableTypes, JMethod method) {
    logger = logger.branch(
        TreeLogger.DEBUG,
        "Adding types reachable from method " + method.getReadableDeclaration(),
        null);

    JType returnType = method.getReturnType();
    if (returnType != null) {
      addTypesReachableFromType(logger, reachableTypes, returnType);
    }

    JParameter[] params = method.getParameters();
    for (int paramIndex = 0; paramIndex < params.length; ++paramIndex) {
      JParameter param = params[paramIndex];
      addTypesReachableFromType(logger, reachableTypes, param.getType());
    }

    JType[] typesThrown = method.getThrows();
    for (int index = 0; index < typesThrown.length; ++index) {
      JType typeThrown = typesThrown[index];
      addTypesReachableFromType(logger, reachableTypes, typeThrown);
    }
  }

  /*
   * Implements the type centric part of the static serializability algorithm.
   */
  private void addTypesReachableFromType(TreeLogger logger,
      HashSet reachableTypes, JType type) {
    assert (type != null);

    if (reachableTypes.contains(type)) {
      return;
    }

    logger = logger.branch(TreeLogger.DEBUG,
        "Adding types reachable from type " + type.getQualifiedSourceName(),
        null);

    reachableTypes.add(type);

    if (type.isPrimitive() != null) {
      return;
    }

    if (type.isArray() != null) {
      addTypesReachableFromArray(logger, reachableTypes, type.isArray());
      return;
    }

    if (type.isParameterized() != null) {
      addTypeReachableFromParameterizedType(logger, reachableTypes,
          type.isParameterized());
      return;
    }

    if (type.isClassOrInterface() == null) {
      return;
    }

    addTypesReachableFromClassOrInterface(logger, reachableTypes,
        type.isClassOrInterface());
  }
}
