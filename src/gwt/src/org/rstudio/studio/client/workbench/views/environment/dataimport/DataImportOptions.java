/*
 * DataImportOptions.java
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

package org.rstudio.studio.client.workbench.views.environment.dataimport;

import org.rstudio.studio.client.workbench.views.environment.dataimport.model.DataImportPreviewResponse;

import com.google.gwt.core.client.JavaScriptObject;

public class DataImportOptions extends JavaScriptObject
{
   protected DataImportOptions()
   {
   }
   
   public static native DataImportOptions create() /*-{
      return {};
   }-*/;
   
   public final native void setDataName(String dataName) /*-{
      this.dataName = dataName;
   }-*/;
   
   public final native void setImportLocation(String importLocation) /*-{
      this.importLocation = importLocation;
   }-*/;
   
   public final String getImportLocation() {
      JavaScriptObject location = getImportLocationNative();
      return location != null ? new String(location.toString()) : null;
   }
   
   public final native void setOptions(JavaScriptObject options) /*-{
      var this_ = this;
      this_.importLocation = options.importLocation;
      this_.mode = options.mode;
      
      if (!this_.columnDefinitions) {
         this_.columnDefinitions = {};
      }
      
      if (options.columnDefinitions) {
         Object.keys(options.columnDefinitions).forEach(function(key) {
            var e = options.columnDefinitions[key];
            if (!this_.columnDefinitions) {
               this_.columnDefinitions = {};
            }
            this_.columnDefinitions[key] = e;
         });
      
         this.columnsOnly = Object.keys(options.columnDefinitions).some(function(key) {
            return options.columnDefinitions[key].only;
         });
      }
   }-*/;
   
   private final native JavaScriptObject getImportLocationNative() /*-{
      return this.importLocation ? this.importLocation : null;
   }-*/;
   
   public final native void setMaxRows(int maxRows) /*-{
      this.maxRows = maxRows > 0 ? maxRows : null;
   }-*/;
   
   public final native JavaScriptObject getColumnDefinitions() /*-{
      return this.columnDefinitions;
   }-*/;
   
   public final native void resetColumnDefinitions() /*-{
      this.columnDefinitions = null;
   }-*/;
   
   public final void setColumnType(String name, String assignedType) {
      setColumnDefinition(name, assignedType, null);
   }
   
   public final native void setColumnDefinition(
      String name, String assignedType, String parseString) /*-{
      if (!this.columnDefinitions) {
         this.columnDefinitions = {};
      };
      if (!this.columnDefinitions[name]) {
         this.columnDefinitions[name] = {};
      };
      
      this.columnDefinitions[name].assignedType = assignedType;
      this.columnDefinitions[name].name = name;
      this.columnDefinitions[name].parseString = parseString;
   }-*/;
   
   public final native String getColumnType(String name) /*-{
      if (!this.columnDefinitions || !this.columnDefinitions[name]) {
         return null;
      };
      
      return this.columnDefinitions[name].assignedType;
   }-*/;
   
   public final native void setOnlyColumn(String name, boolean only) /*-{
      if (!this.columnDefinitions) {
         this.columnDefinitions = {};
      };
      if (!this.columnDefinitions[name]) {
         this.columnDefinitions[name] = {};
      };
      
      this.columnDefinitions[name].only = only ? only : null;
      this.columnDefinitions[name].name = name;
   }-*/;
   
   public final native boolean getColumnOnly(String name) /*-{
      if (!this.columnDefinitions || !this.columnDefinitions[name])
         return false;
         
      return this.columnDefinitions[name].only;
   }-*/;
   
   public final native void setBaseColumnDefinitions(DataImportPreviewResponse response) /*-{
      var this_ = this;
      if (!response.columns) {
         return;
      }
      
      if (!this_.columnDefinitions) {
         this_.columnDefinitions = {};
      }
      
      Object.keys(response.columns).forEach(function(key, index) {
         if (!this_.columnDefinitions[response.columns[key].col_name]) {
            this_.columnDefinitions[response.columns[key].col_name] = {
               index: index,
               name: response.columns[key].col_name,
               assignedType: null,
               rType: response.columns[key].col_type_r
            };
         }
      });
   }-*/;
   
   public final native void setLocalFiles(JavaScriptObject localFiles) /*-{
      this.localFiles = localFiles;
   }-*/;
}
