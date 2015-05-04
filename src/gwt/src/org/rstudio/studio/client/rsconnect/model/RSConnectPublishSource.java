/*
 * RSConnectPublishSource.java
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

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.files.FileSystemItem;

public class RSConnectPublishSource
{
   public RSConnectPublishSource(String sourceDir)
   {
      sourceFile_ = sourceDir;
      deployFile_ = sourceDir;
      isSelfContained_ = false;
      description_ = null;
      deployDir_ = sourceDir;
   }

   public RSConnectPublishSource(String sourceFile, boolean isSelfContained, 
         String description)
   {
      this(sourceFile, sourceFile, isSelfContained, description);
   }
   
   public RSConnectPublishSource(String sourceFile, String outputFile, 
         boolean isSelfContained, String description)
   {
      deployFile_ = outputFile;
      sourceFile_ = sourceFile;
      description_ = description;
      isSelfContained_ = isSelfContained;
      deployDir_ = FileSystemItem.createFile(outputFile).getParentPathString();
   }
   
   public RSConnectPublishSource(RenderedDocPreview preview, 
         boolean isSelfContained, String description)
   {
      deployFile_ = preview.getOutputFile();
      sourceFile_ = preview.getSourceFile();
      description_ = description;
      isSelfContained_ = isSelfContained;
      deployDir_ = FileSystemItem.createFile(preview.getOutputFile())
            .getParentPathString();
   }
   
   public RSConnectPublishSource(String sourceFile, String deployDir, 
         String deployFile, boolean isSelfContained, String description)
   {
      sourceFile_ = sourceFile;
      deployDir_ = deployDir;
      deployFile_ = deployFile;
      isSelfContained_ = isSelfContained;
      description_ = description;
   }
   
   public String getDeployFile()
   {
      return deployFile_;
   }
   
   public String getDeployDir()
   {
      return deployDir_;
   }
   
   public String getSourceFile()
   {
      return sourceFile_;
   }
   
   public boolean isSourceExt(String ext)
   {
       return StringUtil.getExtension(deployFile_).toLowerCase().equals(ext);
   }
   
   public boolean isDocument()
   {
      return isSourceExt("rmd") || isSourceExt("md") || isSourceExt("html") ||
             isSourceExt("htm") || isSourceExt("rpres");
   }
   
   public String getDeployKey()
   {
      return isDocument() ? getSourceFile() : getDeployDir();
   }
   
   public String getDeployFileName()
   {
      return FileSystemItem.createFile(getDeployFile()).getName();
   }
   
   public String getDescription()
   {
      return description_;
   }
   
   public boolean isSelfContained()
   {
      return isSelfContained_;
   }
   
   private final String deployFile_;
   private final String deployDir_;
   private final String sourceFile_;
   private final String description_;
   private final boolean isSelfContained_;
}
