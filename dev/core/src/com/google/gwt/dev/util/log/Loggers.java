/*
 * Copyright 2006 Google Inc.
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
package com.google.gwt.dev.util.log;

import com.google.gwt.core.ext.TreeLogger;

import java.net.URL;

/**
 * Utility methods for creating various sorts of loggers.
 */
public class Loggers {

  /**
   * Produces either a null logger or, if the property
   * <code>gwt.useGuiLogger</code> is set, a graphical tree logger. This
   * method is useful for unit tests, where most of the time you don't want to
   * log.
   */
  public static TreeLogger createOptionalGuiTreeLogger() {
    if (System.getProperty("gwt.useGuiLogger") != null) {
      AbstractTreeLogger atl = TreeLoggerWidget.getAsDetachedWindow(
          "CompilationServiceTest", 800, 600, true);
      return maybeSetDetailLevel(atl);
    } else {
      return TreeLogger.NULL;
    }
  }

  public static void logURLs(TreeLogger logger, TreeLogger.Type type, URL[] urls) {
    for (int i = 0; i < urls.length; i++) {
      URL url = urls[i];
      logger.log(type, url.toExternalForm(), null);
    }
  }

  public static TreeLogger maybeSetDetailLevel(AbstractTreeLogger atl) {
    String s = System.getProperty("gwt.logLevel");
    if (s != null) {
      TreeLogger.Type type = TreeLogger.Type.valueOf(s);
      if (type != null) {
        atl.setMaxDetail(type);
      }
    }
    return atl;
  }
}
