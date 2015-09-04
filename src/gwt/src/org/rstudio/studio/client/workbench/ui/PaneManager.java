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
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.SplitterResizedEvent;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.Triad;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.events.RequestCurrentlyZoomedTabEvent;
import org.rstudio.core.client.events.WindowEnsureVisibleEvent;
import org.rstudio.core.client.events.WindowStateChangeEvent;
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
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.views.console.ConsoleInterruptButton;
import org.rstudio.studio.client.workbench.views.console.ConsolePane;
import org.rstudio.studio.client.workbench.views.output.find.FindOutputTab;
import org.rstudio.studio.client.workbench.views.output.markers.MarkersOutputTab;
import org.rstudio.studio.client.workbench.views.source.SourceShim;
import org.rstudio.studio.client.workbench.views.source.SourceWindowManager;
import org.rstudio.studio.client.workbench.views.source.model.SourceDocument;

import java.util.ArrayList;
import java.util.HashMap;

/*
 * TODO: Push client state when selected tab or layout changes
 */

public class PaneManager
{
   public interface Binder extends CommandBinder<Commands, PaneManager> {}
   
   public enum Tab {
      History, Files, Plots, Packages, Help, VCS, Build,
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

   @Inject
   public PaneManager(Provider<MainSplitPanel> pSplitPanel,
                      WorkbenchServerOperations server,
                      EventBus eventBus,
                      Session session,
                      Binder binder,
                      Commands commands,
                      UIPrefs uiPrefs,
                      @Named("Console") final Widget consolePane,
                      ConsoleInterruptButton consoleInterrupt,
                      SourceShim source,
                      @Named("History") final WorkbenchTab historyTab,
                      @Named("Files") final WorkbenchTab filesTab,
                      @Named("Plots") final WorkbenchTab plotsTab,
                      @Named("Packages") final WorkbenchTab packagesTab,
                      @Named("Help") final WorkbenchTab helpTab,
                      @Named("VCS") final WorkbenchTab vcsTab,
                      @Named("Build") final WorkbenchTab buildTab,
                      @Named("Presentation") final WorkbenchTab presentationTab,
                      @Named("Environment") final WorkbenchTab environmentTab,
                      @Named("Viewer") final WorkbenchTab viewerTab,
                      @Named("Compile PDF") final WorkbenchTab compilePdfTab,
                      @Named("Source Cpp") final WorkbenchTab sourceCppTab,
                      @Named("R Markdown") final WorkbenchTab renderRmdTab,
                      @Named("Deploy") final WorkbenchTab deployContentTab,
                      final MarkersOutputTab markersTab,
                      final FindOutputTab findOutputTab)
   {
      eventBus_ = eventBus;
      session_ = session;
      commands_ = commands;
      consolePane_ = (ConsolePane)consolePane;
      consoleInterrupt_ = consoleInterrupt;
      source_ = source;
      historyTab_ = historyTab;
      filesTab_ = filesTab;
      plotsTab_ = plotsTab;
      packagesTab_ = packagesTab;
      helpTab_ = helpTab;
      vcsTab_ = vcsTab;
      buildTab_ = buildTab;
      presentationTab_ = presentationTab;
      environmentTab_ = environmentTab;
      viewerTab_ = viewerTab;
      compilePdfTab_ = compilePdfTab;
      findOutputTab_ = findOutputTab;
      sourceCppTab_ = sourceCppTab;
      renderRmdTab_ = renderRmdTab;
      deployContentTab_ = deployContentTab;
      markersTab_ = markersTab;
      
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
            
            toggleWindowZoom(window, null);
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
               fullyMaximizeWindow(window, null);
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
            horizontalResizeAnimation(0, newWidth, afterAnimation).run(300);
         }
      });
      
      eventBus_.addHandler(
            RequestCurrentlyZoomedTabEvent.TYPE,
            new RequestCurrentlyZoomedTabEvent.Handler()
            {
               @Override
               public void onRequestCurrentlyZoomedTab(RequestCurrentlyZoomedTabEvent event)
               {
                  event.setZoomedTab(maximizedTab_);
               }
            });
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
            if (el.equals(window.getActiveWidget().getElement()))
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
   
   public void toggleWindowZoom(LogicalWindow window, Tab tab)
   {
      if (isAnimating_)
         return;
      
      if (window.equals(maximizedWindow_))
         restoreLayout();
      else
         fullyMaximizeWindow(window, tab);
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
      
      // Ensure that a couple pixels are left after zoom so that the pane
      // can be manually pulled out (with the mouse).
      double targetSize = isLeftWidget ?
            0 :
            panel_.getOffsetWidth() - 3;
      
      if (targetSize < 0)
         targetSize = 0;
      
      horizontalResizeAnimation(initialSize, targetSize).run(300);
   }
   
   private Animation horizontalResizeAnimation(final double start,
                                               final double end)
   {
      return horizontalResizeAnimation(start, end, null);
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
   
   private void restoreFourPaneLayout()
   {
      // Ensure that all windows are in the 'normal' state. This allows
      // hidden windows to display themselves, and so on. This also forces
      // widgets to size themselves vertically.
      for (LogicalWindow window : panes_)
         window.onWindowStateChange(new WindowStateChangeEvent(WindowState.NORMAL, true));
      
      double rightWidth = panel_.getWidgetSize(right_);
      
      // If the right pane is already visible horizontally, bail.
      if (rightWidth >= 10)
         return;
      
      horizontalResizeAnimation(rightWidth, computeAppropriateWidth()).run(300);
   }
   
   private void restoreSavedLayout()
   {
      // Ensure that all windows are in the 'normal' state. This allows
      // hidden windows to display themselves, and so on.
      for (LogicalWindow window : panes_)
         window.onWindowStateChange(new WindowStateChangeEvent(WindowState.NORMAL, true));
      
      maximizedWindow_.onWindowStateChange(new WindowStateChangeEvent(WindowState.NORMAL, true));
      horizontalResizeAnimation(panel_.getWidgetSize(right_), widgetSizePriorToZoom_).run(300);
      
      // Invalidate the saved state.
      maximizedWindow_ = null;
      maximizedTab_ = null;
      widgetSizePriorToZoom_ = -1;
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
                                  environmentTab_, viewerTab_};
   }

   public void activateTab(Tab tab)
   {
      WorkbenchTabPanel panel = getOwnerTabPanel(tab);
      
      // Ensure that the pane is visible (otherwise tab selection will fail)
      LogicalWindow parent = panel.getParentWindow();
      if (parent.getState() == WindowState.MINIMIZE ||
          parent.getState() == WindowState.HIDE)
      {
         parent.onWindowStateChange(new WindowStateChangeEvent(WindowState.NORMAL));
      }
      
      int index = tabToIndex_.get(tab);
      panel.selectTab(index);
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

      @SuppressWarnings("unused")
      ConsoleTabPanel consoleTabPanel = new ConsoleTabPanel(frame,
                                                            logicalWindow,
                                                            consolePane_,
                                                            compilePdfTab_,
                                                            findOutputTab_,
                                                            sourceCppTab_,
                                                            renderRmdTab_,
                                                            deployContentTab_,
                                                            markersTab_,
                                                            eventBus_,
                                                            consoleInterrupt_,
                                                            goToWorkingDirButton);
      
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
            session_.persistClientState();
         }
      });

      new SelectedTabStateValue(persisterName, tabPanel);

      return new Triad<LogicalWindow, WorkbenchTabPanel, MinimizedModuleTabLayoutPanel>(
            logicalWindow,
            tabPanel,
            minimized);
   }

   private void populateTabPanel(ArrayList<Tab> tabs,
                                 WorkbenchTabPanel tabPanel,
                                 MinimizedModuleTabLayoutPanel minimized)
   {
      ArrayList<WorkbenchTab> tabList = new ArrayList<WorkbenchTab>();
      for (int i = 0; i < tabs.size(); i++)
      {
         Tab tab = tabs.get(i);
         tabList.add(getTab(tab));
         tabToPanel_.put(tab, tabPanel);
         tabToIndex_.put(tab, i);
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
      if (name.equalsIgnoreCase("source"))
         return Tab.Source;
      if (name.equalsIgnoreCase("console"))
         return Tab.Console;
      
      return null;
   }

   private final EventBus eventBus_;
   private final Session session_;
   private final Commands commands_;
   private final FindOutputTab findOutputTab_;
   private final WorkbenchTab compilePdfTab_;
   private final WorkbenchTab sourceCppTab_;
   private final ConsolePane consolePane_;
   private final ConsoleInterruptButton consoleInterrupt_;
   private final SourceShim source_;
   private final WorkbenchTab historyTab_;
   private final WorkbenchTab filesTab_;
   private final WorkbenchTab plotsTab_;
   private final WorkbenchTab packagesTab_;
   private final WorkbenchTab helpTab_;
   private final WorkbenchTab vcsTab_;
   private final WorkbenchTab buildTab_;
   private final WorkbenchTab presentationTab_;
   private final WorkbenchTab environmentTab_;
   private final WorkbenchTab viewerTab_;
   private final WorkbenchTab renderRmdTab_;
   private final WorkbenchTab deployContentTab_;
   private final MarkersOutputTab markersTab_;
   private MainSplitPanel panel_;
   private LogicalWindow sourceLogicalWindow_;
   private final HashMap<Tab, WorkbenchTabPanel> tabToPanel_ =
         new HashMap<Tab, WorkbenchTabPanel>();
   private final HashMap<Tab, Integer> tabToIndex_ =
         new HashMap<Tab, Integer>();
   private HashMap<String, LogicalWindow> panesByName_;
   private DualWindowLayoutPanel left_;
   private DualWindowLayoutPanel right_;
   private ArrayList<LogicalWindow> panes_;
   private WorkbenchTabPanel tabSet1TabPanel_;
   private MinimizedModuleTabLayoutPanel tabSet1MinPanel_;
   private WorkbenchTabPanel tabSet2TabPanel_;
   private MinimizedModuleTabLayoutPanel tabSet2MinPanel_;
   
   // Zoom-related members ----
   private LogicalWindow maximizedWindow_;
   private Tab maximizedTab_;
   private double widgetSizePriorToZoom_ = -1;
   private boolean isAnimating_;
}
