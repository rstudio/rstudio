/*
 * Copyright 2008 Google Inc.
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

import com.google.gwt.core.ext.BadPropertyValueException;
import com.google.gwt.core.ext.ConfigurationProperty;
import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.PropertyOracle;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.dev.util.Util;

import java.io.IOException;
import java.io.OutputStream;

class StaticResourceContext extends AbstractResourceContext {
  /**
   * The name of a deferred binding property that determines whether or not this
   * generator will rename the incoming resources to strong file names.
   */
  static final String ENABLE_RENAMING = "ClientBundle.enableRenaming";

  StaticResourceContext(TreeLogger logger, GeneratorContext context,
      JClassType resourceBundleType, ClientBundleContext clientBundleCtx) {
    super(logger, context, resourceBundleType, clientBundleCtx);
  }

  public String deploy(String suggestedFileName, String mimeType, byte[] data,
      boolean forceExternal) throws UnableToCompleteException {
    TreeLogger logger = getLogger();
    GeneratorContext context = getGeneratorContext();
    PropertyOracle propertyOracle = context.getPropertyOracle();

    // See if filename obfuscation should be enabled
    String enableRenaming = null;
    try {
      ConfigurationProperty prop = propertyOracle.getConfigurationProperty(ENABLE_RENAMING);
      getRequirements().addConfigurationProperty(ENABLE_RENAMING);
      enableRenaming = prop.getValues().get(0);
    } catch (BadPropertyValueException e) {
      logger.log(TreeLogger.ERROR, "Bad value for " + ENABLE_RENAMING, e);
      throw new UnableToCompleteException();
    }

    // Determine the final filename for the resource's file
    String outputName;
    if (Boolean.parseBoolean(enableRenaming)) {
      String strongName = Util.computeStrongName(data);

      // Determine the extension of the original file
      String extension;
      int lastIdx = suggestedFileName.lastIndexOf('.');
      if (lastIdx != -1) {
        extension = suggestedFileName.substring(lastIdx + 1);
      } else {
        extension = "noext";
      }

      // The name will be MD5.cache.ext
      outputName = strongName + ".cache." + extension;
    } else {
      outputName = suggestedFileName.substring(suggestedFileName.lastIndexOf('/') + 1);
    }

    // Ask the context for an OutputStream into the named resource
    OutputStream out = context.tryCreateResource(logger, outputName);

    // This would be null if the resource has already been created in the
    // output (because two or more resources had identical content).
    if (out != null) {
      try {
        out.write(data);

      } catch (IOException e) {
        logger.log(TreeLogger.ERROR, "Unable to write data to output name "
            + outputName, e);
        throw new UnableToCompleteException();
      }

      // If there's an error, this won't be called and there will be nothing
      // created in the output directory.
      context.commitResource(logger, out);

      if (logger.isLoggable(TreeLogger.DEBUG)) {
        logger.log(TreeLogger.DEBUG, "Copied " + data.length + " bytes to "
            + outputName, null);
      }
    }

    // Return a Java expression
    return "GWT.getModuleBaseForStaticFiles() + \"" + outputName + "\"";
  }

  public boolean supportsDataUrls() {
    return false;
  }
}