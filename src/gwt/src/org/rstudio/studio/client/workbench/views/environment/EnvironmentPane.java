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

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.icons.StandardIcons;
import org.rstudio.studio.client.server.*;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;
import org.rstudio.studio.client.workbench.views.environment.model.CallFrame;
import org.rstudio.studio.client.workbench.views.environment.model.EnvironmentServerOperations;
import org.rstudio.studio.client.workbench.views.environment.model.RObject;
import org.rstudio.studio.client.workbench.views.environment.view.EnvironmentObjects;

import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

import java.util.ArrayList;

public class EnvironmentPane extends WorkbenchPane 
                             implements EnvironmentPresenter.Display,
                                        EnvironmentObjects.Observer
{
   @Inject
   public EnvironmentPane(Commands commands,
                          EventBus eventBus,
                          GlobalDisplay globalDisplay,
                          EnvironmentServerOperations serverOperations)
   {
      super("Environment");
      
      commands_ = commands;
      eventBus_ = eventBus;
      server_ = serverOperations;
      globalDisplay_ = globalDisplay;

      expandedObjects_ = new ArrayList<String>();
      scrollPosition_ = 0;
      isClientStateDirty_ = false;

      EnvironmentPaneResources.INSTANCE.environmentPaneStyle().ensureInjected();

      ensureWidget();
   }

   // WorkbenchPane overrides -------------------------------------------------

   @Override
   protected Toolbar createMainToolbar()
   {
      environmentName_ = new Label(GLOBAL_ENVIRONMENT_NAME);
      environmentName_.setStyleName(
              EnvironmentPaneResources.INSTANCE
                      .environmentPaneStyle()
                      .environmentNameLabel());
      functionIndicator_ = new Image(
            StandardIcons.INSTANCE.function());
      functionIndicator_.setVisible(false);

      Toolbar toolbar = new Toolbar();
      toolbar.addLeftWidget(commands_.loadWorkspace().createToolbarButton());
      toolbar.addLeftWidget(commands_.saveWorkspace().createToolbarButton());
      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(createImportMenu());
      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(commands_.clearWorkspace().createToolbarButton());
      toolbar.addRightWidget(functionIndicator_);
      toolbar.addRightWidget(environmentName_);
      toolbar.addRightWidget(commands_.refreshEnvironment().createToolbarButton());

      return toolbar;
   }

   @Override
   protected Widget createMainWidget()
   {
      objects_ = new EnvironmentObjects(this);
      return objects_;
   }

   // EnviromentPresenter.Display implementation ------------------------------

   @Override
   public void addObject(RObject object)
   {
      objects_.addObject(object);
   }

   @Override
   public void addObjects(JsArray<RObject> objects)
   {
      objects_.addObjects(objects);
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

      functionIndicator_.setVisible(contextDepth > 0);
   }

   @Override
   public void clearObjects()
   {
      objects_.clearObjects();
      expandedObjects_.clear();
      scrollPosition_ = 0;
      isClientStateDirty_ = true;
   }

   @Override
   public void setEnvironmentName(String environmentName)
   {
      environmentName_.setText(environmentName);
      objects_.setEnvironmentName(environmentName);
   }

   @Override
   public void setCallFrames(JsArray<CallFrame> frameList)
   {
      objects_.setCallFrames(frameList);
   }

   @Override
   public int getScrollPosition()
   {
      return scrollPosition_;
   }

   @Override
   public void setScrollPosition(int scrollPosition)
   {
      objects_.setScrollPosition(scrollPosition);
   }

   @Override
   public void setExpandedObjects(JsArrayString objects)
   {
      objects_.setExpandedObjects(objects);
      expandedObjects_.clear();
      for (int idx = 0; idx < objects.length(); idx++)
      {
         expandedObjects_.add(objects.get(idx));
      }
   }

   @Override
   public String[] getExpandedObjects()
   {
      return expandedObjects_.toArray(new String[0]);
   }

   @Override
   public void changeContextDepth(int newDepth)
   {
      server_.setContextDepth(newDepth, new ServerRequestCallback<org.rstudio.studio.client.server.Void>()
      {
         @Override
         public void onError(ServerError error)
         {
            globalDisplay_.showErrorMessage("Error opening call frame", error.getUserMessage());
         }
      });
   }

   public boolean clientStateDirty()
   {
      return isClientStateDirty_;
   }

   public void setClientStateClean()
   {
      isClientStateDirty_ = false;
   }

   @Override
   public void resize()
   {
      objects_.onResize();
   }

   @Override
   public void setBrowserLine(int newLineNumber)
   {
      objects_.updateLineNumber(newLineNumber);
   }

   // EnviromentObjects.Observer implementation -------------------------------

   public void setPersistedScrollPosition(int scrollPosition)
   {
      scrollPosition_ = scrollPosition;
      isClientStateDirty_ = true;
   }

   public void setObjectExpanded(String objectName)
   {
      expandedObjects_.add(objectName);
      isClientStateDirty_ = true;
   }

   public void setObjectCollapsed(String objectName)
   {
      expandedObjects_.remove(objectName);
      isClientStateDirty_ = true;
   }

   public void viewObject(String objectName)
   {
      executeFunctionForObject("View", objectName);
   }

   // Private methods ---------------------------------------------------------

   private void executeFunctionForObject(String function, String objectName)
   {
      String editCode =
              function + "(" + StringUtil.toRSymbolName(objectName) + ")";
      SendToConsoleEvent event = new SendToConsoleEvent(editCode, true);
      eventBus_.fireEvent(event);
   }

   private Widget createImportMenu()
   {
      ToolbarPopupMenu menu = new ToolbarPopupMenu();
      menu.addItem(commands_.importDatasetFromFile().createMenuItem(false));
      menu.addItem(commands_.importDatasetFromURL().createMenuItem(false));
      dataImportButton_ = new ToolbarButton(
              "Import Dataset",
              StandardIcons.INSTANCE.import_dataset(),
              menu);
      return dataImportButton_;

   }

   public final static String GLOBAL_ENVIRONMENT_NAME = "Global Environment";

   private ToolbarButton dataImportButton_;
   private Label environmentName_;
   private Image functionIndicator_;
   private EnvironmentObjects objects_;
   private Commands commands_;
   private EventBus eventBus_;
   private GlobalDisplay globalDisplay_;
   private EnvironmentServerOperations server_;
   private ArrayList<String> expandedObjects_;
   private int scrollPosition_;
   private boolean isClientStateDirty_;
}
