/*
 * CompilePdfResult.java
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

package org.rstudio.studio.client.common.compilepdf.model;

import org.rstudio.studio.client.common.synctex.model.PdfLocation;

import com.google.gwt.core.client.JavaScriptObject;

public class CompilePdfResult extends JavaScriptObject
{
   protected CompilePdfResult()
   {
   }
   
   public final native boolean getSucceeded() /*-{
      return this.succeeded;
   }-*/;

   public final native String getTargetFile() /*-{
      return this.target_file;
   }-*/;
   
   public final native String getPdfPath() /*-{
      return this.pdf_path;
   }-*/;
   
   public final native String getViewPdfUrl() /*-{
      return this.view_pdf_url;
   }-*/;
   
   public final native boolean isSynctexAvailable() /*-{
      return this.synctex_available;
   }-*/;
   
   public final native PdfLocation getPdfLocation() /*-{
      return this.pdf_location;
   }-*/;
}
