/*
 * ExportPlot.java
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
package org.rstudio.studio.client.workbench.views.plots.ui.export;

import java.util.HashMap;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.views.plots.model.ExportPlotOptions;
import org.rstudio.studio.client.workbench.views.plots.model.SavePlotAsImageContext;
import org.rstudio.studio.client.workbench.views.plots.model.PlotsServerOperations;
import org.rstudio.studio.client.workbench.views.plots.model.SavePlotAsPdfOptions;

import com.google.gwt.user.client.ui.TextBox;

public class ExportPlot
{
   public void savePlotAsImage(GlobalDisplay globalDisplay,
                               PlotsServerOperations server,
                               SavePlotAsImageContext context, 
                               ExportPlotOptions options,
                               OperationWithInput<ExportPlotOptions> onClose)
   {
      new SavePlotAsImageDialog(globalDisplay,
                                server, 
                                context, 
                                options, 
                                onClose).showModal();
   }
   
   public void savePlotAsPdf(GlobalDisplay globalDisplay,
                             PlotsServerOperations server,
                             SessionInfo sessionInfo,
                             FileSystemItem defaultDirectory,
                             String defaultPlotName,
                             final SavePlotAsPdfOptions options,
                             final OperationWithInput<SavePlotAsPdfOptions> onClose)
   {
      new SavePlotAsPdfDialog(globalDisplay,
                              server,
                              sessionInfo,
                              defaultDirectory,
                              defaultPlotName,
                              options,
                              onClose).showModal();
   }
   
   
   public void copyPlotToClipboard(
                           PlotsServerOperations server,
                           ExportPlotOptions options,
                           OperationWithInput<ExportPlotOptions> onClose)
   {  
   }
   
   
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
                                                FileSystemItem workingDir)
   {
      // do we have a cached initial dir?
      if (initialDirectories_.containsKey(workingDir.getPath()))
         return initialDirectories_.get(workingDir.getPath());
      else
         return workingDir;
   }
   
   public static void setDefaultSaveDirectory(
                                          FileSystemItem workingDir,
                                          FileSystemItem defaultSaveDirectory)
   {
      initialDirectories_.put(workingDir.getPath(),
                              defaultSaveDirectory);
   }
   
   // remember what directory was chosen for plot export for various
   // working directories
   static private HashMap<String, FileSystemItem> initialDirectories_ = 
                                       new HashMap<String,FileSystemItem>();
   

}
