/*
 * PaneManager.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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
package org.rstudio.studio.client.workbench.ui;

import com.google.gwt.animation.client.Animation;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.SplitterResizedEvent;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import com.google.inject.name.Named;

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
import org.rstudio.core.client.events.UpdateTabPanelsEvent;
import org.rstudio.core.client.events.WindowEnsureVisibleEvent;
import org.rstudio.core.client.events.WindowStateChangeEvent;
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
import org.rstudio.studio.client.application.ui.RStudioThemes;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.ZoomPaneEvent;
import org.rstudio.studio.client.workbench.model.ClientState;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.WorkbenchServerOperations;
import org.rstudio.studio.client.workbench.model.helper.IntStateValue;
import org.rstudio.studio.client.workbench.model.helper.JSObjectStateValue;
import org.rstudio.studio.client.workbench.prefs.events.UserPrefsChangedEvent;
import org.rstudio.studio.client.workbench.prefs.events.UserPrefsChangedHandler;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.prefs.views.PaneLayoutPreferencesPane;
import org.rstudio.studio.client.workbench.views.console.ConsolePane;
import org.rstudio.studio.client.workbench.views.output.find.FindOutputTab;
import org.rstudio.studio.client.workbench.views.output.markers.MarkersOutputTab;
import org.rstudio.studio.client.workbench.views.source.Source;
import org.rstudio.studio.client.workbench.views.source.SourceColumn;
import org.rstudio.studio.client.workbench.views.source.SourceColumnManager;
import org.rstudio.studio.client.workbench.views.source.SourceWindowManager;
import org.rstudio.studio.client.workbench.views.source.model.SourceDocument;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/*
 * TODO: Push client state when selected tab or layout changes
 */

@Singleton
public class PaneManager
{
   public interface Binder extends CommandBinder<Commands, PaneManager> {}

   public enum Tab {
      History, Files, Plots, Packages, Help, VCS, Tutorial, Build, Connections,
      Presentation, Environment, Viewer, Source, Console
   }

   public static final String LEFT_COLUMN = "left";
   public static final String RIGHT_COLUMN = "right";

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
            return oldValue.getString(MAXIMIZED_TAB_KEY) !=
                   newValue.getString(MAXIMIZED_TAB_KEY);

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
                      WorkbenchServerOperations server,
                      EventBus eventBus,
                      Session session,
                      Binder binder,
                      Commands commands,
                      UserPrefs userPrefs,
                      @Named("Console") final Widget consolePane,
                      Source source,
                      SourceColumnManager sourceColumnManager,
                      @Named("History") final WorkbenchTab historyTab,
                      @Named("Files") final WorkbenchTab filesTab,
                      @Named("Plots") final WorkbenchTab plotsTab,
                      @Named("Packages") final WorkbenchTab packagesTab,
                      @Named("Help") final WorkbenchTab helpTab,
                      @Named("VCS") final WorkbenchTab vcsTab,
                      @Named("Build") final WorkbenchTab buildTab,
                      @Named("Presentation") final WorkbenchTab presentationTab,
                      @Named("Connections") final WorkbenchTab connectionsTab,
                      @Named("Environment") final WorkbenchTab environmentTab,
                      @Named("Viewer") final WorkbenchTab viewerTab,
                      @Named("Compile PDF") final WorkbenchTab compilePdfTab,
                      @Named("Source Cpp") final WorkbenchTab sourceCppTab,
                      @Named("R Markdown") final WorkbenchTab renderRmdTab,
                      @Named("Deploy") final WorkbenchTab deployContentTab,
                      @Named("Terminal") final WorkbenchTab terminalTab,
                      @Named("Tests") final WorkbenchTab testsTab,
                      @Named("Jobs") final WorkbenchTab jobsTab,
                      @Named("Launcher") final WorkbenchTab launcherJobsTab,
                      @Named("Data Output") final WorkbenchTab dataTab,
                      @Named("Tutorial") final WorkbenchTab tutorialTab,
                      final MarkersOutputTab markersTab,
                      final FindOutputTab findOutputTab,
                      OptionsLoader.Shim optionsLoader)
   {
      eventBus_ = eventBus;
      session_ = session;
      commands_ = commands;
      userPrefs_ = userPrefs;
      consolePane_ = (ConsolePane)consolePane;
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

      binder.bind(commands, this);

      source_.load();
      loadAdditionalSourceColumns();

      PaneConfig config = validateConfig(userPrefs.panes().getValue().cast());
      initPanes(config);

      int splitterSize = RStudioThemes.isFlat(userPrefs) ? 7 : 3;

      panes_ = createPanes(config);
      left_ = createSplitWindow(panes_.get(0), panes_.get(1), LEFT_COLUMN, 0.4, splitterSize);
      right_ = createSplitWindow(panes_.get(2), panes_.get(3), RIGHT_COLUMN, 0.6, splitterSize);
      panel_ = pSplitPanel.get();

      // get the widgets for the extra source columns to be displayed
      ArrayList<Widget> sourceColumns;
      if (sourceColumnManager_.getSize() > 1 && additionalSourceCount_ > 0)
         sourceColumns = new ArrayList<Widget>(sourceColumnManager_.getWidgets(true));
      else
         sourceColumns =  new ArrayList<Widget>();
      panel_.initialize(sourceColumns, left_, right_);

      // count the number of source docs assigned to this window
      JsArray<SourceDocument> docs =
            session_.getSessionInfo().getSourceDocuments();
      String windowId = SourceWindowManager.getSourceWindowId();
      int numDocs = 0;
      for (int i = 0; i < docs.length(); i++)
      {
         String docWindowId = docs.get(i).getSourceWindowId();
         if (docWindowId == windowId)
         {
            numDocs++;
         }
      }

      for (LogicalWindow window : sourceLogicalWindows_)
      {
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
         left_.replaceWindows(newPanes.get(0), newPanes.get(1));
         right_.replaceWindows(newPanes.get(2), newPanes.get(3));

         tabSet1TabPanel_.clear();
         tabSet2TabPanel_.clear();
         hiddenTabSetTabPanel_.clear();
         tabs1_ = tabNamesToTabs(evt.getValue().getTabSet1());
         populateTabPanel(tabs1_, tabSet1TabPanel_, tabSet1MinPanel_);
         tabs2_ = tabNamesToTabs(evt.getValue().getTabSet2());
         populateTabPanel(tabs2_, tabSet2TabPanel_, tabSet2MinPanel_);
         hiddenTabs_ = tabNamesToTabs(evt.getValue().getHiddenTabSet());
         populateTabPanel(hiddenTabs_, hiddenTabSetTabPanel_, hiddenTabSetMinPanel_);

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

         int width = window.getActiveWidget().getOffsetWidth();

         // If the widget is already visible horizontally, then bail
         // (other logic handles vertical visibility)
         if (width > 0)
            return;

         final Command afterAnimation = () -> window.getNormal().onResize();

         int newWidth = computeAppropriateWidth();
         resizeHorizontally(0, newWidth, afterAnimation);
      });

      eventBus_.addHandler(
            ManageLayoutCommandsEvent.TYPE,
            event -> manageLayoutCommands());

      eventBus.addHandler(UpdateTabPanelsEvent.TYPE, event ->
      {
         left_.replaceWindows(panes_.get(0), panes_.get(1));
         right_.replaceWindows(panes_.get(2), panes_.get(3));

         tabSet1TabPanel_.clear();
         tabSet2TabPanel_.clear();
         populateTabPanel(tabs1_, tabSet1TabPanel_, tabSet1MinPanel_);
         populateTabPanel(tabs2_, tabSet2TabPanel_, tabSet2MinPanel_);
         populateTabPanel(hiddenTabs_, hiddenTabSetTabPanel_, hiddenTabSetMinPanel_);

         manageLayoutCommands();

         activateTab(Enum.valueOf(Tab.class, event.getActiveTab()));
      });

      eventBus.addHandler(UserPrefsChangedEvent.TYPE, new UserPrefsChangedHandler()
      {
         @Override
         public void onUserPrefsChanged(UserPrefsChangedEvent e)
         {
            if (additionalSourceCount_ != userPrefs_.panes().getGlobalValue().getAdditionalSourceColumns())
            {
               syncAdditionalColumnCount(userPrefs_.panes().getGlobalValue().getAdditionalSourceColumns());
            }
         }
      });

      manageLayoutCommands();
      new ZoomedTabStateValue();
   }

   int computeAppropriateWidth()
   {
      double windowWidth = Window.getClientWidth();
      double candidateWidth = 2.0 * windowWidth / 5.0;
      return (int) candidateWidth;
   }

   LogicalWindow getLogicalWindow(WindowFrame frame)
   {
      for (LogicalWindow window : panes_)
         if (window.getNormal() == frame)
            return window;

      return null;
   }

   LogicalWindow getParentLogicalWindow(Element el)
   {
      LogicalWindow targetWindow = null;

      while (el != null && targetWindow == null)
      {
         el = el.getParentElement();
         for (LogicalWindow window : panes_)
         {
            Widget activeWidget = window.getActiveWidget();
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

      eventBus_.fireEvent(new ZoomPaneEvent("Console"));
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

   @Handler
   public void onFocusLeftSeparator()
   {
      left_.focusSplitter();
   }

   @Handler
   public void onFocusRightSeparator()
   {
      right_.focusSplitter();
   }

   @Handler
   public void onFocusCenterSeparator()
   {
      panel_.focusSplitter();
   }

   private void swapConsolePane(PaneConfig paneConfig, int consoleTargetIndex)
   {
      int consoleCurrentIndex = paneConfig.getConsoleIndex();
      if (consoleCurrentIndex != consoleTargetIndex)
      {
         JsArrayString panes = JsArrayUtil.copy(paneConfig.getQuadrants());
         panes.set(consoleCurrentIndex, panes.get(consoleTargetIndex));
         panes.set(consoleTargetIndex, "Console");
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
            // If we're zooming a different tab in the same window,
            // just activate that tab.
            if (!equals(tab, maximizedTab_))
            {
               maximizedTab_ = tab;
               manageLayoutCommands();
               activateTab(tab);
            }

            // Otherwise, we're trying to maximize the same tab
            // and the same window. Interpret this as a toggle off.
            else
            {
               restoreLayout();
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

      boolean isLeftWidget =
            DomUtils.contains(left_.getElement(), window.getActiveWidget().getElement());

      window.onWindowStateChange(new WindowStateChangeEvent(WindowState.EXCLUSIVE));

      final double initialSize = panel_.getWidgetSize(right_);

      double targetSize = isLeftWidget ? 0 : panel_.getOffsetWidth();

      if (targetSize < 0)
         targetSize = 0;

      // Ensure focus is sent to Help iframe on activation.
      Command onActivation = null;
      if (maximizedTab_.equals(Tab.Help))
      {
         onActivation = () -> commands_.activateHelp().execute();
      }

      resizeHorizontally(initialSize, targetSize, onActivation);

   }

   private void resizeHorizontally(final double start,
                                   final double end)
   {
      resizeHorizontally(start, end, null);
   }

   private void resizeHorizontally(final double start,
                                   final double end,
                                   final Command afterComplete)
   {
      int duration = (userPrefs_.reducedMotion().getValue() ? 0 : 300);
      horizontalResizeAnimation(start, end, afterComplete).run(duration);
   }

   private Animation horizontalResizeAnimation(final double start,
                                               final double end,
                                               final Command afterComplete)
   {
      return new Animation()
      {
         @Override
         protected void onUpdate(double progress)
         {
            double size =
                  (1 - progress) * start +
                  progress * end;

            panel_.setWidgetSize(right_, size);
         }

         @Override
         protected void onStart()
         {
            isAnimating_ = true;
            super.onStart();
         }

         @Override
         protected void onComplete()
         {
            isAnimating_ = false;
            panel_.onSplitterResized(new SplitterResizedEvent());
            super.onComplete();
            if (afterComplete != null)
               afterComplete.execute();
         }
      };
   }

   private void restoreLayout()
   {
      // If we're currently zoomed, then use that to provide the previous
      // 'non-zoom' state.
      if (maximizedWindow_ != null)
         restoreSavedLayout();
      else
         restoreFourPaneLayout();
   }

   private void invalidateSavedLayoutState(boolean enableSplitter)
   {
      maximizedWindow_ = null;
      maximizedTab_ = null;
      widgetSizePriorToZoom_ = -1;
      panel_.setSplitterEnabled(enableSplitter);
      manageLayoutCommands();
   }

   private void restoreFourPaneLayout()
   {
      // Ensure that all windows are in the 'normal' state. This allows
      // hidden windows to display themselves, and so on. This also forces
      // widgets to size themselves vertically.
      for (LogicalWindow window : panes_)
         window.onWindowStateChange(new WindowStateChangeEvent(WindowState.NORMAL, true));

      restoreTwoColumnLayout();

   }

   private void restoreTwoColumnLayout()
   {
      double rightWidth = panel_.getWidgetSize(right_);
      double panelWidth = panel_.getOffsetWidth();

      double minThreshold = (2.0 / 5.0) * panelWidth;
      double maxThreshold = (3.0 / 5.0) * panelWidth;

      if (rightWidth <= minThreshold)
         resizeHorizontally(rightWidth, minThreshold);
      else if (rightWidth >= maxThreshold)
         resizeHorizontally(rightWidth, maxThreshold);

      invalidateSavedLayoutState(true);
   }

   private void restoreSavedLayout()
   {
      // Ensure that all windows are in the 'normal' state. This allows
      // hidden windows to display themselves, and so on.
      for (LogicalWindow window : panes_)
         window.onWindowStateChange(new WindowStateChangeEvent(WindowState.NORMAL, true));

      maximizedWindow_.onWindowStateChange(new WindowStateChangeEvent(WindowState.NORMAL, true));
      resizeHorizontally(panel_.getWidgetSize(right_), widgetSizePriorToZoom_);
      invalidateSavedLayoutState(true);
   }

   @Handler
   public void onMaximizeConsole()
   {
      LogicalWindow consoleWindow = panesByName_.get("Console");
      if (consoleWindow.getState() != WindowState.MAXIMIZE)
      {
         consoleWindow.onWindowStateChange(
                        new WindowStateChangeEvent(WindowState.MAXIMIZE));
      }
   }

   private ArrayList<LogicalWindow> createPanes(PaneConfig config)
   {
      ArrayList<LogicalWindow> results = new ArrayList<>();

      JsArrayString panes = config.getQuadrants();
      for (int i = 0; i < panes.length(); i++)
      {
         results.add(panesByName_.get(panes.get(i)));
      }
      return results;
   }

   private void loadAdditionalSourceColumns()
   {
      additionalSourceCount_ = userPrefs_.panes().getGlobalValue().getAdditionalSourceColumns();

      // determine the desired number of source columns (add one for the main source)
      // and add any missing
      while (sourceColumnManager_.getSize() <= additionalSourceCount_)
         sourceColumnManager_.add();
   }

   private void initPanes(PaneConfig config)
   {
      panesByName_ = new HashMap<>();
      panesByName_.put("Console", createConsole());

      ArrayList<SourceColumn> columns = sourceColumnManager_.getColumnList();
      for (SourceColumn column : columns)
      {
         panesByName_.put(column.getName(), createSource(column.getName(), column.asWidget()));
      }

      Triad<LogicalWindow, WorkbenchTabPanel, MinimizedModuleTabLayoutPanel> ts1 = createTabSet(
            "TabSet1",
            tabNamesToTabs(config.getTabSet1()));
      panesByName_.put("TabSet1", ts1.first);
      tabSet1TabPanel_ = ts1.second;
      tabSet1MinPanel_ = ts1.third;

      Triad<LogicalWindow, WorkbenchTabPanel, MinimizedModuleTabLayoutPanel> ts2 = createTabSet(
            "TabSet2",
            tabNamesToTabs(config.getTabSet2()));
      panesByName_.put("TabSet2", ts2.first);
      tabSet2TabPanel_ = ts2.second;
      tabSet2MinPanel_ = ts2.third;

      Triad<LogicalWindow, WorkbenchTabPanel, MinimizedModuleTabLayoutPanel> tsHide = createTabSet(
            "HiddenTabSet",
            tabNamesToTabs(config.getHiddenTabSet()));
      panesByName_.put("HiddenTabSet", tsHide.first);
      hiddenTabSetTabPanel_ = tsHide.second;
      hiddenTabSetTabPanel_.setNeverVisible(true);
      hiddenTabSetMinPanel_ = tsHide.third;
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
         case Environment:
            return environmentTab_;
         case Viewer:
            return viewerTab_;
         case Connections:
            return connectionsTab_;
         case Source:
         case Console:
            // not 'real' tabs so should be an error to ask for their tabs
      }
      throw new IllegalArgumentException("Unknown tab");
   }

   public WorkbenchTab[] getAllTabs()
   {
      return new WorkbenchTab[] { historyTab_, filesTab_,
                                  plotsTab_, packagesTab_, helpTab_,
                                  vcsTab_, tutorialTab_, buildTab_, presentationTab_,
                                  environmentTab_, viewerTab_,
                                  connectionsTab_, jobsTab_, launcherJobsTab_ };
   }

   public void activateTab(Tab tab)
   {
      lastSelectedTab_ = tab;
      WorkbenchTabPanel panel = getOwnerTabPanel(tab);
      LogicalWindow parent = panel.getParentWindow();

      // If the tab belongs to the hidden tabset, add it to one being displayed
      if (parent == panesByName_.get("HiddenTabSet"))
      {
         // Try to find a visible tabSet, if both are hidden - add to tabSet1
         LogicalWindow tabSet1 = panesByName_.get("TabSet1");
         LogicalWindow tabSet2 = panesByName_.get("TabSet2");
         if (tabSet1.visible() || !tabSet2.visible())
         {
            parent = tabSet1;
            panel = tabSet1TabPanel_;
            moveHiddenTabToTabSet1(tab, parent, panel, tabSet1MinPanel_, tabs1_);
         }
         else
         {
            parent = tabSet2;
            panel = tabSet2TabPanel_;
            moveHiddenTabToTabSet2(tab, parent, panel, tabSet2MinPanel_, tabs2_);
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

   private void moveTabToVisiblePanel(Tab tab, LogicalWindow window, WorkbenchTabPanel panel,
                               MinimizedModuleTabLayoutPanel minimized, ArrayList<Tab> tabs)
   {
      // Remove tab from hidden tabSet
      hiddenTabs_.remove(tab);

      // Add tab to the back of the new set
      if (tabs.get(tabs.size() - 1).name() == "Presentation")
         tabs.add(tabs.size() - 1, tab);
      else
         tabs.add(tab);
   }

   private void moveHiddenTabToTabSet1(Tab tab, LogicalWindow window, WorkbenchTabPanel panel,
                                       MinimizedModuleTabLayoutPanel minimized, ArrayList<Tab> tabs)
   {
      moveTabToVisiblePanel(tab, window, panel, minimized, tabs);

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

   private void moveHiddenTabToTabSet2(Tab tab, LogicalWindow window, WorkbenchTabPanel panel,
                                       MinimizedModuleTabLayoutPanel minimized, ArrayList<Tab> tabs)
   {
      moveTabToVisiblePanel(tab, window, panel, minimized, tabs);

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

      double rightZoomPosition = panel_.getOffsetWidth();
      if (MathUtil.isEqual(currentColumnSize, rightZoomPosition, 0.0001))
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
      final double initialSize = panel_.getWidgetSize(right_);

      String currentZoomedColumn = getZoomedColumn();
      double targetSize;
      boolean unZooming = false;

      if (StringUtil.equals(currentZoomedColumn, columnId))
      {
         if (widgetSizePriorToZoom_ < 0)
         {
            // no prior position to restore to, just show defaults
            restoreTwoColumnLayout();
            return;
         }
         targetSize = widgetSizePriorToZoom_;
         unZooming = true;
      }
      else if (StringUtil.equals(columnId, LEFT_COLUMN))
      {
         targetSize = 0;
      }
      else if (StringUtil.equals(columnId, RIGHT_COLUMN))
      {
         targetSize = panel_.getOffsetWidth();
      }
      else
      {
         Debug.logWarning("Unexpected column identifier: " + columnId);
         return;
      }

      if (targetSize < 0)
         targetSize = 0;

      if (unZooming)
         widgetSizePriorToZoom_ = -1;
      else if (widgetSizePriorToZoom_ < 0)
         widgetSizePriorToZoom_ = panel_.getWidgetSize(right_);

      resizeHorizontally(initialSize, targetSize, () -> manageLayoutCommands());
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
      return panesByName_.get("Console");
   }

   public void syncAdditionalColumnCount(int count)
   {
      // make sure additionalSourceCount_ is up to date
      additionalSourceCount_ = sourceColumnManager_.getSize() - 1;

      if (count == additionalSourceCount_)
    	  return;

      if (count > additionalSourceCount_)
      {
         int difference = count - additionalSourceCount_;
         for (int i = 0; i < difference; i++)
         {
            addSourceWindow();
         }
      }
      else
      {
         int difference = additionalSourceCount_ - count;
         sourceColumnManager_.consolidateColumns(difference);
         additionalSourceCount_ = sourceColumnManager_.getSize();
      }
   }

   public int addSourceWindow()
   {
      int id = sourceColumnManager_.getSize();
      PaneConfig.addSourcePane();
      String columnName = sourceColumnManager_.add();
      panesByName_.put(columnName,
                       createSource(columnName,
                                    sourceColumnManager_.getWidget(columnName)));
      panel_.addLeftWidget(sourceColumnManager_.getWidget(columnName));
      additionalSourceCount_ = id;
      sourceColumnManager_.beforeShow(columnName);

      PaneConfig paneConfig = getCurrentConfig();
      userPrefs_.panes().setGlobalValue(PaneConfig.create(
         JsArrayUtil.copy(paneConfig.getQuadrants()),
         paneConfig.getTabSet1(),
         paneConfig.getTabSet2(),
         paneConfig.getHiddenTabSet(),
         paneConfig.getConsoleLeftOnTop(),
         paneConfig.getConsoleRightOnTop(),
         id).cast());
      userPrefs_.writeUserPrefs();
      return id;
   }

   public int closeAllAdditionalColumns()
   {
      sourceColumnManager_.closeAllColumns();
      additionalSourceCount_ = sourceColumnManager_.getSize() - 1;
      if (additionalSourceCount_ > 0)
         Debug.logWarning("Could not close all additional columns. Columns may contain open tabs.");

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

      return additionalSourceCount_;
   }

   public void closeSourceWindow(String name)
   {
      // hide the original source window
      if (StringUtil.equals(name, SourceColumnManager.MAIN_SOURCE_NAME))
         getSourceLogicalWindow().onWindowStateChange(
               new WindowStateChangeEvent(WindowState.HIDE));
      else
      {
         SourceColumn column = sourceColumnManager_.findByName(name);

         if (column.getTabCount() == 0)
         {
            panel_.removeLeftWidget(column.asWidget());
            sourceColumnManager_.closeColumn(name);
            panesByName_.remove(name);

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
      String frameName = "Console";
      PrimaryWindowFrame frame = new PrimaryWindowFrame(frameName, null);

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

   private LogicalWindow createSource(String frameName, Widget display)
   {
      WindowFrame sourceFrame = new WindowFrame(frameName);
      sourceFrame.setFillWidget(display);
      LogicalWindow sourceWindow = new LogicalWindow(
            sourceFrame,
            new MinimizedWindowFrame(frameName, frameName));
      sourceLogicalWindows_.add(sourceWindow);
      return sourceWindow;
   }

   private
         Triad<LogicalWindow, WorkbenchTabPanel, MinimizedModuleTabLayoutPanel>
         createTabSet(String persisterName, ArrayList<Tab> tabs)
   {
      final WindowFrame frame = new WindowFrame(persisterName);
      final MinimizedModuleTabLayoutPanel minimized = new MinimizedModuleTabLayoutPanel(persisterName);
      final LogicalWindow logicalWindow = new LogicalWindow(frame, minimized);

      final WorkbenchTabPanel tabPanel = new WorkbenchTabPanel(frame, logicalWindow, persisterName);

      if (persisterName == "TabSet1")
         tabs1_ = tabs;
      else if (persisterName == "TabSet2")
         tabs2_ = tabs;
      else if (persisterName == "HiddenTabSet")
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

      if (persisterName != "HiddenTabSet")
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
         case Connections:
            return getTab(tab).getTitle();
         default:
            return tab.toString();
      }
   }

   private Tab tabForName(String name)
   {
      if (name.equalsIgnoreCase("history"))
         return Tab.History;
      if (name.equalsIgnoreCase("files"))
         return Tab.Files;
      if (name.equalsIgnoreCase("plots"))
         return Tab.Plots;
      if (name.equalsIgnoreCase("packages"))
         return Tab.Packages;
      if (name.equalsIgnoreCase("help"))
         return Tab.Help;
      if (name.equalsIgnoreCase("vcs"))
         return Tab.VCS;
      if (name.equalsIgnoreCase("tutorial"))
         return Tab.Tutorial;
      if (name.equalsIgnoreCase("build"))
         return Tab.Build;
      if (name.equalsIgnoreCase("presentation"))
         return Tab.Presentation;
      if (name.equalsIgnoreCase("environment"))
         return Tab.Environment;
      if (name.equalsIgnoreCase("viewer"))
         return Tab.Viewer;
      if (name.equalsIgnoreCase("connections"))
         return Tab.Connections;
      if (name.equalsIgnoreCase("source"))
         return Tab.Source;
      if (name.equalsIgnoreCase("console"))
         return Tab.Console;

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
      case VCS:          return commands_.layoutZoomVcs();
      case Tutorial:     return commands_.layoutZoomTutorial();
      case Viewer:       return commands_.layoutZoomViewer();
      case Connections:  return commands_.layoutZoomConnections();
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

      return commands;
   }

   private PaneConfig getCurrentConfig()
   {
      PaneConfig config = userPrefs_.panes().getValue().cast();

      // use default config if pref isn't set yet
      if (config == null)
         return PaneConfig.createDefault();

      return config;
   }

   private final EventBus eventBus_;
   private final Session session_;
   private final Commands commands_;
   private final UserPrefs userPrefs_;
   private final FindOutputTab findOutputTab_;
   private final WorkbenchTab compilePdfTab_;
   private final WorkbenchTab sourceCppTab_;
   private final ConsolePane consolePane_;
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
   private final MainSplitPanel panel_;
   private ArrayList<LogicalWindow> sourceLogicalWindows_ = new ArrayList<LogicalWindow>();
   private final HashMap<Tab, WorkbenchTabPanel> tabToPanel_ = new HashMap<>();
   private final HashMap<Tab, Integer> tabToIndex_ = new HashMap<>();
   private final HashMap<WorkbenchTab, Tab> wbTabToTab_ = new HashMap<>();
   private HashMap<String, LogicalWindow> panesByName_;
   private final DualWindowLayoutPanel left_;
   private final DualWindowLayoutPanel right_;
   private ArrayList<LogicalWindow> panes_;
   private ConsoleTabPanel consoleTabPanel_;
   private WorkbenchTabPanel tabSet1TabPanel_;
   private MinimizedModuleTabLayoutPanel tabSet1MinPanel_;
   private WorkbenchTabPanel tabSet2TabPanel_;
   private MinimizedModuleTabLayoutPanel tabSet2MinPanel_;
   private WorkbenchTabPanel hiddenTabSetTabPanel_;
   private MinimizedModuleTabLayoutPanel hiddenTabSetMinPanel_;

   // Zoom-related members ----
   private Tab lastSelectedTab_ = null;
   private LogicalWindow maximizedWindow_ = null;
   private Tab maximizedTab_ = null;
   private double widgetSizePriorToZoom_ = -1;
   private boolean isAnimating_ = false;

   private ArrayList<Tab> tabs1_;
   private ArrayList<Tab> tabs2_;
   private ArrayList<Tab> hiddenTabs_;

   private int additionalSourceCount_; // this does not include the main source
}
