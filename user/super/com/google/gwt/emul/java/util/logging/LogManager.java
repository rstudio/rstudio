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
import com.google.gwt.logging.impl.LogManagerImpl;
import com.google.gwt.logging.impl.LogManagerImplNull;

/**
 *  An emulation of the java.util.logging.LogManager class. See 
 *  <a href="http://java.sun.com/j2se/1.4.2/docs/api/java/util/logging/LogManger.html"> 
 *  The Java API doc for details</a>
 */
public class LogManager {
  private static LogManagerImpl staticImpl =
    GWT.create(LogManagerImplNull.class);

  public static LogManager getLogManager() {
    return staticImpl.getLogManager();
  }
  
  private LogManagerImpl impl;
  
  protected LogManager() {
    impl = GWT.create(LogManagerImplNull.class);
  }
  
  public boolean addLogger(Logger logger) {
    return impl.addLogger(logger);
  }

  public Logger getLogger(String name) {
    return impl.getLogger(name);
  }
  
  /* Not Implemented */
  // public void addPropertyChangeListener(PropertyChangeListener l) {}
  // public void checkAccess() {}
  // public Enumeration getLoggerNames() {}
  // public String getProperty(String name) {}
  // public void readConfiguration() {}
  // public void readConfiguration(InputStream ins) {}
  // public void removePropertyChangeListener(PropertyChangeListener l) {} 
  // public void reset() {}  
}
