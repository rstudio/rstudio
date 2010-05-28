/*
 * Copyright 2010 Google Inc.
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

import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 *  A log manager which delegates to different instances for client
 *  code so that the client and server code do not share a root logger and try
 *  to log to each other's handlers.
 */
public class DevModeLogManager extends LogManager {
  static class LogManagerWithExposedConstructor extends LogManager {
    public LogManagerWithExposedConstructor() {
      super();
    }
  }
  
  protected ThreadLocal<LogManager> clientLogManager =
    new ThreadLocal<LogManager>() {
      public LogManager initialValue() {
        return new LogManagerWithExposedConstructor();
      }
    };
  
  public DevModeLogManager() {
    if (System.getProperty("java.util.logging.oldLogManager") != null) {
      // TODO(unnurg): Instantiate the class stored in oldLogManager and
      // delegate calls to there.
      System.err.println(
          "[WARN] ignoring user-specified value '" +
          System.getProperty("java.util.logging.oldLogManager") +
          "' for java.util.logging.manager");
    }
  }
  
  @Override
  public boolean addLogger(Logger logger) {
    if (isClientCode()) {
      return clientLogManager.get().addLogger(logger);
    }
    return super.addLogger(logger);
  }
  
  @Override
  public void addPropertyChangeListener(PropertyChangeListener l) {
    if (isClientCode()) {
      clientLogManager.get().addPropertyChangeListener(l);
    }
    super.addPropertyChangeListener(l);
  }
  
  @Override
  public void checkAccess() {
    if (isClientCode()) {
      clientLogManager.get().checkAccess();
    }
    super.checkAccess();
  }
  
  @Override
  public Logger getLogger(String name) {
    if (isClientCode()) {
      return clientLogManager.get().getLogger(name);
    }
    return super.getLogger(name);
  }

  @Override
  public Enumeration<String> getLoggerNames() {
    if (isClientCode()) {
      return clientLogManager.get().getLoggerNames();
    }
    return super.getLoggerNames();
  }
  
  @Override
  public String getProperty(String name) {
    if (isClientCode()) {
      return clientLogManager.get().getProperty(name);
    }
    return super.getProperty(name);
  }
  
  @Override
  public void readConfiguration() throws IOException, SecurityException {
    if (isClientCode()) {
      clientLogManager.get().readConfiguration();
    }
    super.readConfiguration();
  }
  
  @Override
  public void readConfiguration(InputStream ins) throws IOException, 
  SecurityException {
    if (isClientCode()) {
      clientLogManager.get().readConfiguration(ins);
    }
    super.readConfiguration(ins);
  }

  @Override
  public void removePropertyChangeListener(PropertyChangeListener l) {
    if (isClientCode()) {
      clientLogManager.get().removePropertyChangeListener(l);
    }
    super.removePropertyChangeListener(l);
  }
  
  @Override
  public void reset() {
    if (isClientCode()) {
      clientLogManager.get().reset();
    }
    super.reset();
  }
    
  protected boolean isClientCode() {
    return (Thread.currentThread() instanceof
        BrowserChannelServer.CodeServerThread);
  }
}
