/*
 * GoogleDocInfo.java
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
package org.rstudio.studio.client.workbench.views.source.model;

import com.google.gwt.core.client.JavaScriptObject;

import java.util.Date;

public class GoogleDocInfo extends JavaScriptObject
{
   protected GoogleDocInfo()
   {
   }

   public static final native GoogleDocInfo create(String title,
                                                   String resourceId,
                                                   String updated,
                                                   String url) /*-{
      var googleDocInfo = new Object();
      googleDocInfo.gdocTitle = title ;
      googleDocInfo.gdocResourceId = resourceId ;
      googleDocInfo.gdocUpdated = updated;
      googleDocInfo.gdocUrl = url;
      return googleDocInfo ;
   }-*/;
   
   
   public native final String getTitle() /*-{
      return this.gdocTitle;
   }-*/;
   
   public native final String getResourceId() /*-{
      return this.gdocResourceId;
   }-*/;
   
   public final Date getUpdated() 
   { 
      double updated = Double.parseDouble(getUpdatedNative());
      Double updatedDate = new Double(updated);
      return new Date(updatedDate.longValue());
   }
   
   public native final String getURL() /*-{
      return this.gdocUrl;
   }-*/;
   
   private final native String getUpdatedNative() /*-{
      return this.gdocUpdated;
   }-*/;
   
}
