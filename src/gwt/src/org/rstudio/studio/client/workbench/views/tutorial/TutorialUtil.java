/*
 * TutorialUtil.java
 *
 * Copyright (C) 2020 by RStudio, PBC
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.tutorial;

import com.google.gwt.core.client.GWT;

public class TutorialUtil
{
   // NOTE: Shiny URLs are either accessed through a proxied path
   // (using the 'p/' or 'p6/' path component as appropriate),
   // or directly from a separate authority (which may be a different
   // port on the same host)
   public static boolean isShinyUrl(String url)
   {
      String host = GWT.getHostPageBaseURL();
      return
            url.startsWith(host + "p/") ||
            url.startsWith(host + "p6/") ||
            !url.startsWith(host);
   }
}
