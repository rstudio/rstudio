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

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.IncrementalGenerator;
import com.google.gwt.core.ext.RebindResult;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.TypeOracle;

/**
 * Generator for producing the asynchronous version of a
 * {@link com.google.gwt.user.client.rpc.RemoteService RemoteService} interface.
 */
public class ServiceInterfaceProxyGenerator extends IncrementalGenerator {

  /*
   * A version id. Increment this as needed, when structural changes are made to
   * the generated output, specifically with respect to it's effect on the
   * caching and reuse of previous generator results. Previously cached
   * generator results will be invalidated automatically if they were generated
   * by a version of this generator with a different version id.
   */
  private static final long GENERATOR_VERSION_ID = 1L;

  @Override
  public RebindResult generateIncrementally(TreeLogger logger, GeneratorContext ctx,
      String requestedClass) throws UnableToCompleteException {

    TypeOracle typeOracle = ctx.getTypeOracle();
    assert (typeOracle != null);

    JClassType remoteService = typeOracle.findType(requestedClass);
    if (remoteService == null) {
      logger.log(TreeLogger.ERROR, "Unable to find metadata for type '" + requestedClass + "'",
          null);
      throw new UnableToCompleteException();
    }

    if (remoteService.isInterface() == null) {
      logger.log(TreeLogger.ERROR, remoteService.getQualifiedSourceName() + " is not an interface",
          null);
      throw new UnableToCompleteException();
    }

    ProxyCreator proxyCreator = createProxyCreator(remoteService);

    TreeLogger proxyLogger =
        logger.branch(TreeLogger.DEBUG, "Generating client proxy for remote service interface '"
            + remoteService.getQualifiedSourceName() + "'", null);

    return proxyCreator.create(proxyLogger, ctx);
  }

  @Override
  public long getVersionId() {
    return GENERATOR_VERSION_ID;
  }

  protected ProxyCreator createProxyCreator(JClassType remoteService) {
    return new ProxyCreator(remoteService);
  }
}
