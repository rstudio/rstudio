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

import org.rstudio.core.client.jsonrpc.RpcError;
import org.rstudio.studio.client.workbench.copilot.model.CopilotTypes.CopilotCompletion;
import org.rstudio.studio.client.workbench.copilot.model.CopilotTypes.CopilotResponse;

import elemental2.core.JsNumber;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;
import jsinterop.base.JsArrayLike;

// These are the classes used as RPC result types (our own, not Copilot's!)
//
// Class names should match method names from CopilotServerOperations.java.
public class CopilotResponseTypes
{
   // This is extra metadata added by us; not part of typical Copilot return values.
   // Used to communicate to the user why the Copilot Agent might not be running.
   public static class CopilotAgentNotRunningReason
   {
      public static String reasonToString(int reason)
      {
         if (reason == Unknown)
         {
            return "An unknown error occurred.";
         }
         else if (reason == NotInstalled)
         {
            return "The GitHub Copilot agent is not installed.";
         }
         else if (reason == DisabledByAdministrator)
         {
            return "GitHub Copilot has been disabled by the system administrator.";
         }
         else if (reason == DisabledViaProjectPreferences)
         {
            return "GitHub Copilot has been disabled via project preferences.";
         }
         else if (reason == DisabledViaGlobalOptions)
         {
            return "GitHub Copilot has been disabled via global options.";
         }
         else if (reason == LaunchError)
         {
            return "An error occurred while attempting to launch GitHub Copilot.";
         }
         else
         {
            return "<unknown>";
         }
      }
      
      public static int Unknown = 0;
      public static int NotInstalled = 1;
      public static int DisabledByAdministrator = 2;
      public static int DisabledViaProjectPreferences = 3;
      public static int DisabledViaGlobalOptions = 4;
      public static int LaunchError = 5;
   }
   
   @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
   public static class CopilotResponseMetadata
   {
      String method;
   }
   
   @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
   public static class CopilotVerifyInstalledResponse
   {
      public boolean installed;
   }
   
   @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
   public static class CopilotInstallAgentResponse
   {
      public String error;
   }
   
   @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
   public static class CopilotSignInResponseResult
   {
      public int expiresIn;
      public int interval;
      public String status;
      public String user;
      public String userCode;
      public String verificationUri;
   }
   
   @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
   public static class CopilotSignInResponse extends CopilotResponse
   {
   }
   
   @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
   public static class CopilotSignOutResponseResult
   {
      public String status;
   }
   
   @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
   public static class CopilotSignOutResponse
   {
      public String jsonrpc;
      public String id;
      public CopilotSignOutResponseResult result;
   }
   
   @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
   public static class CopilotStatusResponseResult
   {
      public String status;
      public String user;
   }
   
   @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
   public static class CopilotStatusResponse
   {
      public String jsonrpc;
      public String id;
      public CopilotStatusResponseResult result;
      
      // These aren't part of a normal Copilot status request; we append
      // this extra information to help report agent launch errors.
      public JsNumber reason;
      public RpcError error;
      public String output;
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
   
   @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
   public static class CopilotGenerateCompletionsResponse extends CopilotResponse
   {
   }
}
