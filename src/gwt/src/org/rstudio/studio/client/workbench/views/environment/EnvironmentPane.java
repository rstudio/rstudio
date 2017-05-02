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

import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.DebugFilePosition;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.SearchWidget;
import org.rstudio.core.client.widget.SecondaryToolbar;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.ui.RStudioThemes;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.ImageMenuItem;
import org.rstudio.studio.client.common.icons.StandardIcons;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;
import org.rstudio.studio.client.workbench.views.environment.model.CallFrame;
import org.rstudio.studio.client.workbench.views.environment.model.EnvironmentContextData;
import org.rstudio.studio.client.workbench.views.environment.model.EnvironmentFrame;
import org.rstudio.studio.client.workbench.views.environment.model.EnvironmentServerOperations;
import org.rstudio.studio.client.workbench.views.environment.model.ObjectContents;
import org.rstudio.studio.client.workbench.views.environment.model.RObject;
import org.rstudio.studio.client.workbench.views.environment.view.EnvironmentObjects;
import org.rstudio.studio.client.workbench.views.environment.view.EnvironmentObjectsObserver;
import org.rstudio.studio.client.workbench.views.environment.view.EnvironmentResources;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class EnvironmentPane extends WorkbenchPane 
                             implements EnvironmentPresenter.Display,
                                        EnvironmentObjectsObserver
{
   @Inject
   public EnvironmentPane(Commands commands,
                          EventBus eventBus,
                          GlobalDisplay globalDisplay,
                          EnvironmentServerOperations serverOperations,
                          Session session,
                          UIPrefs prefs)
   {
      super("Environment");
      
      commands_ = commands;
      eventBus_ = eventBus;
      server_ = serverOperations;
      globalDisplay_ = globalDisplay;
      prefs_ = prefs;

      expandedObjects_ = new ArrayList<String>();
      scrollPosition_ = 0;
      isClientStateDirty_ = false;
      environments_ = null;
      EnvironmentContextData environmentState = 
            session.getSessionInfo().getEnvironmentState();
      environmentName_ = environmentState.environmentName();
      environmentIsLocal_ = environmentState.environmentIsLocal();

      EnvironmentPaneResources.INSTANCE.environmentPaneStyle().ensureInjected();
      
      ensureWidget();
   }

   // WorkbenchPane overrides -------------------------------------------------

   @Override
   protected Toolbar createMainToolbar()
   {
      Toolbar toolbar = new Toolbar();
      toolbar.addLeftWidget(commands_.loadWorkspace().createToolbarButton());
      toolbar.addLeftWidget(commands_.saveWorkspace().createToolbarButton());
      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(createImportMenu());
      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(commands_.clearWorkspace().createToolbarButton());
       
      ToolbarPopupMenu menu = new ToolbarPopupMenu();
      menu.addItem(createViewMenuItem(EnvironmentObjects.OBJECT_LIST_VIEW));
      menu.addItem(createViewMenuItem(EnvironmentObjects.OBJECT_GRID_VIEW));
      viewButton_ = new ToolbarButton(
            nameOfViewType(EnvironmentObjects.OBJECT_LIST_VIEW),
            imageOfViewType(EnvironmentObjects.OBJECT_LIST_VIEW),
            menu);
      toolbar.addRightWidget(viewButton_);
      
      toolbar.addRightSeparator();
      toolbar.addRightWidget(commands_.refreshEnvironment().createToolbarButton());
      

      return toolbar;
   }

   @Override
   protected SecondaryToolbar createSecondaryToolbar()
   {
      SecondaryToolbar toolbar = new SecondaryToolbar();
      
      environmentMenu_ = new EnvironmentPopupMenu();
      environmentButton_ = new ToolbarButton(
            friendlyEnvironmentName(),
            imageOfEnvironment(environmentName_, environmentIsLocal_),
            environmentMenu_);
      toolbar.addLeftWidget(environmentButton_);
      
      SearchWidget searchWidget = new SearchWidget(new SuggestOracle() {
         @Override
         public void requestSuggestions(Request request, Callback callback)
         {
            // no suggestions
            callback.onSuggestionsReady(
                  request,
                  new Response(new ArrayList<Suggestion>()));
         }
      });
      searchWidget.addValueChangeHandler(new ValueChangeHandler<String>() {
         @Override
         public void onValueChange(ValueChangeEvent<String> event)
         {
            objects_.setFilterText(event.getValue());
         }
      });

      if (!RStudioThemes.isFlat(prefs_)) {
         searchWidget.getElement().getStyle().setMarginTop(1, Unit.PX);
      }

      toolbar.addRightWidget(searchWidget);

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
      commands_.importDatasetFromFile().setEnabled(commandsEnabled);
      commands_.importDatasetFromURL().setEnabled(commandsEnabled);
      dataImportButton_.setEnabled(commandsEnabled);
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
   public void setEnvironmentName(String environmentName, boolean local)
   {
      environmentName_ = environmentName;
      environmentButton_.setText(friendlyEnvironmentName());
      environmentButton_.setLeftImage(imageOfEnvironment(environmentName, 
                                                         local));
      objects_.setEnvironmentName(friendlyEnvironmentName());
      if (environmentName.equals(".GlobalEnv"))
         commands_.clearWorkspace().setEnabled(true); 
      else
         commands_.clearWorkspace().setEnabled(false);
   }

   @Override
   public void setCallFrames(JsArray<CallFrame> frameList, boolean autoSize)
   {
      objects_.setCallFrames(frameList, autoSize);
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
   public List<String> getSelectedObjects()
   {
      return objects_.getSelectedObjects();
   }

   @Override
   public void clearSelection()
   {
      objects_.clearSelection();
   }

   @Override
   public void changeContextDepth(int newDepth)
   {
      server_.setContextDepth(newDepth, new ServerRequestCallback<Void>()
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
   public void setBrowserRange(DebugFilePosition range)
   {
      objects_.updateLineNumber(range.getLine());
   }

   @Override
   public void setObjectDisplayType(int type)
   {
      viewButton_.setText(nameOfViewType(type));
      viewButton_.setLeftImage(imageOfViewType(type));
      objects_.setObjectDisplay(type);
   }
   
   @Override
   public int getObjectDisplayType()
   {
      return objects_.getObjectDisplay();
   }
   
   @Override
   public int getSortColumn()
   {
      return objects_.getSortColumn();
   }

   @Override
   public boolean getAscendingSort()
   {
      return objects_.getAscendingSort();
   }

   @Override
   public void setSort(int sortColumn, boolean sortAscending)
   {
      objects_.setSort(sortColumn, sortAscending);
   }

   @Override
   public void setViewDirty()
   {
      isClientStateDirty_ = true;
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
   
   @Override
   public boolean getShowInternalFunctions()
   {
      return prefs_.showInternalFunctionsInTraceback().getValue();
   }

   @Override
   public void setShowInternalFunctions(boolean show)
   {
      prefs_.showInternalFunctionsInTraceback().setProjectValue(show);
   }

   public void fillObjectContents(final RObject object, 
                                  final Operation onCompleted)
   {
      server_.getObjectContents(object.getName(), 
            new ServerRequestCallback<ObjectContents>()
      {
         @Override
         public void onResponseReceived(ObjectContents contents)
         {
            object.setDeferredContents(contents.getContents());
            onCompleted.execute();
         }

         @Override
         public void onError(ServerError error)
         {
            onCompleted.execute();
         }
      });
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
      menu.setAutoOpen(true);
   
      menu.addItem(commands_.importDatasetFromFile().createMenuItem(false));
      menu.addItem(commands_.importDatasetFromURL().createMenuItem(false));
      menu.addItem(commands_.importDatasetFromCsvUsingBase().createMenuItem(false));
      menu.addItem(commands_.importDatasetFromCsvUsingReadr().createMenuItem(false));
      menu.addSeparator();
      menu.addItem(commands_.importDatasetFromXLS().createMenuItem(false));
      menu.addSeparator();
      menu.addItem(commands_.importDatasetFromSAV().createMenuItem(false));
      menu.addItem(commands_.importDatasetFromSAS().createMenuItem(false));
      menu.addItem(commands_.importDatasetFromStata().createMenuItem(false));
      menu.addSeparator();
      menu.addItem(commands_.importDatasetFromXML().createMenuItem(false));
      menu.addItem(commands_.importDatasetFromJSON().createMenuItem(false));
      menu.addSeparator();
      menu.addItem(commands_.importDatasetFromJDBC().createMenuItem(false));
      menu.addItem(commands_.importDatasetFromODBC().createMenuItem(false));
      menu.addSeparator();
      menu.addItem(commands_.importDatasetFromMongo().createMenuItem(false));
      
      dataImportButton_ = new ToolbarButton(
              "Import Dataset",
              new ImageResource2x(StandardIcons.INSTANCE.import_dataset2x()),
              menu);
      return dataImportButton_;

   }
   
   private String friendlyEnvironmentName()
   {
      return friendlyNameOfEnvironment(environmentName_);
   }
   
   private String friendlyNameOfEnvironment(String name)
   {
      if (name.equals(".GlobalEnv") || name.equals("R_GlobalEnv"))
         return GLOBAL_ENVIRONMENT_NAME;
      else if (name.equals("base"))
         return "package:base";
      else 
         return name;
   }
   
   private ImageResource imageOfEnvironment(String name, boolean local)
   {
      if (name.endsWith("()"))
         return new ImageResource2x(EnvironmentResources.INSTANCE.functionEnvironment2x());
      else if (name.equals(".GlobalEnv") || name.equals("R_GlobalEnv"))
         return new ImageResource2x(EnvironmentResources.INSTANCE.globalEnvironment2x());
      else if (name.startsWith("package:") ||
               name.equals("base") || 
               local)
         return new ImageResource2x(EnvironmentResources.INSTANCE.packageEnvironment2x());
      else 
         return new ImageResource2x(EnvironmentResources.INSTANCE.attachedEnvironment2x());
   }
   
   private void setEnvironments(JsArray<EnvironmentFrame> environments)
   {
      environments_ = environments;
      rebuildEnvironmentMenu();
   }
   
   private void rebuildEnvironmentMenu()
   {
      environmentMenu_.clearItems();
      if (environments_ == null)
      {
         return;
      }
      for (int i = 0; i < environments_.length(); i++)
      {
         final EnvironmentFrame frame = environments_.get(i);
         ImageResource img = imageOfEnvironment(frame.getName(), 
                                                frame.isLocal());
         environmentMenu_.addItem(ImageMenuItem.create(img, 
                  friendlyNameOfEnvironment(frame.getName()), 
                  new Scheduler.ScheduledCommand()
                  {
                     @Override
                     public void execute()
                     {
                        loadEnvironmentFrame(frame);
                     }
                  }, 2));
      }
   }
   
   // Called to load a new environment into the environment pane. 
   private void loadEnvironmentFrame(final EnvironmentFrame frame)
   {
      ServerRequestCallback<Void> callback = new ServerRequestCallback<Void>()
      {
         @Override
         public void onResponseReceived(Void v)
         {
            setEnvironmentName(frame.getName(), frame.isLocal());
         }

         @Override
         public void onError(ServerError error)
         {
            
         }
      };
      // If the frame's an active call frame, set it by its index 
      if (frame.getFrame() > 0)
         server_.setEnvironmentFrame(frame.getFrame(), callback);
      // Otherwise, set it by its name
      else
         server_.setEnvironment(frame.getName(), callback);
   }
   
   private String nameOfViewType(int type)
   {
      if (type == EnvironmentObjects.OBJECT_LIST_VIEW)
         return "List";
      else if (type == EnvironmentObjects.OBJECT_GRID_VIEW)
         return "Grid";
      return "";
   }
   
   private ImageResource imageOfViewType(int type)
   {
      if (type == EnvironmentObjects.OBJECT_LIST_VIEW)
         return new ImageResource2x(EnvironmentResources.INSTANCE.objectListView2x());
      else if (type == EnvironmentObjects.OBJECT_GRID_VIEW)
         return new ImageResource2x(EnvironmentResources.INSTANCE.objectGridView2x());
      return null;
   }
   
   private MenuItem createViewMenuItem(final int type)
   {
      return ImageMenuItem.create(
            imageOfViewType(type),
            nameOfViewType(type),
            new Scheduler.ScheduledCommand()
            {
               @Override
               public void execute()
               {
                  setObjectDisplayType(type);
               }
            }, -1);
   }
   
   // An extension of the toolbar popup menu that gets environment names from
   // the server when the menu is invoked. 
   private class EnvironmentPopupMenu extends ToolbarPopupMenu
   {
      @Override
      public void getDynamicPopupMenu 
         (final ToolbarPopupMenu.DynamicPopupMenuCallback callback)
      {
         server_.getEnvironmentNames(
               new ServerRequestCallback<JsArray<EnvironmentFrame>>()
               {
                  @Override
                  public void onResponseReceived(JsArray<EnvironmentFrame> response)
                  {
                     setEnvironments(response);
                     callback.onPopupMenu(environmentMenu_);
                  }
         
                  @Override
                  public void onError(ServerError error)
                  {
                     // Just live with a stale list.
                     callback.onPopupMenu(environmentMenu_);
                  }
               });
      }
   }
   
   public static final String GLOBAL_ENVIRONMENT_NAME = "Global Environment";

   private final Commands commands_;
   private final EventBus eventBus_;
   private final GlobalDisplay globalDisplay_;
   private final EnvironmentServerOperations server_;
   private final UIPrefs prefs_;

   private ToolbarButton dataImportButton_;
   private ToolbarPopupMenu environmentMenu_;
   private ToolbarButton environmentButton_;
   private ToolbarButton viewButton_;
   private EnvironmentObjects objects_;

   private ArrayList<String> expandedObjects_;
   private int scrollPosition_;
   private boolean isClientStateDirty_;
   private JsArray<EnvironmentFrame> environments_;
   private String environmentName_;
   private boolean environmentIsLocal_;
}
