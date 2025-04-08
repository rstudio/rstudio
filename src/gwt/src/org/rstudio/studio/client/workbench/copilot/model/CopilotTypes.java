/*
 * CopilotTypes.java
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

import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;
import jsinterop.base.Any;

// The set of types returned by the GitHub Copilot agent.
// There is some overlap with LSP types here.
public class CopilotTypes
{
   @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
   public static class CopilotDiagnostics
   {
      public String report;
   }
   
   @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
   public static class CopilotPosition
   {
      public int line;
      public int character;
   }
   
   @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
   public static class CopilotRange
   {
      public CopilotPosition start;
      public CopilotPosition end;
   }
   
   @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
   public static class CopilotCompletionCommand
   {
      public String command;
      public String[] arguments;
   }
   
   @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
   public static class CopilotCompletion
   {
      public String insertText;
      public CopilotRange range;
      public CopilotCompletionCommand command;
   }
   
   @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
   public static class CopilotError
   {
      public int code;
      public String message;
   }
   
   @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
   public static class CopilotResponse
   {
      public String jsonrpc;
      public String id;
      public Any result;
      public CopilotError error;
      public boolean cancelled;
   }
      
   
}
