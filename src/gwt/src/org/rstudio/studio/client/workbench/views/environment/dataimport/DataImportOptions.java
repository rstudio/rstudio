/*
 * DataImportOptions.java
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

import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.json.client.JSONString;

public class DataImportOptions
{
   private String dataName_;
   private String importLocation_;
   private String code_;
   private int maxRows_;
   
   public void setDataName(String dataName)
   {
      dataName_ = dataName;
   }
   
   public String getDataName()
   {
      return dataName_;
   }
   
   public void setImportLocation(String importLocation)
   {
      importLocation_ = importLocation;
   }
   
   public String getImportLocation()
   {
      return importLocation_;
   }
   
   public String getCode()
   {
      return code_;
   }
   
   public int getMaxRows()
   {
      return maxRows_;
   }
   
   public void setCode(String code)
   {
      code_ = code;
   }
   
   public void setMaxRows(int maxRows)
   {
      maxRows_ = maxRows;
   }
   
   public JSONObject toJSONObject()
   {
      JSONObject json = new JSONObject();
      
      json.put("importLocation", importLocation_ != null ? new JSONString(importLocation_) : null);
      json.put("dataName", dataName_ != null ? new JSONString(dataName_) : null);
      json.put("maxRows", maxRows_ > 0 ? new JSONNumber(maxRows_) : null);
      
      return json;
   }
}
