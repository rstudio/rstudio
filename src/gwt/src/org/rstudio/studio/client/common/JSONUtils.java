/*
 * JSONUtils.java
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


package org.rstudio.studio.client.common;

import java.util.List;

import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONString;

public class JSONUtils
{
   public static <T extends Number> JSONArray toJSONNumberArray (List<T> list)
   {
      JSONArray json = new JSONArray();
      for (int i = 0; i < list.size(); i++)
      {
         json.set(i, new JSONNumber(list.get(i).intValue()));
      }
      return json;
   }
   
   public static <T> JSONArray toJSONStringArray(List<T> list)
   {
      JSONArray json = new JSONArray();
      for (int i = 0; i < list.size(); i++)
      {
         json.set(i, new JSONString((String)list.get(i)));
      }
      return json;
   }
}
