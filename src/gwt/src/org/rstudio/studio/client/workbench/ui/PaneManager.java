/*
 * PaneManager.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.ui;

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.Triad;
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
import org.rstudio.studio.client.workbench.model.ClientState;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.WorkbenchServerOperations;
import org.rstudio.studio.client.workbench.model.helper.IntStateValue;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.views.console.ConsoleInterruptButton;
import org.rstudio.studio.client.workbench.views.console.ConsolePane;
import org.rstudio.studio.client.workbench.views.source.SourceShim;

import java.util.ArrayList;
import java.util.HashMap;

/*
 * TODO: Push client state when selected tab or layout changes
 */

public class PaneManager
{
   public enum Tab {
      Workspace, History, Files, Plots, Packages, Help, VCS
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
                      Commands commands,
                      UIPrefs uiPrefs,
                      @Named("Console") final Widget consolePane,
                      ConsoleInterruptButton consoleInterrupt,
                      SourceShim source,
                      @Named("Workspace") final WorkbenchTab workspaceTab,
                      @Named("History") final WorkbenchTab historyTab,
                      @Named("Files") final WorkbenchTab filesTab,
                      @Named("Plots") final WorkbenchTab plotsTab,
                      @Named("Packages") final WorkbenchTab packagesTab,
                      @Named("Help") final WorkbenchTab helpTab,
                      @Named("VCS") final WorkbenchTab vcsTab,
                      @Named("Compile PDF") final WorkbenchTab compilePdfTab,
                      @Named("Find") final WorkbenchTab findOutputTab)
   {
      eventBus_ = eventBus;
      session_ = session;
      commands_ = commands;
      consolePane_ = (ConsolePane)consolePane;
      consoleInterrupt_ = consoleInterrupt;
      source_ = source;
      workspaceTab_ = workspaceTab;
      historyTab_ = historyTab;
      filesTab_ = filesTab;
      plotsTab_ = plotsTab;
      packagesTab_ = packagesTab;
      helpTab_ = helpTab;
      vcsTab_ = vcsTab;
      compilePdfTab_ = compilePdfTab;
      findOutputTab_ = findOutputTab;

      PaneConfig config = validateConfig(uiPrefs.paneConfig().getValue());
      initPanes(config);

      panes_ = createPanes(config);
      left_ = createSplitWindow(panes_.get(0), panes_.get(1), "left");
      right_ = createSplitWindow(panes_.get(2), panes_.get(3), "right");

      panel_ = pSplitPanel.get();
      panel_.initialize(left_, right_);

      if (session_.getSessionInfo().getSourceDocuments().length() == 0
            && sourceLogicalWindow_.getState() != WindowState.HIDE)
      {
         sourceLogicalWindow_.onWindowStateChange(
               new WindowStateChangeEvent(WindowState.HIDE));
      }
      else if (session_.getSessionInfo().getSourceDocuments().length() > 0
               && sourceLogicalWindow_.getState() == WindowState.HIDE)
      {
         sourceLogicalWindow_.onWindowStateChange(
               new WindowStateChangeEvent(WindowState.NORMAL));
      }

      uiPrefs.paneConfig().addValueChangeHandler(new ValueChangeHandler<PaneConfig>()
      {
         public void onValueChange(ValueChangeEvent<PaneConfig> evt)
         {
            ArrayList<LogicalWindow> newPanes = createPanes(evt.getValue());
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
         case Workspace:
            return workspaceTab_;
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
      }
      throw new IllegalArgumentException("Unknown tab");
   }

   public WorkbenchTab[] getAllTabs()
   {
      return new WorkbenchTab[] { workspaceTab_, historyTab_, filesTab_,
                                  plotsTab_, packagesTab_, helpTab_,
                                  vcsTab_ };
   }

   public void activateTab(Tab tab)
   {
      tabToPanel_.get(tab).selectTab(tabToIndex_.get(tab));
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

   private DualWindowLayoutPanel createSplitWindow(LogicalWindow top,
                                                   LogicalWindow bottom,
                                                   String name)
   {
      return new DualWindowLayoutPanel(
            eventBus_,
            top,
            bottom,
            session_,
            name,
            WindowState.NORMAL,
            (int) (Window.getClientHeight()*0.6));
   }

   private LogicalWindow createConsole()
   {
      PrimaryWindowFrame frame = new PrimaryWindowFrame("Console", null);
      ConsoleTabPanel consoleTabPanel = new ConsoleTabPanel(frame,
                                                            consolePane_,
                                                            compilePdfTab_,
                                                            findOutputTab_,
                                                            eventBus_);
      frame.setContextButton(consoleInterrupt_,
                             consoleInterrupt_.getWidth(),
                             consoleInterrupt_.getHeight());

      ToolbarButton goToWorkingDirButton =
                           commands_.goToWorkingDir().createToolbarButton();
      goToWorkingDirButton.addStyleName(
            ThemeResources.INSTANCE.themeStyles().windowFrameToolbarButton());
      frame.addLeftWidget(goToWorkingDirButton);

      return new LogicalWindow(frame, new MinimizedWindowFrame("Console"));
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
      final WorkbenchTabPanel tabPanel = new WorkbenchTabPanel(frame);
      MinimizedModuleTabLayoutPanel minimized = new MinimizedModuleTabLayoutPanel();

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
            new LogicalWindow(frame, minimized),
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
         case Workspace:
            return "Workspace";
         case History:
            return "History";
         case Files:
            return "Files";
         case Plots:
            return "Plots";
         case Packages:
            return "Packages";
         case Help:
            return "Help";
         case VCS:
            return getTab(tab).getTitle();
      }
      return "??";
   }

   private final EventBus eventBus_;
   private final Session session_;
   private final Commands commands_;
   private final WorkbenchTab findOutputTab_;
   private final WorkbenchTab compilePdfTab_;
   private final ConsolePane consolePane_;
   private final ConsoleInterruptButton consoleInterrupt_;
   private final SourceShim source_;
   private final WorkbenchTab workspaceTab_;
   private final WorkbenchTab historyTab_;
   private final WorkbenchTab filesTab_;
   private final WorkbenchTab plotsTab_;
   private final WorkbenchTab packagesTab_;
   private final WorkbenchTab helpTab_;
   private final WorkbenchTab vcsTab_;
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
}
