/*
 * RenderedDocPreview.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

package org.rstudio.studio.client.rsconnect.model;

import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.htmlpreview.model.HTMLPreviewResult;
import org.rstudio.studio.client.rmarkdown.model.RmdPreviewParams;
import org.rstudio.studio.client.rmarkdown.model.RmdRenderResult;

public class RenderedDocPreview
{
   public RenderedDocPreview(RmdPreviewParams params)
   {
      sourceFile_ = params.getTargetFile();
      outputFile_ = params.getOutputFile();
      isStatic_ = !params.isShinyDocument();
      websiteDir_ = params.getWebsiteDir();
      isQuarto_ = params.getResult() != null && params.getResult().isQuarto();
   }
   
   public RenderedDocPreview(HTMLPreviewResult result)
   {
      sourceFile_ = result.getSourceFile();
      outputFile_ = result.getHtmlFile();
      isStatic_ = true;
      websiteDir_ = null;
   }
   
   public RenderedDocPreview(RmdRenderResult result)
   {
      sourceFile_ = result.getTargetFile();
      outputFile_ = result.getOutputFile();
      isStatic_ = !result.isShinyDocument();
      websiteDir_ = null;
      isQuarto_ = result.isQuarto();
   }
   
   public RenderedDocPreview(String sourceFile, String outputFile, 
                             boolean isStatic, boolean isQuarto)
   {
      sourceFile_ = sourceFile;
      outputFile_ = outputFile;
      isStatic_ = isStatic;
      websiteDir_ = null;
      isQuarto_ = isQuarto;
   }
   
   public String getSourceFile()
   {
      return sourceFile_;
   }
   
   public String getOutputFile()
   {
      return outputFile_;
   }

   public void setOutputFile(String outputFile)
   {
      outputFile_ = outputFile;
   }

   public String getWebsiteDir()
   {
      return websiteDir_;
   }

   public void setWebsiteDir(String websiteDir)
   {
      websiteDir_ = websiteDir;
   }

   public boolean isStatic()
   {
      return isStatic_;
   }

   public boolean isQuarto()
   {
      return isQuarto_;
   }
   
   public void setIsStatic(boolean isStatic)
   {
      isStatic_ = isStatic;
   }
   
   public boolean isWebsite()
   {
      return !StringUtil.isNullOrEmpty(websiteDir_);
   }

   private final String sourceFile_;
   private String websiteDir_;
   private String outputFile_;
   private boolean isStatic_;
   private boolean isQuarto_ = false;
}
