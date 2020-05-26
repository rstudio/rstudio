/*
 * RSConnectDeploymentFiles.java
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
package org.rstudio.studio.client.rsconnect.model;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;

public class RSConnectDeploymentFiles extends JavaScriptObject
{
   protected RSConnectDeploymentFiles()
   {
   }

   public final native JsArrayString getDirList() /*-{
      return this.dir_list;
   }-*/;

   public final native int getDirSize() /*-{
      return this.dir_size;
   }-*/;

   public final native int getMaxSize() /*-{
      return this.max_size;
   }-*/;
}
