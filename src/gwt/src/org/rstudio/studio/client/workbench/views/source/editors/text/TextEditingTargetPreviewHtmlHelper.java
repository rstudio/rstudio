/*
 * TextEditingTargetPreviewHtmlHelper.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.source.editors.text;

import com.google.inject.Inject;

import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.filetypes.FileTypeCommands;
import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.workbench.model.Session;

public class TextEditingTargetPreviewHtmlHelper
{
   public TextEditingTargetPreviewHtmlHelper()
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
   }
   
   @Inject
   public void initialize(Session session,
                          FileTypeCommands fileTypeCommands)
   {
      session_ = session;
      fileTypeCommands_ = fileTypeCommands;
   }
   
   public String detectExtendedType(String extendedType,
                                    TextFileType fileType)
   {
      if (extendedType.length() == 0 && 
          fileType.isRmd() &&
          session_.getSessionInfo().getRMarkdownInstalled())
      {
         return "rmarkdown";
      }
      else
      {
         return extendedType;
      }
   }
   
   public boolean verifyPrerequisites(WarningBarDisplay display,
                                      TextFileType fileType)
   {
      return verifyPrerequisites(null, display, fileType);
   }
   
   public boolean verifyPrerequisites(String feature,
                                      WarningBarDisplay display,
                                      TextFileType fileType)
   {
      if (feature == null)
         feature = fileType.getLabel();
      
      // if this file requires knitr then validate pre-reqs
      boolean haveRMarkdown = 
         fileTypeCommands_.getHTMLCapabiliites().isRMarkdownSupported();
      if (!haveRMarkdown)
      {
         if (fileType.isRpres())
         {
            showKnitrPreviewWarning(display, "R Presentations", "1.2");
            return false;
         }
         else if (fileType.requiresKnit())
         {
   
            showKnitrPreviewWarning(display, feature, "1.2");
            return false;
         }
      }
      
      return true;
   }
   
   private void showKnitrPreviewWarning(WarningBarDisplay display,
                                        String feature, 
                                        String requiredVersion)
   {
      display.showWarningBar(feature + " requires the " +
                             "knitr package (version " + requiredVersion + 
                             " or higher)");
   }
   
   private Session session_;
   private FileTypeCommands fileTypeCommands_;
}
