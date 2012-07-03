/*
 * BuildPane.java
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
package org.rstudio.studio.client.workbench.views.buildtools;

import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

import org.rstudio.core.client.widget.HorizontalCenterPanel;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.buildtools.ui.BuildPaneResources;

public class BuildPane extends WorkbenchPane implements Build.Display
{
   @Inject
   public BuildPane(Commands commands,
                    Session session)
   {
      super("Build");
      commands_ = commands;
      session_ = session;
      ensureWidget();
   }
   
   @Override
   protected Toolbar createMainToolbar()
   {
      Toolbar toolbar = new Toolbar();
      
      // always include build all
      toolbar.addLeftWidget(commands_.buildAll().createToolbarButton());
      toolbar.addLeftSeparator();
      
      // packages get checkPackage
      String type = session_.getSessionInfo().getBuildToolsType();
      if (type.equals(SessionInfo.BUILD_TOOLS_PACKAGE))
      {
         toolbar.addLeftWidget(commands_.checkPackage().createToolbarButton());
         toolbar.addLeftSeparator();
      }
      
      // always include configuration
      toolbar.addLeftWidget(
               commands_.buildToolsProjectSetup().createToolbarButton());
      
      return toolbar;
   }
   
   @Override 
   protected Widget createMainWidget()
   {
      Label label = new Label("Under Construction");
      label.getElement().getStyle().setColor("#888");
      return new HorizontalCenterPanel(label, 100);
   }

   
   private Commands commands_;
   private Session session_;
   
   @SuppressWarnings("unused")
   private static BuildPaneResources RES = BuildPaneResources.INSTANCE;
   
   
}
