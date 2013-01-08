/*
 * CompileError.java
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

package org.rstudio.studio.client.common.compile;

import org.rstudio.core.client.js.JsUtil;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

public class CompileError extends JavaScriptObject
{
   // all possible errors across all compilation types should be listed
   // here -- we do this because the way we display error icons is via
   // css styles and it's a bit complicated to dynamically compose the
   // stylesheet for different compilation scenarios
   public static final int ERROR = 0;
   public static final int WARNING = 1;
   public static final int BOX = 2;  // LaTeX bad box error
   
   protected CompileError()
   {
   }
   
   public final native int getType() /*-{
      return this.type;
   }-*/;
   
   public final native String getPath() /*-{
      return this.path;
   }-*/;
   
   /*
    * Can return -1 for unknown line
    */
   public final native int getLine() /*-{
      return this.line;
   }-*/;
   
   public final native int getColumn() /*-{
      return this.column;
   }-*/;
   
   public final native String getMessage() /*-{
      return this.message;
   }-*/;
   
   /*
    * Can return empty string for no log path
    */
   public final native String getLogPath() /*-{
      return this.log_path;
   }-*/;

   /*
    * Can return -1 for unknown line
    */
   public final native int getLogLine() /*-{
      return this.log_line;
   }-*/;
   
   public final native boolean getShowErrorList() /*-{
      if (this.show_error_list === null)
         return true;
      else
         return this.show_error_list;
   }-*/;
 
   public final static boolean showErrorList(JsArray<CompileError> errors)
   { 
      if (errors == null)
         return false;
      
      for (CompileError error : JsUtil.asIterable(errors))
      {  
         if ((error.getType() == CompileError.ERROR) && 
             error.getShowErrorList())
           return true;
      }
      
      return false;
   }
   
   public final static CompileError getFirstError(JsArray<CompileError> errors)
   {
      for (int i=0; i<errors.length(); i++)
      {
         CompileError error = errors.get(i);
         if (error.getType() == CompileError.ERROR)
            return error;
      }
      
      return null;
   }
}
