/*
 * CppDiagnostic.java
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
package org.rstudio.studio.client.workbench.views.source.model;


import org.rstudio.core.client.FileLocation;
import org.rstudio.core.client.FilePosition;

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
    
   public native final String getMessage() /*-{
      return this.message;
   }-*/;

   public native final FileLocation getLocation() /*-{
      return this.location;
   }-*/;
   
   public native final JsArray<FilePosition> getRanges() /*-{
      return this.ranges;
   }-*/;
}
