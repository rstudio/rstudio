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
package com.google.gwt.dev.linker.impl;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.dev.linker.ModuleResource;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

/**
 * The standard implementation of {@link ModuleResource}.
 */
public abstract class StandardModuleResource implements ModuleResource {
  private final String id;
  private final URL url;

  protected StandardModuleResource(String id, URL url) {
    this.id = id;
    this.url = url;
  }

  public String getId() {
    return id;
  }

  public URL getURL() {
    return url;
  }

  public InputStream tryGetResourceAsStream(TreeLogger logger)
      throws UnableToCompleteException {
    if (url == null) {
      logger.branch(TreeLogger.DEBUG, "No contents for resource", null);
      return null;
    }

    logger = logger.branch(TreeLogger.DEBUG, "Attempting to get stream for "
        + url.toExternalForm(), null);

    try {
      return url.openStream();
    } catch (IOException e) {
      logger.log(TreeLogger.ERROR, "Unable to open stream", e);
      throw new UnableToCompleteException();
    }
  }
}
