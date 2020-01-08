/*
 * TutorialUtil.java
 *
 * Copyright (C) 2009-20 by RStudio, Inc.
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
   public static boolean isShinyUrl(String url)
   {
      return
            url.startsWith(GWT.getHostPageBaseURL() + "p/") ||
            url.startsWith(GWT.getHostPageBaseURL() + "p6/") ||
            !url.startsWith(GWT.getHostPageBaseURL());
   }
}
