/*
 * ApplicationUtils.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

package org.rstudio.studio.client.application;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.regex.Pattern;

import com.google.gwt.core.client.GWT;
import com.google.gwt.http.client.URL;
import com.google.gwt.user.client.Window;


public class ApplicationUtils
{
   public static String getHostPageBaseURLWithoutContext(boolean includeSlash)
   {
      String replaceWith = includeSlash ? "/" : "";
      String url = GWT.getHostPageBaseURL();
      Pattern pattern = Pattern.create("/s/[A-Fa-f0-9]{5}[A-Fa-f0-9]{8}[A-Fa-f0-9]{8}/");
      return pattern.replaceAll(url, replaceWith);
   }
   
   /**
    * Compares two version strings, which may be in these formats:
    *   * MAJOR.MINOR.PATCH - e.g. 3.6.0
    *   * YEAR.MONTH.PATCH+COMMITS - e.g. 2023.06.0+421
    *   * MAJOR.MINOR.PATCH-NUM - e.g. 1.4.1743-4
    * @example compareVersions("2023.06.0+421", "2023.06.1+524") returns < 0
    *          compareVersions("2023.06.1+524", "2023.06.1+524") returns 0
    *          compareVersions("2023.06.2+999", "2023.06.1+524") returns > 0
    * @param version1 The first version string to compare
    * @param version2 The second version string to compare
    * @return < 0 if version1 is earlier than version 2
    *         0 if version1 and version2 are the same
    *         > 0 if version1 is later than version 2
    */
   public static int compareVersions(String version1, String version2)
   {
      String versionRegex = "\\.|\\+|\\-";
      String[] v1parts = version1.split(versionRegex); // example: ["2023", "06", "0", "421"]
      String[] v2parts = version2.split(versionRegex); // example: ["2023", "06", "1", "524"]
      int numParts = Math.min(v1parts.length, v2parts.length);
      for (int i = 0; i < numParts; i++)
      {
         int result = Integer.parseInt(v1parts[i]) - 
                      Integer.parseInt(v2parts[i]);
         if (result != 0)
            return result;
      }
      return 0;
   }
   
   public static String getRemainingQueryString(List<String> removeKeys)
   {  
      Map<String, List<String>> params = Window.Location.getParameterMap();
      StringBuilder queryString =  new StringBuilder();
      String prefix = "";
      for (Map.Entry<String, List<String>> entry : params.entrySet()) {
         
         // skip keys we are supposed to remove
         if (removeKeys.contains(entry.getKey()))
            continue;
         
         for (String val : entry.getValue()) {
          queryString.append(prefix)
              .append(URL.encodeQueryString(entry.getKey()))
              .append('=');
          if (val != null) {
            queryString.append(URL.encodeQueryString(val));
          }
          prefix = "&";
        }
      }
     
      // return string
      return queryString.toString();
   }
   
   public static void removeQueryParam(String param)
   {
      ArrayList<String> params = new ArrayList<>();
      params.add(param);
      removeQueryParams(params);
   }
   
   public static void removeQueryParams(List<String> removeKeys)
   {
      // determine the new URL
      String url = GWT.getHostPageBaseURL();
      String queryString = getRemainingQueryString(removeKeys);
      if (queryString.length() > 0)
         url = url + "?" + queryString;
      
      // replace history state (try/catch in case the browser doesn't
      // support this or has it disabled)
      try
      {
         WindowEx.get().replaceHistoryState(url);
      }
      catch(Exception e)
      {
         Debug.logException(e);
      }
   }
}
