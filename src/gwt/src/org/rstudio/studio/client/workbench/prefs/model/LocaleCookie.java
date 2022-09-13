/*
 * LocaleCookie.java
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
 * Manage cookie used for setting the UI Language. This cookie's name corresponds to the
 * value specified for locale.cookie property in RStudio.gwt.xml.
 */
public class LocaleCookie
{
   /**
    * @return Current UI language setting, or the default (en) if none set
    */
   public static String getUiLanguage()
   {
      String cookieValue = Cookies.getCookie(UI_LANG_COOKIE);
      if (StringUtil.isNullOrEmpty(cookieValue))
         cookieValue = "en";
      return cookieValue;
   }

   /**
    * Set the cookie for UI language.
    * @param uiLanguage
    */
   @SuppressWarnings("deprecation") // Date is deprecated but the replacement isn't available in GWT
   public static void setUiLanguage(String uiLanguage)
   {
      Cookies.setCookie(UI_LANG_COOKIE, uiLanguage, new Date(5000, Calendar.DECEMBER, 31, 18, 0));
   }

   private static final String UI_LANG_COOKIE = "LOCALE";
}
