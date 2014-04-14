/*
 * Copyright 2009 Google Inc.
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

import com.google.gwt.dev.util.log.AbstractTreeLogger;

import java.net.URL;

import javax.swing.Icon;
import javax.swing.ImageIcon;

/**
 * Purely static class which holds references to all the icons needed in the
 * development mode shell.
 */
public class Icons {

  // log level icons
  private static final Icon LOG_ITEM_ERROR = loadIcon(
      AbstractTreeLogger.class, "log-item-error.gif");
  private static final Icon LOG_ITEM_WARNING = loadIcon(
      AbstractTreeLogger.class, "log-item-warning.gif");
  private static final Icon LOG_ITEM_INFO = loadIcon(
      AbstractTreeLogger.class, "log-item-info.gif");
  private static final Icon LOG_ITEM_DEBUG = loadIcon(
      AbstractTreeLogger.class, "log-item-debug.gif");
  private static final Icon LOG_ITEM_TRACE = loadIcon(
      AbstractTreeLogger.class, "log-item-trace.gif");
  private static final Icon LOG_ITEM_SPAM = loadIcon(
      AbstractTreeLogger.class, "log-item-spam.gif");

  // browser icons left out for now

  private static final ImageIcon CLOSE = loadIcon("close.png");

  /**
   * @return a *x24 icon suitable for identifying Chrome, or null if none.
   */
  public static ImageIcon getChrome24() {
    return null;
  }

  public static ImageIcon getClose() {
    return CLOSE;
  }

  /**
   * @return a *x24 icon suitable for identifying Firefox, or null if none.
   */
  public static ImageIcon getFirefox24() {
    return null;
  }

  /**
   * @return a *x24 icon suitable for identifying IE, or null if none.
   */
  public static ImageIcon getIE24() {
    return null;
  }

  public static Icon getLogItemDebug() {
    return LOG_ITEM_DEBUG;
  }

  public static Icon getLogItemError() {
    return LOG_ITEM_ERROR;
  }

  public static Icon getLogItemInfo() {
    return LOG_ITEM_INFO;
  }

  public static Icon getLogItemSpam() {
    return LOG_ITEM_SPAM;
  }

  public static Icon getLogItemTrace() {
    return LOG_ITEM_TRACE;
  }

  public static Icon getLogItemWarning() {
    return LOG_ITEM_WARNING;
  }

  /**
   * @return a *x24 icon suitable for identifying Safari, or null if none.
   */
  public static ImageIcon getSafari24() {
    return null;
  }

  private static ImageIcon loadIcon(Class<?> clazz, String name) {
    URL url = clazz.getResource(name);
    if (url != null) {
      ImageIcon image = new ImageIcon(url);
      return image;
    } else {
      // Bad image.
      return new ImageIcon();
    }
  }

  private static ImageIcon loadIcon(String name) {
    return loadIcon(Icons.class, name);
  }

  // prevent instantiation
  private Icons() {
  }
}
