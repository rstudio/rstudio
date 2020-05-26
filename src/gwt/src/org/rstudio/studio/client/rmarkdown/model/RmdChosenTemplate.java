/*
 * RmdChosenTemplate.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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
package org.rstudio.studio.client.rmarkdown.model;

public class RmdChosenTemplate
{
   public RmdChosenTemplate(String templatePath, String fileName, 
                            String directory, boolean createDir)
   {
      templatePath_ = templatePath;
      fileName_ = fileName;
      directory_ = directory;
      createDir_ = createDir;
   }

   public String getTemplatePath()
   {
      return templatePath_;
   }
   
   public String getFileName()
   {
      return fileName_;
   }
   
   public String getDirectory()
   {
      return directory_;
   }
   
   public boolean createDir()
   {
      return createDir_;
   }
      
   private String templatePath_;
   private String fileName_;
   private String directory_;
   private boolean createDir_;
}
