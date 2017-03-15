/*
 * BuildCommands.java
 *
 * Copyright (C) 2009-15 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.buildtools;

import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.views.buildtools.ui.BuildPaneResources;

public class BuildCommands
{
   public static void setBuildCommandState(Commands commands, 
         SessionInfo sessionInfo)
   {
      // remove devtools commands if it isn't installed
      if (!sessionInfo.isDevtoolsInstalled())
      {
         commands.devtoolsLoadAll().remove();
      }
      
      // adapt or remove package commands if this isn't a package
      String type = sessionInfo.getBuildToolsType();
      if (!type.equals(SessionInfo.BUILD_TOOLS_PACKAGE))
      {
         commands.devtoolsLoadAll().remove();
         commands.buildSourcePackage().remove();
         commands.buildBinaryPackage().remove();
         commands.roxygenizePackage().remove();
         commands.checkPackage().remove();
         commands.testPackage().remove();
         commands.buildAll().setImageResource(
                           new ImageResource2x(
                              BuildPaneResources.INSTANCE.iconBuild2x()
                           ));
         commands.buildAll().setMenuLabel("_Build All");
         commands.buildAll().setButtonLabel("Build All");
         commands.buildAll().setDesc("Build all");
         
      }
      
      // remove makefile commands if this isn't a makefile
      if (type.equals(SessionInfo.BUILD_TOOLS_CUSTOM) ||
          type.equals(SessionInfo.BUILD_TOOLS_WEBSITE))
      {
         commands.rebuildAll().remove();
      }
      
      if (type.equals(SessionInfo.BUILD_TOOLS_CUSTOM) ||
          type.equals(SessionInfo.BUILD_TOOLS_PACKAGE))
      {
         commands.cleanAll().remove();
      }
      
      // remove all other commands if there are no build tools
      if (type.equals(SessionInfo.BUILD_TOOLS_NONE))
      {
         commands.buildAll().remove();
         commands.rebuildAll().remove();
         commands.cleanAll().remove();
         commands.stopBuild().remove();
         commands.activateBuild().remove();
         commands.layoutZoomBuild().remove();
      }
   }
}
