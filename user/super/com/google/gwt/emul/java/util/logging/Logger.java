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
package java.util.logging;

import com.google.gwt.core.client.GWT;
import com.google.gwt.logging.impl.LoggerImpl;
import com.google.gwt.logging.impl.LoggerImplNull;

/**
 *  An emulation of the java.util.logging.Logger class. See 
 *  <a href="http://java.sun.com/j2se/1.4.2/docs/api/java/util/logging/Logger.html"> 
 *  The Java API doc for details</a>
 */
public class Logger {
  private static LoggerImpl staticImpl = GWT.create(LoggerImplNull.class);
  
  public static Logger getLogger(String name) {
    return staticImpl.getLoggerHelper(name);
  }

  private LoggerImpl impl;
  
  protected Logger(String name, String resourceName) {
    impl = GWT.create(LoggerImplNull.class);
    impl.setName(name);
  }

  public void addHandler(Handler handler) {
    impl.addHandler(handler);
  }
  
  public void config(String msg) {
    impl.config(msg);
  } 
   
  public void fine(String msg) {
    impl.fine(msg);
  } 
  
  public void finer(String msg) {
    impl.finer(msg);
  }
  
  public void finest(String msg) {
    impl.finest(msg);
  }
  
  public Handler[] getHandlers() {
    return impl.getHandlers();
  }
  
  public Level getLevel() {
    return impl.getLevel();
  } 
  
  public String getName() {
    return impl.getName();
  }
  
  public Logger getParent() {
    return impl.getParent();
  }
  
  public boolean getUseParentHandlers() {
    return impl.getUseParentHandlers();
  }
  
  public void info(String msg) {
    impl.info(msg);
  } 
  
  public boolean isLoggable(Level messageLevel) {
    return impl.isLoggable(messageLevel);
  }
  
  public void log(Level level, String msg) {
    impl.log(level, msg);
  }
  
  public void log(Level level, String msg, Throwable thrown) {
    impl.log(level, msg, thrown);
  }

  public void log(LogRecord record) {
    impl.log(record);
  }
  
  public void removeHandler(Handler handler) {
    impl.removeHandler(handler);
  }
  
  public void setLevel(Level newLevel) {
    impl.setLevel(newLevel);
  }
  
  public void setParent(Logger newParent) {
    impl.setParent(newParent);
  }
  
  public void setUseParentHandlers(boolean newUseParentHandlers) {
    impl.setUseParentHandlers(newUseParentHandlers);
  }
  
  public void severe(String msg) {
    impl.severe(msg);
  }
  
  public void warning(String msg) {
    impl.warning(msg);
  }
 
  /* Not Implemented */
  // public static Logger getAnonymousLogger() {
  // public static Logger getAnonymousLogger(String resourceBundleName) {} 
  // public Filter getFilter() {}
  // public static Logger getLogger(String name, String resourceBundleName) {} 
  // public ResourceBundle getResourceBundle() {} 
  // public String getResourceBundleName() {}
  // public void setFilter(Filter newFilter) {}
  // public void entering(String sourceClass, String sourceMethod) {} 
  // public void entering(String sourceClass, String sourceMethod, Object param1) {} 
  // public void entering(String sourceClass, String sourceMethod, Object[] params) {}
  // public void exiting(String sourceClass, String sourceMethod, Object result) {} 
  // public void exiting(String sourceClass, String sourceMethod) {}
  // public void log(Level level, String msg, Object param1) {} 
  // public void log(Level level, String msg, Object[] params) {}
  // public void logp(Level level, String sourceClass, String sourceMethod, String msg) {} 
  // public void logp(Level level, String sourceClass, String sourceMethod, String msg, Object param1) {} 
  // public void logp(Level level, String sourceClass, String sourceMethod, String msg, Object[] params) {}
  // public void logp(Level level, String sourceClass, String sourceMethod, String msg, Throwable thrown) {}
  // public void logrb(Level level, String sourceClass, String sourceMethod, String bundleName, String msg) {}
  // public void logrb(Level level, String sourceClass, String sourceMethod, String bundleName, String msg, Object param1) {} 
  // public void logrb(Level level, String sourceClass, String sourceMethod, String bundleName, String msg, Object[] params) {}
  // public void logrb(Level level, String sourceClass, String sourceMethod, String bundleName, String msg, Throwable thrown) {}
  // public void throwing(String sourceClass, String sourceMethod, Throwable thrown) {} 
}