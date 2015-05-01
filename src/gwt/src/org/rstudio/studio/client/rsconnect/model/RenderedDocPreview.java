/*
 * RenderedDocPreview.java
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

import org.rstudio.studio.client.htmlpreview.model.HTMLPreviewResult;
import org.rstudio.studio.client.rmarkdown.model.RmdPreviewParams;

public class RenderedDocPreview
{
   public RenderedDocPreview(RmdPreviewParams params)
   {
      sourceFile_ = params.getTargetFile();
      outputFile_ = params.getOutputFile();
      isStatic_ = !params.isShinyDocument();
   }
   
   public RenderedDocPreview(HTMLPreviewResult result)
   {
      sourceFile_ = result.getSourceFile();
      outputFile_ = result.getHtmlFile();
      isStatic_ = true;
   }
   
   public RenderedDocPreview(String sourceFile, String outputFile, 
                             boolean isStatic)
   {
      sourceFile_ = sourceFile;
      outputFile_ = outputFile;
      isStatic_ = isStatic;
   }
   
   public String getSourceFile()
   {
      return sourceFile_;
   }
   
   public String getOutputFile()
   {
      return outputFile_;
   }
   
   public boolean isStatic()
   {
      return isStatic_;
   }
   
   public void setIsStatic(boolean isStatic)
   {
      isStatic_ = isStatic;
   }
   
   private final String sourceFile_;
   private final String outputFile_;
   private boolean isStatic_;
}
