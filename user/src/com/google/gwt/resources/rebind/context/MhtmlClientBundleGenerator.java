/*
 * Copyright 2009 Google Inc.
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
package com.google.gwt.resources.rebind.context;

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.core.ext.typeinfo.JType;
import com.google.gwt.core.ext.typeinfo.TypeOracleException;
import com.google.gwt.dev.util.Util;
import com.google.gwt.resources.ext.ClientBundleFields;
import com.google.gwt.resources.ext.ClientBundleRequirements;

/**
 * Generates Multipart HTML files.
 */
public class MhtmlClientBundleGenerator extends AbstractClientBundleGenerator {

  private static final String BUNDLE_EXTENSION = ".cache.txt";
  private static int counter = 0;

  private MhtmlResourceContext resourceContext;
  private String partialPath;

  @Override
  protected AbstractResourceContext createResourceContext(TreeLogger logger,
      GeneratorContext context, JClassType resourceBundleType) {
    resourceContext = new MhtmlResourceContext(logger, context,
        resourceBundleType);

    /*
     * We use a counter to ensure that the generated resources have unique
     * names. Previously we used the system time, but it sometimes led to non-
     * unique names if subsequent calls happened within the same millisecond.
     * 
     * TODO: figure out how to make the filename stable based on actual content.
     */
    counter++;
    partialPath = Util.computeStrongName(Util.getBytes(resourceBundleType.getQualifiedSourceName()
        + counter))
        + BUNDLE_EXTENSION;
    resourceContext.setPartialPath(partialPath);

    return resourceContext;
  }

  @Override
  protected void doAddFieldsAndRequirements(TreeLogger logger,
      GeneratorContext generatorContext, ClientBundleFields fields,
      ClientBundleRequirements requirements) throws UnableToCompleteException {
    JType booleanType;
    JType stringType;
    try {
      booleanType = generatorContext.getTypeOracle().parse("boolean");
      stringType = generatorContext.getTypeOracle().parse("java.lang.String");
    } catch (TypeOracleException e) {
      logger.log(TreeLogger.ERROR, "Expected type not in type oracle", e);
      throw new UnableToCompleteException();
    }

    // GWT.getModuleBaseURL().startsWith("https")
    String isHttpsIdent = fields.define(booleanType, "isHttps",
        "GWT.getModuleBaseURL().startsWith(\"https\")", true, true);
    resourceContext.setIsHttpsIdent(isHttpsIdent);

    // "mhtml:" + GWT.getModuleBaseURL() + "partialPath!cid:"
    String bundleBaseIdent = fields.define(stringType, "bundleBase",
        "\"mhtml:\" + GWT.getModuleBaseURL() + \"" + partialPath + "!cid:\"",
        true, true);
    resourceContext.setBundleBaseIdent(bundleBaseIdent);
  }

  @Override
  protected void doFinish() throws UnableToCompleteException {
    resourceContext.finish();
  }
}
