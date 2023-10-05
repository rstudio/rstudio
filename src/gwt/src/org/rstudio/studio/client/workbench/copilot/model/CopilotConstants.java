/*
 * CopilotConstants.java
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

public class CopilotConstants
{
   public static final String STATUS_OK = "OK";
   public static final String STATUS_ALREADY_SIGNED_IN = "AlreadySignedIn";
   public static final String STATUS_NOT_AUTHORIZED = "NotAuthorized";
   public static final String STATUS_NOT_SIGNED_IN = "NotSignedIn";
   public static final String STATUS_PROMPT_USER_DEVICE_FLOW = "PromptUserDeviceFlow";
   
   public static class ErrorCodes
   {
      public static final int DOCUMENT_NOT_FOUND = -32602;
      public static final int UNABLE_TO_GET_LOCAL_ISSUER_CERTIFICATE = -32603;
      public static final int NOT_SIGNED_IN = 1000;
   }
}
