/*
 * RnwChunkOptions.java
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
package org.rstudio.studio.client.workbench.views.source.model;

import java.util.ArrayList;
import java.util.Collections;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;

public class RnwChunkOptions extends JavaScriptObject
{   
   protected RnwChunkOptions()
   {
   }
   
   public final ArrayList<String> getOptions() 
   {
      ArrayList<String> options = new ArrayList<String>(
                                                new JSONObject(this).keySet());
      Collections.sort(options);
      return options;
   }
   
   // types: "numeric", "character", "logical", "list"
   public final String getOptionType(String name)
   {
      JSONArray arr = new JSONArray(getOptionTypeNative(name));;
      if (arr.size() == 1)
         return arr.get(0).isString().stringValue();
      else
         return "list";
   }
   
   public final ArrayList<String> getOptionValues(String name)
   {
      JSONArray arr = new JSONArray(getOptionTypeNative(name));
      ArrayList<String> values = new ArrayList<String>();
      for (int i=0; i<arr.size(); i++)
         values.add(arr.get(i).isArray().get(0).isString().stringValue());
      return values;
   }
   
   private native final JavaScriptObject getOptionTypeNative(String name) /*-{
      return this[name];
   }-*/;
   
   
}
