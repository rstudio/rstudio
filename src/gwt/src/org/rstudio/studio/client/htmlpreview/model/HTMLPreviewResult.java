/*
 * HTMLPreviewResult.java
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

public class HTMLPreviewResult extends JavaScriptObject
{
   protected HTMLPreviewResult()
   {
   }
   
   public final native boolean getSucceeded() /*-{
      return this.succeeded;
   }-*/;
   
   public final native String getTitle() /*-{
      return this.title;
   }-*/;
   
   public final native String getPreviewURL() /*-{
      return this.preview_url;
   }-*/;
   
   public final native String getSourceFile() /*-{
      return this.source_file;
   }-*/;
   
   public final native String getHtmlFile() /*-{
      return this.html_file;
   }-*/;
   
   public final native boolean getEnableFileLabel() /*-{
      return this.enable_file_label;
   }-*/;
   
   public final native boolean getEnableSaveAs() /*-{
      return this.enable_saveas;
   }-*/;
   
   public final native boolean getEnableReexecute() /*-{
      return this.enable_reexecute;
   }-*/;
   
   public final native boolean getEnablePublish() /*-{
      return this.succeeded && (this.source_file !== null);
   }-*/;

   public final native boolean getPreviouslyPublished() /*-{
      return this.previously_published;
   }-*/;
}
