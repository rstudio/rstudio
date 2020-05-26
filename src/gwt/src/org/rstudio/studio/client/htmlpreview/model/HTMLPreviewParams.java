/*
 * HTMLPreviewParams.java
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
package org.rstudio.studio.client.htmlpreview.model;

import com.google.gwt.core.client.JavaScriptObject;

public class HTMLPreviewParams extends JavaScriptObject
{
   protected HTMLPreviewParams()
   {
   }
   
   public final static native HTMLPreviewParams create(String path,
                                                       String encoding,
                                                       boolean isMarkdown,
                                                       boolean requiresKnit,
                                                       boolean isNotebook) /*-{
      var params = new Object();
      params.path = path;
      params.encoding = encoding;
      params.is_markdown = isMarkdown;
      params.requires_knit = requiresKnit;
      params.is_notebook = isNotebook;
      return params;
   }-*/; 
   
   public final native String getPath() /*-{
      return this.path;
   }-*/;
   
   public final native String getEncoding() /*-{
      return this.encoding;
   }-*/;
   
   public final native boolean isMarkdown() /*-{
      return this.is_markdown;
   }-*/;
   
   public final native boolean getRequiresKnit() /*-{
      return this.requires_knit;
   }-*/;

   public final native boolean isNotebook() /*-{
      return this.is_notebook;
   }-*/;
}
