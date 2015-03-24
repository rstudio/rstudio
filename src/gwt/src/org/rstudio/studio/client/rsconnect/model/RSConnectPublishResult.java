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
      deployFiles_     = deployFiles; 
      additionalFiles_ = null; 
      ignoredFiles_    = null;
   }

   public RSConnectPublishResult(String appName, 
         RSConnectAccount account, 
         String sourceDir,
         String sourceFile,
         ArrayList<String> deployFiles, 
         ArrayList<String> additionalFiles, 
         ArrayList<String> ignoredFiles)
   {     
      publishType_     = PUBLISH_CODE;
      appName_         = appName; 
      account_         = account; 
      sourceDir_       = sourceDir;
      sourceFile_      = sourceFile;
      deployFiles_     = deployFiles; 
      additionalFiles_ = additionalFiles; 
      ignoredFiles_    = ignoredFiles;
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

   public ArrayList<String> getDeployFiles()
   {
      return deployFiles_;
   }

   public ArrayList<String> getAdditionalFiles()
   {
      return additionalFiles_;
   }

   public ArrayList<String> getIgnoredFiles()
   {
      return ignoredFiles_;
   }
   
   public int getPublishType()
   {
      return publishType_;
   }

   private final String appName_; 
   private final RSConnectAccount account_; 
   private final String sourceDir_;
   private final String sourceFile_;
   private final ArrayList<String> deployFiles_; 
   private final ArrayList<String> additionalFiles_; 
   private final ArrayList<String> ignoredFiles_;
   private final int publishType_;
   
   public final static int PUBLISH_RPUBS = 0;
   public final static int PUBLISH_STATIC = 1;
   public final static int PUBLISH_CODE = 2;
}
