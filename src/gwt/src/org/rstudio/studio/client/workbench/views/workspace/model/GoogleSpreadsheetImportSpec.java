/*
 * GoogleSpreadsheetImportSpec.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.workspace.model;


import com.google.gwt.core.client.JavaScriptObject;

public class GoogleSpreadsheetImportSpec extends JavaScriptObject
{
   protected GoogleSpreadsheetImportSpec()
   {
   }
   
   public final static native GoogleSpreadsheetImportSpec create(
                                             String resourceId,
                                             String objectName) /*-{
      var importSpec = new Object();
      importSpec.resourceId = resourceId;
      importSpec.objectName = objectName;
      return importSpec;                                            
   }-*/;
  
   public final native String getResourceId() /*-{
      return this.resourceId;
   }-*/;
   
   public final native String getObjectName() /*-{
      return this.objectName;
   }-*/;
   
   
 
  
}
