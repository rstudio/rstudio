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

import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.common.OutputBuffer;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.buildtools.ui.BuildPaneResources;

public class BuildPane extends WorkbenchPane implements BuildPresenter.Display
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
      
      // makefiles get extra build menu entries
      String type = session_.getSessionInfo().getBuildToolsType();
      if (type.equals(SessionInfo.BUILD_TOOLS_MAKEFILE))
      {
         ToolbarPopupMenu buildMenu = new ToolbarPopupMenu();
         buildMenu.addItem(commands_.buildAll().createMenuItem(false));
         buildMenu.addItem(commands_.rebuildAll().createMenuItem(false));
         buildMenu.addSeparator();
         buildMenu.addItem(commands_.cleanAll().createMenuItem(false));
         ToolbarButton buildMenuButton = new ToolbarButton(buildMenu, true);
         toolbar.addLeftWidget(buildMenuButton);
      }
      
      toolbar.addLeftSeparator();
      
      // packages get checkPackage
      if (type.equals(SessionInfo.BUILD_TOOLS_PACKAGE))
      {
         toolbar.addLeftWidget(commands_.checkPackage().createToolbarButton());
         toolbar.addLeftSeparator();
      }
      
      // always include configuration
      toolbar.addLeftWidget(
               commands_.buildToolsProjectSetup().createToolbarButton());
      
      // stop button (initially hidden)
      ImageResource stopImage = commands_.interruptR().getImageResource();
      stopButton_ = new ToolbarButton(stopImage, null);
      stopButton_.setVisible(false);
      toolbar.addRightWidget(stopButton_);
      
      return toolbar;
   }
   
   @Override 
   protected Widget createMainWidget()
   {
      panel_ = new SimplePanel();
     
      outputBuffer_ = new OutputBuffer();
         
      panel_.setWidget(outputBuffer_);
 
      return panel_;
      
   }
   
   @Override
   public void buildStarted()
   {
     outputBuffer_.clear();
     stopButton_.setVisible(true);
      
   }

   @Override
   public void showOutput(String output)
   {
      outputBuffer_.append(output);
      
   }

   @Override
   public void buildCompleted()
   {
      stopButton_.setVisible(false);
      
   }
   
   @Override
   public HasClickHandlers stopButton()
   {
      return stopButton_;
   }
   
   @Override
   public void scrollToBottom()
   {
      outputBuffer_.scrollToBottom();
      
   }


   
   private Commands commands_;
   private Session session_;
   
   private SimplePanel panel_;
   private OutputBuffer outputBuffer_;
   private ToolbarButton stopButton_;
   
   @SuppressWarnings("unused")
   private static BuildPaneResources RES = BuildPaneResources.INSTANCE; 
}
