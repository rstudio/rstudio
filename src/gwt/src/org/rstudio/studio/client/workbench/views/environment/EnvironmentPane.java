/*
 * EnvironmentPane.java
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

package org.rstudio.studio.client.workbench.views.environment;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.DebugFilePosition;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.MonitoringMenuItem;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.SearchWidget;
import org.rstudio.core.client.widget.SecondaryToolbar;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.core.client.widget.ToolbarMenuButton;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.SessionSerializationEvent;
import org.rstudio.studio.client.application.events.SuspendAndRestartEvent;
import org.rstudio.studio.client.application.model.SessionSerializationAction;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.ImageMenuItem;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.Value;
import org.rstudio.studio.client.common.dependencies.DependencyManager;
import org.rstudio.studio.client.common.icons.StandardIcons;
import org.rstudio.studio.client.events.ReticulateEvent;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;
import org.rstudio.studio.client.workbench.views.environment.events.MemoryUsageChangedEvent;
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
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.SuggestOracle;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.rstudio.studio.client.workbench.views.environment.view.MemUsageWidget;

public class EnvironmentPane extends WorkbenchPane
                             implements EnvironmentPresenter.Display,
                                        EnvironmentObjectsObserver,
                                        SessionInitEvent.Handler,
                                        ReticulateEvent.Handler,
                                        SuspendAndRestartEvent.Handler,
                                        MemoryUsageChangedEvent.Handler,
                                        SessionSerializationEvent.Handler
{
   @Inject
   public EnvironmentPane(Commands commands,
                          EventBus events,
                          GlobalDisplay globalDisplay,
                          EnvironmentServerOperations serverOperations,
                          Session session,
                          UserPrefs prefs,
                          DependencyManager dependencyManager)
   {
      super("Environment", events);

      commands_ = commands;
      server_ = serverOperations;
      globalDisplay_ = globalDisplay;
      session_ = session;
      prefs_ = prefs;
      dependencyManager_ = dependencyManager;

      expandedObjects_ = new ArrayList<>();
      scrollPosition_ = 0;
      isClientStateDirty_ = false;
      environments_ = null;

      EnvironmentContextData environmentState = session.getSessionInfo().getEnvironmentState();
      environmentName_ = environmentState.environmentName();
      environmentIsLocal_ = environmentState.environmentIsLocal();
      environmentMonitoring_ = new Value<>(environmentState.environmentMonitoring());

      EnvironmentPaneResources.INSTANCE.environmentPaneStyle().ensureInjected();

      refreshTimer_ = new Timer()
      {
         @Override
         public void run()
         {
            commands_.refreshEnvironment().execute();
         }
      };

      events.addHandler(SessionInitEvent.TYPE, this);
      events.addHandler(ReticulateEvent.TYPE, this);
      events.addHandler(SuspendAndRestartEvent.TYPE, this);
      events.addHandler(MemoryUsageChangedEvent.TYPE, this);
      events.addHandler(SessionSerializationEvent.TYPE, this);

      // Sync the state of the memory usage toggle command to the preference
      prefs.showMemoryUsage().bind(val -> commands.toggleShowMemoryUsage().setChecked(val));

      ensureWidget();
   }

   // WorkbenchPane overrides -------------------------------------------------

   @Override
   protected Toolbar createMainToolbar()
   {
      Toolbar toolbar = new Toolbar(constants_.environmentTab());
      toolbar.addLeftWidget(commands_.loadWorkspace().createToolbarButton());
      toolbar.addLeftWidget(commands_.saveWorkspace().createToolbarButton());
      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(createImportMenu());
      toolbar.addLeftSeparator();
      memUsage_ = new MemUsageWidget(session_.getSessionInfo().getMemoryUsage(), prefs_);
      toolbar.addLeftWidget(memUsage_);
      toolbar.addLeftSeparator();
      toolbar.addLeftWidget(commands_.clearWorkspace().createToolbarButton());

      ToolbarPopupMenu menu = new ToolbarPopupMenu();
      menu.addItem(createViewMenuItem(EnvironmentObjects.OBJECT_LIST_VIEW));
      menu.addItem(createViewMenuItem(EnvironmentObjects.OBJECT_GRID_VIEW));
      viewButton_ = new ToolbarMenuButton(
            nameOfViewType(EnvironmentObjects.OBJECT_LIST_VIEW),
            ToolbarButton.NoTitle,
            imageOfViewType(EnvironmentObjects.OBJECT_LIST_VIEW),
            menu);
      ElementIds.assignElementId(viewButton_, ElementIds.MB_OBJECT_LIST_VIEW);
      toolbar.addRightWidget(viewButton_);

      toolbar.addRightSeparator();

      refreshButton_ = commands_.refreshEnvironment().createToolbarButton();
      refreshButton_.addStyleName(ThemeStyles.INSTANCE.refreshToolbarButton());
      toolbar.addRightWidget(refreshButton_);

      ToolbarPopupMenu refreshMenu = new ToolbarPopupMenu();
      refreshMenu.addItem(new EnvironmentMonitoringMenuItem(true));
      refreshMenu.addItem(new EnvironmentMonitoringMenuItem(false));
      refreshMenu.addSeparator();

      refreshMenu.addItem(new MenuItem(
            AppCommand.formatMenuLabel(null, constants_.refreshNow(), null),
            true, // as HTML
            () -> commands_.refreshEnvironment().execute()));
      ToolbarMenuButton refreshMenuBtn =
         new ToolbarMenuButton(ToolbarButton.NoText, constants_.refreshOptions(), refreshMenu, false);
      ElementIds.assignElementId(refreshMenuBtn, ElementIds.MB_REFRESH_OPTS);
      toolbar.addRightWidget(refreshMenuBtn);

      return toolbar;
   }

   @Override
   protected SecondaryToolbar createSecondaryToolbar()
   {
      initSecondaryToolbar();
      return secondaryToolbar_;
   }

   private void initSecondaryToolbar()
   {
      SecondaryToolbar toolbar = new SecondaryToolbar(constants_.environmentTabSecond());

      languageMenu_ = new ToolbarPopupMenu();
      toolbar.addHandler(new ResizeHandler()
      {
         @Override
         public void onResize(ResizeEvent event)
         {
            manageToolbarSizes(event.getWidth());
         }
      }, ResizeEvent.getType());

      MenuItem rMenuItem = new MenuItem("R", () -> setActiveLanguage("R", true));
      languageMenu_.addItem(rMenuItem);

      MenuItem pyMenuItem = new MenuItem("Python", () ->
      {
         dependencyManager_.withReticulate(
               constants_.viewingPythonObjectsCapitalized(),
               constants_.viewingPythonObjects(),
               () -> setActiveLanguage("Python", true));
      });
      languageMenu_.addItem(pyMenuItem);

      languageButton_ = new ToolbarMenuButton(
            "R",
            ToolbarButton.NoTitle,
            (ImageResource) null,
            languageMenu_);
      ElementIds.assignElementId(languageButton_, ElementIds.MB_ENV_LANGUAGE);

      // nudge language selector up to baseline align with environment selector
      languageButton_.getElement().getStyle().setMarginTop(-4, Unit.PX);

      toolbar.addLeftWidget(languageButton_);
      toolbar.addLeftSeparator();

      environmentMenu_ = new EnvironmentPopupMenu();
      environmentButton_ = new ToolbarMenuButton(
            friendlyEnvironmentName(),
            ToolbarButton.NoTitle,
            imageOfEnvironment(environmentName_, environmentIsLocal_),
            environmentMenu_);
      ElementIds.assignElementId(environmentButton_, ElementIds.MB_ENVIRONMENT_LIST);
      toolbar.addLeftWidget(environmentButton_);

      ThemeStyles styles = ThemeStyles.INSTANCE;
      toolbar.getWrapper().addStyleName(styles.tallerToolbarWrapper());

      SearchWidget searchWidget = new SearchWidget(constants_.searchEnvironment(), new SuggestOracle() {
         @Override
         public void requestSuggestions(Request request, Callback callback)
         {
            // no suggestions
            callback.onSuggestionsReady(
                  request,
                  new Response(new ArrayList<>()));
         }
      });

      ElementIds.assignElementId(searchWidget, ElementIds.SW_ENVIRONMENT);
      searchWidget.addValueChangeHandler(new ValueChangeHandler<String>() {
         @Override
         public void onValueChange(ValueChangeEvent<String> event)
         {
            objects_.setFilterText(event.getValue());
         }
      });

      toolbar.addRightWidget(searchWidget);

      secondaryToolbar_ = toolbar;
   }

   @Override
   protected Widget createMainWidget()
   {
      objects_ = new EnvironmentObjects(this);

      EnvironmentContextData data = session_.getSessionInfo().getEnvironmentState();
      if (StringUtil.equals(data.language(), "Python"))
      {
         setPythonEnabled(true);
         setActiveLanguage("Python", false);
      }

      return objects_;
   }

   // EnvironmentPresenter.Display implementation ------------------------------

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
      environmentButton_.setLeftImage(imageOfEnvironment(environmentName, local));

      String friendlyName = friendlyEnvironmentName();
      objects_.setEnvironmentName(friendlyName);
      if (friendlyName.equals(GLOBAL_ENVIRONMENT_NAME))
         commands_.clearWorkspace().setEnabled(true);
      else
         commands_.clearWorkspace().setEnabled(false);
   }

   @Override
   public void setEnvironmentMonitoring(boolean monitoring)
   {
      if (monitoring == environmentMonitoring_.getValue())
         return;
      environmentMonitoring_.setValue(monitoring, true);
   }

   @Override
   public boolean environmentMonitoring()
   {
      return environmentMonitoring_.getValue();
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
      server_.setContextDepth(
            newDepth,
            new SimpleRequestCallback<>(constants_.errorOpeningCallFrame()));
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
      // Adjust the List/Grid text label, but only if that text label is shown (it can be hidden
      // when the Environment pane is very narrow)
      viewButton_.setText(viewButton_.hasLabel(), nameOfViewType(type));
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

   // EnvironmentObjects.Observer implementation -------------------------------

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

   public void viewObject(String action, String objectName)
   {
      executeFunctionForObject(action, objectName);
   }

   @Override
   public boolean getShowInternalFunctions()
   {
      return prefs_.showInternalFunctions().getValue();
   }

   @Override
   public void setShowInternalFunctions(boolean show)
   {
      prefs_.showInternalFunctions().setProjectValue(show);
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

      SendToConsoleEvent event = new SendToConsoleEvent(editCode, activeLanguage_, true);
      events_.fireEvent(event);
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
      menu.addItem(commands_.importDatasetFromArrow().createMenuItem(false));

      dataImportButton_ = new ToolbarMenuButton(
              constants_.importDataset(),
              ToolbarButton.NoTitle,
              new ImageResource2x(StandardIcons.INSTANCE.import_dataset2x()),
              menu);

      ElementIds.assignElementId(dataImportButton_, ElementIds.MB_IMPORT_DATASET);
      return dataImportButton_;

   }

   private String friendlyEnvironmentName()
   {
      return friendlyNameOfEnvironment(environmentName_);
   }

   private String friendlyNameOfEnvironment(String name)
   {
      boolean isGlobalEnv =
            StringUtil.equals(name, ".GlobalEnv") ||
            StringUtil.equals(name, "R_GlobalEnv");

      if (isGlobalEnv)
         return GLOBAL_ENVIRONMENT_NAME;

      boolean isBase =
            StringUtil.equals(name, "base");

      if (isBase)
         return "package:base";

      boolean isPythonMain =
            StringUtil.equals(name, "main") ||
            StringUtil.equals(name, "__main__");

      if (isPythonMain)
         return "Main Module";

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

      if (frame.getFrame() > 0)
      {
         // If the frame's an active call frame, set it by its index
         server_.setEnvironmentFrame(frame.getFrame(), callback);
      }
      else
      {
         // Otherwise, set it by its name
         setEnvironmentName(frame.getName(), frame.isLocal());
         server_.setEnvironment(frame.getName(), callback);
      }
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

   @Override
   public void onSessionInit(SessionInitEvent sie)
   {
      boolean initialized = session_.getSessionInfo().getPythonInitialized();
      setPythonEnabled(initialized);

      memUsage_.setMemoryUsage(session_.getSessionInfo().getMemoryUsage());
   }

   @Override
   public void onReticulate(ReticulateEvent event)
   {
      String type = event.getType();

      if (StringUtil.equals(type, ReticulateEvent.TYPE_PYTHON_INITIALIZED))
      {
         setPythonEnabled(true);
      }
      else if (StringUtil.equals(type, ReticulateEvent.TYPE_REPL_INITIALIZED))
      {
         setActiveLanguage("Python", true);
      }
      else if (StringUtil.equals(type, ReticulateEvent.TYPE_REPL_TEARDOWN))
      {
         setActiveLanguage("R", true);
      }
   }

   @Override
   public void onSuspendAndRestart(SuspendAndRestartEvent event)
   {
      setActiveLanguage("R", false);
   }

   @Override
   public void onMemoryUsageChanged(MemoryUsageChangedEvent event)
   {
      memUsage_.setMemoryUsage(event.getMemoryUsage());
   }

   @Override
   public void onSessionSerialization(SessionSerializationEvent event)
   {
      if (event.getAction().getType() == SessionSerializationAction.SUSPEND_SESSION)
      {
         // Show suspension hint in memory usage widget
         memUsage_.setSuspended(true);
      }
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
               activeLanguage_,
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

   private class EnvironmentMonitoringMenuItem extends MonitoringMenuItem
   {
      public EnvironmentMonitoringMenuItem(boolean monitoredValue)
      {
         super(
               refreshButton_,
               environmentMonitoring_,
               environmentMonitoring_.getValue(),
               monitoredValue);
      }

      @Override
      public void onInvoked()
      {
         server_.setEnvironmentMonitoring(monitoredValue_, new ServerRequestCallback<Void>()
         {
            @Override
            public void onResponseReceived(Void v)
            {
               environmentMonitoring_.setValue(monitoredValue_, true);
            }

            @Override
            public void onError(ServerError error)
            {
               globalDisplay_.showErrorMessage(
                     constants_.couldNotChangeMonitoringState(),
                     error.getMessage());
            }
         });

      }
   }

   public void setPythonEnabled(boolean enabled)
   {
      languageButton_.setEnabled(enabled);
      languageButton_.setVisible(enabled);
      secondaryToolbar_.manageSeparators();
   }

   // NOTE: 'syncWithSession = false' should only be used
   // for cases where the front-end is synchronizing based
   // on the state of the session; 'syncWithSession = true'
   // is normally done to reflect a user action that should
   // then update the session state as well
   public void setActiveLanguage(String language,
                                 boolean syncWithSession)
   {
      if (!syncWithSession)
      {
         setActiveLanguageImpl(language);
         return;
      }

      server_.environmentSetLanguage(
            language,
            new VoidServerRequestCallback()
            {
               @Override
               public void onResponseReceived(Void response)
               {
                  setActiveLanguageImpl(language);
               }
            });
   }

   private void setActiveLanguageImpl(String language)
   {
      languageButton_.setText(language);
      activeLanguage_ = language;

      if (StringUtil.equals(language, "R"))
      {
         setEnvironmentName("R_GlobalEnv", false);
      }
      else if (StringUtil.equals(language, "Python"))
      {
         setEnvironmentName("__main__", false);
      }
      else
      {
         Debug.logWarning("Unknown language '" + language + "'");
      }

      Scheduler.get().scheduleDeferred(() -> commands_.refreshEnvironment().execute());
   }

   /**
    * Manages the size of the Environment pane's toolbar, reducing the width of elements
    * so all the controls are visible and usable even at narrow widths.
    *
    * @param width The new width of the Environment pane
    */
   private void manageToolbarSizes(int width)
   {
      // At startup, we get a resize event with 0 width; ignore this.
      if (width == 0)
      {
         return;
      }

      // The View button has text by default
      viewButton_.setText(true, viewButton_.getText());

      if (width > 400)
      {
         // Full width: show full label for data import
         dataImportButton_.setText(constants_.importDataset());
      }
      else if (width > 350)
      {
         // Reduced width: shorten label
         dataImportButton_.setText(constants_.importCapitalized());
      }
      else if (width > 325)
      {
         // Even more reduced width: shorten label more
         dataImportButton_.setText("");
      }
      else
      {
         // Extremely narrow: no labels at all, just buttons
         dataImportButton_.setText("");
         viewButton_.setText(false, viewButton_.getText());
      }
   }

   public String getActiveLanguage()
   {
      return activeLanguage_;
   }

   public String getMonitoredEnvironment()
   {
      return environmentName_;
   }

   public static final String GLOBAL_ENVIRONMENT_NAME = "Global Environment";

   private final Commands commands_;
   private final GlobalDisplay globalDisplay_;
   private final EnvironmentServerOperations server_;
   private final Session session_;
   private final UserPrefs prefs_;
   private final DependencyManager dependencyManager_;
   private final Value<Boolean> environmentMonitoring_;
   @SuppressWarnings("unused")
   private final Timer refreshTimer_;

   private SecondaryToolbar secondaryToolbar_;
   private ToolbarMenuButton languageButton_;
   private ToolbarPopupMenu languageMenu_;
   private ToolbarMenuButton dataImportButton_;
   private ToolbarPopupMenu environmentMenu_;
   private ToolbarMenuButton environmentButton_;
   private ToolbarMenuButton viewButton_;
   private ToolbarButton refreshButton_;
   private EnvironmentObjects objects_;
   private MemUsageWidget memUsage_;

   private ArrayList<String> expandedObjects_;
   private int scrollPosition_;
   private boolean isClientStateDirty_;
   private JsArray<EnvironmentFrame> environments_;
   private String environmentName_;
   private boolean environmentIsLocal_;
   private String activeLanguage_ = "R";
   private static final ViewEnvironmentConstants constants_ = GWT.create(ViewEnvironmentConstants.class);
}
