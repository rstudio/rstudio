/*
 * SourceControlPrefs.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

import com.google.gwt.core.client.JavaScriptObject;

public class SourceControlPrefs extends JavaScriptObject
{
   protected SourceControlPrefs() {}

   // create source control prefs -- don't pass the rsa_public_key_path
   // and have_rsa_public_key parameters because they are read-only riders
   // for the prefs ui
   public static final native SourceControlPrefs create(boolean vcsEnabled,
                                                        String gitExePath,
                                                        String svnExePath,
                                                        String terminalPath,
                                                        boolean useGitBash) 
                                                                           /*-{
      var prefs = new Object();
      prefs.vcs_enabled = vcsEnabled;
      prefs.git_exe_path = gitExePath;
      prefs.svn_exe_path = svnExePath;
      prefs.terminal_path = terminalPath;
      prefs.use_git_bash = useGitBash;
      return prefs ;
   }-*/;

   public native final boolean getVcsEnabled() /*-{
      return this.vcs_enabled;
   }-*/;
   
   public native final String getGitExePath() /*-{
      return this.git_exe_path;
   }-*/;
   
   public native final String getSvnExePath() /*-{
      return this.svn_exe_path;
   }-*/;
   
   public native final String getTerminalPath() /*-{
      return this.terminal_path;
   }-*/;
   
   public native final boolean getUseGitBash() /*-{
      return this.use_git_bash;
   }-*/;
   
   public native final String getRsaKeyPath() /*-{
      return this.rsa_key_path;
   }-*/;
   
   public native final boolean getHaveRsaKey() /*-{
      return this.have_rsa_key;
   }-*/;
}
