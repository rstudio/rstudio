/*
 * RSConnectPublishResult.java
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

public class RSConnectPublishResult
{
   public RSConnectPublishResult(RSConnectPublishSource source)
   {
      ArrayList<String> deployFiles = new ArrayList<String>();
      deployFiles.add(source.getDeployFile());
      publishType_ = PUBLISH_RPUBS;
      appName_     = ""; 
      appTitle_    = "";
      appId_       = "";
      account_     = null; 
      source_      = source;
      settings_    = new RSConnectPublishSettings(deployFiles, null, null, false,
         true);
      isUpdate_    = false;
   }

   public RSConnectPublishResult(String appName, 
         String appTitle,
         String appId,
         RSConnectAccount account, 
         RSConnectPublishSource source,
         RSConnectPublishSettings settings,
         boolean isUpdate)
   {     
      this(settings.getAsStatic() ? 
               PUBLISH_STATIC : PUBLISH_CODE, 
           appName, appTitle, appId, account, source, settings,
           isUpdate);
   }
   
   private RSConnectPublishResult(int publishType, 
         String appName, 
         String appTitle,
         String appId,
         RSConnectAccount account, 
         RSConnectPublishSource source,
         RSConnectPublishSettings settings,
         boolean isUpdate)
   {
      publishType_ = publishType;
      appName_     = appName; 
      appTitle_    = appTitle; 
      appId_       = appId;
      account_     = account; 
      source_      = source;
      settings_    = settings;
      isUpdate_    = isUpdate;
   }
   
   public String getAppName()
   {
      return appName_;
   }
   
   public String getAppTitle()
   {
      return appTitle_;
   }
   
   public String getAppId()
   {
      return appId_;
   }

   public RSConnectAccount getAccount()
   {
      return account_;
   }

   public int getPublishType()
   {
      return publishType_;
   }
   
   public RSConnectPublishSettings getSettings()
   {
      return settings_;
   }
   
   public RSConnectPublishSource getSource()
   {
      return source_;
   }
   
   public boolean isUpdate()
   {
      return isUpdate_;
   }

   private final String appName_; 
   private final String appTitle_; 
   private final String appId_;
   private final RSConnectAccount account_; 
   private final int publishType_;
   private final RSConnectPublishSettings settings_;
   private final RSConnectPublishSource source_;
   private final boolean isUpdate_;
   
   public final static int PUBLISH_RPUBS  = 0;
   public final static int PUBLISH_STATIC = 1;
   public final static int PUBLISH_CODE   = 2;
}
