/*
 * SynctexUtils.java
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


package org.rstudio.studio.client.common.synctex;

import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.DesktopInfo;

public class SynctexUtils
{
   public static String getDesktopSynctexViewer()
   {
      if (Desktop.isDesktop())
         return DesktopInfo.getDesktopSynctexViewer();
      else
         return "";
   }
   
   public static void maybeShowFirefoxWarning(String target)
   {
      /*
      if (!messageShown_)
      {
         messageShown_ = true;
         
         RStudioGinjector.INSTANCE.getGlobalDisplay().showMessage(
            MessageDialog.POPUP_BLOCKED,
            "Unable to Switch Windows",
            "The " + target + " was updated to sync to the current location " +
            "however Firefox has prevented RStudio from switching to the " +
            target + " window.\n\n" +
            "To avoid this problem in the future you may want to use " +
            "Google Chrome rather than Firefox when compiling and " +
            "previewing PDFs.");
      }
      */
   }
   
   @SuppressWarnings("unused")
   private static boolean messageShown_ = false;
}
