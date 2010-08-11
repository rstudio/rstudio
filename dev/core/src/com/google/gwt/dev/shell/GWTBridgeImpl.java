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
package com.google.gwt.dev.shell;

import com.google.gwt.core.client.GWTBridge;
import com.google.gwt.dev.About;

/**
 * This class is the hosted-mode peer for {@link com.google.gwt.core.client.GWT}.
 */
public class GWTBridgeImpl extends GWTBridge {
      
  protected static ThreadLocal<String> uniqueID =
    new ThreadLocal<String>() {
    private int counter = 0;
        
    @Override
    public String initialValue() {
      return "DevModeThread" + ++counter;
    }
  };

  private final ShellJavaScriptHost host;

  public GWTBridgeImpl(ShellJavaScriptHost host) {
    this.host = host;
  }

  /**
   * Resolves a deferred binding request and create the requested object.
   */
  @Override
  public <T> T create(Class<?> requestedClass) {
    String className = requestedClass.getName();
    try {
      return host.<T> rebindAndCreate(className);
    } catch (Throwable e) {
      String msg = "Deferred binding failed for '" + className
          + "' (did you forget to inherit a required module?)";
      throw new RuntimeException(msg, e);
    }
  }

  @Override
  public String getThreadUniqueID() {
    // TODO(unnurg): Remove this function once Dev Mode rewriting classes are
    // in gwt-dev.
    return uniqueID.get();
  }

  @Override
  public String getVersion() {
    return About.getGwtVersionNum();
  }

  /**
   * Yes, we're running as client code in the hosted mode classloader.
   */
  @Override
  public boolean isClient() {
    return true;
  }

  /**
   * Logs in dev shell.
   */
  @Override
  public void log(String message, Throwable e) {
    host.log(message, e);
  }

}
