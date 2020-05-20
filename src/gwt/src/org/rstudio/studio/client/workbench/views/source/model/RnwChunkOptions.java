/*
 * RnwChunkOptions.java
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
package org.rstudio.studio.client.workbench.views.source.model;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;
import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.common.r.RToken;
import org.rstudio.studio.client.common.r.RTokenizer;
import org.rstudio.studio.client.common.rnw.RnwWeave;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Stack;

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
   
   // types: "numeric", "character", "logical", "list", or null for unknown
   public final String getOptionType(String name)
   {
      if (!hasOption(name))
         return null;

      JSONArray arr = new JSONArray(getOptionTypeNative(name));
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
      {
         JSONArray array = arr.get(i).isArray();
         if (array == null)
            break;

         if (array.size() == 0)
            break;

         JSONString string = array.get(0).isString();
         if (string == null)
            break;

         values.add(string.stringValue());
      }
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

   public final RnwOptionCompletionResult getCompletions(String line,
                                                         int optionsStartOffset,
                                                         int cursorPos,
                                                         RnwWeave rnwWeave)
   {
      assert cursorPos >= optionsStartOffset :
            "cursorPos was less than optionsStartOffset";

      String linePart = line.substring(optionsStartOffset, cursorPos);

      // This can be pretty simple because Noweb doesn't allow = or , to appear
      // in names or values (i.e. no quotes or escaping to make parsing more
      // complicated).

      String token = null;
      JsArrayString completions = JsArrayString.createArray().cast();
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
         token = value;
         // If value is not null, we follow an equal sign; try to complete
         // based on value.
         completeValue(rnwWeave, name, value, completions);
      }
      else if (name != null)
      {
         token = name;
         for (String optionName : this.getOptions())
            if (optionName.startsWith(name))
               completions.push(optionName + "=");
      }

      RnwOptionCompletionResult result = new RnwOptionCompletionResult();
      result.token = token;
      result.completions = completions;
      return result;
   }

   private void completeValue(RnwWeave rnwWeave,
                              String name,
                              String value,
                              JsArrayString completions)
   {
      String optionType = StringUtil.notNull(this.getOptionType(name));
      if (optionType.equals("logical"))
      {
         CompletionOptions options = new CompletionOptions();
         options.addOption("TRUE", 0);
         options.addOption("FALSE", 0);
         if (!rnwWeave.usesCodeForOptions())
         {
            // Legacy Sweave is case insensitive
            options.addOption("true", 1);
            options.addOption("false", 1);
            options.addOption("True", 2);
            options.addOption("False", 2);
         }
         for (String logical : options.getCompletions(value))
            completions.push(logical);
      }
      else if (optionType.equals("list"))
      {
         CompletionOptions options = new CompletionOptions();
         ArrayList<String> optionValues = this.getOptionValues(name);
         if (!rnwWeave.usesCodeForOptions())
         {
            // Legacy Sweave
            for (String optionVal : optionValues)
               options.addOption(optionVal, 0);
         }
         else
         {
            for (String optionVal : optionValues)
               options.addOption("'" + optionVal + "'", 0);
            for (String optionVal : optionValues)
               options.addOption('"' + optionVal + '"', 1);
         }

         for (String option : options.getCompletions(value))
            completions.push(option);
      }
   }

   private static void parseRnwChunkHeader(String line,
                                           ArrayList<String> names,
                                           ArrayList<String> values)
   {
      String currentName = null;
      String currentValue = null;

      int currentPartBegin = 0;
      Stack<Integer> braceStack = new Stack<Integer>();

      RTokenizer tokenizer = new RTokenizer(line);
      for (RToken token; null != (token = tokenizer.nextToken()); )
      {
         switch (token.getTokenType())
         {
            case RToken.OPER:
               if (token.getContent() == "=" &&
                   currentName == null &&
                   braceStack.empty())
               {
                  String part = line.substring(currentPartBegin,
                                               token.getOffset());
                  currentName = part;
                  currentPartBegin = token.getOffset() + token.getLength();
               }
               break;
            case RToken.COMMA:
               if (braceStack.empty())
               {
                  String part = line.substring(currentPartBegin,
                                               token.getOffset());
                  if (currentName == null)
                     currentName = part;
                  else
                     currentValue = part;

                  names.add(currentName.trim());
                  values.add(currentValue != null ?
                             StringUtil.trimLeft(currentValue) :
                             null);
                  currentName = null;
                  currentValue = null;

                  currentPartBegin = token.getOffset() + token.getLength();
               }
               break;

            case RToken.LBRACE:     braceStack.push(RToken.RBRACE);    break;
            case RToken.LBRACKET:   braceStack.push(RToken.RBRACKET);  break;
            case RToken.LDBRACKET:  braceStack.push(RToken.RDBRACKET); break;
            case RToken.LPAREN:     braceStack.push(RToken.RPAREN);    break;

            case RToken.RBRACE:
            case RToken.RBRACKET:
            case RToken.RDBRACKET:
            case RToken.RPAREN:
               int distance = braceStack.search(token.getTokenType());
               if (distance > 0)
               {
                  for (int i = 0; i < distance; i++)
                     braceStack.pop();
               }
               break;
         }
      }

      String part = line.substring(currentPartBegin,
                                   line.length());
      if (currentName == null)
         currentName = part;
      else
         currentValue = part;

      if (currentValue == null)
      {
         names.add(StringUtil.trimLeft(currentName));
         values.add(null);
      }
      else
      {
         names.add(currentName.trim());
         values.add(StringUtil.trimLeft(currentValue));
      }
   }
}
