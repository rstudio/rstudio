/*
 * StatusAndPathInfo.java
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

public class StatusAndPathInfo extends JavaScriptObject
{
   protected StatusAndPathInfo()
   {}

   public native final String getStatus() /*-{
      return this.status;
   }-*/;

   public native final String getPath() /*-{
      return this.path;
   }-*/;

   public native final String getRawPath() /*-{
      return this.raw_path;
   }-*/;

   public native final String getChangelist() /*-{
      return this.changelist || "";
   }-*/;

   public native final boolean isDiscardable() /*-{
      return !!this.discardable;
   }-*/;
   
   public native final boolean isDirectory() /*-{
      return !!this.is_directory;
   }-*/;
   
   public native final double getFileSize() /*-{
      return this.size || 0;
   }-*/;

   public native static StatusAndPathInfo create(String status,
                                          String path,
                                          String rawPath,
                                          boolean discardable,
                                          boolean directory) /*-{
      return {
         status: status,
         path: path,
         raw_path: rawPath,
         discardable: discardable,
         is_directory: directory
      };
   }-*/;
}
