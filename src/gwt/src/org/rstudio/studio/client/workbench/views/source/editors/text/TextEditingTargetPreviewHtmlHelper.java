/*
 * TextEditingTargetPreviewHtmlHelper.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
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
import org.rstudio.studio.client.workbench.model.HTMLCapabilities;

public class TextEditingTargetPreviewHtmlHelper
{
   public TextEditingTargetPreviewHtmlHelper()
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
   }
   
   @Inject
   public void initialize(FileTypeCommands fileTypeCommands)
   {
      fileTypeCommands_ = fileTypeCommands;
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
         feature = fileType.getLabel() + " files";
      
      // if this file requires knitr then validate pre-reqs
      if (fileType.requiresKnit())
      {
         HTMLCapabilities htmlCaps = fileTypeCommands_.getHTMLCapabiliites();
         if (fileType.isMarkdown())
         {
            if (!htmlCaps.isRMarkdownSupported())
            {
               showKnitrPreviewWarning(display, feature, "0.5");
               return false;
            }
         }
         else // must be html
         {
            if (!htmlCaps.isRHtmlSupported())
            {
               showKnitrPreviewWarning(display, feature,  "0.5");
               return false;
            }
         }
      }
      
      return true;
   }
   
   private void showKnitrPreviewWarning(WarningBarDisplay display,
                                        String feature, 
                                        String requiredVersion)
   {
      display.showWarningBar(feature + " require the " +
                             "knitr package (version " + requiredVersion + 
                             " or higher)");
   }
   
   private FileTypeCommands fileTypeCommands_;
  
   
 
}
