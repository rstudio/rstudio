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

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.studio.client.server.ServerRequestCallback;

import java.util.ArrayList;
import java.util.Collections;

public class RnwChunkOptions extends JavaScriptObject
{
   public interface AsyncProvider
   {
      void getChunkOptions(
                        ServerRequestCallback<RnwChunkOptions> requestCallback);
   }

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
   
   // types: "numeric", "character", "logical", "list", or null for unknown
   public final String getOptionType(String name)
   {
      if (!hasOption(name))
         return null;

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

   private native final boolean hasOption(String name) /*-{
      return typeof(this[name]) != 'undefined';
   }-*/;

   public static class RnwOptionCompletionResult
   {
      public String token;
      public JsArrayString completions;
   }

   public final RnwOptionCompletionResult getCompletions(String line, int pos)
   {
      assert line.startsWith("<<");
      String linePart = line.substring(2, pos);

      // This can be pretty simple because Noweb doesn't allow = or , to appear
      // in names or values (i.e. no quotes or escaping to make parsing more
      // complicated).

      String token = null;
      JsArrayString completions = JsArrayString.createArray().cast();
      Match match = Pattern.create("([=,])?\\s*([^=,]*)$").match(linePart,
                                                                 0);
      if (match != null)
      {
         boolean isValue = match.hasGroup(1) && match.getGroup(1).equals("=");
         token = match.getGroup(2);
         if (!isValue)
         {
            for (String option : this.getOptions())
            {
               if (option.startsWith(token))
                  completions.push(option + "=");
            }
         }
         else
         {
            ArrayList<String> names = new ArrayList<String>();
            ArrayList<String> values = new ArrayList<String>();
            parseRnwChunkHeader(linePart, names, values);

            assert names.size() == values.size();

            String name = names.size() == 0
                          ? null : names.get(names.size()-1);
            String value = values.size() == 0
                           ? null : values.get(values.size()-1);

            if (value != null)
            {
               // If value is not null, we follow an equal sign; try to complete
               // based on value.

               String optionType = StringUtil.notNull(this.getOptionType(
                     name));
               if (optionType.equals("logical"))
               {
                  ArrayList<String> logicals = new ArrayList<String>();
                  logicals.add("FALSE");
                  logicals.add("TRUE");
                  if (value.length() > 0)
                  {
                     logicals.add("true");
                     logicals.add("false");
                     if (value.length() > 1)
                     {
                        logicals.add("True");
                        logicals.add("False");
                     }
                  }

                  for (String logical : logicals)
                     if (logical.startsWith(value))
                        completions.push(logical);
               }
               else if (optionType.equals("list"))
               {
                  for (String optionVal : this.getOptionValues(name))
                     if (optionVal.startsWith(value))
                        completions.push(optionVal);
               }
            }
            else if (name != null)
            {
               for (String optionName : this.getOptions())
                  if (optionName.startsWith(name))
                     completions.push(optionName + "=");
            }
         }
      }

      RnwOptionCompletionResult result = new RnwOptionCompletionResult();
      result.token = token;
      result.completions = completions;
      return result;
   }

   private static void parseRnwChunkHeader(String line,
                                           ArrayList<String> names,
                                           ArrayList<String> values)
   {
      if (line.trim().length() == 0)
         return;

      String[] chunks = line.split(",");
      for (String chunk : chunks)
      {
         if (chunk.indexOf('=') < 0)
         {
            names.add(chunk.trim());
            values.add(null);
         }
         else
         {
            String[] subChunks = chunk.split("=", 2);
            names.add(subChunks[0].trim());
            values.add(subChunks.length > 1 ? StringUtil.trimLeft(subChunks[1])
                                            : "");
         }
      }
   }
}
