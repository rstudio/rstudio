/*
 * GeneralPrefs.java
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.prefs.model;

import org.rstudio.studio.client.application.model.RVersionSpec;

import com.google.gwt.core.client.JavaScriptObject;

public class GeneralPrefs extends JavaScriptObject
{
   protected GeneralPrefs() {}

   public static final native GeneralPrefs create(String showUserHomePage,
                                                  boolean reuseSessionsForProjectLinks,
                                                  int saveAction,
                                                  boolean loadRData,
                                                  boolean rProfileOnResume,
                                                  String initialWorkingDir,
                                                  RVersionSpec defaultRVersion,
                                                  boolean restoreProjectRVersion,
                                                  boolean showLastDotValue) /*-{
      var prefs = new Object();
      prefs.show_user_home_page = showUserHomePage;
      prefs.reuse_sessions_for_project_links = reuseSessionsForProjectLinks;
      prefs.save_action = saveAction;
      prefs.load_rdata = loadRData;
      prefs.rprofile_on_resume = rProfileOnResume;
      prefs.initial_working_dir = initialWorkingDir;
      prefs.default_r_version = defaultRVersion;
      prefs.restore_project_r_version = restoreProjectRVersion;
      prefs.show_last_dot_value = showLastDotValue;
      return prefs ;
   }-*/;

   public native final String getShowUserHomePage() /*-{
      return this.show_user_home_page;
   }-*/;
   
   public native final boolean getReuseSessionsForProjectLinks() /*-{
      return this.reuse_sessions_for_project_links;
   }-*/;
   
   public native final int getSaveAction() /*-{
      return this.save_action;
   }-*/;

   public native final boolean getLoadRData() /*-{
      return this.load_rdata;
   }-*/;
   
   public native final boolean getRprofileOnResume() /*-{
      return this.rprofile_on_resume;
   }-*/;

   public native final String getInitialWorkingDirectory() /*-{
      return this.initial_working_dir;
   }-*/;
   
   public native final RVersionSpec getDefaultRVersion() /*-{
      return this.default_r_version;
   }-*/;
   
   public native final boolean getRestoreProjectRVersion() /*-{
      return this.restore_project_r_version;
   }-*/;
   
   public native final boolean getShowLastDotValue() /*-{
      return this.show_last_dot_value;
   }-*/;
}
