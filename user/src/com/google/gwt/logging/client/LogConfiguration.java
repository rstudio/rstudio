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

package com.google.gwt.logging.client;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.Window.Location;
import com.google.gwt.user.client.ui.HasWidgets;

import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Configures client side logging using the query params and gwt.xml settings.
 */
public class LogConfiguration implements EntryPoint {
  
  private interface LogConfigurationImpl {
    void configureClientSideLogging();
  }
  
  /** 
   * Implementation which does nothing and compiles out if logging is disabled.
   */
  private static class LogConfigurationImplNull
  implements LogConfigurationImpl {
    public void configureClientSideLogging() { }
  }
  
  /** 
   * Implementation which is used when logging is enabled.
   */
  private static class LogConfigurationImplRegular
  implements LogConfigurationImpl {
  
    public void configureClientSideLogging() {
      Logger root = Logger.getLogger("");
      setLevels(root);
      setDefaultHandlers(root);
    }
  
    private void addHandlerIfNotNull(Logger l, Handler h) {
      if (!(h instanceof NullLogHandler)) {
        l.addHandler(h);
      }
    }
   
    private Level parseLevel(String s) {
      if (s == null) {
        return null;
      }
      if (s.equals(Level.OFF.getName())) {
        return Level.OFF;
      } else if (s.equals(Level.SEVERE.getName())) {
        return Level.SEVERE;
      } else if (s.equals(Level.WARNING.getName())) {
        return Level.WARNING;
      } else if (s.equals(Level.INFO.getName())) {
        return Level.INFO;
      } else if (s.equals(Level.CONFIG.getName())) {
        return Level.CONFIG;
      } else if (s.equals(Level.FINE.getName())) {
        return Level.FINE;
      } else if (s.equals(Level.FINER.getName())) {
        return Level.FINER;
      } else if (s.equals(Level.FINEST.getName())) {
        return Level.FINEST;
      } else if (s.equals(Level.ALL.getName())) {
        return Level.ALL;
      }
      return null;
    }
    
    private void setDefaultHandlers(Logger l) {
      // Add the default handlers. If users want some of these disabled, they
      // will specify that in the gwt.xml file, which will replace the handler
      // with an instance of NullLogHandler, effectively disabling it.
      Handler console = GWT.create(ConsoleLogHandler.class);
      addHandlerIfNotNull(l, console);
      Handler dev = GWT.create(DevelopmentModeLogHandler.class);
      addHandlerIfNotNull(l, dev);
      Handler firebug = GWT.create(FirebugLogHandler.class);
      addHandlerIfNotNull(l, firebug);
      Handler system = GWT.create(SystemLogHandler.class);
      addHandlerIfNotNull(l, system);
      HasWidgets loggingWidget = GWT.create(BasicLoggingPopup.class);
      if (!(loggingWidget instanceof NullLoggingPopup)) {
        addHandlerIfNotNull(l, new HasWidgetsLogHandler(loggingWidget));
      }
    }
  
    private void setLevels(Logger l) {
      // try to pull the log level from the query param
      Level paramLevel = parseLevel(Location.getParameter("logLevel"));
      if (paramLevel != null) {
        l.setLevel(paramLevel);
      } else {
        // if it isn't there, then pull it from the gwt.xml file
        DefaultLevel defaultLevel = GWT.create(DefaultLevel.class);
        l.setLevel(defaultLevel.getLevel());
      }
    }
  }
  
  private static LogConfigurationImpl impl =
    GWT.create(LogConfigurationImplNull.class);

  public void onModuleLoad() {
    impl.configureClientSideLogging();
  }

}
