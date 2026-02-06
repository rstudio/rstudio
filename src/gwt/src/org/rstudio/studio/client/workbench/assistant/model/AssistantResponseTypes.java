/*
 * AssistantResponseTypes.java
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

import org.rstudio.core.client.jsonrpc.RpcError;
import org.rstudio.studio.client.workbench.assistant.AssistantUIConstants;
import org.rstudio.studio.client.workbench.assistant.model.AssistantTypes.AssistantCompletion;
import org.rstudio.studio.client.workbench.assistant.model.AssistantTypes.AssistantCompletionCommand;
import org.rstudio.studio.client.workbench.assistant.model.AssistantTypes.AssistantRange;
import org.rstudio.studio.client.workbench.assistant.model.AssistantTypes.AssistantResponse;
import org.rstudio.studio.client.workbench.assistant.model.AssistantTypes.AssistantTextDocument;

import com.google.gwt.core.client.GWT;

import elemental2.core.JsNumber;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;
import jsinterop.base.JsArrayLike;

// These are the classes used as RPC result types (our own, not the assistant's!)
//
// Class names should match method names from AssistantServerOperations.java.
public class AssistantResponseTypes
{
   // This is extra metadata added by us; not part of typical assistant return values.
   // Used to communicate to the user why the assistant agent might not be running.
   public static class AssistantAgentNotRunningReason
   {
      public static String reasonToString(int reason, String assistantName)
      {
         if (reason == Unknown)
         {
            return constants_.assistantUnknownError();
         }
         else if (reason == NotInstalled)
         {
            return constants_.assistantNotInstalledError(assistantName);
         }
         else if (reason == DisabledByAdministrator)
         {
            return constants_.assistantDisabledByAdministratorError(assistantName);
         }
         else if (reason == DisabledViaProjectPreferences)
         {
            return constants_.assistantDisabledViaProjectPreferencesError(assistantName);
         }
         else if (reason == DisabledViaGlobalOptions)
         {
            return constants_.assistantDisabledViaGlobalOptionsError(assistantName);
         }
         else if (reason == LaunchError)
         {
            return constants_.assistantLaunchError(assistantName);
         }
         else
         {
            return constants_.assistantUnknownErrorShort();
         }
      }

      public static boolean isError(JsNumber reason)
      {
         if (reason == null)
            return false;

         int intReason = (int) reason.valueOf();
         return intReason == Unknown || intReason == NotInstalled || intReason == LaunchError;
      }

      public static int Unknown = 0;
      public static int NotInstalled = 1;
      public static int DisabledByAdministrator = 2;
      public static int DisabledViaProjectPreferences = 3;
      public static int DisabledViaGlobalOptions = 4;
      public static int LaunchError = 5;
   }

   @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
   public static class AssistantDiagnosticsResponse extends AssistantResponse
   {
   }


   @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
   public static class AssistantResponseMetadata
   {
      String method;
   }

   @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
   public static class AssistantSignInResponseResult
   {
      public int expiresIn;
      public int interval;
      public String status;
      public String user;
      public String userCode;
      public String verificationUri;
   }

   @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
   public static class AssistantSignInResponse extends AssistantResponse
   {
   }

   @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
   public static class AssistantSignOutResponseResult
   {
      public String status;
   }

   @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
   public static class AssistantSignOutResponse
   {
      public String jsonrpc;
      public String id;
      public AssistantSignOutResponseResult result;
   }

   @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
   public static class AssistantStatusResponseResult
   {
      public String status;
      public String user;
   }

   @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
   public static class AssistantStatusResponse
   {
      public String jsonrpc;
      public String id;
      public AssistantStatusResponseResult result;

      // These aren't part of a normal assistant status request; we append
      // this extra information to help report agent launch errors.
      public JsNumber reason;
      public RpcError error;
      public String output;
   }

   @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
   public static class AssistantCodeCompletionResponseResult
   {
      public JsArrayLike<AssistantCompletion> completions;
      public String cancellationReason;
   }

   @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
   public static class AssistantCodeCompletionResponse
   {
      public String jsonrpc;
      public String id;
      public AssistantCodeCompletionResponseResult result;
   }

   @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
   public static class AssistantGenerateCompletionsResponse extends AssistantResponse
   {
      // These aren't part of a normal assistant completions request; we append
      // this extra information to report whether the assistant is enabled for this document.
      public Boolean enabled;
   }

   @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
   public static class AssistantNextEditSuggestionsResultEntry
   {
      public String text;
      public AssistantTextDocument textDocument;
      public AssistantRange range;
      public AssistantCompletionCommand command;
   }

   @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
   public static class AssistantNextEditSuggestionsResult
   {
      public JsArrayLike<AssistantNextEditSuggestionsResultEntry> edits;
   }

   @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
   public static class AssistantNextEditSuggestionsResponse
   {
      public String jsonrpc;
      public String id;
      public AssistantNextEditSuggestionsResult result;
   }

   @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
   public static class AssistantNextCommandSuggestionSuggestion
   {
      public String type;
      public String oldString;
      public String newString;
      public String nextCommand;
   }

   @JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
   public static class AssistantNextCommandSuggestionResponse
   {
      public String jsonrpc;
      public String id;
      public AssistantNextCommandSuggestionSuggestion result;
   }

   private static final AssistantUIConstants constants_ = GWT.create(AssistantUIConstants.class);
}
