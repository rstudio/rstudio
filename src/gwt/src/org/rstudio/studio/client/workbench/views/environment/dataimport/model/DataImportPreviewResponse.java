/*
 * DataImportPreviewResponse.java
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

package org.rstudio.studio.client.workbench.views.environment.dataimport.model;

import com.google.gwt.core.client.JavaScriptObject;

public class DataImportPreviewResponse extends JavaScriptObject
{
   protected DataImportPreviewResponse()
   {
   }
   
   public final native String getErrorMessage() /*-{
      return this.error ? this.error.message.join(' ') : null;
   }-*/;
   
   public final native String assignColumnTypes(JavaScriptObject columnDefinitions) /*-{
      var columnsRef = this.columns;
      if (columnDefinitions && columnsRef) {
         columnDefinitions.forEach(function(def) {
            var col = columnsRef.find(function(e) {
               return def.name === e.col_name; 
            });
            
            if (col && def.originalType) {
               col.col_type_original = def.originalType;
               col.col_type_assigned = def.assignedType;
            }
         });
      }
   }-*/;
}
