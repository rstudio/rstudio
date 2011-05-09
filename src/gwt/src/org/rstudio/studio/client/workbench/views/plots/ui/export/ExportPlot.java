package org.rstudio.studio.client.workbench.views.plots.ui.export;

import java.util.HashMap;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.workbench.views.plots.model.ExportPlotOptions;
import org.rstudio.studio.client.workbench.views.plots.model.SavePlotContext;
import org.rstudio.studio.client.workbench.views.plots.model.PlotsServerOperations;

public abstract class ExportPlot
{
   public void savePlotAsImage(GlobalDisplay globalDisplay,
                               PlotsServerOperations server,
                               SavePlotContext context, 
                               ExportPlotOptions options,
                               OperationWithInput<ExportPlotOptions> onClose)
   {
      new SavePlotAsImageDialog(globalDisplay,
                                server, 
                                context, 
                                options, 
                                onClose).showModal();
   }
   
   
   public abstract void copyPlotToClipboard(
                           PlotsServerOperations server,
                           ExportPlotOptions options,
                           OperationWithInput<ExportPlotOptions> onClose);
   
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
