/*
 * RProjectAssistantOptions.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
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
package org.rstudio.studio.client.projects.model;

import jsinterop.annotations.JsOverlay;
import jsinterop.annotations.JsPackage;
import jsinterop.annotations.JsType;

@JsType(isNative = true, namespace = JsPackage.GLOBAL, name = "Object")
public class RProjectAssistantOptions
{
   @JsOverlay
   public static RProjectAssistantOptions createEmpty()
   {
      RProjectAssistantOptions options = new RProjectAssistantOptions();
      options.assistant = "default";
      options.chat_provider = "default";
      options.copilot_enabled = -1;  // DefaultValue
      options.copilot_indexing_enabled = -1;  // DefaultValue
      return options;
   }

   // Assistant selection: "default", "none", "posit", "copilot"
   public String assistant;

   // Chat provider selection: "default", "none", "posit"
   public String chat_provider;

   // NOTE: These map to the 'YesNoAskValue' enum used for project options.
   public int copilot_enabled;  // deprecated, use assistant
   public int copilot_indexing_enabled;
}
