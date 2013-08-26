/*
 * URLUtils.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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

package org.rstudio.studio.client.common;


import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.URL;

public class URLUtils
{
   public static String getApplicationURL(String pathName)
   {      
      return GWT.getHostPageBaseURL() + pathName;
   }
   
   public static String getProjectURL(String project)
   {
      String url = getApplicationURL("");
      String encodedProject = URL.encodeQueryString(project);
      encodedProject = encodedProject.replace("%2F", "/");
      url += "?project=" + encodedProject;
      return url;
   }
}
