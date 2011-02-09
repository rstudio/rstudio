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

import com.google.gwt.core.ext.GeneratorContext;
import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;
import com.google.gwt.core.ext.typeinfo.JClassType;
import com.google.gwt.dev.util.Util;
import com.google.gwt.resources.ext.ClientBundleRequirements;
import com.google.gwt.resources.ext.ResourceContext;
import com.google.gwt.resources.ext.ResourceGenerator;
import com.google.gwt.resources.ext.ResourceGeneratorUtil;

import java.io.IOException;
import java.net.URL;

/**
 * Defines base methods for ResourceContext implementations.
 */
public abstract class AbstractResourceContext implements ResourceContext {
  /**
   * The largest file size that will be inlined. Note that this value is taken
   * before any encodings are applied.
   */
  protected static final int MAX_INLINE_SIZE = 2 << 15;

  protected static String toBase64(byte[] data) {
    // This is bad, but I am lazy and don't want to write _another_ encoder
    sun.misc.BASE64Encoder enc = new sun.misc.BASE64Encoder();
    String base64Contents = enc.encode(data).replaceAll("\\s+", "");
    return base64Contents;
  }

  private final TreeLogger logger;
  private final ClientBundleContext clientBundleCtx;
  private final GeneratorContext context;
  private String currentResourceGeneratorType;
  private ClientBundleRequirements requirements = null;
  private final JClassType resourceBundleType;
  private String simpleSourceName;

  protected AbstractResourceContext(TreeLogger logger,
      GeneratorContext context, JClassType resourceBundleType,
      ClientBundleContext clientBundleCtx) {
    this.logger = logger;
    this.context = context;
    this.resourceBundleType = resourceBundleType;
    this.clientBundleCtx = clientBundleCtx;
  }

  @Deprecated
  public String deploy(URL resource, boolean forceExternal)
      throws UnableToCompleteException {
    return deploy(resource, null, forceExternal);
  }
  
  public String deploy(URL resource, String mimeType, boolean forceExternal)
      throws UnableToCompleteException {
    String fileName = ResourceGeneratorUtil.baseName(resource);
    byte[] bytes = Util.readURLAsBytes(resource);
    try {
      String finalMimeType = (mimeType != null)
          ? mimeType : resource.openConnection().getContentType();
      return deploy(fileName, finalMimeType, bytes, forceExternal);
    } catch (IOException e) {
      getLogger().log(TreeLogger.ERROR,
          "Unable to determine mime type of resource", e);
      throw new UnableToCompleteException();
    }
  }

  public <T> T getCachedData(String key, Class<T> clazz) {
    return clazz.cast(clientBundleCtx.getCachedData(currentResourceGeneratorType + ":" + key));
  }

  public JClassType getClientBundleType() {
    return resourceBundleType;
  }

  public GeneratorContext getGeneratorContext() {
    return context;
  }

  public String getImplementationSimpleSourceName() {
    if (simpleSourceName == null) {
      throw new IllegalStateException(
          "Simple source name has not yet been set.");
    }
    return simpleSourceName;
  }
  
  public ClientBundleRequirements getRequirements() {
    return requirements;
  }

  public <T> boolean putCachedData(String key, T value) {
    key = currentResourceGeneratorType + ":" + key;
    return value != clientBundleCtx.putCachedData(key, value);
  }
  
  public void setRequirements(ClientBundleRequirements requirements) {
    this.requirements = requirements;
  }

  protected GeneratorContext getContext() {
    return context;
  }

  protected TreeLogger getLogger() {
    return logger;
  }

  void setCurrentResourceGenerator(ResourceGenerator rg) {
    currentResourceGeneratorType = rg.getClass().getName();
  }

  void setSimpleSourceName(String name) {
    simpleSourceName = name;
  }
}
