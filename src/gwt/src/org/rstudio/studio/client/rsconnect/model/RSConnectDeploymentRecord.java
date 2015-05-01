/*
 * RSConnectDeploymentRecord.java
 *
 * Copyright (C) 2009-14 by RStudio, Inc.
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
package org.rstudio.studio.client.rsconnect.model;

import java.util.ArrayList;

import org.rstudio.core.client.JsArrayUtil;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;

public class RSConnectDeploymentRecord extends JavaScriptObject 
{
   protected RSConnectDeploymentRecord()
   {
   }
   
   public static final native RSConnectDeploymentRecord create(
         String name, 
         RSConnectAccount account, 
         String url) /*-{
      return {
         'name': name,
         'account': account.name,
         'server': account.server,
         'url': url
         };
   }-*/;
   
   public final native String getName() /*-{
      return this.name;
   }-*/;

   public final native String getAccountName() /*-{
      return this.account;
   }-*/;

   public final native String getServer() /*-{
      return this.server;
   }-*/;

   public final native String getBundleId() /*-{
      return this.bundleId;
   }-*/;

   public final native double getWhen() /*-{
      return this.when || 0;
   }-*/;

   public final native String getUrl() /*-{
      return this.url;
   }-*/;
   
   public final RSConnectAccount getAccount()
   {
      return RSConnectAccount.create(getAccountName(), getServer());
   };
   
   public final ArrayList<String> getAdditionalFiles() 
   {
      return JsArrayUtil.fromJsArrayString(getFileList("additionalFiles"));
   }
   
   public final ArrayList<String> getIgnoredFiles() 
   {
      return JsArrayUtil.fromJsArrayString(getFileList("ignoredFiles"));
   }
   
   public final native boolean getAsMultiple() /*-{
      if (typeof this.asMultiple === "undefined") 
         return false;
      else
         return this.asMultiple === "TRUE" ? true : false;
   }-*/;
   
   public final native boolean getAsStatic() /*-{
      if (typeof this.asStatic === "undefined") 
         return false;
      else
         return this.asStatic === "TRUE" ? true : false;
   }-*/;
   
   private final native JsArrayString getFileList(String name) /*-{
      if (typeof this[name] === "undefined" || this[name] === null) 
         return [];
      else
         return this[name].split("|");
   }-*/;
}
