/*
 * WebDialogCookie.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.prefs.model;

import com.google.gwt.user.client.Cookies;
import org.rstudio.core.client.StringUtil;

import java.util.Calendar;
import java.util.Date;

/**
 * Manage cookie used to tell GWT to use web-based dialogs for picking files and folders, and showing messages, 
 * on Desktop (instead of the default operating-system native dialogs).
 */
public class WebDialogCookie
{
   /**
    * @return Whether cookie requesting use of web-based dialog boxes is set
    */
   public static boolean getUseWebDialogs()
   {
      return !StringUtil.isNullOrEmpty(Cookies.getCookie(WEB_DIALOG_COOKIE));
   }

   /**
    * Set/unset the cookie requesting use of web-based dialog boxes
    * @param useWebDialogs
    */
   @SuppressWarnings("deprecation") // Date is deprecated but the replacement isn't available in GWT
   public static void setUseWebDialogs(boolean useWebDialogs)
   {
      if (useWebDialogs)
         Cookies.setCookie(WEB_DIALOG_COOKIE, "1", new Date(5000, Calendar.DECEMBER, 31, 18, 0));
      else
         Cookies.removeCookie(WEB_DIALOG_COOKIE);
   }

   private static final String WEB_DIALOG_COOKIE = "WEBDIALOG";
}
