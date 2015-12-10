/*
 * ApplicationUtils.java
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

package org.rstudio.studio.client.application;

import org.rstudio.core.client.regex.Pattern;

import com.google.gwt.core.client.GWT;


public class ApplicationUtils
{
   public static String getHostPageBaseURLWithoutContext(boolean includeSlash)
   {
      String replaceWith = includeSlash ? "/" : "";
      String url = GWT.getHostPageBaseURL();
      Pattern pattern = Pattern.create("/s/[A-Fa-f0-9]{5}[A-Fa-f0-9]{8}[A-Fa-f0-9]{8}/");
      return pattern.replaceAll(url, replaceWith);
   }
   
}
