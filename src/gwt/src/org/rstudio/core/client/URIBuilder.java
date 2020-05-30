/*
 * URIBuilder.java
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

public class URIBuilder
{
   private URIBuilder(String url)
   {
      url_ = url;
   }
   
   public static URIBuilder fromUrl(String url)
   {
      return new URIBuilder(url);
   }
   
   public URIBuilder queryParam(String key, String val)
   {
      url_ = URIUtils.addQueryParam(url_, key, val);
      return this;
   }
   
   public String get()
   {
      return url_;
   }

   private String url_;
}
