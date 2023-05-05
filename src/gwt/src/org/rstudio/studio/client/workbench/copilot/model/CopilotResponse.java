/*
 * CopilotRequests.java
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

import java.util.List;

import org.rstudio.studio.client.workbench.copilot.model.CopilotTypes.CopilotCompletion;

import com.google.gwt.core.client.JsArray;

import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;
import jsinterop.base.JsArrayLike;
import jsinterop.base.JsPropertyMap;

// These are the classes used as RPC result types (our own, not Copilot's!)
//
// Class names should match method names from CopilotServerOperations.java.
public class CopilotResponse
{
   @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
   public static class CopilotVerifyInstalledResponse
   {
      public boolean installed;
   }
   
   @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
   public static class CopilotInstallAgentResponse
   {
      public boolean installed;
   }
   
   @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
   public static class CopilotCodeCompletionResponseResult
   {
      public JsArrayLike<CopilotCompletion> completions;
      public String cancellationReason;
   }
   
   @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
   public static class CopilotCodeCompletionResponse
   {
      public String jsonrpc;
      public String id;
      public CopilotCodeCompletionResponseResult result;
   }
}
