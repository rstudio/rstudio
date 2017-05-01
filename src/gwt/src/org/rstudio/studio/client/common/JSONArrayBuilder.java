/*
 * JSONArrayBuilder.java
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.json.client.JSONArray;
import com.google.gwt.json.client.JSONBoolean;
import com.google.gwt.json.client.JSONNull;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.json.client.JSONValue;

public class JSONArrayBuilder
{
   public JSONArrayBuilder()
   {
      array_ = new JSONArray();
      index_ = 0;
   }
   
   public final JSONArray get()
   {
      return array_;
   }
   
   public final JSONArrayBuilder add(String value)
   {
      append(fromString(value));
      return this;
   }
   
   public final JSONArrayBuilder add(boolean value)
   {
      append(JSONBoolean.getInstance(value));
      return this;
   }
   
   public final JSONArrayBuilder add(int value)
   {
      append(new JSONNumber(value));
      return this;
   }
   
   public final JSONArrayBuilder add(double value)
   {
      append(new JSONNumber(value));
      return this;
   }
   
   public final JSONArrayBuilder add(JsArrayString value)
   {
      JSONArray array = new JSONArray();
      for (int i = 0, n = value.length(); i < n; i++)
         array.set(i, fromString(value.get(i)));
      append(array);
      return this;
   }
   
   private final JSONValue fromString(String value)
   {
      return (value == null)
            ? JSONNull.getInstance()
            : new JSONString(value);
   }
   
   private final void append(JSONValue value)
   {
      array_.set(index_++, value);
   }
   
   private final JSONArray array_;
   private int index_;
}
