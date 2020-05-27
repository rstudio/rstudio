/*
 * CallFrame.java
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

package org.rstudio.studio.client.workbench.views.environment.model;

import org.rstudio.core.client.DebugFilePosition;

import com.google.gwt.core.client.JavaScriptObject;

public class CallFrame extends JavaScriptObject
{
   protected CallFrame()
   {
   }

   public final native String getFunctionName() /*-{
       return this.function_name;
   }-*/;

   public final native int getContextDepth() /*-{
       return this.context_depth;
   }-*/;

   public final native String getFileName() /*-{
       return this.file_name;
   }-*/;

   public final native String getAliasedFileName() /*-{
       return this.aliased_file_name;
   }-*/;

   public final native int getLineNumber() /*-{
       return this.line_number;
   }-*/;
   
   public final native int getEndLineNumber() /*-{
       return this.end_line_number;
   }-*/;
   
   public final native int getCharacterNumber() /*-{
       return this.character_number;
   }-*/;
   
   public final native int getEndCharacterNumber() /*-{
       return this.end_character_number;
   }-*/;

   public final native String getCallSummary() /*-{
       return this.call_summary;
   }-*/;
   
   public final native int getFunctionLineNumber() /*-{
       return this.function_line_number;
   }-*/;
   
   public final native int getFunctionCharacterNumber() /*-{
       return this.function_character_number;
   }-*/;
   
   public final native String getShinyFunctionLabel() /*-{
       return this.shiny_function_label;
   }-*/;
   
   public final native boolean hasRealSrcref() /*-{
       return this.real_sourceref;
   }-*/;
   
   public final native boolean isErrorHandler() /*-{
       return this.is_error_handler;
   }-*/;
   
   public final native boolean isHidden() /*-{
       return this.is_hidden;
   }-*/;
   
   public final native boolean isSourceEquiv() /*-{
      return this.is_source_equiv;
   }-*/;

   public final DebugFilePosition getRange() 
   {
      return DebugFilePosition.create(
            getLineNumber(), 
            getEndLineNumber(), 
            getCharacterNumber(), 
            getEndCharacterNumber());
   }
   
   public final boolean isNavigable()
   {
      return hasRealSrcref();
   }
   
   public final static boolean isNavigableFilename(String fileName)
   {
      if (fileName.length() > 0 &&
          !fileName.equalsIgnoreCase("NULL") &&
          !fileName.equalsIgnoreCase("<tmp>") &&
          !fileName.equalsIgnoreCase("<text>") &&
          !fileName.equalsIgnoreCase("~/.active-rstudio-document"))
      {
         return true;
      }
      return false;
   }
}
