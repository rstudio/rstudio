/*
 * RSConnectDeploymentRecord.java
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
package org.rstudio.studio.client.rsconnect.model;

import java.util.ArrayList;

import org.rstudio.core.client.JsArrayUtil;
import org.rstudio.core.client.StringUtil;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayString;

public class RSConnectDeploymentRecord extends JavaScriptObject 
{
   protected RSConnectDeploymentRecord()
   {
   }
   
   public static final native RSConnectDeploymentRecord create(
         String name, 
         String title,
         String id,
         RSConnectAccount account, 
         String url) /*-{
      return {
         'name': name,
         'title': title,
         'appId': id,
         'account': account.name,
         'server': account.server,
         'url': url
         };
   }-*/;
   
   public static final native RSConnectDeploymentRecord create(
         String name, 
         String title,
         String id,
         String account,
         String server) /*-{
      return {
         'name': name,
         'title': title,
         'appId': id,
         'account': account,
         'server': server,
         };
   }-*/;
   
   public final native String getName() /*-{
      return this.name;
   }-*/;
   
   public final native String getTitle() /*-{
      return this.title;
   }-*/;

   public final native String getAccountName() /*-{
      return this.account;
   }-*/;

   public final native String getServer() /*-{
      return this.server;
   }-*/;

   public final native String getAppId() /*-{
      return this.appId;
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
   
   public final native String getUsername() /*-{
      return this.username;
   }-*/;
   
   public final native String getHostUrl() /*-{
      return this.hostUrl;
   }-*/;
   
   public final native boolean isServerRegistered() /*-{
      return this.serverRegistered;
   }-*/;
   
   public final RSConnectAccount getAccount()
   {
      return RSConnectAccount.create(getAccountName(), getServer());
   }
   
   public final ArrayList<String> getAdditionalFiles() 
   {
      return JsArrayUtil.fromJsArrayString(getFileList("additionalFiles"));
   }
   
   public final ArrayList<String> getIgnoredFiles() 
   {
      return JsArrayUtil.fromJsArrayString(getFileList("ignoredFiles"));
   }
   
   public final String getDisplayName()
   {
      if (StringUtil.isNullOrEmpty(getTitle()))
         return getName();
      return getTitle();
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
