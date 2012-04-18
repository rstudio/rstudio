/*
 * GeneralPrefs.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.prefs.model;

import com.google.gwt.core.client.JavaScriptObject;

public class GeneralPrefs extends JavaScriptObject
{
   protected GeneralPrefs() {}

   public static final native GeneralPrefs create(int saveAction,
                                                  boolean loadRData,
                                                  String initialWorkingDir) /*-{
      var prefs = new Object();
      prefs.save_action = saveAction;
      prefs.load_rdata = loadRData;
      prefs.initial_working_dir = initialWorkingDir;
      return prefs ;
   }-*/;

   
   public native final int getSaveAction() /*-{
      return this.save_action;
   }-*/;

   public native final boolean getLoadRData() /*-{
      return this.load_rdata;
   }-*/;

   public native final String getInitialWorkingDirectory() /*-{
      return this.initial_working_dir;
   }-*/;
}
