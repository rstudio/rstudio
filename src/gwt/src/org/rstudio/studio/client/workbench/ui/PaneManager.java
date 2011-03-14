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
import org.rstudio.studio.client.application.events.EventBus;
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
      Workspace, History, Files, Plots, Packages, Help
   }

   class SelectedTabStateValue extends IntStateValue
   {
      SelectedTabStateValue(String name,
                            WorkbenchTabPanel tabPanel)
      {
         super("workbenchp", name, true,
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
                      UIPrefs uiPrefs,
                      @Named("Console") final Widget consolePane,
                      ConsoleInterruptButton consoleInterrupt,
                      SourceShim source,
                      @Named("Workspace") final WorkbenchTab workspaceTab,
                      @Named("History") final WorkbenchTab historyTab,
                      @Named("Files") final WorkbenchTab filesTab,
                      @Named("Plots") final WorkbenchTab plotsTab,
                      @Named("Packages") final WorkbenchTab packagesTab,
                      @Named("Help") final WorkbenchTab helpTab)
   {
      eventBus_ = eventBus;
      session_ = session;
      consolePane_ = (ConsolePane)consolePane;
      consoleInterrupt_ = consoleInterrupt;
      source_ = source;
      workspaceTab_ = workspaceTab;
      historyTab_ = historyTab;
      filesTab_ = filesTab;
      plotsTab_ = plotsTab;
      packagesTab_ = packagesTab;
      helpTab_ = helpTab;

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

      uiPrefs.paneConfig().addValueChangeHandler(new ValueChangeHandler<PaneConfig>()
      {
         public void onValueChange(ValueChangeEvent<PaneConfig> evt)
         {
            WindowFrame[] normals = new WindowFrame[4];
            MinimizedWindowFrame[] minimized = new MinimizedWindowFrame[4];

            ArrayList<LogicalWindow> newPanes = createPanes(evt.getValue());
            for (int i = 0; i < newPanes.size(); i++)
            {
               normals[i] = newPanes.get(i).getNormal();
               minimized[i] = newPanes.get(i).getMinimized();
            }

            for (int i = 0; i < normals.length; i++)
            {
               panes_.get(i).replace(normals[i], minimized[i]);
            }

            panes_ = newPanes;

            left_.reloadPanes();
            right_.reloadPanes();

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
      if (!config.isValid())
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
      }
      throw new IllegalArgumentException("Unknown tab");
   }

   public WorkbenchTab[] getAllTabs()
   {
      return new WorkbenchTab[] { workspaceTab_, historyTab_, filesTab_,
                                  plotsTab_, packagesTab_, helpTab_ };
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

   public PrimaryWindowFrame getConsoleFrame()
   {
      return consoleWindowFrame_;
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
      consoleWindowFrame_ = new PrimaryWindowFrame("Console", consolePane_);
      consoleWindowFrame_.setContextButton(consoleInterrupt_,
                                           consoleInterrupt_.getWidth(),
                                           consoleInterrupt_.getHeight());
      return new LogicalWindow(consoleWindowFrame_,
                               new MinimizedWindowFrame("Console"));
   }

   private LogicalWindow createSource()
   {
      WindowFrame sourceFrame = new WindowFrame();
      sourceFrame.setFillWidget(source_.toWidget());
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

      String[] labels = new String[tabs.size()];
      for (int i = 0; i < labels.length; i++)
         labels[i] = getTabLabel(tabs.get(i));
      minimized.setTabs(labels);
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
      }
      return "??";
   }

   private final EventBus eventBus_;
   private final Session session_;
   private final ConsolePane consolePane_;
   private final ConsoleInterruptButton consoleInterrupt_;
   private final SourceShim source_;
   private final WorkbenchTab workspaceTab_;
   private final WorkbenchTab historyTab_;
   private final WorkbenchTab filesTab_;
   private final WorkbenchTab plotsTab_;
   private final WorkbenchTab packagesTab_;
   private final WorkbenchTab helpTab_;
   private MainSplitPanel panel_;
   private LogicalWindow sourceLogicalWindow_;
   private PrimaryWindowFrame consoleWindowFrame_;
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
