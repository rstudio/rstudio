/*
 * URIUtils.java
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

package org.rstudio.core.client;

import org.rstudio.core.client.dom.DomUtils;

import com.google.gwt.http.client.URL;

public class URIUtils
{
   public static String addQueryParam(String url, String name, String value)
   {
      // first split into base and anchor
      String base = StringUtil.create(url);
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
   
   /**
    * Indicates whether the given URL refers to a resource on a machine-local
    * (loopback) network interface.
    * 
    * @param url The URL to test.
    * @return True if the URL is local.
    */
   public static boolean isLocalUrl(String url)
   {
      // ensure URL is absolute
      String absolute = DomUtils.makeAbsoluteUrl(url);
      
      // extract host and see if it's on the whitelist of loopback hosts
      String host = StringUtil.getHostFromUrl(absolute);
      return host.equals("localhost") || host.equals("127.0.0.1") || host.equals("::1");
   }
}
