/*
 * BuildCommands.java
 *
 * Copyright (C) 2022 by RStudio, PBC
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

import com.google.gwt.core.client.GWT;
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
      if (type != SessionInfo.BUILD_TOOLS_PACKAGE)
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
         commands.buildAll().setMenuLabel("_" + constants_.buildAllLabel());
         commands.buildAll().setButtonLabel(constants_.buildAllLabel());
         commands.buildAll().setDesc(constants_.buildAllDesc());
      }
      
      if (type == SessionInfo.BUILD_TOOLS_CUSTOM ||
          type == SessionInfo.BUILD_TOOLS_PACKAGE ||
          type == SessionInfo.BUILD_TOOLS_QUARTO)
      {
         commands.cleanAll().remove();
      }
      
      if (type != SessionInfo.BUILD_TOOLS_QUARTO)
      {
         commands.serveQuartoSite().remove();
      }
      
      if (type == SessionInfo.BUILD_TOOLS_QUARTO)
      {
         String projType = constants_.projectTypeText();
         
         if (sessionInfo.getQuartoConfig().project_type.equals(
                      SessionInfo.QUARTO_PROJECT_TYPE_BOOK)) 
         {
            projType = constants_.bookText();
         }
         if (sessionInfo.getQuartoConfig().project_type.equals(
               SessionInfo.QUARTO_PROJECT_TYPE_WEBSITE)) 
         {
            projType = constants_.projectWebsiteText();
         }
         commands.buildAll().setMenuLabel("_" + constants_.renderLabel() + projType);
         commands.buildAll().setButtonLabel(constants_.renderLabel() + projType);
         commands.buildAll().setDesc(constants_.renderLabel() + projType.toLowerCase());
         commands.buildAll().setImageResource(commands.quartoRenderDocument().getImageResource());
         commands.serveQuartoSite().setMenuLabel("_" + constants_.serveLabel() + " " + projType);
         commands.serveQuartoSite().setButtonLabel(constants_.serveLabel() + " " + projType);
      }
      
      // remove all other commands if there are no build tools
      if (type == SessionInfo.BUILD_TOOLS_NONE)
      {
         commands.buildAll().remove();
         commands.cleanAll().remove();
         commands.stopBuild().remove();
         commands.activateBuild().remove();
         commands.layoutZoomBuild().remove();
         commands.clearBuild().remove();
      }
   }
   private static final ViewBuildtoolsConstants constants_ = GWT.create(ViewBuildtoolsConstants.class);
}
