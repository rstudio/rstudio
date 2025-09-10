/*
 * PaneManager.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.ui;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.JsArrayUtil;
import org.rstudio.core.client.MathUtil;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.Triad;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.events.ManageLayoutCommandsEvent;
import org.rstudio.core.client.events.WindowEnsureVisibleEvent;
import org.rstudio.core.client.events.WindowStateChangeEvent;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.core.client.layout.DualWindowLayoutPanel;
import org.rstudio.core.client.layout.LogicalWindow;
import org.rstudio.core.client.layout.WindowState;
import org.rstudio.core.client.theme.MinimizedModuleTabLayoutPanel;
import org.rstudio.core.client.theme.MinimizedWindowFrame;
import org.rstudio.core.client.theme.PrimaryWindowFrame;
import org.rstudio.core.client.theme.WindowFrame;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.ZoomPaneEvent;
import org.rstudio.studio.client.workbench.model.ClientState;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.helper.IntStateValue;
import org.rstudio.studio.client.workbench.model.helper.JSObjectStateValue;
import org.rstudio.studio.client.workbench.prefs.events.UserPrefsChangedEvent;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefsAccessor;
import org.rstudio.studio.client.workbench.prefs.views.PaneLayoutPreferencesPane;
import org.rstudio.studio.client.workbench.views.console.ConsoleConstants;
import org.rstudio.studio.client.workbench.views.console.ConsoleInterpreterVersion;
import org.rstudio.studio.client.workbench.views.console.ConsolePane;
import org.rstudio.studio.client.workbench.views.output.find.FindOutputTab;
import org.rstudio.studio.client.workbench.views.output.markers.MarkersOutputTab;
import org.rstudio.studio.client.workbench.views.source.Source;
import org.rstudio.studio.client.workbench.views.source.SourceColumn;
import org.rstudio.studio.client.workbench.views.source.SourceColumnManager;
import org.rstudio.studio.client.workbench.views.source.SourceColumnManager.ColumnName;
import org.rstudio.studio.client.workbench.views.source.SourceWindowManager;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTarget;
import org.rstudio.studio.client.workbench.views.source.model.SourceDocument;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.Element;
import com.google.gwt.layout.client.Layout.AnimationCallback;
import com.google.gwt.layout.client.Layout.Layer;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.SplitterResizedEvent;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

import elemental2.dom.DomGlobal;

/*
 * TODO: Push client state when selected tab or layout changes
 */

@Singleton
public class PaneManager
{
   public interface Binder extends CommandBinder<Commands, PaneManager> {}

   public enum Tab {
      History, Files, Plots, Packages, Help, VCS, Tutorial, Build, Connections,
      Presentation, Presentations, Environment, Viewer, Source, Console, SourceColumn
   }

   public static final String LEFT_COLUMN = "left";
   public static final String RIGHT_COLUMN = "right";
   public static final String SIDEBAR_COLUMN = "sidebar";

   class SelectedTabStateValue extends IntStateValue
   {
      SelectedTabStateValue(String name,
                            WorkbenchTabPanel tabPanel)
      {
         super("workbench-pane", name, ClientState.PROJECT_PERSISTENT,
               session_.getSessionInfo().getClientState(), true);
         tabPanel_ = tabPanel;
         finishInit(session_.getSessionInfo().getClientState());
      }

      @Override
      protected void onInit(Integer value)
      {
         if (value != null && tabPanel_.getWidgetCount() > 0)
            tabPanel_.selectTab(value);
      }

      @Override
      protected Integer getValue() { return tabPanel_.getSelectedIndex(); }

      private final WorkbenchTabPanel tabPanel_;
   }

   private class ZoomedTabStateValue extends JSObjectStateValue
   {
      public ZoomedTabStateValue()
      {
         super("workbench-pane", "TabZoom", ClientState.PROJECT_PERSISTENT,
               session_.getSessionInfo().getClientState(), true);
         finishInit(session_.getSessionInfo().getClientState());
      }

      @Override
      protected void onInit(final JsObject value)
      {
         if (value == null)
            return;

         if (!value.hasKey(MAXIMIZED_TAB_KEY) || !value.hasKey(WIDGET_SIZE_KEY))
            return;

         // Time-out action just to ensure all client state is ready
         new Timer()
         {
            @Override
            public void run()
            {
               String tabString = value.getString(MAXIMIZED_TAB_KEY);
               double widgetSize = value.getDouble(WIDGET_SIZE_KEY);

               maximizedTab_ = Tab.valueOf(tabString);
               maximizedWindow_ = getWindowForTab(maximizedTab_);
               widgetSizePriorToZoom_ = widgetSize;
               fullyMaximizeWindow(maximizedWindow_, maximizedTab_);
               manageLayoutCommands();
            }
         }.schedule(200);
      }

      @Override
      protected boolean hasChanged()
      {
         if (lastValue_ == null)
            return true;

         JsObject oldValue = lastValue_;
         JsObject newValue = getValue();

         boolean oldHasKey = oldValue.hasKey(MAXIMIZED_TAB_KEY);
         boolean newHasKey = newValue.hasKey(MAXIMIZED_TAB_KEY);

         if (oldHasKey && newHasKey)
            return !StringUtil.equals(oldValue.getString(MAXIMIZED_TAB_KEY),
                   newValue.getString(MAXIMIZED_TAB_KEY));

         return oldHasKey != newHasKey;
      }

      @Override
      protected JsObject getValue()
      {
         final JsObject object = JsObject.createJsObject();
         if (maximizedTab_ != null)
            object.setString(MAXIMIZED_TAB_KEY, maximizedTab_.toString());

         if (widgetSizePriorToZoom_ >= 0)
            object.setDouble(WIDGET_SIZE_KEY, widgetSizePriorToZoom_);

         lastValue_ = object;
         return object;
      }

      private static final String MAXIMIZED_TAB_KEY = "MaximizedTab";
      private static final String WIDGET_SIZE_KEY = "WidgetSize";

      private JsObject lastValue_ = null;

   }

   private LogicalWindow getWindowForTab(Tab tab)
   {
      switch (tab)
      {
      case Console: return getConsoleLogicalWindow();
      case Source:  return getSourceLogicalWindow();
      default:      return getOwnerTabPanel(tab).getParentWindow();
      }
   }

   @Inject
   public PaneManager(Provider<MainSplitPanel> pSplitPanel,
                      EventBus eventBus,
                      Session session,
                      Binder binder,
                      Commands commands,
                      UserPrefs userPrefs,
                      @Named(CONSOLE_PANE) final Widget consolePane,
                      FileTypeRegistry fileTypeRegistry,
                      Source source,
                      SourceColumnManager sourceColumnManager,
                      @Named(HISTORY_PANE) final WorkbenchTab historyTab,
                      @Named(FILES_PANE) final WorkbenchTab filesTab,
                      @Named(PLOTS_PANE) final WorkbenchTab plotsTab,
                      @Named(PACKAGES_PANE) final WorkbenchTab packagesTab,
                      @Named(HELP_PANE) final WorkbenchTab helpTab,
                      @Named(VCS_PANE) final WorkbenchTab vcsTab,
                      @Named(BUILD_PANE) final WorkbenchTab buildTab,
                      @Named(PRESENTATION_PANE) final WorkbenchTab presentationTab,
                      @Named(PRESENTATIONS_PANE) final WorkbenchTab presentation2Tab,
                      @Named(CONNECTIONS_PANE) final WorkbenchTab connectionsTab,
                      @Named(ENVIRONMENT_PANE) final WorkbenchTab environmentTab,
                      @Named(VIEWER_PANE) final WorkbenchTab viewerTab,
                      @Named(COMPILE_PDF_PANE) final WorkbenchTab compilePdfTab,
                      @Named(SOURCE_CPP_PANE) final WorkbenchTab sourceCppTab,
                      @Named(RMARKDOWN_PANE) final WorkbenchTab renderRmdTab,
                      @Named(DEPLOY_PANE) final WorkbenchTab deployContentTab,
                      @Named(TERMINAL_PANE) final WorkbenchTab terminalTab,
                      @Named(TESTS_PANE) final WorkbenchTab testsTab,
                      @Named(JOBS_PANE) final WorkbenchTab jobsTab,
                      @Named(LAUNCHER_PANE) final WorkbenchTab launcherJobsTab,
                      @Named(DATA_OUTPUT_PANE) final WorkbenchTab dataTab,
                      @Named(TUTORIAL_PANE) final WorkbenchTab tutorialTab,
                      final MarkersOutputTab markersTab,
                      final FindOutputTab findOutputTab,
                      OptionsLoader.Shim optionsLoader,
                      Provider<GlobalDisplay> pGlobalDisplay)
   {
      eventBus_ = eventBus;
      session_ = session;
      commands_ = commands;
      userPrefs_ = userPrefs;
      consolePane_ = (ConsolePane)consolePane;
      fileTypeRegistry_ = fileTypeRegistry;
      source_ = source;
      sourceColumnManager_ = sourceColumnManager;
      historyTab_ = historyTab;
      filesTab_ = filesTab;
      plotsTab_ = plotsTab;
      packagesTab_ = packagesTab;
      helpTab_ = helpTab;
      vcsTab_ = vcsTab;
      buildTab_ = buildTab;
      presentationTab_ = presentationTab;
      presentation2Tab_ = presentation2Tab;
      connectionsTab_ = connectionsTab;
      environmentTab_ = environmentTab;
      viewerTab_ = viewerTab;
      compilePdfTab_ = compilePdfTab;
      findOutputTab_ = findOutputTab;
      sourceCppTab_ = sourceCppTab;
      renderRmdTab_ = renderRmdTab;
      deployContentTab_ = deployContentTab;
      markersTab_ = markersTab;
      terminalTab_ = terminalTab;
      jobsTab_ = jobsTab;
      launcherJobsTab_ = launcherJobsTab;
      optionsLoader_ = optionsLoader;
      testsTab_ = testsTab;
      dataTab_ = dataTab;
      tutorialTab_ = tutorialTab;
      pGlobalDisplay_ = pGlobalDisplay;

      binder.bind(commands, this);
      source_.load();

      PaneConfig config = validateConfig(userPrefs.panes().getValue().cast());
      initPanes(config);

      int splitterSize = 7;

      panes_ = createPanes(config);
      center_ = createSplitWindow(panes_.get(0), panes_.get(1), LEFT_COLUMN, 0.4, splitterSize);
      right_ = createSplitWindow(panes_.get(2), panes_.get(3), RIGHT_COLUMN, 0.6, splitterSize);
      panel_ = pSplitPanel.get();

      // get the widgets for the extra source columns to be displayed
      additionalSourceCount_ = userPrefs_.panes().getValue().getAdditionalSourceColumns();
      if (additionalSourceCount_ != sourceColumnManager_.getSize() - 1)
         syncAdditionalColumnCount(additionalSourceCount_, false /* refreshDisplay */);
      if (additionalSourceCount_ > 0)
      {
         if (userPrefs_.allowSourceColumns().getGlobalValue())
         {
            for (int i = 0; i < sourceColumnManager_.getSize(); i++)
            {
               String name = sourceColumnManager_.get(i).getName();
               String accessibleName = sourceColumnManager_.get(i).getAccessibleName();
               if (!StringUtil.equals(name, SourceColumnManager.MAIN_SOURCE_NAME))
                  leftList_.add(0, createSourceColumnWindow(name, accessibleName));
            }
         }
         else
         {
            sourceColumnManager_.consolidateColumns(0);
            PaneConfig paneConfig = userPrefs_.panes().getValue().cast();
            userPrefs_.panes().setGlobalValue(PaneConfig.create(
               JsArrayUtil.copy(paneConfig.getQuadrants()),
               paneConfig.getTabSet1(),
               paneConfig.getTabSet2(),
               paneConfig.getHiddenTabSet(),
               paneConfig.getConsoleLeftOnTop(),
               paneConfig.getConsoleRightOnTop(),
               0).cast());
         }
      }
      // Initialize sidebar if configured
      Widget sidebarWidget = null;
      String sidebarLocation = config.getSidebarLocation();
      if (config.getSidebarVisible())
      {
         LogicalWindow sidebarWindow = panesByName_.get(UserPrefsAccessor.Panes.QUADRANTS_SIDEBAR);
         if (sidebarWindow != null)
         {
            // For sidebar, we use just the WindowFrame directly (no vertical split)
            sidebarWindow.transitionToState(WindowState.NORMAL);
            sidebarWidget = sidebarWindow.getNormal();
            sidebar_ = sidebarWidget;
         }
      }
      
      panel_.initialize(leftList_, center_, right_, sidebarWidget, sidebarLocation);

      for (LogicalWindow window : sourceLogicalWindows_)
      {
         // count the number of source docs assigned to this window
         JsArray<SourceDocument> docs =
            session_.getSessionInfo().getSourceDocuments();
         String windowId = SourceWindowManager.getSourceWindowId();
         int numDocs = 0;
         for (int i = 0; i < docs.length(); i++)
         {
            String docWindowId = docs.get(i).getSourceWindowId();

            // Check the SourceColumn of the SourceDocument. If for some reason we cannot find
            // its column, default to the main source window.
            SourceColumn column =
               sourceColumnManager_.getByName(docs.get(i).getSourceDisplayName());
            boolean mainSourceWindow = (column == null ||
               StringUtil.equals(column.getName(), SourceColumnManager.MAIN_SOURCE_NAME)) ?
               true : false;

            LogicalWindow columnWindow = mainSourceWindow ?
                                         sourceLogicalWindows_.get(0) :
                                         getParentLogicalWindow(column.asWidget().getElement());

            if (StringUtil.equals(docWindowId, windowId) &&
                (columnWindow == null && mainSourceWindow ||
                 window == columnWindow))
            {
               numDocs++;
            }
         }

         if (numDocs == 0 && window.getState() != WindowState.HIDE)
         {
            window.onWindowStateChange(
                  new WindowStateChangeEvent(WindowState.HIDE));
         }
         else if (numDocs > 0 && window.getState() == WindowState.HIDE)
         {
            window.onWindowStateChange(
                  new WindowStateChangeEvent(WindowState.NORMAL));
         }
      }

      source_.loadDisplay();
      
      userPrefs.panes().addValueChangeHandler(evt ->
      {
         ArrayList<LogicalWindow> newPanes = createPanes(
               validateConfig(evt.getValue().cast()));
         panes_ = newPanes;
         center_.replaceWindows(newPanes.get(0), newPanes.get(1));
         right_.replaceWindows(newPanes.get(2), newPanes.get(3));

         tabs1_ = tabNamesToTabs(evt.getValue().getTabSet1());
         tabs2_ = tabNamesToTabs(evt.getValue().getTabSet2());

         WindowState oldTabSet1State = panesByName_.get(UserPrefsAccessor.Panes.QUADRANTS_TABSET1).getState();
         WindowState oldTabSet2State = panesByName_.get(UserPrefsAccessor.Panes.QUADRANTS_TABSET2).getState();
         WindowState tabSet1State = setWindowStateOnTabChange(panesByName_.get(UserPrefsAccessor.Panes.QUADRANTS_TABSET1),
         tabSet1TabPanel_, tabs1_);
         WindowState tabSet2State = setWindowStateOnTabChange(panesByName_.get(UserPrefsAccessor.Panes.QUADRANTS_TABSET2),
            tabSet2TabPanel_, tabs2_);

         // Additional checks when tab set panes are in the same column and either has had a
         // state change
         if (getCurrentConfig().getTabSet1Left() == getCurrentConfig().getTabSet2Left() &&
             (oldTabSet1State != tabSet1State || oldTabSet2State != tabSet2State))
         {
            if (tabSet1State == WindowState.MINIMIZE &&
                tabSet2State == WindowState.MINIMIZE)
            {
               double rightTargetSize = 0.0;
               if (getCurrentConfig().getTabSet1Left())
                  rightTargetSize = right_.getOffsetWidth() + center_.getOffsetWidth();
               resizeHorizontally(rightTargetSize, panel_.getLeftWidgetSizes());
            }
            else if (center_.getOffsetWidth() == 0 || right_.getOffsetWidth() == 0)
               resizeHorizontally(panel_.getDefaultSplitterWidth(), panel_.getLeftWidgetSizes());
         }

         tabSet1TabPanel_.clear();
         tabSet2TabPanel_.clear();
         hiddenTabSetTabPanel_.clear();
         populateTabPanel(tabs1_, tabSet1TabPanel_, tabSet1MinPanel_);
         populateTabPanel(tabs2_, tabSet2TabPanel_, tabSet2MinPanel_);
         hiddenTabs_ = tabNamesToTabs(evt.getValue().getHiddenTabSet());
         populateTabPanel(hiddenTabs_, hiddenTabSetTabPanel_, hiddenTabSetMinPanel_);

         // manage source column commands
         boolean visible = userPrefs.allowSourceColumns().getValue() &&
            (userPrefs.panes().getValue().getAdditionalSourceColumns() < MAX_COLUMN_COUNT);

         commands_.newSourceColumn().setVisible(visible);
         commands_.openSourceDocNewColumn().setVisible(visible);

         manageLayoutCommands();
      });

      eventBus_.addHandler(ZoomPaneEvent.TYPE, event ->
      {
         String pane = event.getPane();
         LogicalWindow window = panesByName_.get(pane);
         assert window != null :
            "No pane with name '" + pane + "'";

         toggleWindowZoom(window, tabForName(event.getTab()));
      });

      eventBus_.addHandler(WindowEnsureVisibleEvent.TYPE, event ->
      {
         final LogicalWindow window = getLogicalWindow(event.getWindowFrame());
         if (window == null)
            return;

         // If we're currently zooming a pane, and we're now ensuring
         // a separate window is visible (e.g. a pane raising itself),
         // then transfer zoom to that window.
         if (maximizedWindow_ != null && !maximizedWindow_.equals(window))
         {
            fullyMaximizeWindow(window, lastSelectedTab_);
            return;
         }

         Double width = Double.valueOf(window.getActiveWidget().getOffsetWidth());

         // If the widget is already visible horizontally, then bail
         // (other logic handles vertical visibility)
         if (width > 0)
            return;

         // Attempt to change the widths of as few columns as possible
         ArrayList<Widget> columns = new ArrayList<>();
         columns.add(right_);
         columns.add(center_);
         columns.addAll(leftList_);

         ArrayList<Double> sizes = getValidColumnWidths(columns, false);
         final Command afterAnimation = () -> window.getNormal().onResize();

         double rightEnd = sizes.get(0);
         ArrayList<Double> leftTargets = new ArrayList<>();
         if (sizes.size() > 2)
            leftTargets.addAll(sizes.subList(2, sizes.size()));

         resizeHorizontally(rightEnd, leftTargets, afterAnimation);
      });

      eventBus_.addHandler(
            ManageLayoutCommandsEvent.TYPE,
            event -> manageLayoutCommands());

      eventBus.addHandler(UserPrefsChangedEvent.TYPE, new UserPrefsChangedEvent.Handler()
      {
         @Override
         public void onUserPrefsChanged(UserPrefsChangedEvent e)
         {
            if (!userPrefs_.showPanelFocusRectangle().getValue())
            {
               clearFocusIndicator();
            }
         }
      });

      // highlight pane containing keyboard focus
      DomGlobal.document.addEventListener("focusin", (Event) ->
      {
         if (!userPrefs_.showPanelFocusRectangle().getValue())
         {
            clearFocusIndicator();
            return;
         }

         Element activeEl = DomUtils.getActiveElement();
         if (activeEl == null)
         {
            clearFocusIndicator();
            return;
         }

         LogicalWindow activeWindow = getParentLogicalWindow(activeEl);
         if (activeWindow == lastFocusedWindow_)
            return;
         if (activeWindow == null)
         {
            clearFocusIndicator();
            return;
         }

         if (lastFocusedWindow_ != null)
            lastFocusedWindow_.showWindowFocusIndicator(false);
         lastFocusedWindow_ = activeWindow;
         activeWindow.showWindowFocusIndicator(true);
      });

      manageLayoutCommands();
      new ZoomedTabStateValue();
   }

   LogicalWindow getLogicalWindow(WindowFrame frame)
   {
      for (LogicalWindow window : panes_)
         if (window.getNormal() == frame)
            return window;
      for (LogicalWindow window : sourceLogicalWindows_)
         if (window.getNormal() == frame)
            return window;

      return null;
   }

   LogicalWindow getParentLogicalWindow(Element el)
   {
      LogicalWindow targetWindow = null;
      ArrayList<LogicalWindow> windowList = new ArrayList<>(panes_);
      windowList.addAll(sourceLogicalWindows_);

      while (el != null && targetWindow == null)
      {
         el = el.getParentElement();
         for (LogicalWindow window : windowList)
         {
            Widget activeWidget;
            if (window.getState() != null)
               activeWidget = window.getActiveWidget();
            else
               activeWidget = window.getNormal();
            if (activeWidget == null)
               continue;

            Element activeEl = activeWidget.getElement();
            if (el.equals(activeEl))
            {
               targetWindow = window;
               break;
            }
         }
      }
      return targetWindow;
   }

   public LogicalWindow getActiveLogicalWindow()
   {
      Element activeEl = DomUtils.getActiveElement();
      return getParentLogicalWindow(activeEl);
   }

   @Handler
   public void onActivateConsolePane()
   {
      // Ensure that the console window is activated
      LogicalWindow consoleWindow = getConsoleLogicalWindow();
      if (consoleWindow.getState().equals(WindowState.MINIMIZE))
      {
         WindowStateChangeEvent event =
               new WindowStateChangeEvent(WindowState.NORMAL);
         consoleWindow.onWindowStateChange(event);
      }

      // The console tab panel is initialized lazily -- while a console
      // pane will always be available, the owning tab panel will only
      // be constructed once a neighbor (e.g. the Terminal) has been
      // created.
      if (consoleTabPanel_.isEmpty())
      {
         consolePane_.focus();
      }
      else
      {
         LogicalWindow activeWindow = getActiveLogicalWindow();
         if (consoleWindow.equals(activeWindow))
         {
            consoleTabPanel_.selectNextTab();
         }
         else
         {
            consoleTabPanel_.selectTab(consoleTabPanel_.getSelectedIndex());
         }
      }
   }

   @Handler
   public void onLayoutZoomConsolePane()
   {
      if (consoleTabPanel_.isEmpty())
         consolePane_.focus();
      else
         consoleTabPanel_.selectTab(consoleTabPanel_.getSelectedIndex());

      eventBus_.fireEvent(new ZoomPaneEvent(PaneManager.CONSOLE_PANE));
   }

   @Handler
   public void onLayoutZoomCurrentPane()
   {
      LogicalWindow activeWindow = getActiveLogicalWindow();
      if (activeWindow == null)
         return;
      toggleWindowZoom(activeWindow, null);
   }

   @Handler
   public void onLayoutEndZoom()
   {
      restoreLayout();
   }

   @Handler
   public void onLayoutConsoleOnLeft()
   {
      if (!commands_.layoutConsoleOnLeft().isChecked())
      {
         PaneConfig paneConfig = getCurrentConfig();
         int consoleTargetIndex = paneConfig.getConsoleLeftOnTop() ? 0 : 1;
         swapConsolePane(paneConfig, consoleTargetIndex);
      }
   }

   @Handler
   public void onLayoutConsoleOnRight()
   {
      if (!commands_.layoutConsoleOnRight().isChecked())
      {
         PaneConfig paneConfig = getCurrentConfig();
         int consoleTargetIndex = paneConfig.getConsoleRightOnTop() ? 2 : 3;
         swapConsolePane(paneConfig, consoleTargetIndex);
      }
   }

   // The following commands need to be updated to fit the new layout with additional source
   // columns. The mismatched names/actions are temporarily intentional until this is handled.
   @Handler
   public void onFocusLeftSeparator()
   {
      center_.focusSplitter();
   }

   @Handler
   public void onFocusRightSeparator()
   {
      right_.focusSplitter();
   }

   @Handler
   public void onFocusCenterSeparator()
   {
      panel_.focusSplitter(right_);
   }

   @Handler
   public void onFocusSourceColumnSeparator()
   {
      LogicalWindow window = getParentLogicalWindow(sourceColumnManager_.getActiveElement());
      if (window == null ||
          window.getNormal() == null ||
          !window.getNormal().getName().contains(SourceColumnManager.COLUMN_PREFIX))
      {
         Debug.logWarning("Attempted focusSourceColumnSeparator when source column not active.");
         return;
      }
      panel_.focusSplitter(window.getNormal());
   }

   @Handler
   public void onFocusNextPane()
   {
      LogicalWindow currentFocus = getActiveLogicalWindow();
      if (currentFocus == null)
         return;

      focusAdjacentWindow(currentFocus, false /* before */);
   }

   @Handler
   public void onFocusPreviousPane()
   {
      LogicalWindow currentFocus = getActiveLogicalWindow();
      if (currentFocus == null)
         return;

      focusAdjacentWindow(currentFocus, true /* before */);
   }

   @Handler
   public void onNewSourceColumn()
   {
      if (validateNewColumnRequest())
         createAndDisplaySourceColumn();
   }

   @Handler
   public void onOpenSourceDocNewColumn()
   {
      if (validateNewColumnRequest())
      {
         ColumnName name = createSourceColumn();
         SourceColumn column = sourceColumnManager_.getByName(name.getName());
         column.incrementNewTabPending();
         panel_.addLeftWidget(
            createSourceColumnWindow(name.getName(),
               name.getAccessibleName()));
         sourceColumnManager_.setActive(column);
         Command onCancelled = () ->
         {
            column.decrementNewTabPending();
            closeSourceWindow(name.getName());
         };
         Command onCompleted = () -> column.decrementNewTabPending();
         source_.openSourceDoc(onCancelled, onCompleted);
      }
   }

   public void openFileInNewColumn(FileSystemItem targetFile, Command onOpen)
   {
      if (targetFile != null && validateNewColumnRequest())
      {
         ColumnName name = createSourceColumn();
         SourceColumn column = sourceColumnManager_.getByName(name.getName());
         column.incrementNewTabPending();
         panel_.addLeftWidget(
               createSourceColumnWindow(
                  name.getName(),
                  name.getAccessibleName()
                  )
               );
         sourceColumnManager_.openFile(
               targetFile, 
               fileTypeRegistry_.getTextTypeForFile(targetFile),
               column,
               new CommandWithArg<EditingTarget>() 
                  {
                     @Override
                     public void execute(EditingTarget target)
                     {
                        column.decrementNewTabPending();
                        if (onOpen != null) 
                           onOpen.execute();
                     }
                  });
      }
   }

   public void openFileInNewColumn(FileSystemItem targetFile) 
   {
      openFileInNewColumn(targetFile, null);
   }

   private boolean validateNewColumnRequest()
   {
      if (additionalSourceCount_ >= MAX_COLUMN_COUNT)
      {
         pGlobalDisplay_.get().showMessage(GlobalDisplay.MSG_INFO, constants_.cannotAddColumnText(),
            constants_.cannotAddMoreColumnsText(MAX_COLUMN_COUNT));
         return false;
      }
      return true;
   }

   private String getAdjacentWindow(LogicalWindow window, boolean before)
   {
      if (window.getNormal() == null)
         return "";

      String name = window.getNormal().getName();
      int lastIndex = panes_.size() - 1;

      // if we're the last (!before or first (before) window in the display, wrap around to the
      // front
      if (!before && window == panes_.get(lastIndex))
         return additionalSourceCount_ > 0 ? sourceColumnManager_.getLeftColumnName() :
            panes_.get(0).getNormal().getName();
      else if (before)
      {
         if ((additionalSourceCount_ > 0 &&
            StringUtil.equals(name, sourceColumnManager_.getLeftColumnName())) ||
            (additionalSourceCount_ == 0 && window == panes_.get(0)))
            return panes_.get(lastIndex).getNormal().getName();
         else if (window == panes_.get(0))
            return sourceColumnManager_.get(1).getName();
      }

      // source columns have special handling
      if (sourceColumnManager_.getByName(name) != null &&
         !StringUtil.equals(name, SourceColumnManager.MAIN_SOURCE_NAME))
      {
         if (before)
            return sourceColumnManager_.getPreviousColumnName();
         else
         {
            String nextName = sourceColumnManager_.getNextColumnName();
            if (!StringUtil.equals(nextName, SourceColumnManager.MAIN_SOURCE_NAME))
               return nextName;
            return panes_.get(0).getNormal().getName();
         }
      }

      for (int i = 0; i < panes_.size(); i++)
      {
         if (window.equals(panes_.get(i)))
         {
            if (before)
               return panes_.get(i - 1).getNormal().getName();
            return panes_.get(i + 1).getNormal().getName();
         }
      }

      Debug.log("Couldn't locate adjacent pane for " + name);
      return "";
   }

   private WindowState setWindowStateOnTabChange(LogicalWindow window, WorkbenchTabPanel tabPanel,
                                                 ArrayList<Tab> tabs)
   {
      WindowState newState = window.getState();
      if (tabs.isEmpty() && window.getState() != WindowState.MINIMIZE)
         newState = WindowState.MINIMIZE;
      else if (tabs.size() == 1 && tabs.get(0) == Tab.Presentation)
      {
         if (!session_.getSessionInfo().getPresentationState().isActive() &&
             window.getState() != WindowState.MINIMIZE)
            newState = WindowState.MINIMIZE;
      }
      else if (tabPanel.isEmpty() && !tabs.isEmpty())
         newState = WindowState.NORMAL;

      if (newState != window.getState())
         window.onWindowStateChange(new WindowStateChangeEvent(newState));
      return newState;
   }

   @SuppressWarnings("rawtypes")
   private void focusWindow(String name)
   {
      if (StringUtil.isNullOrEmpty(name))
         return;

      // activate next pane
      if (name.contains(SourceColumnManager.COLUMN_PREFIX))
         sourceColumnManager_.activateColumn(name, null);
      else
      {
         WorkbenchTab selected;
         if (StringUtil.equals(PaneManager.CONSOLE_PANE, name))
         {
            selected = consoleTabPanel_.getSelectedIndex() >= 0 ?
               consoleTabPanel_.getSelectedTab() :
               null;

            // Special handling for Console; Console does not have a WorkbenchTab when there are
            // no other Console tabs open on start up and none have been added.
            if (selected == null || StringUtil.equals(selected.getTitle(), consoleConstants_.consoleLabel()));
            {
               commands_.activateConsole().execute();
               return;
            }
         }
         else
         {
            if (StringUtil.equals(UserPrefsAccessor.Panes.QUADRANTS_TABSET1, name))
               selected = tabSet1TabPanel_.getSelectedTab();
            else
               selected = tabSet2TabPanel_.getSelectedTab();
            activateTab(wbTabToTab_.get(selected));
         }
         if (selected instanceof DelayLoadWorkbenchTab)
            ((DelayLoadWorkbenchTab)selected).ensureVisible(true);
         selected.setFocus();
      }
   }

   private void focusAdjacentWindow(LogicalWindow window, boolean before)
   {
      String adjacent = getAdjacentWindow(window, before);

      // TabSet1 and TabSet2 could be empty, if so skip to the next pane
      while ((StringUtil.equals(UserPrefsAccessor.Panes.QUADRANTS_TABSET1, adjacent) && tabSet1TabPanel_.isEmpty()) ||
             (StringUtil.equals(UserPrefsAccessor.Panes.QUADRANTS_TABSET2, adjacent) && tabSet2TabPanel_.isEmpty()))
         adjacent = getAdjacentWindow(panesByName_.get(adjacent), before);

      focusWindow(adjacent);
   }

   private void swapConsolePane(PaneConfig paneConfig, int consoleTargetIndex)
   {
      int consoleCurrentIndex = paneConfig.getConsoleIndex();
      if (consoleCurrentIndex != consoleTargetIndex)
      {
         JsArrayString panes = JsArrayUtil.copy(paneConfig.getQuadrants());
         panes.set(consoleCurrentIndex, panes.get(consoleTargetIndex));
         panes.set(consoleTargetIndex, PaneManager.CONSOLE_PANE);
         userPrefs_.panes().setGlobalValue(PaneConfig.create(
            panes,
            paneConfig.getTabSet1(),
            paneConfig.getTabSet2(),
            paneConfig.getHiddenTabSet(),
            paneConfig.getConsoleLeftOnTop(),
            paneConfig.getConsoleRightOnTop(),
            sourceColumnManager_.getSize() - 1).cast());
         userPrefs_.writeUserPrefs();
      }
   }

   @Handler
   public void onPaneLayout()
   {
      optionsLoader_.showOptions(PaneLayoutPreferencesPane.class, true);
   }
   
   @Handler
   public void onToggleSidebar()
   {
      // Toggle the preference value and update UI
      PaneConfig paneConfig = userPrefs_.panes().getValue().cast();
      boolean newVisibility = !paneConfig.getSidebarVisible();
      
      // Update the preference
      userPrefs_.panes().setGlobalValue(PaneConfig.create(
         JsArrayUtil.copy(paneConfig.getQuadrants()),
         paneConfig.getTabSet1(),
         paneConfig.getTabSet2(),
         paneConfig.getHiddenTabSet(),
         paneConfig.getConsoleLeftOnTop(),
         paneConfig.getConsoleRightOnTop(),
         paneConfig.getAdditionalSourceColumns(),
         paneConfig.getSidebar(),
         newVisibility,
         paneConfig.getSidebarLocation()));
      
      // Persist the preference change
      userPrefs_.writeUserPrefs();
      
      // Update the UI
      if (newVisibility)
         showSidebar();
      else
         hideSidebar();
   }

   @Handler
   public void onMoveSidebar()
   {
      // Toggle the sidebar location between left and right
      PaneConfig paneConfig = userPrefs_.panes().getValue().cast();
      String currentLocation = paneConfig.getSidebarLocation();
      String newLocation = "left".equals(currentLocation) ? "right" : "left";
      
      // Update the preference
      userPrefs_.panes().setGlobalValue(PaneConfig.create(
         JsArrayUtil.copy(paneConfig.getQuadrants()),
         paneConfig.getTabSet1(),
         paneConfig.getTabSet2(),
         paneConfig.getHiddenTabSet(),
         paneConfig.getConsoleLeftOnTop(),
         paneConfig.getConsoleRightOnTop(),
         paneConfig.getAdditionalSourceColumns(),
         paneConfig.getSidebar(),
         paneConfig.getSidebarVisible(),
         newLocation));
      
      // Persist the preference change
      userPrefs_.writeUserPrefs();
      
      // Update the UI by hiding and re-showing the sidebar in the new location
      if (sidebar_ != null)
      {
         hideSidebar();
         showSidebar();
      }
   }

   public void showSidebar()
   {
      if (sidebar_ == null)
      {
         // Create sidebar configuration
         PaneConfig config = userPrefs_.panes().getValue().cast();
         JsArrayString sidebarTabs = config.getSidebar();
         
         // Create sidebar tabset if not already created
         LogicalWindow sidebarWindow = panesByName_.get(UserPrefsAccessor.Panes.QUADRANTS_SIDEBAR);
         if (sidebarWindow == null)
         {
            Triad<LogicalWindow, WorkbenchTabPanel, MinimizedModuleTabLayoutPanel> sidebar = createTabSet(
                  UserPrefsAccessor.Panes.QUADRANTS_SIDEBAR,
                  tabNamesToTabs(sidebarTabs));
            panesByName_.put(UserPrefsAccessor.Panes.QUADRANTS_SIDEBAR, sidebar.first);
            sidebarTabPanel_ = sidebar.second;
            sidebarMinPanel_ = sidebar.third;
            sidebarTabs_ = tabNamesToTabs(sidebarTabs);
            sidebarWindow = sidebar.first;
         }
         
         if (sidebarWindow != null)
         {
            // For sidebar, we use just the WindowFrame directly (no vertical split)
            sidebarWindow.transitionToState(WindowState.NORMAL);
            sidebar_ = sidebarWindow.getNormal();
            String location = config.getSidebarLocation();
            panel_.setSidebarWidget(sidebar_, location);
         }
      }
   }
   
   public void hideSidebar()
   {
      if (sidebar_ != null)
      {
         panel_.removeSidebarWidget();
         sidebar_ = null;
      }
   }
   
   public boolean isSidebarVisible()
   {
      PaneConfig config = userPrefs_.panes().getValue().cast();
      return config.getSidebarVisible();
      // return sidebar_ != null && panel_.hasSidebarWidget();
   }
   
   public void setSidebarLocation(String location)
   {
      // Update preference and refresh the sidebar if visible
      PaneConfig paneConfig = userPrefs_.panes().getValue().cast();
      
      // Only update if location has changed
      if (!location.equals(paneConfig.getSidebarLocation()))
      {
         userPrefs_.panes().setGlobalValue(PaneConfig.create(
            JsArrayUtil.copy(paneConfig.getQuadrants()),
            paneConfig.getTabSet1(),
            paneConfig.getTabSet2(),
            paneConfig.getHiddenTabSet(),
            paneConfig.getConsoleLeftOnTop(),
            paneConfig.getConsoleRightOnTop(),
            paneConfig.getAdditionalSourceColumns(),
            paneConfig.getSidebar(),
            paneConfig.getSidebarVisible(),
            location));
         
         userPrefs_.writeUserPrefs();
         
         // If sidebar is visible, refresh it in the new location
         if (sidebar_ != null)
         {
            hideSidebar();
            showSidebar();
         }
      }
   }
   
   private <T> boolean equals(T lhs, T rhs)
   {
      if (lhs == null)
         return rhs == null;

      return lhs.equals(rhs);
   }

   public void toggleWindowZoom(LogicalWindow window, Tab tab)
   {
      if (isAnimating_)
         return;

      boolean hasZoom = maximizedWindow_ != null;

      if (hasZoom)
      {
         if (equals(window, maximizedWindow_))
         {
            // If we're trying to maximize the same pane that is currently maximized, interpret
            // as a toggle off. There is only one tab per source column so always assume toggle off.
            if (tab == Tab.SourceColumn ||
                equals(tab, maximizedTab_))
               restoreLayout();

            // If we're zooming a different tab in the same window,
            // just activate that tab.
            else
            {
               maximizedTab_ = tab;
               manageLayoutCommands();
               activateTab(tab);
            }
         }
         else
         {
            // We're transferring zoom from one window to another --
            // maximize the new window.
            fullyMaximizeWindow(window, tab);
         }
      }
      else
      {
         // No zoom currently on -- just zoom the selected window + tab.
         fullyMaximizeWindow(window, tab);
      }
   }

   private void fullyMaximizeWindow(final LogicalWindow window, final Tab tab)
   {
      if (window.equals(getSourceLogicalWindow()))
         maximizedTab_ = Tab.Source;
      else if (window.equals(getConsoleLogicalWindow()))
         maximizedTab_ = Tab.Console;
      else if (sourceLogicalWindows_.contains(window))
         maximizedTab_ = Tab.SourceColumn;
      else
         maximizedTab_ = tab;
      maximizedWindow_ = window;

      manageLayoutCommands();
      panel_.setSplitterEnabled(false);

      if (widgetSizePriorToZoom_ < 0)
         widgetSizePriorToZoom_ = panel_.getWidgetSize(right_);

      // Put all of the panes in NORMAL mode, just to ensure an appropriate
      // transfer to EXCLUSIVE mode works. (It seems that 'exclusive' -> 'exclusive'
      // transfers don't always propagate as expected)
      for (LogicalWindow pane : panes_)
         pane.onWindowStateChange(new WindowStateChangeEvent(WindowState.NORMAL));
      for (LogicalWindow pane : sourceLogicalWindows_)
         pane.onWindowStateChange(new WindowStateChangeEvent(WindowState.NORMAL));

      boolean isRightWidget =
            DomUtils.contains(right_.getElement(), window.getActiveWidget().getElement());
      boolean isCenterWidget =
            DomUtils.contains(center_.getElement(), window.getActiveWidget().getElement());
      boolean isSidebarWidget = sidebar_ != null &&
            DomUtils.contains(sidebar_.getElement(), window.getActiveWidget().getElement());

      window.onWindowStateChange(new WindowStateChangeEvent(WindowState.EXCLUSIVE));

      ArrayList<Double> leftTargets = new ArrayList<>();
      {
         if (leftWidgetSizePriorToZoom_.size() != leftList_.size())
            leftWidgetSizePriorToZoom_.clear();
         for (Widget column : leftList_)
         {
            leftWidgetSizePriorToZoom_.add(panel_.getWidgetSize(column));
            if (!isRightWidget &&
                !isCenterWidget &&
                DomUtils.contains(column.getElement(), window.getActiveWidget().getElement()))
               leftTargets.add((double) panel_.getOffsetWidth());
            else
               leftTargets.add(0.0);
         }
      }

      double rightTargetSize = isRightWidget ? panel_.getOffsetWidth() : 0;

      if (rightTargetSize < 0)
         rightTargetSize = 0;

      // Ensure focus is sent to Help iframe on activation.
      Command onActivation = null;
      if (maximizedTab_.equals(Tab.Help))
      {
         onActivation = () -> commands_.activateHelp().execute();
      }

      resizeHorizontally(rightTargetSize, leftTargets, onActivation);
   }

   private void resizeHorizontally(final double rightTarget,
                                   final ArrayList<Double> leftTargets)
   {
      resizeHorizontally(rightTarget, leftTargets, null);
   }

   private void resizeHorizontally(final double rightTarget,
                                   final ArrayList<Double> leftTargets,
                                   final Command afterComplete)
   {
      panel_.setWidgetSize(right_, rightTarget);
      for (int i = 0; i < leftList_.size(); i++)
         panel_.setWidgetSize(leftList_.get(i), leftTargets.get(i));

      int duration = (userPrefs_.reducedMotion().getValue() ? 0 : 300);
      panel_.animate(duration, new AnimationCallback()
      {
         public void onAnimationComplete()
         {
            panel_.onSplitterResized(new SplitterResizedEvent());
            if (afterComplete != null)
               afterComplete.execute();
         }

         public void onLayout(Layer layer, double progress)
         {
         }
      });
   }

   private void restoreLayout()
   {
      // If we're currently zoomed, then use that to provide the previous
      // 'non-zoom' state.
      if (maximizedWindow_ != null &&
          leftList_.size() == leftWidgetSizePriorToZoom_.size())
         restoreSavedLayout();
      else
         restorePaneLayout();
   }

   private void invalidateSavedLayoutState(boolean enableSplitter)
   {
      maximizedWindow_ = null;
      maximizedTab_ = null;
      widgetSizePriorToZoom_ = -1;
      leftWidgetSizePriorToZoom_.clear();
      panel_.setSplitterEnabled(enableSplitter);
      manageLayoutCommands();
   }

   private void restorePaneLayout()
   {
      restorePaneStateToDefault();
      restoreColumnLayout();
   }

   private Double getValidColumnWidth(Widget w, boolean set)
   {
      return getValidColumnWidths(new ArrayList<> (Arrays.asList(w)),
         new ArrayList<>(Arrays.asList(panel_.getWidgetSize(w))), set).get(0);
   }

   private ArrayList<Double> getValidColumnWidths(final ArrayList<Widget> widgets, boolean set)
   {
      ArrayList<Double> currentWidths = new ArrayList<>();
      for (Widget widget : widgets)
         currentWidths.add(panel_.getWidgetSize(widget));
      return getValidColumnWidths(widgets, currentWidths, set);
   }

   /**
    * Calculate a visible width for each widget provided while attempting to keep each
    * widget as close to its requested size. The non-allocated portion of the total panel should
    * be enough to display columns not provided.
    * @param widgets List of widgets to set sizes of. These widgets must be part of panel_.
    * @param widths List of ideal widths for each widget. This can be empty.
    * @param set When true, sets the width on panel_.
    * @return The list of valid sizes
    */
   private ArrayList<Double> getValidColumnWidths(
      final ArrayList<Widget> widgets,
      final ArrayList<Double> widths,
      boolean set)
   {
      final double columnWidth = panel_.getOffsetWidth() / getColumnCount();

      // The pixels allocated for the widgets provided leave enough pixels so that each column
      // will have a width of at least half of columnWidth
      double minColumnWidth = columnWidth / 2;
      double maxColumnWidth = columnWidth * 2;

      // Calculate the min and max amount of pixels we must leave for columns not specified.
      int remainingColumns = getColumnCount() - widgets.size();
      double minAllocated = panel_.getOffsetWidth() - (remainingColumns * minColumnWidth);
      double maxAllocated = panel_.getOffsetWidth() - (remainingColumns * maxColumnWidth);

      // Because each panel cannot take up the max threshold, leave enough space so every
      // remaining column can at least contain the minimum threshold.
      if (maxAllocated < (widgets.size() * minColumnWidth))
        maxAllocated = Math.max((maxAllocated - (widgets.size() * minColumnWidth)), minAllocated);

      // Determine if each provided width is within our threshold.
      // If not, set it size to the min or max threshold.
      // If no width is provided, the width is set to the columnWidth calculated above.
      double minThreshold = minAllocated / widgets.size();
      double maxThreshold = maxAllocated / widgets.size();
      ArrayList<Double> result = new ArrayList<>();
      for (int i = 0; i < widgets.size(); i++)
      {
         double width = columnWidth;
         if (widths.size() > i)
         {
            width = widths.get(i);
            if (width < minThreshold)
               width = minThreshold;
            else if (width > maxThreshold)
               width = maxThreshold;
         }
         if (set)
            panel_.setWidgetSize(widgets.get(i), width);
         result.add(width);
      }
      return result;
   }

   private void restoreColumnLayout()
   {
      getValidColumnWidth(right_, true);
      getValidColumnWidths(leftList_, leftWidgetSizePriorToZoom_, true);
      invalidateSavedLayoutState(true);
   }

   private void restoreSavedLayout()
   {
      restorePaneStateToDefault();

      maximizedWindow_.onWindowStateChange(new WindowStateChangeEvent(WindowState.NORMAL, true));

      ArrayList<Double> leftTargets = new ArrayList<>();
      if (leftList_.size() > 0)
      {
         if (leftWidgetSizePriorToZoom_.size() != leftList_.size())
            leftTargets = getValidColumnWidths(leftList_, leftWidgetSizePriorToZoom_, false);
         else
            leftTargets.addAll(leftWidgetSizePriorToZoom_);
      }

      resizeHorizontally(widgetSizePriorToZoom_, leftTargets);
      invalidateSavedLayoutState(true);
   }

   private void restorePaneStateToDefault()
   {
      // Ensure that all windows are in the 'normal' state. This allows hidden windows to display
      // themselves, and so on. This also forces widgets to size themselves vertically.
      // TabSet Panes without any tabs should remain minimized.
      for (LogicalWindow window : panes_)
      {
         if ((window == panesByName_.get(UserPrefsAccessor.Panes.QUADRANTS_TABSET1) && tabSet1TabPanel_.isEmpty()) ||
             (window == panesByName_.get(UserPrefsAccessor.Panes.QUADRANTS_TABSET2) && tabSet2TabPanel_.isEmpty()))
         {
            if (window.getState() != WindowState.MINIMIZE)
               window.onWindowStateChange(new WindowStateChangeEvent(WindowState.MINIMIZE));
         }
         else
            window.onWindowStateChange(
               new WindowStateChangeEvent(WindowState.NORMAL, true));
      }
   }

   @Handler
   public void onMaximizeConsole()
   {
      // acts as a toggle (maximize/restore)
      getConsoleLogicalWindow().onWindowStateChange(new WindowStateChangeEvent(WindowState.MAXIMIZE));
   }

   @Handler
   public void onMaximizeSource()
   {
      // acts as a toggle (maximize/restore)
      getSourceLogicalWindow().onWindowStateChange(new WindowStateChangeEvent(WindowState.MAXIMIZE));
   }

   @Handler
   public void onMaximizeTabSet1()
   {
      // acts as a toggle (maximize/restore)
      LogicalWindow tabSet1 = panesByName_.get(UserPrefsAccessor.Panes.QUADRANTS_TABSET1);
      tabSet1.onWindowStateChange(new WindowStateChangeEvent(WindowState.MAXIMIZE));
   }

   @Handler
   public void onMaximizeTabSet2()
   {
      // acts as a toggle (maximize/restore)
      LogicalWindow tabSet2 = panesByName_.get(UserPrefsAccessor.Panes.QUADRANTS_TABSET2);
      tabSet2.onWindowStateChange(new WindowStateChangeEvent(WindowState.MAXIMIZE));
   }

   private ArrayList<LogicalWindow> createPanes(PaneConfig config)
   {
      ArrayList<LogicalWindow> results = new ArrayList<>();

      // Do not include hiddenTabSet in panes since it should never be displayed
      JsArrayString panes = config.getQuadrants();
      for (int i = 0; i < panes.length(); i++)
      {
         if (StringUtil.equals(panes.get(i), UserPrefsAccessor.Panes.QUADRANTS_HIDDENTABSET))
            continue;
         results.add(panesByName_.get(panes.get(i)));
      }
      return results;
   }

   private void initPanes(PaneConfig config)
   {
      panesByName_ = new HashMap<>();
      panesByName_.put(PaneManager.CONSOLE_PANE, createConsole());

      ArrayList<SourceColumn> columns = sourceColumnManager_.getColumnList();
      for (SourceColumn column : columns)
      {
         panesByName_.put(column.getName(), createSource(column.getName(), column.getAccessibleName(),
            column.asWidget()));
      }

      Triad<LogicalWindow, WorkbenchTabPanel, MinimizedModuleTabLayoutPanel> ts1 = createTabSet(
            UserPrefsAccessor.Panes.QUADRANTS_TABSET1,
            tabNamesToTabs(config.getTabSet1()));
      panesByName_.put(UserPrefsAccessor.Panes.QUADRANTS_TABSET1, ts1.first);
      tabSet1TabPanel_ = ts1.second;
      tabSet1MinPanel_ = ts1.third;

      Triad<LogicalWindow, WorkbenchTabPanel, MinimizedModuleTabLayoutPanel> ts2 = createTabSet(
            UserPrefsAccessor.Panes.QUADRANTS_TABSET2,
            tabNamesToTabs(config.getTabSet2()));
      panesByName_.put(UserPrefsAccessor.Panes.QUADRANTS_TABSET2, ts2.first);
      tabSet2TabPanel_ = ts2.second;
      tabSet2MinPanel_ = ts2.third;

      Triad<LogicalWindow, WorkbenchTabPanel, MinimizedModuleTabLayoutPanel> tsHide = createTabSet(
            UserPrefsAccessor.Panes.QUADRANTS_HIDDENTABSET,
            tabNamesToTabs(config.getHiddenTabSet()));
      panesByName_.put(UserPrefsAccessor.Panes.QUADRANTS_HIDDENTABSET, tsHide.first);
      hiddenTabSetTabPanel_ = tsHide.second;
      hiddenTabSetTabPanel_.setNeverVisible(true);
      hiddenTabSetMinPanel_ = tsHide.third;
      
      // Initialize sidebar (always create it, even if not initially visible)
      Triad<LogicalWindow, WorkbenchTabPanel, MinimizedModuleTabLayoutPanel> sidebar = createTabSet(
            UserPrefsAccessor.Panes.QUADRANTS_SIDEBAR,
            tabNamesToTabs(config.getSidebar()));
      panesByName_.put(UserPrefsAccessor.Panes.QUADRANTS_SIDEBAR, sidebar.first);
      sidebarTabPanel_ = sidebar.second;
      sidebarMinPanel_ = sidebar.third;
   }

   private ArrayList<Tab> tabNamesToTabs(JsArrayString tabNames)
   {
      ArrayList<Tab> tabList = new ArrayList<>();
      // this is necessary to avoid issues when moving from 1.3 where hiddenTabSet did not exist
      if (tabNames != null)
      {
         for (int j = 0; j < tabNames.length(); j++)
            tabList.add(Enum.valueOf(Tab.class, tabNames.get(j)));
      }
      return tabList;
   }

   private PaneConfig validateConfig(PaneConfig config)
   {
      if (config == null)
         config = PaneConfig.createDefault();
      if (!config.validateAndAutoCorrect())
      {
         Debug.log("Pane config is not valid");
         config = PaneConfig.createDefault();
      }
      return config;
   }

   public MainSplitPanel getPanel()
   {
      return panel_;
   }

   public WorkbenchTab getTab(Tab tab)
   {
      switch (tab)
      {
         case History:
            return historyTab_;
         case Files:
            return filesTab_;
         case Plots:
            return plotsTab_;
         case Packages:
            return packagesTab_;
         case Help:
            return helpTab_;
         case VCS:
            return vcsTab_;
         case Tutorial:
            return tutorialTab_;
         case Build:
            return buildTab_;
         case Presentation:
            return presentationTab_;
         case Presentations:
            return presentation2Tab_;
         case Environment:
            return environmentTab_;
         case Viewer:
            return viewerTab_;
         case Connections:
            return connectionsTab_;
         case Source:
         case SourceColumn:
         case Console:
            // not 'real' tabs so should be an error to ask for their tabs
      }
      throw new IllegalArgumentException("Unknown tab");
   }

   public WorkbenchTab[] getAllTabs()
   {
      return new WorkbenchTab[] { historyTab_, filesTab_,
                                  plotsTab_, packagesTab_, helpTab_,
                                  vcsTab_, tutorialTab_, buildTab_, 
                                  presentationTab_, presentation2Tab_,
                                  environmentTab_, viewerTab_,
                                  connectionsTab_, jobsTab_, launcherJobsTab_ };
   }

   public void activateTab(Tab tab)
   {
      lastSelectedTab_ = tab;
      WorkbenchTabPanel panel = getOwnerTabPanel(tab);
      LogicalWindow parent = panel.getParentWindow();

      // If the tab belongs to the hidden tabset, add it to one being displayed
      if (parent == panesByName_.get(UserPrefsAccessor.Panes.QUADRANTS_HIDDENTABSET))
      {
         // Try to find a visible tabSet, if both are hidden - add to tabSet1
         LogicalWindow tabSet1 = panesByName_.get(UserPrefsAccessor.Panes.QUADRANTS_TABSET1);
         LogicalWindow tabSet2 = panesByName_.get(UserPrefsAccessor.Panes.QUADRANTS_TABSET2);
         if (tabSet1.visible() || !tabSet2.visible())
         {
            parent = tabSet1;
            panel = tabSet1TabPanel_;
            moveHiddenTabToTabSet1(tab, tabs1_);
         }
         else
         {
            parent = tabSet2;
            panel = tabSet2TabPanel_;
            moveHiddenTabToTabSet2(tab, tabs2_);
         }
      }

      // Ensure that the pane is visible (otherwise tab selection will fail)
      if (!parent.visible())
      {
         parent.onWindowStateChange(new WindowStateChangeEvent(WindowState.NORMAL));
      }

      if (tabToIndex_.containsKey(tab))
      {
         int index = tabToIndex_.get(tab);
         panel.selectTab(index);
      }
      else
      {
         // unexpected; why are we trying to activate a suppressed tab?
         Debug.logWarning("Attempt to activate suppressed or unavailable " +
                          "tab '" + tab.name() + "')");
      }
   }

   public void activateTab(String tabName)
   {
      Tab tab = tabForName(tabName);
      if (tab != null)
         activateTab(tab);
   }
   
   public void focusTab(Tab tab)
   {
      WorkbenchTab wbTab = getTab(tab);
      if (wbTab != null)
         wbTab.setFocus();
   }

   public void zoomTab(Tab tab)
   {
      activateTab(tab);
      WorkbenchTabPanel tabPanel = getOwnerTabPanel(tab);
      LogicalWindow parentWindow = tabPanel.getParentWindow();
      if (parentWindow == null)
         return;

      toggleWindowZoom(parentWindow, tab);
   }

   private JsArrayString tabListToJsArrayString(ArrayList<Tab> tabs)
   {
      JsArrayString tabSet = JsArrayString.createArray().cast();
      for (Tab tab : tabs)
         tabSet.push(tab.name());
      return tabSet;
   }

   private void moveTabToVisiblePanel(Tab tab, ArrayList<Tab> tabs)
   {
      // Remove tab from hidden tabSet
      hiddenTabs_.remove(tab);

      // Add tab to the back of the new set
      if (StringUtil.equals(tabs.get(tabs.size() - 1).name(), PaneManager.PRESENTATION_PANE))
         tabs.add(tabs.size() - 1, tab);
      else
         tabs.add(tab);
   }

   private void moveHiddenTabToTabSet1(Tab tab, ArrayList<Tab> tabs)
   {
      moveTabToVisiblePanel(tab, tabs);

      PaneConfig paneConfig = getCurrentConfig();
      userPrefs_.panes().setGlobalValue(PaneConfig.create(
         JsArrayUtil.copy(paneConfig.getQuadrants()),
         tabListToJsArrayString(tabs),
         paneConfig.getTabSet2(),
         tabListToJsArrayString(hiddenTabs_),
         paneConfig.getConsoleLeftOnTop(),
         paneConfig.getConsoleRightOnTop(),
         sourceColumnManager_.getSize() - 1).cast());
      userPrefs_.writeUserPrefs();
   }

   private void moveHiddenTabToTabSet2(Tab tab, ArrayList<Tab> tabs)
   {
      moveTabToVisiblePanel(tab, tabs);

      PaneConfig paneConfig = getCurrentConfig();
      userPrefs_.panes().setGlobalValue(PaneConfig.create(
         JsArrayUtil.copy(paneConfig.getQuadrants()),
         paneConfig.getTabSet1(),
         tabListToJsArrayString(tabs),
         tabListToJsArrayString(hiddenTabs_),
         paneConfig.getConsoleLeftOnTop(),
         paneConfig.getConsoleRightOnTop(),
         sourceColumnManager_.getSize() - 1).cast());
      userPrefs_.writeUserPrefs();
   }

   /**
    * @return name of zoomed column, or null if no zoomed column; zoomed in this case means
    *         the splitter is dragged all the way to the left or right
    */
   private String getZoomedColumn()
   {
      double currentColumnSize = panel_.getWidgetSize(right_);
      if (MathUtil.isEqual(currentColumnSize, 0.0, 0.0001))
         return LEFT_COLUMN;

      // If MainSplitPanel.enforceBoundaries has been called then the rightZoomPosition is the
      // offsetWidth - 3
      double rightZoomPosition = panel_.getOffsetWidth();
      if (MathUtil.isEqual(currentColumnSize, rightZoomPosition, 0.0001) ||
          MathUtil.isEqual(currentColumnSize, rightZoomPosition - 3, 0.0001))
         return RIGHT_COLUMN;

      return null;
   }

   /**
    * Zoom (or unzoom if invoked on an already-zoomed) column
    *
    * @param columnId
    */
   public void zoomColumn(String columnId)
   {
      double rightTargetSize;
      ArrayList<Double> leftTargetSize = new ArrayList<>();

      String currentZoomedColumn = getZoomedColumn();
      boolean unZooming = false;

      if (StringUtil.equals(currentZoomedColumn, columnId))
      {
         if (widgetSizePriorToZoom_ < 0 ||
            (leftWidgetSizePriorToZoom_.size() != additionalSourceCount_))
         {
            // no prior position to restore to, just show defaults
            restoreColumnLayout();
            return;
         }
         rightTargetSize = widgetSizePriorToZoom_;
         for (Double s : leftWidgetSizePriorToZoom_)
            leftTargetSize.add(s);
         unZooming = true;
      }
      else if (StringUtil.equals(columnId, LEFT_COLUMN))
      {
         rightTargetSize = 0.0;
      }
      else if (StringUtil.equals(columnId, RIGHT_COLUMN))
      {
         rightTargetSize = panel_.getOffsetWidth();
      }
      else
      {
         Debug.logWarning("Unexpected column identifier: " + columnId);
         return;
      }
      // Currently we cannot zoom on left widgets
      if (!unZooming)
      {  
         for (int i=0; i<leftList_.size(); i++)
            leftTargetSize.add(0.0);
      }

      if (rightTargetSize < 0)
         rightTargetSize = 0.0;
      for (int i = 0; i < leftTargetSize.size(); i++)
      {
         if (leftTargetSize.get(i) < 0)
            leftTargetSize.set(i, 0.0);
      }

      if (unZooming)
      {
         widgetSizePriorToZoom_ = -1;
         leftWidgetSizePriorToZoom_.clear();
      }
      else
      {
         if (widgetSizePriorToZoom_ < 0)
            widgetSizePriorToZoom_ = panel_.getWidgetSize(right_);
         if (leftWidgetSizePriorToZoom_.size() != leftList_.size())
         {
            for (Widget w : leftList_)
               leftWidgetSizePriorToZoom_.add(panel_.getWidgetSize(w));
         }
      }

      resizeHorizontally(rightTargetSize, leftTargetSize, () -> manageLayoutCommands());
   }

   public LogicalWindow getZoomedWindow()
   {
      return maximizedWindow_;
   }

   public ConsolePane getConsole()
   {
      return consolePane_;
   }

   public WorkbenchTabPanel getOwnerTabPanel(Tab tab)
   {
      return tabToPanel_.get(tab);
   }

   public LogicalWindow getSourceLogicalWindow()
   {
      return sourceLogicalWindows_.get(0);
   }

   public LogicalWindow getConsoleLogicalWindow()
   {
      return panesByName_.get(PaneManager.CONSOLE_PANE);
   }

   public int syncAdditionalColumnCount(int count, boolean refreshDisplay)
   {
      // make sure additionalSourceCount_ is up to date
      additionalSourceCount_ = sourceColumnManager_.getSize() - 1;

      if (count == additionalSourceCount_)
         return additionalSourceCount_;

      if (count > additionalSourceCount_)
      {
         int difference = count - additionalSourceCount_;
         for (int i = 0; i < difference; i++)
         {
            if (refreshDisplay)
               createAndDisplaySourceColumn();
            else
               sourceColumnManager_.add();
         }
      }
      else
      {
         ArrayList<String> removedColumns = sourceColumnManager_.consolidateColumns(count + 1);
         if (refreshDisplay)
         {
            for (String name : removedColumns)
               closeSourceWindow(name);
         }
      }
      additionalSourceCount_ = sourceColumnManager_.getSize() - 1;
      return additionalSourceCount_;
   }

   private void createAndDisplaySourceColumn()
   {
      ColumnName name = createSourceColumn();
      Widget panel = createSourceColumnWindow(name.getName(), name.getAccessibleName());
      panel_.addLeftWidget(panel);
   }

   private ColumnName createSourceColumn()
   {
      PaneConfig.addSourcePane();
      ColumnName name = sourceColumnManager_.add();
      additionalSourceCount_ = sourceColumnManager_.getSize() - 1;
      return name;
   }

   private Widget createSourceColumnWindow(String name, String accessibleName)
   {
      if (panesByName_.get(name) != null)
         return panesByName_.get(name).getNormal();
      panesByName_.put(name, createSource(name, accessibleName, sourceColumnManager_.getWidget(name)));

      PaneConfig paneConfig = getCurrentConfig();
      userPrefs_.panes().setGlobalValue(PaneConfig.create(
         JsArrayUtil.copy(paneConfig.getQuadrants()),
         paneConfig.getTabSet1(),
         paneConfig.getTabSet2(),
         paneConfig.getHiddenTabSet(),
         paneConfig.getConsoleLeftOnTop(),
         paneConfig.getConsoleRightOnTop(),
         additionalSourceCount_).cast());
      userPrefs_.writeUserPrefs();

      return panesByName_.get(name).getNormal();
   }

   @SuppressWarnings("unlikely-arg-type")
   public void closeSourceWindow(String name)
   {
      // hide the original source window
      if (StringUtil.equals(name, SourceColumnManager.MAIN_SOURCE_NAME))
         getSourceLogicalWindow().onWindowStateChange(
               new WindowStateChangeEvent(WindowState.HIDE));
      else
      {
         SourceColumn column = sourceColumnManager_.getByName(name);
         if (column != null)
            sourceColumnManager_.closeColumn(column, true);

         for (LogicalWindow window : sourceLogicalWindows_)
         {
            if (window.equals(panesByName_.get(name)))
            {
               leftList_.remove(window);
               panesByName_.remove(name);
               panel_.removeLeftWidget(window.getNormal());
               sourceLogicalWindows_.remove(window);
               break;
            }
         }

         additionalSourceCount_ = sourceColumnManager_.getSize() - 1;
         PaneConfig paneConfig = getCurrentConfig();
         userPrefs_.panes().setGlobalValue(PaneConfig.create(
            JsArrayUtil.copy(paneConfig.getQuadrants()),
            paneConfig.getTabSet1(),
            paneConfig.getTabSet2(),
            paneConfig.getHiddenTabSet(),
            paneConfig.getConsoleLeftOnTop(),
            paneConfig.getConsoleRightOnTop(),
            additionalSourceCount_).cast());
         userPrefs_.writeUserPrefs();
      }
   }

   private DualWindowLayoutPanel createSplitWindow(LogicalWindow top,
                                                   LogicalWindow bottom,
                                                   String name,
                                                   double bottomDefaultPct,
                                                   int splitterSize)
   {
      return new DualWindowLayoutPanel(
            eventBus_,
            top,
            bottom,
            session_,
            name,
            WindowState.NORMAL,
            (int) (Window.getClientHeight()*bottomDefaultPct),
            splitterSize);
   }

   private LogicalWindow createConsole()
   {
      String frameName = PaneManager.CONSOLE_PANE;
      
      PrimaryWindowFrame frame = new PrimaryWindowFrame(frameName, null);
      frame.setTitleWidget(new ConsoleInterpreterVersion());

      ToolbarButton goToWorkingDirButton =
            commands_.goToWorkingDir().createToolbarButton();
      goToWorkingDirButton.addStyleName(
            ThemeResources.INSTANCE.themeStyles().windowFrameToolbarButton());

      LogicalWindow logicalWindow =
            new LogicalWindow(frame, new MinimizedWindowFrame(frameName, frameName));

      consoleTabPanel_ = new ConsoleTabPanel(
            frame,
            logicalWindow,
            consolePane_,
            compilePdfTab_,
            findOutputTab_,
            sourceCppTab_,
            renderRmdTab_,
            deployContentTab_,
            markersTab_,
            terminalTab_,
            eventBus_,
            goToWorkingDirButton,
            testsTab_,
            dataTab_,
            jobsTab_,
            launcherJobsTab_);

      consoleTabPanel_.addLayoutStyles(frame.getElement());

      return logicalWindow;
   }

   private LogicalWindow createSource(String frameName, String accessibleName, Widget display)
   {
      WindowFrame sourceFrame = new WindowFrame(frameName, accessibleName);
      sourceFrame.setFillWidget(display);
      LogicalWindow sourceWindow = new LogicalWindow(
            sourceFrame,
            new MinimizedWindowFrame(frameName, accessibleName));
      sourceWindow.transitionToState(WindowState.NORMAL);
      sourceLogicalWindows_.add(sourceWindow);
      return sourceWindow;
   }

   private
         Triad<LogicalWindow, WorkbenchTabPanel, MinimizedModuleTabLayoutPanel>
         createTabSet(String persisterName, ArrayList<Tab> tabs)
   {
      final WindowFrame frame = new WindowFrame(persisterName, persisterName);
      final MinimizedModuleTabLayoutPanel minimized = new MinimizedModuleTabLayoutPanel(persisterName);
      final LogicalWindow logicalWindow = new LogicalWindow(frame, minimized);

      final WorkbenchTabPanel tabPanel = new WorkbenchTabPanel(frame, logicalWindow, persisterName);

      if (StringUtil.equals(persisterName, UserPrefsAccessor.Panes.QUADRANTS_TABSET1))
         tabs1_ = tabs;
      else if (StringUtil.equals(persisterName, UserPrefsAccessor.Panes.QUADRANTS_TABSET2))
         tabs2_ = tabs;
      else if (StringUtil.equals(persisterName, UserPrefsAccessor.Panes.QUADRANTS_HIDDENTABSET))
         hiddenTabs_ = tabs;

      populateTabPanel(tabs, tabPanel, minimized);

      frame.setFillWidget(tabPanel);

      minimized.addSelectionHandler(integerSelectionEvent ->
      {
         int tab = integerSelectionEvent.getSelectedItem();
         tabPanel.selectTab(tab);
      });

      tabPanel.addSelectionHandler(integerSelectionEvent ->
      {
         int index = integerSelectionEvent.getSelectedItem();
         WorkbenchTab selected = tabPanel.getTab(index);
         lastSelectedTab_ = workbenchTabToTab(selected);
         session_.persistClientState();
      });

      if (!StringUtil.equals(persisterName, UserPrefsAccessor.Panes.QUADRANTS_HIDDENTABSET))
         new SelectedTabStateValue(persisterName, tabPanel);

      return new Triad<>(
         logicalWindow,
         tabPanel,
         minimized);
   }

   private Tab workbenchTabToTab(WorkbenchTab tab)
   {
      return wbTabToTab_.get(tab);
   }

   private void populateTabPanel(ArrayList<Tab> tabs,
                                 WorkbenchTabPanel tabPanel,
                                 MinimizedModuleTabLayoutPanel minimized)
   {
      ArrayList<WorkbenchTab> tabList = new ArrayList<>();
      int tabIdx = 0;
      for (Tab tab : tabs)
      {
         WorkbenchTab wbTab = getTab(tab);

         wbTabToTab_.put(wbTab, tab);
         tabToPanel_.put(tab, tabPanel);

         // exclude suppressed tabs from the index since they aren't added to
         // the panel
         if (!wbTab.isSuppressed())
            tabToIndex_.put(tab, tabIdx++);

         tabList.add(wbTab);
      }
      tabPanel.setTabs(tabList);

      ArrayList<String> labels = new ArrayList<>();
      for (Tab tab : tabs)
      {
         if (!getTab(tab).isSuppressed())
            labels.add(getTabLabel(tab));
      }
      minimized.setTabs(labels.toArray(new String[labels.size()]));
   }

   private String getTabLabel(Tab tab)
   {
      switch (tab)
      {
         case VCS:
         case Presentation:
         // The "Presentations" tab should always be displayed as "Presentation" (since 
         // the tab only shows a single presentation at a time. We named it "Presentations"
         // under the hood so it wouldn't conflict in config with the existing 
         // Presentation tab
         case Presentations:
         case Connections:
            return getTab(tab).getTitle();
         default:
            return tab.toString();
      }
   }

   private Tab tabForName(String name)
   {
      if (name.equalsIgnoreCase(HISTORY_PANE))
         return Tab.History;
      if (name.equalsIgnoreCase(FILES_PANE))
         return Tab.Files;
      if (name.equalsIgnoreCase(PLOTS_PANE))
         return Tab.Plots;
      if (name.equalsIgnoreCase(PACKAGES_PANE))
         return Tab.Packages;
      if (name.equalsIgnoreCase(HELP_PANE))
         return Tab.Help;
      if (name.equalsIgnoreCase(VCS_PANE))
         return Tab.VCS;
      if (name.equalsIgnoreCase(TUTORIAL_PANE))
         return Tab.Tutorial;
      if (name.equalsIgnoreCase(BUILD_PANE))
         return Tab.Build;
      if (name.equalsIgnoreCase(PRESENTATION_PANE))
         return Tab.Presentation;
      if (name.equalsIgnoreCase(PRESENTATIONS_PANE))
         return Tab.Presentations;
      if (name.equalsIgnoreCase(ENVIRONMENT_PANE))
         return Tab.Environment;
      if (name.equalsIgnoreCase(VIEWER_PANE))
         return Tab.Viewer;
      if (name.equalsIgnoreCase(CONNECTIONS_PANE))
         return Tab.Connections;
      if (name.equalsIgnoreCase(UserPrefsAccessor.Panes.QUADRANTS_SOURCE))
         return Tab.Source;
      if (name.equalsIgnoreCase(CONSOLE_PANE))
         return Tab.Console;
      if (name.equalsIgnoreCase(SOURCE_COLUMN))
         return Tab.SourceColumn;

      return null;
   }

   private AppCommand getLayoutCommandForTab(Tab tab)
   {
      if (tab == null)
         return commands_.layoutEndZoom();

      switch (tab)
      {
      case Build:        return commands_.layoutZoomBuild();
      case Console:      return commands_.layoutZoomConsole();
      case Environment:  return commands_.layoutZoomEnvironment();
      case Files:        return commands_.layoutZoomFiles();
      case Help:         return commands_.layoutZoomHelp();
      case History:      return commands_.layoutZoomHistory();
      case Packages:     return commands_.layoutZoomPackages();
      case Plots:        return commands_.layoutZoomPlots();
      case Source:       return commands_.layoutZoomSource();
      case SourceColumn: return commands_.layoutZoomSource();
      case VCS:          return commands_.layoutZoomVcs();
      case Tutorial:     return commands_.layoutZoomTutorial();
      case Viewer:       return commands_.layoutZoomViewer();
      case Connections:  return commands_.layoutZoomConnections();
      case Presentations: return commands_.layoutZoomPresentation2();
      default:
         throw new IllegalArgumentException("Unexpected tab '" + tab.toString() + "'");
      }
   }

   private void manageLayoutCommands()
   {
      List<AppCommand> layoutCommands = getLayoutCommands();
      AppCommand activeCommand = getLayoutCommandForTab(maximizedTab_);

      for (AppCommand command : layoutCommands)
         command.setChecked(activeCommand.equals(command));

      // manage console left/right commands
      boolean maximized = maximizedTab_ != null;
      commands_.layoutConsoleOnLeft().setVisible(!maximized);
      commands_.layoutConsoleOnRight().setVisible(!maximized);
      if (!maximized)
      {
         PaneConfig config = getCurrentConfig();
         commands_.layoutConsoleOnLeft().setChecked(config.getConsoleLeft());
         commands_.layoutConsoleOnRight().setChecked(config.getConsoleRight());
      }
      else
      {
         commands_.layoutConsoleOnLeft().setVisible(false);
         commands_.layoutConsoleOnRight().setVisible(false);
      }

      manageZoomColumnCommands();
   }

   private void manageZoomColumnCommands()
   {
      boolean zoomLeftChecked = false;
      boolean zoomRightChecked = false;

      String column = getZoomedColumn();
      if (StringUtil.equals(column, LEFT_COLUMN))
         zoomLeftChecked = true;
      else if (StringUtil.equals(column, RIGHT_COLUMN))
         zoomRightChecked = true;

      commands_.layoutZoomLeftColumn().setChecked(zoomLeftChecked);
      commands_.layoutZoomRightColumn().setChecked(zoomRightChecked);
   }

   private List<AppCommand> getLayoutCommands()
   {
      List<AppCommand> commands = new ArrayList<>();

      commands.add(commands_.layoutEndZoom());
      commands.add(commands_.layoutZoomBuild());
      commands.add(commands_.layoutZoomConsole());
      commands.add(commands_.layoutZoomEnvironment());
      commands.add(commands_.layoutZoomFiles());
      commands.add(commands_.layoutZoomHelp());
      commands.add(commands_.layoutZoomHistory());
      commands.add(commands_.layoutZoomPackages());
      commands.add(commands_.layoutZoomPlots());
      commands.add(commands_.layoutZoomSource());
      commands.add(commands_.layoutZoomVcs());
      commands.add(commands_.layoutZoomTutorial());
      commands.add(commands_.layoutZoomViewer());
      commands.add(commands_.layoutZoomConnections());
      commands.add(commands_.layoutZoomPresentation2());

      return commands;
   }

   private int getColumnCount()
   {
      return additionalSourceCount_ + 2;
   }

   private PaneConfig getCurrentConfig()
   {
      PaneConfig config = userPrefs_.panes().getValue().cast();

      // use default config if pref isn't set yet
      if (config == null)
         return PaneConfig.createDefault();

      return config;
   }

   private void clearFocusIndicator()
   {
      if (lastFocusedWindow_ != null)
      {
         lastFocusedWindow_.showWindowFocusIndicator(false);
         lastFocusedWindow_ = null;
      }
   }

   private final EventBus eventBus_;
   private final Session session_;
   private final Commands commands_;
   private final UserPrefs userPrefs_;
   private final FindOutputTab findOutputTab_;
   private final WorkbenchTab compilePdfTab_;
   private final WorkbenchTab sourceCppTab_;
   private final ConsolePane consolePane_;
   private final FileTypeRegistry fileTypeRegistry_;
   private final Source source_;
   private final SourceColumnManager sourceColumnManager_;
   private final WorkbenchTab historyTab_;
   private final WorkbenchTab filesTab_;
   private final WorkbenchTab plotsTab_;
   private final WorkbenchTab packagesTab_;
   private final WorkbenchTab helpTab_;
   private final WorkbenchTab vcsTab_;
   private final WorkbenchTab buildTab_;
   private final WorkbenchTab presentationTab_;
   private final WorkbenchTab presentation2Tab_;
   private final WorkbenchTab connectionsTab_;
   private final WorkbenchTab environmentTab_;
   private final WorkbenchTab viewerTab_;
   private final WorkbenchTab renderRmdTab_;
   private final WorkbenchTab deployContentTab_;
   private final MarkersOutputTab markersTab_;
   private final WorkbenchTab terminalTab_;
   private final WorkbenchTab testsTab_;
   private final WorkbenchTab jobsTab_;
   private final WorkbenchTab launcherJobsTab_;
   private final WorkbenchTab dataTab_;
   private final WorkbenchTab tutorialTab_;
   private final OptionsLoader.Shim optionsLoader_;
   private final Provider<GlobalDisplay> pGlobalDisplay_;
   private final MainSplitPanel panel_;
   private final ArrayList<LogicalWindow> sourceLogicalWindows_ = new ArrayList<>();
   private final HashMap<Tab, WorkbenchTabPanel> tabToPanel_ = new HashMap<>();
   private final HashMap<Tab, Integer> tabToIndex_ = new HashMap<>();
   private final HashMap<WorkbenchTab, Tab> wbTabToTab_ = new HashMap<>();
   private HashMap<String, LogicalWindow> panesByName_;
   private final DualWindowLayoutPanel center_;
   private final DualWindowLayoutPanel right_;
   private final ArrayList<Widget> leftList_ = new ArrayList<>();
   private ArrayList<LogicalWindow> panes_;
   private ConsoleTabPanel consoleTabPanel_;
   private WorkbenchTabPanel tabSet1TabPanel_;
   private MinimizedModuleTabLayoutPanel tabSet1MinPanel_;
   private WorkbenchTabPanel tabSet2TabPanel_;
   private MinimizedModuleTabLayoutPanel tabSet2MinPanel_;
   private WorkbenchTabPanel hiddenTabSetTabPanel_;
   private MinimizedModuleTabLayoutPanel hiddenTabSetMinPanel_;
   private WorkbenchTabPanel sidebarTabPanel_;
   private MinimizedModuleTabLayoutPanel sidebarMinPanel_;
   private Widget sidebar_;
   private ArrayList<Tab> sidebarTabs_;

   // Zoom-related members ----
   private Tab lastSelectedTab_ = null;
   private LogicalWindow maximizedWindow_ = null;
   private LogicalWindow lastFocusedWindow_ = null;
   private Tab maximizedTab_ = null;
   private double widgetSizePriorToZoom_ = -1;
   private boolean isAnimating_ = false;
   private final ArrayList<Double> leftWidgetSizePriorToZoom_ = new ArrayList<>();

   private ArrayList<Tab> tabs1_;
   private ArrayList<Tab> tabs2_;
   private ArrayList<Tab> hiddenTabs_;

   private int additionalSourceCount_; // this does not include the main source
   public final static int MAX_COLUMN_COUNT = 3;
   private static final UIConstants constants_ = GWT.create(UIConstants.class);
   private static final ConsoleConstants consoleConstants_ = GWT.create(ConsoleConstants.class);

   // Internal identifiers for the workbench tabs
   public static final String CONSOLE_PANE = "Console"; //$NON-NLS-1$
   public static final String HISTORY_PANE = "History"; //$NON-NLS-1$
   public static final String FILES_PANE = "Files"; //$NON-NLS-1$
   public static final String PLOTS_PANE = "Plots"; //$NON-NLS-1$
   public static final String PACKAGES_PANE = "Packages"; //$NON-NLS-1$
   public static final String HELP_PANE = "Help"; //$NON-NLS-1$
   public static final String VCS_PANE = "VCS"; //$NON-NLS-1$
   public static final String BUILD_PANE = "Build"; //$NON-NLS-1$
   public static final String PRESENTATION_PANE = "Presentation"; //$NON-NLS-1$
   public static final String PRESENTATIONS_PANE = "Presentations"; //$NON-NLS-1$
   public static final String CONNECTIONS_PANE = "Connections"; //$NON-NLS-1$
   public static final String ENVIRONMENT_PANE = "Environment"; //$NON-NLS-1$
   public static final String VIEWER_PANE = "Viewer"; //$NON-NLS-1$
   public static final String COMPILE_PDF_PANE = "Compile PDF"; //$NON-NLS-1$
   public static final String SOURCE_CPP_PANE = "Source Cpp"; //$NON-NLS-1$
   public static final String RMARKDOWN_PANE = "R Markdown"; //$NON-NLS-1$
   public static final String DEPLOY_PANE = "Deploy"; //$NON-NLS-1$
   public static final String TERMINAL_PANE = "Terminal"; //$NON-NLS-1$
   public static final String TESTS_PANE = "Tests"; //$NON-NLS-1$
   public static final String JOBS_PANE = "Jobs"; //$NON-NLS-1$
   public static final String LAUNCHER_PANE = "Launcher"; //$NON-NLS-1$
   public static final String DATA_OUTPUT_PANE = "Data Output"; //$NON-NLS-1$
   public static final String TUTORIAL_PANE = "Tutorial"; //$NON-NLS-1$
   public static final String SOURCE_COLUMN = "SourceColumn"; //$NON-NLS-1$
   public static final String FIND_PANE = "Find"; //$NON-NLS-1$
   public static final String MARKERS_PANE = "Markers"; //$NON-NLS-1$
}
