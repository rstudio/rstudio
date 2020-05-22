/*
 * SharedProjectDetails.java
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

import org.rstudio.core.client.files.FileSystemItem;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;

public class SharedProjectDetails extends JavaScriptObject
{
   protected SharedProjectDetails()
   {
   }
   
   public final native String getProjectDir() /*-{
      return this.project_dir;
   }-*/;
   
   public final native String getProjectFile() /*-{
      return this.project_file;
   }-*/;

   public final native String getProjectOwner() /*-{
      return this.project_owner;
   }-*/;

   public final native JsArrayString getSharedWith() /*-{
      return this.shared_with;
   }-*/;
   
   public final String getName()
   {
      return FileSystemItem.getNameFromPath(getProjectDir());
   }
}
