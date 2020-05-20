/*
 * CppDiagnostic.java
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
package org.rstudio.studio.client.workbench.views.source.model;


import org.rstudio.core.client.FilePosition;
import org.rstudio.core.client.FileRange;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

public class CppDiagnostic extends JavaScriptObject
{
   protected CppDiagnostic()
   {
   }
   
   // severity types
   public static final int IGNORED = 0;
   public static final int NOTE = 1;
   public static final int WARNING = 2;
   public static final int ERROR = 3;
   public static final int FATAL = 4; 
   
   public native final int getSeverity() /*-{
      return this.severity;
   }-*/; 
   
   public native final int getCategory() /*-{
      return this.category;
   }-*/; 
   
   public native final String getCategoryText() /*-{
      return this.category_text;
   }-*/;
   
   public native final String getEnableOption() /*-{
      return this.enable_option;
   }-*/;
   
   public native final String getDisableOption() /*-{
      return this.disable_option;
   }-*/;

   public native final String getMessage() /*-{
      return this.message;
   }-*/;

   // (can be null)
   public native final String getFile() /*-{
      return this.file;
   }-*/;
   
   // (can be null)
   public native final FilePosition getPosition() /*-{
      return this.position;
   }-*/;
   
   public native final JsArray<FileRange> getRanges() /*-{
      return this.ranges;
   }-*/;
   
   public native final JsArray<CppDiagnosticFixIt> getFixIts() /*-{
      return this.fixits;
   }-*/;
   
   public native final JsArray<CppDiagnostic> getChildren() /*-{
      return this.children;
   }-*/;
}
