/*
 * EnvironmentPane.java
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

package org.rstudio.studio.client.workbench.views.environment;

import com.google.gwt.user.client.ui.Label;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.icons.StandardIcons;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;
import org.rstudio.studio.client.workbench.views.environment.model.RObject;
import org.rstudio.studio.client.workbench.views.environment.view.EnvironmentObjects;

import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class EnvironmentPane extends WorkbenchPane 
                             implements EnvironmentPresenter.Display,
                                        EnvironmentObjects.Observer
{
   @Inject
   public EnvironmentPane(Commands commands,
                          EventBus eventBus)
   {
      super("Environment");
      
      commands_ = commands;
      eventBus_ = eventBus;
      
      ensureWidget();
   }

   @Override
   protected Toolbar createMainToolbar()
   {
      environmentName_ = new Label("Global");
      Toolbar toolbar = new Toolbar();
      toolbar.addLeftWidget(commands_.loadWorkspace().createToolbarButton());
      toolbar.addLeftWidget(commands_.saveWorkspace().createToolbarButton());
      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(createImportMenu());
      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(commands_.clearWorkspace().createToolbarButton());
      toolbar.addRightWidget(environmentName_);
      toolbar.addRightWidget(commands_.refreshEnvironment().createToolbarButton());

      return toolbar;
   }

   private Widget createImportMenu()
   {
      ToolbarPopupMenu menu = new ToolbarPopupMenu();
      menu.addItem(commands_.importDatasetFromFile().createMenuItem(false));
      menu.addItem(commands_.importDatasetFromURL().createMenuItem(false));
      dataImportButton_ = new ToolbarButton("Import Dataset",
                                             StandardIcons.INSTANCE.import_dataset(),
                                             menu);
      return dataImportButton_;

   }

   @Override
   protected Widget createMainWidget()
   {
      objects_ = new EnvironmentObjects();
      objects_.setObserver(this);
      return objects_;
   }

   @Override
   public void addObject(RObject object)
   {
      objects_.addObject(object);
   }
   
   @Override
   public void removeObject(String objectName)
   {
      objects_.removeObject(objectName);
   }
   
   @Override
   public void setContextDepth(int contextDepth)
   {
      objects_.setContextDepth(contextDepth);

      // if the environment we're about to show is nested, turn off the toolbar
      // commands that act on the global environment
      Boolean commandsEnabled = contextDepth == 0;
      commands_.loadWorkspace().setEnabled(commandsEnabled);
      commands_.saveWorkspace().setEnabled(commandsEnabled);
      commands_.clearWorkspace().setEnabled(commandsEnabled);
      commands_.importDatasetFromFile().setEnabled(commandsEnabled);
      commands_.importDatasetFromURL().setEnabled(commandsEnabled);
      dataImportButton_.setEnabled(commandsEnabled);
   }

   @Override
   public void clearObjects()
   {
      objects_.clearObjects();
   }

   @Override
   public void setEnvironmentName(String environmentName)
   {
      environmentName_.setText(environmentName);
      objects_.setEnvironmentName(environmentName);
   }

   public void editObject(String objectName)
   {
      executeFunctionForObject("fix", objectName);
   }

   public void viewObject(String objectName)
   {
      executeFunctionForObject("View", objectName);
   }

   private void executeFunctionForObject(String function, String objectName)
   {
      String editCode = function + "(" + StringUtil.toRSymbolName(objectName) + ")";
      SendToConsoleEvent event = new SendToConsoleEvent(editCode, true);
      eventBus_.fireEvent(event);
   }


   private ToolbarButton dataImportButton_;
   private Label environmentName_;
   private EnvironmentObjects objects_;
   private Commands commands_;
   private EventBus eventBus_;
}
