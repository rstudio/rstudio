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

import com.google.gwt.core.ext.GeneratorContextExt;
import com.google.gwt.core.ext.GeneratorExt;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;
import com.google.gwt.dev.javac.rebind.RebindResult;
import com.google.gwt.dev.javac.rebind.RebindStatus;

/**
 * Generator for producing the asynchronous version of a
 * {@link com.google.gwt.user.client.rpc.RemoteService RemoteService} interface.
 */
public class ServiceInterfaceProxyGenerator extends GeneratorExt {

  @Override
  public RebindResult generateIncrementally(TreeLogger logger, GeneratorContextExt ctx,
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

    ProxyCreator proxyCreator = createProxyCreator(remoteService);

    TreeLogger proxyLogger = logger.branch(TreeLogger.DEBUG,
        "Generating client proxy for remote service interface '"
            + remoteService.getQualifiedSourceName() + "'", null);

    String returnTypeName = proxyCreator.create(proxyLogger, ctx);
    
    /*
     * Return with RebindStatus.USE_PARTIAL_CACHED, since we are implementing an
     * incremental scheme, which allows us to use a mixture of previously cached
     * and newly generated compilation units and artifacts.  For example, the
     * field serializers only need to be generated fresh if their source type
     * has changed (or if no previously cached version exists).
     */
    return new RebindResult(RebindStatus.USE_PARTIAL_CACHED, returnTypeName);
  }

  protected ProxyCreator createProxyCreator(JClassType remoteService) {
    return new ProxyCreator(remoteService);
  }
}
