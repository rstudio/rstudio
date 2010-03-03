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
package com.google.gwt.core.ext.linker.impl;

import com.google.gwt.core.ext.LinkerContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.linker.ArtifactSet;
import com.google.gwt.core.ext.linker.Shardable;
import com.google.gwt.util.tools.Utility;

import java.io.IOException;

/**
 * This is a partial implementation of the Linker interface to support hosted
 * mode.
 */
@Shardable
public final class HostedModeLinker extends SelectionScriptLinker {

  public static String getHostedHtml() throws IOException {
    return Utility.getFileFromClassPath("com/google/gwt/core/ext/linker/impl/hosted.html");
  }

  /**
   * TODO: When this class is removed, move SelectionScriptLinker to gwt-user.
   */

  @Override
  public String generateSelectionScript(TreeLogger logger,
      LinkerContext context, ArtifactSet artifacts)
      throws UnableToCompleteException {
    return super.generateSelectionScript(logger, context, artifacts);
  }

  @Override
  public String getDescription() {
    return "Hosted Mode";
  }

  @Override
  public ArtifactSet link(TreeLogger logger, LinkerContext context,
      ArtifactSet artifacts) throws UnableToCompleteException {
    return unsupported(logger);
  }

  @Override
  protected String getCompilationExtension(TreeLogger logger,
      LinkerContext context) throws UnableToCompleteException {
    return unsupported(logger);
  }

  @Override
  protected String getModulePrefix(TreeLogger logger, LinkerContext context,
      String strongName) throws UnableToCompleteException {
    return unsupported(logger);
  }

  @Override
  protected String getModuleSuffix(TreeLogger logger, LinkerContext context)
      throws UnableToCompleteException {
    return unsupported(logger);
  }

  @Override
  protected String getSelectionScriptTemplate(TreeLogger logger, LinkerContext context) {
    return "com/google/gwt/core/ext/linker/impl/HostedModeTemplate.js";
  }

  private <T> T unsupported(TreeLogger logger) throws UnableToCompleteException {
    logger.log(TreeLogger.ERROR,
        "HostedModeLinker does not support this function", null);
    throw new UnableToCompleteException();
  }
}
