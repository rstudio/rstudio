/*
 * ActiveSession.java
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
package org.rstudio.studio.client.application.model;

import com.google.gwt.core.client.JavaScriptObject;

public class ActiveSession extends JavaScriptObject
{
   protected ActiveSession()
   {  
   }
   
   public native final String getDisplayName() /*-{
      return this.display_name;
   }-*/;
   
   public native final void setDisplayName(String displayName) /*-{
      this.display_name = displayName;
   }-*/;

   public native final String getUrl() /*-{
      return this.url;
   }-*/;
   
   public native final String getProject() /*-{
      return this.project;
   }-*/;
   
   public native final String getWorkingDir() /*-{
      return this.working_dir;
   }-*/;
   
   public native final boolean getRunning() /*-{
      return this.running;
   }-*/;
   
   public native final boolean getExecuting() /*-{
      return this.executing;
   }-*/;
   
   public native final boolean getSavePromptRequired() /*-{
      return this.save_prompt_required;
   }-*/;
   
   public native final double getLastUsed() /*-{
      return this.last_used;
   }-*/;
   
   public native final String getRVersion() /*-{
      return this.r_version;
   }-*/;
   
   public native final String getRVersionHome() /*-{
      return this.r_version_home;
   }-*/;
}
