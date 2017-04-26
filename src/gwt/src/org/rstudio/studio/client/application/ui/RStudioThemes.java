/*
 * RStudioThemes.java
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

package org.rstudio.studio.client.application.ui;

import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;

import com.google.gwt.dom.client.Document;

public class RStudioThemes
{
   public static void initializeThemes(UIPrefs uiPrefs, Document document)
   {
      if (uiPrefs.getUseFlatThemes().getGlobalValue()) {
         document.getBody().removeClassName("rstudio-themes-flat");
         document.getBody().removeClassName("rstudio-themes-dark");
         document.getBody().removeClassName("rstudio-themes-default");
         document.getBody().removeClassName("rstudio-themes-dark-grey");
         document.getBody().removeClassName("rstudio-themes-alternate");
         
         String themeName = uiPrefs.getFlatTheme().getGlobalValue();
         document.getBody().addClassName("rstudio-themes-flat");
         if (themeName.contains("dark")) {
            document.getBody().addClassName("rstudio-themes-dark");
         }
         document.getBody().addClassName("rstudio-themes-" + themeName);
      }
   }
}
