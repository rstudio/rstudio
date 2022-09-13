/*
 * QuartoHelper.java
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
            (config.project_type == SessionInfo.QUARTO_PROJECT_TYPE_WEBSITE ||
            config.project_type == SessionInfo.QUARTO_PROJECT_TYPE_BOOK);
   }
   
   public static boolean isQuartoBookConfig(QuartoConfig config)
   {
      return config.is_project &&
             config.project_type == SessionInfo.QUARTO_PROJECT_TYPE_BOOK;
   }
   
   public static boolean isQuartoWebsiteDoc(String qmd, QuartoConfig config)
   {
      return !StringUtil.isNullOrEmpty(qmd) &&
             isQuartoWebsiteConfig(config) &&
             isWithinQuartoProjectDir(qmd, config);
   }
   
   public static boolean isQuartoBookDoc(String qmd, QuartoConfig config)
   {
      return !StringUtil.isNullOrEmpty(qmd) &&
             isQuartoBookConfig(config) &&
             isWithinQuartoProjectDir(qmd, config);
   }
   
   
   public static boolean isWithinQuartoProjectDir(String qmd, QuartoConfig config)
   {
      if (!StringUtil.isNullOrEmpty(qmd))
      {
         FileSystemItem projectDir = FileSystemItem.createDir(config.project_dir);
         FileSystemItem qmdFile = FileSystemItem.createFile(qmd);
         return qmdFile.getPathRelativeTo(projectDir) != null;
      } 
      else
      {
         return false;
      }
     
   }
}
