/*
 * VcsCloneOptions.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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
package org.rstudio.studio.client.common.vcs;

import com.google.gwt.core.client.JavaScriptObject;

public class VcsCloneOptions extends JavaScriptObject
{
   protected VcsCloneOptions()
   {
   }
   
   public native static final VcsCloneOptions create(String vcsName,
                                                     String repoUrl,
                                                     String username,
                                                     String directoryName,
                                                     String parentPath)
                                                                          /*-{
      var options = new Object();
      options.vcs_name = vcsName;
      options.repo_url = repoUrl;
      options.username = username;
      options.directory_name = directoryName;
      options.parent_path = parentPath;
      return options;
   }-*/;

   public native final String getVcsName() /*-{
      return this.vcs_name;
   }-*/;
   

   public native final String getRepoUrl() /*-{
      return this.repo_url;
   }-*/;
   
   public native final String getUsername() /*-{
      return this.username;
   }-*/;

   public native final String getDirectoryName() /*-{
      return this.directory_name;
   }-*/;

   public native final String getParentPath() /*-{
      return this.parent_path;
   }-*/;
}
