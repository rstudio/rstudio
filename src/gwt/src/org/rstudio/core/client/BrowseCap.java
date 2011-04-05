/*
 * BrowseCap.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Document;

public class BrowseCap
{
   public static double getFontSkew()
   {
      if (hasMetaKey())
         return -1;
      else
         return 0;
   }

   public static final BrowseCap INSTANCE = GWT.create(BrowseCap.class);

   public boolean suppressBraceHighlighting()
   {
      return false;
   }

   public boolean emulatedHomeAndEnd()
   {
      return false;
   }

   public boolean aceVerticalScrollBarIssue()
   {
      return false;
   }

   public boolean suppressBrowserForwardBack()
   {
      return false;
   }

   public static boolean hasMetaKey()
   {
      return OPERATING_SYSTEM.equals("macintosh");
   }

   public static boolean isLinux()
   {
      return OPERATING_SYSTEM.equals("linux");
   }

   private static native final String getOperatingSystem() /*-{
      var ua = navigator.userAgent.toLowerCase();
      if (ua.indexOf("linux") != -1) {
         return "linux";
      } else if (ua.indexOf("macintosh") != -1) {
         return "macintosh";
      }
      return "windows";
   }-*/;
   private static final String OPERATING_SYSTEM = getOperatingSystem();

   static
   {
      Document.get().getBody().addClassName(OPERATING_SYSTEM);
   }
}
