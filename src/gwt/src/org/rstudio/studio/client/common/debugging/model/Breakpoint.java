/*
 * Breakpoint.java
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

package org.rstudio.studio.client.common.debugging.model;

import com.google.gwt.core.client.JavaScriptObject;

// This class represents a breakpoint in nearly every context: 
// instances are used in the editor (AceEditorWidget), in the breakpoint manager
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
   public final static int STATE_REMOVING = 3;
   
   public final static int TYPE_FUNCTION = 0;
   public final static int TYPE_TOPLEVEL = 1;
   
   public native static Breakpoint create(
         int breakpointId,
         String path,
         String functionName,
         int lineNumber, 
         int initialState,
         int type)
   /*-{
      return {
         id : breakpointId,
         path : path.trim(),
         line_number : lineNumber,
         function_steps : "",
         function_name : functionName,
         state : initialState,
         editor_state: initialState, 
         editor_line_number: lineNumber,
         is_pending_debug_completion: false,
         needs_updated_steps: false,
         type: type,
         is_package_breakpoint: false,
         package_name: ""
      };
   }-*/;
      
   public final native void addFunctionSteps(
         String function_name,
         int lineNumber,
         String functionSteps)
   /*-{
      this.function_name = function_name;
      this.line_number = lineNumber;
      this.function_steps = functionSteps;
   }-*/;
   
   public final native int getBreakpointId()
   /*-{
      return this.id;
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
      // when the breakpoint becomes active, clear the flag indicating that 
      // it needs updating
      if (this.state == 1)
      {
         this.needs_updated_steps = false;
      }
   }-*/;
   
   public final native int getEditorState()
   /*-{
      return this.editor_state;
   }-*/;
   
   public final native void setEditorState (int state)
   /*-{
      this.editor_state = state;
   }-*/;
   
   public final native int getEditorLineNumber()
   /*-{
      return this.editor_line_number;
   }-*/;

   public final native void setEditorLineNumber(int lineNumber)
   /*-{
      this.editor_line_number = lineNumber;
   }-*/;   

   public final native void moveToLineNumber(int lineNumber)
   /*-{
     this.line_number = lineNumber;
     this.editor_line_number = lineNumber;
   }-*/;
   
   public final native boolean isPendingDebugCompletion()
   /*-{
     return this.is_pending_debug_completion;
   }-*/;

   public final native boolean setPendingDebugCompletion(boolean pending)
   /*-{
     this.is_pending_debug_completion = pending;
   }-*/;
   
   public final native boolean needsUpdatedSteps()
   /*-{
     return this.needs_updated_steps;
   }-*/;
   
   public final native void markStepsNeedUpdate()
   /*-{
      this.needs_updated_steps = true;
   }-*/;
   
   public final native boolean isInFile(String filename)
   /*-{
      return this.path == filename;
   }-*/;
   
   public final native boolean isInPath(String path)
   /*-{
      return this.path.indexOf(path) == 0;
   }-*/;
   
   public final native String getPath()
   /*-{
     return this.path;
   }-*/;
   
   public final native int getType()
   /*-{
      return this.type;
   }-*/;

   public final native String getPackageName() /*-{
      return this.package_name;
   }-*/;
   
   public final native boolean isPackageBreakpoint() /*-{
      return this.is_package_breakpoint;
   }-*/;
   
   public final native void markAsPackageBreakpoint(String packageName) /*-{
      this.is_package_breakpoint = true;
      this.package_name = packageName;
   }-*/;
}
