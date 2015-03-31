/*
 * RSConnectPublishResult.java
 *
 * Copyright (C) 2009-15 by RStudio, Inc.
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
   public RSConnectPublishResult()
   {
      // TODO: remove this
      this(null, null, null);
   }

   public RSConnectPublishResult(String sourceDir,
         String sourceFile, 
         String rpubsHtmlFile)
   {
      ArrayList<String> deployFiles = new ArrayList<String>();
      deployFiles.add(rpubsHtmlFile);
      publishType_     = PUBLISH_RPUBS;
      appName_         = ""; 
      account_         = null; 
      sourceDir_       = sourceDir;
      sourceFile_      = sourceFile;
      settings_ = new RSConnectPublishSettings(deployFiles, null, null, false,
         true);
   }

   public RSConnectPublishResult(String appName, 
         RSConnectAccount account, 
         String sourceDir,
         String sourceFile,
         RSConnectPublishSettings settings)
   {     
      this(settings.getAsStatic() ? 
               PUBLISH_STATIC : PUBLISH_CODE, 
           appName, account, sourceDir, sourceFile, settings);
   }
   
   private RSConnectPublishResult(int publishType, 
         String appName, 
         RSConnectAccount account, 
         String sourceDir,
         String sourceFile,
         RSConnectPublishSettings settings)
   {
      publishType_ = publishType;
      appName_     = appName; 
      account_     = account; 
      sourceDir_   = sourceDir;
      sourceFile_  = sourceFile;
      settings_    = settings;
   }
   
   public String getSourceDir()
   {
      return sourceDir_;
   }

   public String getAppName()
   {
      return appName_;
   }

   public RSConnectAccount getAccount()
   {
      return account_;
   }

   public String getSourceFile()
   {
      return sourceFile_;
   }

   public int getPublishType()
   {
      return publishType_;
   }
   
   public RSConnectPublishSettings getSettings()
   {
      return settings_;
   }

   private final String appName_; 
   private final RSConnectAccount account_; 
   private final String sourceDir_;
   private final String sourceFile_;
   private final int publishType_;
   private final RSConnectPublishSettings settings_;
   
   public final static int PUBLISH_RPUBS = 0;
   public final static int PUBLISH_STATIC = 1;
   public final static int PUBLISH_CODE = 2;
}
