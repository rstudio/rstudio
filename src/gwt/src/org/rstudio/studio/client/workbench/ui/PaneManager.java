/*
 * PaneManager.java
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
package org.rstudio.studio.client.workbench.ui;

import com.google.gwt.animation.client.Animation;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.SplitterResizedEvent;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.JsArrayUtil;
import org.rstudio.core.client.Triad;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.events.ManageLayoutCommandsEvent;
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
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.ZoomPaneEvent;
import org.rstudio.studio.client.workbench.model.ClientState;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.WorkbenchServerOperations;
import org.rstudio.studio.client.workbench.model.helper.IntStateValue;
import org.rstudio.studio.client.workbench.model.helper.JSObjectStateValue;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.prefs.views.PaneLayoutPreferencesPane;
import org.rstudio.studio.client.workbench.views.console.ConsolePane;
import org.rstudio.studio.client.workbench.views.output.find.FindOutputTab;
import org.rstudio.studio.client.workbench.views.output.markers.MarkersOutputTab;
import org.rstudio.studio.client.workbench.views.source.SourceShim;
import org.rstudio.studio.client.workbench.views.source.SourceWindowManager;
import org.rstudio.studio.client.workbench.views.source.model.SourceDocument;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/*
 * TODO: Push client state when selected tab or layout changes
 */

public class PaneManager
{
   public interface Binder extends CommandBinder<Commands, PaneManager> {}
   
   public enum Tab {
      History, Files, Plots, Packages, Help, VCS, Build, Connections,
      Presentation, Environment, Viewer, Source, Console
   }
   
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
         if (value != null)
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
            return !oldValue.getString(MAXIMIZED_TAB_KEY).equals(newValue.getString(MAXIMIZED_TAB_KEY));
         
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
                      UIPrefs uiPrefs,
                      @Named("Console") final Widget consolePane,
                      SourceShim source,
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
                      final MarkersOutputTab markersTab,
                      final FindOutputTab findOutputTab,
                      OptionsLoader.Shim optionsLoader)
   {
      eventBus_ = eventBus;
      session_ = session;
      commands_ = commands;
      uiPrefs_ = uiPrefs;
      consolePane_ = (ConsolePane)consolePane;
      source_ = source;
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
      optionsLoader_ = optionsLoader;
      
      binder.bind(commands, this);
      
      PaneConfig config = validateConfig(uiPrefs.paneConfig().getValue());
      initPanes(config);

      panes_ = createPanes(config);
      left_ = createSplitWindow(panes_.get(0), panes_.get(1), "left", 0.4);
      right_ = createSplitWindow(panes_.get(2), panes_.get(3), "right", 0.6);

      panel_ = pSplitPanel.get();
      panel_.initialize(left_, right_);
      
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
      
      if (numDocs == 0 && sourceLogicalWindow_.getState() != WindowState.HIDE)
      {
         sourceLogicalWindow_.onWindowStateChange(
               new WindowStateChangeEvent(WindowState.HIDE));
      }
      else if (numDocs > 0
               && sourceLogicalWindow_.getState() == WindowState.HIDE)
      {
         sourceLogicalWindow_.onWindowStateChange(
               new WindowStateChangeEvent(WindowState.NORMAL));
      }

      uiPrefs.paneConfig().addValueChangeHandler(new ValueChangeHandler<PaneConfig>()
      {
         public void onValueChange(ValueChangeEvent<PaneConfig> evt)
         {
            ArrayList<LogicalWindow> newPanes = createPanes(validateConfig(evt.getValue()));
            panes_ = newPanes;
            left_.replaceWindows(newPanes.get(0), newPanes.get(1));
            right_.replaceWindows(newPanes.get(2), newPanes.get(3));

            tabSet1TabPanel_.clear();
            tabSet2TabPanel_.clear();
            populateTabPanel(tabNamesToTabs(evt.getValue().getTabSet1()),
                             tabSet1TabPanel_, tabSet1MinPanel_);
            populateTabPanel(tabNamesToTabs(evt.getValue().getTabSet2()),
                             tabSet2TabPanel_, tabSet2MinPanel_);
            
            manageLayoutCommands();
         }
      });
      
      eventBus_.addHandler(ZoomPaneEvent.TYPE, new ZoomPaneEvent.Handler()
      {
         @Override
         public void onZoomPane(ZoomPaneEvent event)
         {
            String pane = event.getPane();
            LogicalWindow window = panesByName_.get(pane);
            assert window != null :
               "No pane with name '" + pane + "'";
            
            toggleWindowZoom(window, tabForName(event.getTab()));
         }
      });
      
      eventBus_.addHandler(WindowEnsureVisibleEvent.TYPE, new WindowEnsureVisibleEvent.Handler()
      {
         @Override
         public void onWindowEnsureVisible(WindowEnsureVisibleEvent event)
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
            
            final Command afterAnimation = new Command()
            {
               @Override
               public void execute()
               {
                  window.getNormal().onResize();
               }
            };
            
            int newWidth = computeAppropriateWidth();
            resizeHorizontally(0, newWidth, afterAnimation);
         }
      });
      
      eventBus_.addHandler(
            ManageLayoutCommandsEvent.TYPE,
            new ManageLayoutCommandsEvent.Handler()
            {
               @Override
               public void onManageLayoutCommands(ManageLayoutCommandsEvent event)
               {
                  manageLayoutCommands();
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
   
   
   private void swapConsolePane(PaneConfig paneConfig, int consoleTargetIndex)
   {
      int consoleCurrentIndex = paneConfig.getConsoleIndex();
      if (consoleCurrentIndex != consoleTargetIndex)
      {
         JsArrayString panes = JsArrayUtil.copy(paneConfig.getPanes());
         panes.set(consoleCurrentIndex, panes.get(consoleTargetIndex));
         panes.set(consoleTargetIndex, "Console");
         uiPrefs_.paneConfig().setGlobalValue(PaneConfig.create(
            panes, 
            paneConfig.getTabSet1(), 
            paneConfig.getTabSet2(),
            paneConfig.getConsoleLeftOnTop(),
            paneConfig.getConsoleRightOnTop()));
         uiPrefs_.writeUIPrefs();
      }
   }
   
   @Handler
   public void onPaneLayout()
   {
      optionsLoader_.showOptions(PaneLayoutPreferencesPane.class);
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
         onActivation = new Command()
         {
            @Override
            public void execute()
            {
               commands_.activateHelp().execute();
            }
         };
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
      horizontalResizeAnimation(start, end, afterComplete).run(300);
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
      ArrayList<LogicalWindow> results = new ArrayList<LogicalWindow>();

      JsArrayString panes = config.getPanes();
      for (int i = 0; i < 4; i++)
      {
         results.add(panesByName_.get(panes.get(i)));
      }
      return results;
   }

   private void initPanes(PaneConfig config)
   {
      panesByName_ = new HashMap<String, LogicalWindow>();
      panesByName_.put("Console", createConsole());
      panesByName_.put("Source", createSource());

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
   }
   
   private ArrayList<Tab> tabNamesToTabs(JsArrayString tabNames)
   {
      ArrayList<Tab> tabList = new ArrayList<Tab>();
      for (int j = 0; j < tabNames.length(); j++)
         tabList.add(Enum.valueOf(Tab.class, tabNames.get(j)));
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
                                  vcsTab_, buildTab_, presentationTab_,
                                  environmentTab_, viewerTab_,
                                  connectionsTab_};
   }

   public void activateTab(Tab tab)
   {
      lastSelectedTab_ = tab;
      WorkbenchTabPanel panel = getOwnerTabPanel(tab);
      
      // Ensure that the pane is visible (otherwise tab selection will fail)
      LogicalWindow parent = panel.getParentWindow();
      if (parent.getState() == WindowState.MINIMIZE ||
          parent.getState() == WindowState.HIDE)
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
      return sourceLogicalWindow_;
   }
   
   public LogicalWindow getConsoleLogicalWindow()
   {
      return panesByName_.get("Console");
   }

   private DualWindowLayoutPanel createSplitWindow(LogicalWindow top,
                                                   LogicalWindow bottom,
                                                   String name,
                                                   double bottomDefaultPct)
   {
      return new DualWindowLayoutPanel(
            eventBus_,
            top,
            bottom,
            session_,
            name,
            WindowState.NORMAL,
            (int) (Window.getClientHeight()*bottomDefaultPct));
   }

   private LogicalWindow createConsole()
   {
      PrimaryWindowFrame frame = new PrimaryWindowFrame("Console", null);
      
      ToolbarButton goToWorkingDirButton =
            commands_.goToWorkingDir().createToolbarButton();
      goToWorkingDirButton.addStyleName(
            ThemeResources.INSTANCE.themeStyles().windowFrameToolbarButton());
      
      LogicalWindow logicalWindow =
            new LogicalWindow(frame, new MinimizedWindowFrame("Console"));

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
            goToWorkingDirButton);
      
      consoleTabPanel_.addLayoutStyles(frame.getElement());
      
      return logicalWindow;
   }

   private LogicalWindow createSource()
   {
      WindowFrame sourceFrame = new WindowFrame();
      sourceFrame.setFillWidget(source_.asWidget());
      source_.forceLoad();
      return sourceLogicalWindow_ = new LogicalWindow(
            sourceFrame,
            new MinimizedWindowFrame("Source"));
   }

   private
         Triad<LogicalWindow, WorkbenchTabPanel, MinimizedModuleTabLayoutPanel>
         createTabSet(String persisterName, ArrayList<Tab> tabs)
   {
      final WindowFrame frame = new WindowFrame();
      final MinimizedModuleTabLayoutPanel minimized = new MinimizedModuleTabLayoutPanel();
      final LogicalWindow logicalWindow = new LogicalWindow(frame, minimized);

      final WorkbenchTabPanel tabPanel = new WorkbenchTabPanel(frame, logicalWindow);

      populateTabPanel(tabs, tabPanel, minimized);

      frame.setFillWidget(tabPanel);

      minimized.addSelectionHandler(new SelectionHandler<Integer>()
      {
         public void onSelection(SelectionEvent<Integer> integerSelectionEvent)
         {
            int tab = integerSelectionEvent.getSelectedItem();
            tabPanel.selectTab(tab);
         }
      });

      tabPanel.addSelectionHandler(new SelectionHandler<Integer>()
      {
         public void onSelection(SelectionEvent<Integer> integerSelectionEvent)
         {
            int index = integerSelectionEvent.getSelectedItem();
            WorkbenchTab selected = tabPanel.getTab(index);
            lastSelectedTab_ = workbenchTabToTab(selected);
            session_.persistClientState();
         }
      });

      new SelectedTabStateValue(persisterName, tabPanel);

      return new Triad<LogicalWindow, WorkbenchTabPanel, MinimizedModuleTabLayoutPanel>(
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
      ArrayList<WorkbenchTab> tabList = new ArrayList<WorkbenchTab>();
      int tabIdx = 0;
      for (int i = 0; i < tabs.size(); i++)
      {
         Tab tab = tabs.get(i);
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

      ArrayList<String> labels = new ArrayList<String>();
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
            return getTab(tab).getTitle();
         case Presentation:
            return getTab(tab).getTitle();
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
   }
   
   private List<AppCommand> getLayoutCommands()
   {
      List<AppCommand> commands = new ArrayList<AppCommand>();
      
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
      commands.add(commands_.layoutZoomViewer());
      commands.add(commands_.layoutZoomConnections());
      
      return commands;
   }
   
   private PaneConfig getCurrentConfig()
   {
      PaneConfig config = uiPrefs_.paneConfig().getValue();

      // use default config if pref isn't set yet
      if (config == null)
         return PaneConfig.createDefault();

      return config;
   }

   private final EventBus eventBus_;
   private final Session session_;
   private final Commands commands_;
   private final UIPrefs uiPrefs_;
   private final FindOutputTab findOutputTab_;
   private final WorkbenchTab compilePdfTab_;
   private final WorkbenchTab sourceCppTab_;
   private final ConsolePane consolePane_;
   private final SourceShim source_;
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
   private final OptionsLoader.Shim optionsLoader_;
   private MainSplitPanel panel_;
   private LogicalWindow sourceLogicalWindow_;
   private final HashMap<Tab, WorkbenchTabPanel> tabToPanel_ =
         new HashMap<Tab, WorkbenchTabPanel>();
   private final HashMap<Tab, Integer> tabToIndex_ =
         new HashMap<Tab, Integer>();
   private final HashMap<WorkbenchTab, Tab> wbTabToTab_ =
         new HashMap<WorkbenchTab, Tab>();
   private HashMap<String, LogicalWindow> panesByName_;
   private DualWindowLayoutPanel left_;
   private DualWindowLayoutPanel right_;
   private ArrayList<LogicalWindow> panes_;
   private ConsoleTabPanel consoleTabPanel_;
   private WorkbenchTabPanel tabSet1TabPanel_;
   private MinimizedModuleTabLayoutPanel tabSet1MinPanel_;
   private WorkbenchTabPanel tabSet2TabPanel_;
   private MinimizedModuleTabLayoutPanel tabSet2MinPanel_;
   
   // Zoom-related members ----
   private Tab lastSelectedTab_ = null;
   private LogicalWindow maximizedWindow_ = null;
   private Tab maximizedTab_ = null;
   private double widgetSizePriorToZoom_ = -1;
   private boolean isAnimating_ = false;
}
