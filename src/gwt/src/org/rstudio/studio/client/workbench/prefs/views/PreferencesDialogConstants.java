/*
 * PreferencesDialogConstants.java
 *
 * Copyright (C) 2024 by Posit Software, PBC
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
package org.rstudio.studio.client.workbench.prefs.views;

//
// This class exists primarily to make changes to the preference dialog's
// dimensions more straightforward. A variety of sizes are still hardcoded
// in different dialogs, but where possible, those values should either be
// moved here, or computed based on existing values herein.
//
public class PreferencesDialogConstants
{
   public static final int PANEL_CONTAINER_WIDTH  = 640;
   public static final int PANEL_CONTAINER_HEIGHT = 580;
   
   public static final String panelContainerWidth()
   {
      return PANEL_CONTAINER_WIDTH + "px";
   }
   
   public static final String panelContainerHeight()
   {
      return PANEL_CONTAINER_HEIGHT + "px";
   }
   
}
