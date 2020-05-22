/*
 * DataImportAssembleResponse.java
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

public class DataImportAssembleResponse extends JavaScriptObject
{
   protected DataImportAssembleResponse()
   {
   }
   
   public final native String getErrorMessage() /*-{
      return this.error ? this.error.message.join(' ') : null;
   }-*/;
   
   public final native String getPreviewCode() /*-{
      return this.previewCode ? this.previewCode.join(' ') : null;
   }-*/;
   
   public final native String getImportCode() /*-{
      return this.importCode ? this.importCode.join(' ') : null;
   }-*/;
   
   public final native String getDataName() /*-{
      return this.dataName ? this.dataName.join(' ') : null;
   }-*/;
   
   public final native JavaScriptObject getLocalFiles() /*-{
      return this.localFiles;
   }-*/;

   public final native String getPackageVersion() /*-{
      return this.packageVersion ? this.packageVersion.join(' ') : null;
   }-*/;
}
