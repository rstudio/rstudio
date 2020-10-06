/*
 * UserPrefsUtils.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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
package org.rstudio.studio.client.workbench.prefs.model;

import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.DesktopInfo;

public class UserPrefUtils
{
   public static String getDefaultPdfPreview()
   {
      if (Desktop.isDesktop())
      {
         // if there is a desktop synctex viewer available then default to it
         if (DesktopInfo.getDesktopSynctexViewer().length() > 0)
         {
            return UserPrefs.PDF_PREVIEWER_DESKTOP_SYNCTEX;
         }
         
         // otherwise default to the internal viewer
         else
         {
            return UserPrefs.PDF_PREVIEWER_RSTUDIO;
         }
      }
      
      // web mode -- always default to internal viewer
      else
      {
         return UserPrefs.PDF_PREVIEWER_RSTUDIO;
      }
   }
}
