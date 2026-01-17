/*
 * AssistantTypes.java
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
package org.rstudio.studio.client.workbench.assistant.model;

import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;
import jsinterop.base.Any;

// The set of types returned by LSP-based AI assistant agents.
// There is some overlap with LSP types here.
public class AssistantTypes
{
   @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
   public static class AssistantDiagnostics
   {
      public String report;
   }

   @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
   public static class AssistantPosition
   {
      public int line;
      public int character;
   }

   @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
   public static class AssistantTextDocument
   {
      String uri;
      int version;
   }

   @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
   public static class AssistantRange
   {
      public AssistantPosition start;
      public AssistantPosition end;
   }

   @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
   public static class AssistantCompletionCommand
   {
      public String title;
      public String command;
      public String[] arguments;
   }

   @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
   public static class AssistantCompletion
   {
      public String insertText;
      public AssistantRange range;
      public AssistantCompletionCommand command;
   }

   @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
   public static class AssistantError
   {
      public int code;
      public String message;
   }

   @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
   public static class AssistantResponse
   {
      public String jsonrpc;
      public String id;
      public Any result;
      public AssistantError error;
      public boolean cancelled;
   }


}
