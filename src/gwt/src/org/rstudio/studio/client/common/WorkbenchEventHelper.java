package org.rstudio.studio.client.common;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;

public class WorkbenchEventHelper
{
   public static void sendSetWdToConsole(FileSystemItem dir, EventBus eventBus)
   {
      String escaped = dir.getPath().replaceAll("\\\\", "\\\\\\\\");
      if (escaped.equals("~"))
         escaped = "~/";
      eventBus.fireEvent(
            new SendToConsoleEvent("setwd(\"" + escaped + "\")", true));
   }
   

}
