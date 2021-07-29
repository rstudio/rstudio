/*
 * QuartoHelper.java
 *
 * Copyright (C) 2021 by RStudio, PBC
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

package org.rstudio.studio.client.quarto;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.quarto.model.QuartoConfig;
import org.rstudio.studio.client.workbench.model.SessionInfo;

public class QuartoHelper
{
   public static boolean isQuartoWebsiteConfig(QuartoConfig config)
   {
      return config.is_project &&
            (config.project_type == SessionInfo.QUARTO_PROJECT_TYPE_SITE ||
            config.project_type == SessionInfo.QUARTO_PROJECT_TYPE_BOOK);
   }
   
   public static boolean isQuartoWebsiteDoc(String qmd, QuartoConfig config)
   {
      // Determine whether or not this Quarto file is part of a website; we
      // presume it to be if it's inside the current project and the current project is a 
      // Quarto website project or book
      boolean isWebsite = false;
      if (!StringUtil.isNullOrEmpty(qmd))
      {
         if (isQuartoWebsiteConfig(config))
         {
            FileSystemItem projectDir = FileSystemItem.createDir(config.project_dir);
            FileSystemItem qmdFile = FileSystemItem.createFile(qmd);
            if (qmdFile.getPathRelativeTo(projectDir) != null)
            {
               isWebsite = true;
            }
         }
      }
      return isWebsite;
   }
}
