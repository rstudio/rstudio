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
package com.google.gwt.dev.shell;

import com.google.gwt.core.ext.TreeLogger;
import com.google.gwt.core.ext.UnableToCompleteException;

import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Shell;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

/**
 * Performs platform-specific class selection.
 */
public class PlatformSpecific {

  private static final String[] browserClassNames = new String[] {
      "com.google.gwt.dev.shell.ie.BrowserWidgetIE6",
      "com.google.gwt.dev.shell.moz.BrowserWidgetMoz",
      "com.google.gwt.dev.shell.mac.BrowserWidgetSaf"};

  private static final String[] updaterClassNames = new String[] {
      "com.google.gwt.dev.shell.ie.CheckForUpdatesIE6",
      "com.google.gwt.dev.shell.moz.CheckForUpdatesMoz",
      "com.google.gwt.dev.shell.mac.CheckForUpdatesSaf"};

  public static BrowserWidget createBrowserWidget(TreeLogger logger,
      Composite parent, BrowserWidgetHost host)
      throws UnableToCompleteException {
    Throwable caught = null;
    try {
      for (int i = 0; i < browserClassNames.length; i++) {
        Class clazz = null;
        try {
          clazz = Class.forName(browserClassNames[i]);
          Constructor ctor = clazz.getDeclaredConstructor(new Class[] {
              Shell.class, BrowserWidgetHost.class});
          BrowserWidget bw = (BrowserWidget) ctor.newInstance(new Object[] {
              parent, host});
          return bw;
        } catch (ClassNotFoundException e) {
          caught = e;
        }
      }
      logger.log(TreeLogger.ERROR,
          "No instantiable browser widget class could be found", caught);
      throw new UnableToCompleteException();
    } catch (SecurityException e) {
      caught = e;
    } catch (NoSuchMethodException e) {
      caught = e;
    } catch (IllegalArgumentException e) {
      caught = e;
    } catch (InstantiationException e) {
      caught = e;
    } catch (IllegalAccessException e) {
      caught = e;
    } catch (InvocationTargetException e) {
      caught = e.getTargetException();
    } catch (ClassCastException e) {
      caught = e;
    }
    logger.log(TreeLogger.ERROR,
        "The browser widget class could not be instantiated", caught);
    throw new UnableToCompleteException();
  }

  public static CheckForUpdates createUpdateChecker() {
    try {
      for (int i = 0; i < updaterClassNames.length; i++) {
        try {
          Class clazz = Class.forName(updaterClassNames[i]);
          Constructor ctor = clazz.getDeclaredConstructor(new Class[] {});
          CheckForUpdates checker = (CheckForUpdates) ctor.newInstance(new Object[] {});
          return checker;
        } catch (ClassNotFoundException e) {
          // keep trying
        }
      }
    } catch (Throwable e) {
      // silently ignore any errors
    }
    return null;
  }
}
