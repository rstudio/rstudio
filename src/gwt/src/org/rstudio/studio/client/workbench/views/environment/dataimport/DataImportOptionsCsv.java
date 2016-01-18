/*
 * DataImportOptionsCsv.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

package org.rstudio.studio.client.workbench.views.environment.dataimport;

import com.google.gwt.json.client.JSONBoolean;
import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;

public class DataImportOptionsCsv extends DataImportOptions
{
   public DataImportOptionsCsv(String dataName,
                               Character delimiter,
                               String quotes,
                               Boolean escapeBackslash,
                               Boolean escapeDouble,
                               Boolean columnNames,
                               Boolean trimSpaces,
                               String locale,
                               String na,
                               String comments,
                               int skip)
   {
      setDataName(dataName);
      delimiter_ = delimiter;
      quotes_ = quotes;
      escapeBackslash_ = escapeBackslash;
      escapeDouble_ = escapeDouble;
      columnNames_ = columnNames;
      trimSpaces_ = trimSpaces;
      locale_ = locale;
      na_ = na;
      comments_ = comments;
      skip_ = skip;
   }
   
   public Character getDelimiter()
   {
      return delimiter_;
   }
   
   public String getQuotes()
   {
      return quotes_;
   }
   
   public Boolean getEscapeBackslash()
   {
      return escapeBackslash_;
   }
   
   public Boolean getEscapeDouble()
   {
      return escapeDouble_;
   }
   
   public Boolean getColumnNames()
   {
      return columnNames_;
   }
   
   public Boolean getTrimSpaces()
   {
      return trimSpaces_;
   }
   
   public String getLocale()
   {
      return locale_;
   }
   
   public String getNa()
   {
      return na_;
   }
   
   public String getComments()
   {
      return comments_;
   }
   
   public int getSkip()
   {
      return skip_;
   }
   
   @Override
   public JSONObject toJSONObject()
   {
      JSONObject json = super.toJSONObject();
      
      json.put("delimiter", delimiter_ != null ? new JSONString(delimiter_.toString()) : null);
      json.put("quotes", quotes_ != null ? new JSONString(quotes_) : null);
      json.put("escapeBackslash", escapeBackslash_ != null ? JSONBoolean.getInstance(escapeBackslash_) : null);
      json.put("escapeDouble", escapeDouble_ != null ? JSONBoolean.getInstance(escapeDouble_) : null);
      json.put("columnNames", columnNames_ != null ? JSONBoolean.getInstance(columnNames_) : null);
      json.put("trimSpaces", trimSpaces_ != null ? JSONBoolean.getInstance(trimSpaces_) : null);
      json.put("locale", locale_ != null ? new JSONString(locale_) : null);
      json.put("na", na_ != null ? new JSONString(na_) : null);
      json.put("comments", comments_ != null ? new JSONString(comments_) : null);
      json.put("skip", comments_ != null ? new JSONNumber(skip_) : null);
      
      return json;
   }
   
   String name_;
   
   Character delimiter_;
   
   String quotes_;
   
   Boolean escapeBackslash_;
   
   Boolean escapeDouble_;
   
   Boolean columnNames_;
   
   Boolean trimSpaces_;
   
   String locale_;
   
   String na_;
   
   String comments_;
   
   int skip_;
}
