/*
 * SourceControlPrefs.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
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

public class SourceControlPrefs extends JavaScriptObject
{
   protected SourceControlPrefs() {}

   public static final native SourceControlPrefs create(boolean vcsEnabled,
                                                        String gitBinDir,
                                                        String sshKeyPath) 
                                                                           /*-{
      var prefs = new Object();
      prefs.vcs_enabled = vcsEnabled;
      prefs.git_bin_dir = gitBinDir;
      prefs.ssh_key_path = sshKeyPath;
      prefs.default_ssh_key_dir = "";
      return prefs ;
   }-*/;

   public native final boolean getVcsEnabled() /*-{
      return this.vcs_enabled;
   }-*/;
   
   public native final String getGitBinDir() /*-{
      return this.git_bin_dir;
   }-*/;
   
   public native final String getSSHKeyPath() /*-{
      return this.ssh_key_path;
   }-*/;
   
   /*
    * This isn't a pref per-se but rides along inside this structure
    * since it is required to render the prefs ui
    */
   public native final String getDefaultSSHKeyDir() /*-{
      return this.default_ssh_key_dir;
   }-*/;
}
