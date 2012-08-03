/*
 * UndoManager.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.source.editors.text.ace;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONParser;
import com.google.gwt.json.client.JSONValue;

public class UndoManager extends JavaScriptObject
{
   protected UndoManager()
   {}

   public native final JavaScriptObject peek() /*-{
      return this.peek();
   }-*/;
   
   public final String saveState()
   {
      JavaScriptObject stateObj = saveStateNative();
      return new JSONObject(stateObj).toString();
   }
   
   public final void restoreState(String state)
   {
      JSONValue stateJson = JSONParser.parseStrict(state);
      JSONObject stateObj = stateJson.isObject();
      
      JSONArray undoStack = stateObj.get("undoStack").isArray();
      setUndoStack(undoStack.getJavaScriptObject());
      
      JSONArray redoStack = stateObj.get("redoStack").isArray();
      setRedoStack(redoStack.getJavaScriptObject());
   }
   
   
   private native final JavaScriptObject saveStateNative() /*-{
      var state = new Object();
      state.undoStack = this.$undoStack; 
      state.redoStack = this.$redoStack; 
      return state;
   }-*/;
   
   private native final void setUndoStack(JavaScriptObject undoStack) /*-{
      this.$undoStack.length = 0;
      for(var i=0,len=undoStack.length; i<len; i++)
         this.$undoStack.push(undoStack[i]);
   }-*/;
   
   private native final void setRedoStack(JavaScriptObject redoStack) /*-{
       this.$redoStack.length = 0;
       for(var i=0,len=redoStack.length; i<len; i++)
         this.$redoStack.push(redoStack[i]);
   }-*/;
}   
