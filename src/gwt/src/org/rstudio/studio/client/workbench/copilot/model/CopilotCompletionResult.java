/*
 * CopilotCompletionResult.java
 *
 * Copyright (C) 2023 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.copilot.model;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;

public class CopilotCompletionResult extends JavaScriptObject
{
   public static class CopilotPosition extends JavaScriptObject
   {
      protected CopilotPosition()
      {
      }
      
      public final native int getLine()
      /*-{
         return this["line"];
      }-*/;
      
      public final native int getCharacter()
      /*-{
         return this["character"];
      }-*/;
   }
   
   public static class CopilotRange extends JavaScriptObject
   {
      protected CopilotRange()
      {
      }
      
      public final native CopilotPosition getStart()
      /*-{
         return this["start"];
      }-*/;
      
      public final native CopilotPosition getEnd()
      /*-{
         return this["end"];
      }-*/;
   }
   
   public static class CopilotCompletion extends JavaScriptObject
   {
      protected CopilotCompletion()
      {
      }
      
      public final native String getText()
      /*-{
         return this["text"];
      }-*/;
      
      public final native CopilotRange getRange()
      /*-{
         return this["range"];
      }-*/;
      
      public final native CopilotPosition getPosition()
      /*-{
         return this["position"];
      }-*/;
   }
   
   protected CopilotCompletionResult()
   {
   }
   
   public final native JsArray<CopilotCompletion> getCompletions()
   /*-{
      return this.result.completions || [];
   }-*/;
}
