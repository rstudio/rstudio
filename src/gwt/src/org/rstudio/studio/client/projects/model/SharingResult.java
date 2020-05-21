/*
 * SharingResult.java
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

public class SharingResult extends JavaScriptObject
{
   protected SharingResult()
   {
   }
   
   public final native boolean succeeded() /*-{
      return this.succeeded;
   }-*/;

   public final native JsArrayString getFailedPaths() /*-{
      return this.failed_paths;
   }-*/;
   
   public final native String getErrorMessage() /*-{
      return this.error_message;
   }-*/;
}
