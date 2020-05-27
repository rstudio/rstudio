/*
 * RProjectVcsContext.java
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
package org.rstudio.studio.client.projects.model;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;

public class RProjectVcsContext extends JavaScriptObject
{
   protected RProjectVcsContext()
   {
   }

   public native final String getDetectedVcs() /*-{
      return this.detected_vcs;
   }-*/;
   
   public native final JsArrayString getApplicableVcs() /*-{
      return this.applicable_vcs;
   }-*/;
   
   public native final String getSvnRepositoryRoot() /*-{
      return this.svn_repository_root;
   }-*/;
   
   public native final String getGitRemoteOriginUrl() /*-{
      return this.git_remote_origin_url;
   }-*/;
}
