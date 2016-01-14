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

public class DataImportOptionsCsv extends DataImportOptions
{
   public DataImportOptionsCsv(String dataName,
                               Character delimiter,
                               String quotes,
                               Boolean escapeBackslash,
                               Boolean escapeDouble,
                               Boolean columnNames,
                               Boolean trimSpaces)
   {
      setDataName(dataName);
      delimiter_ = delimiter;
      quotes_ = quotes;
      escapeBackslash_ = escapeBackslash;
      escapeDouble_ = escapeDouble;
      columnNames_ = columnNames;
      trimSpaces_ = trimSpaces;
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
   
   String name_;
   
   Character delimiter_;
   
   String quotes_;
   
   Boolean escapeBackslash_;
   
   Boolean escapeDouble_;
   
   Boolean columnNames_;
   
   Boolean trimSpaces_;
}
