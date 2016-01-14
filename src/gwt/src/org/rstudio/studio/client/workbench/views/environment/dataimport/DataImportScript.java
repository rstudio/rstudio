/*
 * DataImportScript.java
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

import org.rstudio.core.client.StringUtil;

public class DataImportScript
{
   public DataImportScript()
   {
      
   }
   
   public String getImportScript(DataImportModes mode, DataImportOptions options)
   {
      if (options.getDataName().isEmpty())
      {
         options.setDataName("dataset");
      }
      
      switch (mode)
      {
      case Csv:
         return getImportScriptCsv((DataImportOptionsCsv)options);
      default:
         return "";
      }
   }
   
   public String getImportScriptCsv(DataImportOptionsCsv options)
   {
      String var = StringUtil.toRSymbolName(options.getDataName());
      String code =
            var +
            " <- readr::read_delim(" + 
                  "\"" + options.getImportLocation() + "\", " + 
                  "\"" + options.getDelimiter() + "\", " +
                  "escape_backslash=" + (options.getEscapeBackslash() ? "TRUE" : "FALSE") + "," +
                  "escape_double=" + (options.getEscapeDouble() ? "TRUE" : "FALSE") + "," +
                  "col_names=" + (options.getColumnNames() ? "TRUE" : "FALSE") + "," +
                  ")" + "\n" +
            "View(" + var + ")";
      
      return code;
   }
}
