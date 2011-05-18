/*
 * WorkbenchEventHelper.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.common;

import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;

public class WorkbenchHelper
{
   public static void sendSetWdToConsole(FileSystemItem dir, EventBus eventBus)
   {
      String escaped = dir.getPath().replaceAll("\\\\", "\\\\\\\\");
      if (escaped.equals("~"))
         escaped = "~/";
      eventBus.fireEvent(
            new SendToConsoleEvent("setwd(\"" + escaped + "\")", true));
   }
   
   public static void sendFileCommandToConsole(String command, 
                                               FileSystemItem file,
                                               EventBus eventBus)
   {
      String code = command + "(\"" + file.getPath() + "\")";
      eventBus.fireEvent(new SendToConsoleEvent(code, true));
   }
}
