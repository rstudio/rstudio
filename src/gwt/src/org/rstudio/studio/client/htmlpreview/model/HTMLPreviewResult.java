/*
 * HTMLPreviewResult.java
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
package org.rstudio.studio.client.htmlpreview.model;

import com.google.gwt.core.client.JavaScriptObject;

public class HTMLPreviewResult extends JavaScriptObject
{
   protected HTMLPreviewResult()
   {
   }
   
   public final native boolean getSucceeded() /*-{
      return this.succeeded;
   }-*/;
   
   public final native String getPreviewURL() /*-{
      return this.preview_url;
   }-*/;
   
   public final native boolean getEnableScripts() /*-{
      return this.enable_scripts;
   }-*/;
}
