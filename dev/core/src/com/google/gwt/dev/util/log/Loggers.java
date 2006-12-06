// Copyright 2006 Google Inc. All Rights Reserved.
package com.google.gwt.dev.util.log;

import com.google.gwt.core.ext.TreeLogger;

import java.net.URL;

public class Loggers {

  /**
   * Produces either a null logger or, if the property
   * <code>gwt.useGuiLogger</code> is set, a graphical tree logger. This method
   * is useful for unit tests, where most of the time you don't want to log.
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

  public static void logURLs(TreeLogger logger, TreeLogger.Type type, URL[] urls) {
    for (int i = 0; i < urls.length; i++) {
      URL url = urls[i];
      logger.log(type, url.toExternalForm(), null);
    }
  }
}
