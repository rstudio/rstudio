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

import com.google.gwt.core.client.JavaScriptObject;

public class DataImportOptions extends JavaScriptObject
{
   protected DataImportOptions()
   {
   }
   
   public final static native DataImportOptionsCsv create() /*-{
      return {};
   }-*/;
   
   public final native void setDataName(String dataName) /*-{
      this.dataName = dataName;
   }-*/;
   
   public final native void setImportLocation(String importLocation) /*-{
      this.importLocation = importLocation;
   }-*/;
   
   public final String getImportLocation() {
      JavaScriptObject locaiton = getImportLocationNative();
      return locaiton != null ? new String(locaiton.toString()) : null;
   }
   
   private final native JavaScriptObject getImportLocationNative() /*-{
      return this.importLocation ? this.importLocation : null;
   }-*/;
   
   public final native void setMaxRows(int maxRows) /*-{
      this.maxRows = maxRows > 0 ? maxRows : null;
   }-*/;
   
   public final native void setColumnDefinitions(JavaScriptObject columnDefinitions) /*-{
      this.columnDefinitions = columnDefinitions;
      
      this.columnsOnly = columnDefinitions.find(function (e) {
         return e.assignedType == "only"
      }) ? true : false;
   }-*/;
}
