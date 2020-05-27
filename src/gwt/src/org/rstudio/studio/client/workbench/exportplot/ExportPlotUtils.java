/*
 * ExportPlotUtils.java
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

package org.rstudio.studio.client.workbench.exportplot;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.files.FileSystemItem;

import com.google.gwt.user.client.ui.TextBox;

public class ExportPlotUtils
{
   // utility for calculating display of directory
   public static String shortDirectoryName(FileSystemItem directory,
                                           int maxWidth)
   {
      return StringUtil.shortPathName(directory, "gwt-Label", maxWidth);
   }
   
   public static FileSystemItem composeTargetPath(String ext,
                                                  TextBox fileNameTextBox,
                                                  FileSystemItem directory)
   {
      // get the filename
      String filename = fileNameTextBox.getText().trim();
      if (filename.length() == 0)
         return null;
      
      // compute the target path
      FileSystemItem targetPath = FileSystemItem.createFile(
                                          directory.completePath(filename));
      
      // if the extension isn't already correct then append it
      if (!targetPath.getExtension().equalsIgnoreCase(ext))
         targetPath = FileSystemItem.createFile(targetPath.getPath() + ext);
      
      // return the path
      return targetPath;
   }
   
   
   // track which directory to suggest as the default for various 
   // working directories
   
   
   public static FileSystemItem getDefaultSaveDirectory(
                                                   FileSystemItem defaultDir)
   {
      if (defaultSaveDirectory_ == null)
         defaultSaveDirectory_ = defaultDir;
      
      return defaultSaveDirectory_;
   }
   
   public static void setDefaultSaveDirectory(
                                          FileSystemItem defaultSaveDirectory)
   {
      defaultSaveDirectory_ = defaultSaveDirectory;
   }
   
   // remember last save directory
   static private FileSystemItem defaultSaveDirectory_ = null;
}
