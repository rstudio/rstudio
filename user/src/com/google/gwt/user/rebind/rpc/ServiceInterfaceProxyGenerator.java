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

import com.google.gwt.core.ext.Generator;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;

/**
 * Generator for producing the asynchronous version of a {@link RemoteService}
 * interface.
 */
public class ServiceInterfaceProxyGenerator extends Generator {

  public String generate(TreeLogger logger, GeneratorContext ctx,
      String requestedClass) throws UnableToCompleteException {

    TypeOracle typeOracle = ctx.getTypeOracle();
    assert (typeOracle != null);

    JClassType remoteService = typeOracle.findType(requestedClass);
    if (remoteService == null) {
      logger.log(TreeLogger.ERROR, "Unable to find metadata for type '"
          + requestedClass + "'", null);
      throw new UnableToCompleteException();
    }

    if (remoteService.isInterface() == null) {
      logger.log(TreeLogger.ERROR, remoteService.getQualifiedSourceName()
          + " is not an interface", null);
      throw new UnableToCompleteException();
    }

    logger = logger.branch(TreeLogger.DEBUG,
        "Generating client proxy for remote service interface '"
            + remoteService.getQualifiedSourceName() + "'", null);
    
    SerializableTypeOracleBuilder stob = new SerializableTypeOracleBuilder(
        logger, typeOracle);
    SerializableTypeOracle sto = stob.build(ctx.getPropertyOracle(),
        remoteService);

    generateTypeSerializer(logger, ctx, remoteService, sto);

    return generateProxy(logger, ctx, remoteService, sto);
  }

  private String generateProxy(TreeLogger logger, GeneratorContext ctx,
      JClassType serviceIntf, SerializableTypeOracle serializableTypeOracle) {
    ProxyCreator proxyCreator = new ProxyCreator(serviceIntf,
        serializableTypeOracle);

    return proxyCreator.create(logger, ctx);
  }

  private String generateTypeSerializer(TreeLogger logger,
      GeneratorContext ctx, JClassType serviceIntf,
      SerializableTypeOracle serializableTypeOracle) {
    TypeSerializerCreator typeSerializerCreator = new TypeSerializerCreator(
        logger, serializableTypeOracle, ctx, serviceIntf);
    return typeSerializerCreator.realize(logger);
  }
}
