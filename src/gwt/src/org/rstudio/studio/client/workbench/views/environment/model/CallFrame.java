/*
 * CallFrame.java
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

   public final native String getArgumentList() /*-{
       return this.argument_list;
   }-*/;
   
   public final native int getFunctionLineNumber() /*-{
       return this.function_line_number;
   }-*/;
   
   public final native int getFunctionCharacterNumber() /*-{
       return this.function_character_number;
   }-*/;
   
   public final native boolean isShinyFunction() /*-{
       return this.is_shiny_function;
   }-*/;

   public final DebugFilePosition getRange() 
   {
      return DebugFilePosition.create(
            getLineNumber(), 
            getEndLineNumber(), 
            getCharacterNumber(), 
            getEndCharacterNumber());
   };
   
   public final boolean isNavigable()
   {
      return getLineNumber() > 0;
   }
   
   public final static boolean isNavigableFilename(String fileName)
   {
      if (fileName.length() > 0 &&
          !fileName.equalsIgnoreCase("NULL") &&
          !fileName.equalsIgnoreCase("<tmp>"))
      {
         return true;
      }
      return false;
   }
}
