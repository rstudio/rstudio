/*
 * Breakpoint.java
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

package org.rstudio.studio.client.common.debugging.model;

import com.google.gwt.core.client.JavaScriptObject;

// This class represents a breakpoint in nearly every context: 
// instances are used in the editor, in the breakpoint manager
// (BreakpointManager), and are also persisted raw in the project state
// (BreakpointState). 
public class Breakpoint extends JavaScriptObject
{   
   protected Breakpoint() {}

   // Syntactically an enum would be preferable here but GWT enum wrappers 
   // don't serialize well inside native JS objects. Do not change these values;
   // they are persisted in project storage. 
   public final static int STATE_PROCESSING = 0;
   public final static int STATE_ACTIVE = 1;
   public final static int STATE_INACTIVE = 2;
   
   public native static Breakpoint create(
         int breakpointId,
         String fileName,
         String functionName,
         int lineNumber, 
         int initialState)
   /*-{
      return {
         id : breakpointId,
         file_name : fileName,
         line_number : lineNumber,
         function_steps : "",
         function_name : functionName,
         state : initialState,
         editor_state: initialState
      };
   }-*/;
   
   public final native void addFunctionSteps(
         String function_name,
         int lineNumber,
         int functionSteps)
   /*-{
      this.function_name = function_name;
      this.line_number = lineNumber;
      this.function_steps = functionSteps;
   }-*/;
   
   public final native int getBreakpointId()
   /*-{
      return this.id;
   }-*/;
   
   public final native String getFileName()
   /*-{
      return this.file_name;
   }-*/;
   
   public final native int getLineNumber()
   /*-{
      return this.line_number;
   }-*/;
   
   public final native String getFunctionName()
   /*-{
      return this.function_name;
   }-*/;
   
   public final native String getFunctionSteps()
   /*-{
      return this.function_steps;
   }-*/;
   
   public final native int getState()
   /*-{
      return this.state;
   }-*/;
   
   public final native void setState (int state)
   /*-{
      this.state = state;
   }-*/;
   
   public final native int getEditorState()
   /*-{
      return this.editor_state;
   }-*/;
   
   public final native void setEditorState (int state)
   /*-{
      this.editor_state = state;
   }-*/;
   
   public final native void moveToLineNumber(int lineNumber)
   /*-{
     this.line_number = lineNumber;
   }-*/;
}
