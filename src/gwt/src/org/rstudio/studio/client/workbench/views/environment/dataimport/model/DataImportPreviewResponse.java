/*
 * DataImportPreviewResponse.java
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

package org.rstudio.studio.client.workbench.views.environment.dataimport.model;

import com.google.gwt.core.client.JavaScriptObject;

public class DataImportPreviewResponse extends JavaScriptObject
{
   protected DataImportPreviewResponse()
   {
   }
   
   public final native String getErrorMessage() /*-{
      return (this.error && this.error.message) ? this.error.message.join(' ') : null;
   }-*/;
   
   public final native int getParsingErrors() /*-{
      return this.parsingErrors;
   }-*/;
   
   public final native void setColumnDefinitions(DataImportPreviewResponse response) /*-{
      if (response) {
         return this.columns = response.columns;
      }
   }-*/;
   
   public final native boolean supportsColumnOperations() /*-{
      return this.supportsColumnOperations == true;
   }-*/;
   
   public final native String[] getSupportedColumnTypes() /*-{
      return this.options && this.options.columnTypes ? this.options.columnTypes : [];
   }-*/;
   
   public final native JavaScriptObject getLocalFiles() /*-{
      return this.localFiles;
   }-*/;
}
