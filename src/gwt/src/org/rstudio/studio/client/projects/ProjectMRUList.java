/*
 * ProjectMRUList.java
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
package org.rstudio.studio.client.projects;

import java.util.ArrayList;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.DuplicateHelper;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.projects.events.OpenProjectNewWindowEvent;
import org.rstudio.studio.client.projects.events.SwitchToProjectEvent;
import org.rstudio.studio.client.workbench.MRUList;
import org.rstudio.studio.client.workbench.WorkbenchListManager;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;

import com.google.gwt.resources.client.ImageResource;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class ProjectMRUList extends MRUList
{
   @Inject 
   public ProjectMRUList(Commands commands, 
                         WorkbenchListManager listManager,
                         final EventBus eventBus,
                         Session session)
   {
      super(listManager.getProjectMruList(),
            new AppCommand[] {
                  commands.projectMru0(),
                  commands.projectMru1(),
                  commands.projectMru2(),
                  commands.projectMru3(),
                  commands.projectMru4(),
                  commands.projectMru5(),
                  commands.projectMru6(),
                  commands.projectMru7(),
                  commands.projectMru8(),
                  commands.projectMru9(),
                  commands.projectMru10(),
                  commands.projectMru11(),
                  commands.projectMru12(),
                  commands.projectMru13(),
                  commands.projectMru14()
            },
            commands.clearRecentProjects(),
            false,
            new OperationWithInput<String>() 
            {
               @Override
               public void execute(String file)
               {
                  if (openInNewWindow_)
                     eventBus.fireEvent(new OpenProjectNewWindowEvent(file));
                  else
                     eventBus.fireEvent(new SwitchToProjectEvent(file));
               }
            });
      
      // set right image for project MRU commands
      if ((Desktop.isDesktop() && !BrowseCap.isWindowsDesktop())
          || session.getSessionInfo().getMultiSession())
      {
         ImageResource image = commands.openHtmlExternal().getImageResource();
         String desc = "Open project in a new R session";
         commands.projectMru0().setRightImage(image, desc);
         commands.projectMru1().setRightImage(image, desc);
         commands.projectMru2().setRightImage(image, desc);
         commands.projectMru3().setRightImage(image, desc);
         commands.projectMru4().setRightImage(image, desc);
         commands.projectMru5().setRightImage(image, desc);
         commands.projectMru6().setRightImage(image, desc);
         commands.projectMru7().setRightImage(image, desc);
         commands.projectMru8().setRightImage(image, desc);
         commands.projectMru9().setRightImage(image, desc);
         commands.projectMru10().setRightImage(image, desc);
         commands.projectMru11().setRightImage(image, desc);
         commands.projectMru12().setRightImage(image, desc);
         commands.projectMru13().setRightImage(image, desc);
         commands.projectMru14().setRightImage(image, desc);
      }
   }
   
   public static void setOpenInNewWindow(boolean openInNewWindow)
   {
      openInNewWindow_ = openInNewWindow;
   }
   
   @Override
   protected String transformMruEntryPath(String entryPath)
   {
      return FileSystemItem.createFile(entryPath).getParentPathString();
   }
   
   @Override
   protected ArrayList<String> generateLabels(
		   ArrayList<String> mruEntries, boolean includeExt)
   {
	   return DuplicateHelper.getPathLabels(mruEntries, true);
   }
   
   private static boolean openInNewWindow_ = false;
   
}
