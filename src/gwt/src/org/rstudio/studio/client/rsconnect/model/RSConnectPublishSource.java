/*
 * RSConnectPublishSource.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
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
import org.rstudio.core.client.js.JsObject;
import org.rstudio.studio.client.rsconnect.RSConnect;

import com.google.gwt.core.client.JavaScriptObject;

public class RSConnectPublishSource
{
   // invoked when publishing Shiny applications or Plumber APIs
   public RSConnectPublishSource(String sourceDir, String sourceFile, boolean isAPI)
   {
      sourceFile_ = sourceDir;
      deployFile_ = sourceFile;
      isSelfContained_ = false;
      description_ = null;
      deployDir_ = sourceDir;
      contentCategory_ = isAPI ? RSConnect.CONTENT_CATEGORY_API : null;
      isShiny_ = !isAPI;
      websiteDir_ = null;
      isStatic_ = false;
      isSingleFileShiny_ = sourceDir != sourceFile;
   }

   public RSConnectPublishSource(String sourceFile, String websiteDir, 
         boolean isSelfContained, boolean isStatic, boolean isShiny, boolean isQuarto,
         String description, int type)
   {
      this(sourceFile, sourceFile, websiteDir, null, isSelfContained, isStatic, 
            isShiny, isQuarto, description, type);
   }
   
   public RSConnectPublishSource(String sourceFile, String outputFile, 
         String websiteDir, String websiteOutputDir, boolean isSelfContained, boolean isStatic, 
         boolean isShiny, boolean isQuarto, String description, int type)
   {
      deployFile_ = outputFile;
      sourceFile_ = sourceFile;
      description_ = description;
      isSelfContained_ = isSelfContained;
      isShiny_ = isShiny;
      isStatic_ = isStatic;
      isSingleFileShiny_ = false;
      websiteDir_ = websiteDir;
      isQuarto_ = isQuarto;
      boolean isWebsite = type == RSConnect.CONTENT_TYPE_WEBSITE || type == RSConnect.CONTENT_TYPE_QUARTO_WEBSITE;

      String category = null;
      if (isWebsite)
      {
         // websites are always deployed as sites
         category = RSConnect.CONTENT_CATEGORY_SITE;
      }
      else if (type == RSConnect.CONTENT_TYPE_PLOT || 
               type == RSConnect.CONTENT_TYPE_HTML) 
      {
         // consider plots and raw HTML published from the viewer pane to be 
         // plots 
         category = RSConnect.CONTENT_CATEGORY_PLOT;
      }
      contentCategory_ = category;

      if (isWebsite)
      {
         if (isStatic && !StringUtil.isNullOrEmpty(websiteOutputDir))
         {
            // publishing static output from a website: use the output directory if we have it
            deployDir_ = FileSystemItem.createFile(websiteOutputDir).getPath();
         }
         else
         {
            // for all other website publishing purposes, publish the website directory
            deployDir_ = FileSystemItem.createFile(websiteDir).getPath();
         }
      }
      else
      {
         deployDir_ = FileSystemItem.createFile(outputFile).getParentPathString();
      }
   }
   
   public RSConnectPublishSource(RenderedDocPreview preview, 
         String websiteDir, boolean isSelfContained, boolean isStatic, 
         boolean isShiny, String description)
   {
      deployFile_ = preview.getOutputFile();
      sourceFile_ = preview.getSourceFile();
      description_ = description;
      isQuarto_ = preview.isQuarto();
      isSelfContained_ = isSelfContained;
      isStatic_ = isStatic;
      isShiny_ = isShiny;
      isSingleFileShiny_ = false;
      contentCategory_ = !StringUtil.isNullOrEmpty(websiteDir) ? 
            RSConnect.CONTENT_CATEGORY_SITE : null;
      websiteDir_ = websiteDir;
      if (StringUtil.isNullOrEmpty(preview.getWebsiteDir()))
         deployDir_ = FileSystemItem.createFile(preview.getOutputFile()).getParentPathString();
      else
         deployDir_ = preview.getOutputFile();
   }
   
   public RSConnectPublishSource(String sourceFile, String deployDir, 
         String deployFile, String websiteDir, boolean isSelfContained, 
         boolean isStatic, boolean isShiny, boolean isQuarto, String description)
   {
      sourceFile_ = sourceFile;
      deployDir_ = deployDir;
      deployFile_ = deployFile;
      websiteDir_ = websiteDir;
      isSelfContained_ = isSelfContained;
      isStatic_ = isStatic;
      isShiny_ = isShiny;
      isQuarto_ = isQuarto;
      isSingleFileShiny_ = false;
      description_ = description;
      contentCategory_ = !StringUtil.isNullOrEmpty(websiteDir) ? 
            RSConnect.CONTENT_CATEGORY_SITE : null;
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
      if (StringUtil.isNullOrEmpty(deployFile_))
      {
         // Happens for websites (where no particular file is being deployed)
         return false;
      }
      return isSourceExt("rmd") || isSourceExt("md") || isSourceExt("html") ||
             isSourceExt("htm") || isSourceExt("rpres") || isSourceExt("pdf") ||
             isSourceExt("docx") || isSourceExt("odt") || isSourceExt("rtf") ||
             isSourceExt("pptx") || isSourceExt("qmd");
   }
   
   public String getDeployKey()
   {
      if (isSingleFileShiny_)
         return FileSystemItem.createDir(getDeployDir())
                              .completePath(getDeployFile());
      else if (contentCategory_ == RSConnect.CONTENT_CATEGORY_SITE)
         return getDeployDir();
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
   
   public boolean isShiny()
   {
      return isShiny_;
   }
   
   public boolean isSingleFileShiny()
   {
      return isSingleFileShiny_;
   }
   
   public String getContentCategory()
   {
      return contentCategory_;
   }
   
   public boolean isWebsiteRmd()
   {
      return !StringUtil.isNullOrEmpty(websiteDir_);
   }
   
   public String getWebsiteDir()
   {
      return websiteDir_;
   }
   
   public boolean isStatic()
   {
      return isStatic_;
   }

   public boolean isQuarto()
   {
      return isQuarto_;
   }

   public void setIsQuarto(boolean quarto)
   {
      isQuarto_ = quarto;
   }
   
   public JavaScriptObject toJso()
   {
      // create summary of publish source for server
      JsObject obj = JsObject.createJsObject();
      obj.setString("deploy_dir", getDeployDir());
      obj.setString("deploy_file", isDocument() ||
            isSingleFileShiny() ? getDeployFileName() : "");
      obj.setString("source_file", 
            isDocument() && 
            getSourceFile() != null && 
            getContentCategory() != RSConnect.CONTENT_CATEGORY_SITE ?
               getSourceFile() : "");
      obj.setString("content_category", StringUtil.notNull(
            getContentCategory()));
      obj.setString("website_dir", StringUtil.notNull(getWebsiteDir()));
      obj.setBoolean("is_quarto", isQuarto_);
      return obj.cast();
   }
   
   private final String deployFile_;
   private final String deployDir_;
   private final String sourceFile_;
   private final String description_;
   private final String contentCategory_;
   private final String websiteDir_;
   private final boolean isSelfContained_;
   private final boolean isShiny_;
   private final boolean isSingleFileShiny_;
   private final boolean isStatic_;
   private boolean isQuarto_ = false;
}
