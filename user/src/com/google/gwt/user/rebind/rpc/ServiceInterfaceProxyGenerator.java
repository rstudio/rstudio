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

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.NotFoundException;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.core.ext.typeinfo.TypeOracleException;
import com.google.gwt.user.client.rpc.RemoteService;

/**
 * Generator for rpc service interface.
 */
public class ServiceInterfaceProxyGenerator extends Generator {
  
  public String generate(TreeLogger logger, GeneratorContext ctx,
      String requestedClass) throws UnableToCompleteException {

    TypeOracle typeOracle = ctx.getTypeOracle();
    assert (typeOracle != null);

    JType requestedType = typeOracle.findType(requestedClass);
    if (requestedType == null) {
      logger.log(TreeLogger.ERROR, "Unable to find metadata for type '"
        + requestedClass + "'", null);
      throw new UnableToCompleteException();
    }

    JClassType serviceIntf = requestedType.isInterface();
    if (serviceIntf == null) {
      logger.log(TreeLogger.ERROR, requestedType.getQualifiedSourceName()
        + " is not an interface", null);
      throw new UnableToCompleteException();
    }

    JType[] reachableTypes = getReachableTypes(logger, serviceIntf);

    SerializableTypeOracle serializableTypeOracle;
    try {
      // If we did not determine that java.lang.String was reachable then we
      // force it to be here since the proxy code requires that it be
      //
      JClassType stringType = typeOracle.getType("java.lang.String");
      if (!contains(reachableTypes, stringType)) {
        reachableTypes = add(reachableTypes, stringType);
      }

      serializableTypeOracle = getSerializableTypeOracle(logger, typeOracle,
        reachableTypes);

      if (!isValidServiceInterface(logger, ctx, serializableTypeOracle,
        serviceIntf)) {
        return null;
      }

      generateTypeSerializer(logger, ctx, serviceIntf, serializableTypeOracle);

    } catch (NotFoundException e) {
      logger.log(TreeLogger.ERROR, e.getMessage(), null);
      throw new UnableToCompleteException();

    } catch (TypeOracleException e) {
      logger.log(TreeLogger.ERROR, "Unexpected TypeOracleException", e);
      throw new UnableToCompleteException();
    }

    // The name of the proxy class must be the first one in the ';' delimited
    // list of items.
    //
    String proxyClassName = generateProxy(logger, ctx, serviceIntf,
      serializableTypeOracle);
    return proxyClassName;
  }

  public ProxyCreator getProxyCreator(JClassType serviceIntf,
      SerializableTypeOracle serializableTypeOracle) {
    return new ProxyCreator(serviceIntf, serializableTypeOracle);
  }

  public boolean isCompatibleWith(Class classOrInterface) {
    return classOrInterface.isInterface()
      && RemoteService.class.isAssignableFrom(classOrInterface);
  }

  private JType[] add(JType[] reachableTypes, JClassType typeToAdd) {
    JType[] newArray = new JType[reachableTypes.length + 1];
    System.arraycopy(reachableTypes, 0, newArray, 0, reachableTypes.length);
    newArray[reachableTypes.length] = typeToAdd;
    return newArray;
  }

  private boolean contains(JType[] reachableTypes, JClassType stringType) {
    for (int i = 0; i < reachableTypes.length; ++i) {
      if (reachableTypes[i] == stringType) {
        return true;
      }
    }

    return false;
  }

  private String generateProxy(TreeLogger logger, GeneratorContext ctx,
      JClassType serviceIntf, SerializableTypeOracle serializableTypeOracle) {
    ProxyCreator proxyCreator = getProxyCreator(serviceIntf,
      serializableTypeOracle);

    return proxyCreator.create(logger, ctx);
  }

  private String generateTypeSerializer(TreeLogger logger,
      GeneratorContext ctx, JClassType serviceIntf,
      SerializableTypeOracle serializableTypeOracle) {
    TypeSerializerCreator typeSerializerCreator = new TypeSerializerCreator(
      serializableTypeOracle);
    return typeSerializerCreator.realize(logger, ctx, serviceIntf);
  }

  private JType[] getReachableTypes(TreeLogger logger, JClassType serviceIntf) {
    ReachableTypeOracle reachableTypeOracle = new ReachableTypeOracleImpl(
      logger);
    JType[] reachableTypes = reachableTypeOracle.getTypesReachableFromInterface(serviceIntf);

    return reachableTypes;
  }

  private SerializableTypeOracle getSerializableTypeOracle(TreeLogger logger,
      TypeOracle typeOracle, JType[] reachableTypes) throws NotFoundException {
    SerializableTypeOracleBuilder serializableTypeOracleBuilder = new SerializableTypeOracleBuilder(
      logger, typeOracle);
    SerializableTypeOracle serializableTypeOracle = serializableTypeOracleBuilder.build(reachableTypes);
    return serializableTypeOracle;
  }

  private boolean isValidServiceInterface(TreeLogger logger,
      GeneratorContext ctx, SerializableTypeOracle serializableTypeOracle,
      JClassType serviceIntf) throws TypeOracleException {
    ServiceInterfaceValidator serviceInterfaceValidator = new ServiceInterfaceValidator(
      logger, ctx, serializableTypeOracle, serviceIntf);
    if (!serviceInterfaceValidator.isValid()) {
      return false;
    }

    return true;
  }
}
