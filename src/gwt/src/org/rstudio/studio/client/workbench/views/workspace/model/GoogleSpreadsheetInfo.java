/*
 * GoogleSpreadsheetInfo.java
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

import java.util.Date;

public class GoogleSpreadsheetInfo extends JavaScriptObject
{
   protected GoogleSpreadsheetInfo()
   {
   }
  
   public final native String getTitle() /*-{
      return this.title;
   }-*/;

   public final native String getResourceId() /*-{
      return this.resourceId;
   }-*/;
   
   public final Date getUpdated() 
   { 
      // R POSIXct class is represented as seconds since epoch,
      // so multiple the underlying value by 1000 
      Double updated = new Double(getUpdatedNative() * 1000);
      return new Date(updated.longValue());
   }
   
   private final native double getUpdatedNative() /*-{
      return this.updated;
   }-*/;

  
}
