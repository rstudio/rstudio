/*
 * URIUtils.java
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

package org.rstudio.core.client;

import com.google.gwt.http.client.URL;

public class URIUtils
{
   public static String addQueryParam(String url, String name, String value)
   {
      // first split into base and anchor
      String base = new String(url);
      String anchor = new String();
      int anchorPos = base.indexOf('#');
      if (anchorPos != -1)
      {
         anchor = base.substring(anchorPos);
         base = base.substring(0, anchorPos);
      }
      
      // add the query param
      if (!base.contains("?"))
         base = base + "?";
      else
         base = base + "&";
      base = base + name + "=" + URL.encodeQueryString(value);
     
      // add the anchor back on
      return base + anchor;
   }
}
