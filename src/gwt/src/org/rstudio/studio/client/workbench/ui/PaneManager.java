package org.rstudio.studio.client.workbench.ui;

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.core.client.layout.DualWindowLayoutPanel;
import org.rstudio.core.client.layout.LogicalWindow;
import org.rstudio.core.client.layout.WindowState;
import org.rstudio.core.client.theme.MinimizedModuleTabLayoutPanel;
import org.rstudio.core.client.theme.MinimizedWindowFrame;
import org.rstudio.core.client.theme.PrimaryWindowFrame;
import org.rstudio.core.client.theme.WindowFrame;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
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

      config_ = validateConfig(uiPrefs.paneConfig().getValue());
      ArrayList<LogicalWindow> panes = createPanes(config_);

      DualWindowLayoutPanel left = createSplitWindow(
            panes.get(0),
            panes.get(1),
            "left");

      DualWindowLayoutPanel right = createSplitWindow(
            panes.get(2),
            panes.get(3),
            "right");

      panel_ = pSplitPanel.get();
      panel_.initialize(left, right);
   }

   private ArrayList<LogicalWindow> createPanes(PaneConfig config)
   {
      ArrayList<LogicalWindow> results = new ArrayList<LogicalWindow>();

      JsArrayString panes = config.getPanes();
      for (int i = 0; i < 4; i++)
      {
         String pane = panes.get(i);
         if (pane.equals("Console"))
            results.add(createConsole());
         else if (pane.equals("Source"))
            results.add(createSource());
         else if (pane.equals("TabSet1") || pane.equals("TabSet2"))
         {
            JsArrayString tabNames = pane.equals("TabSet1")
                                     ? config.getTabSet1()
                                     : config.getTabSet2();

            ArrayList<Tab> tabList = new ArrayList<Tab>();
            for (int j = 0; j < tabNames.length(); j++)
               tabList.add(Enum.valueOf(Tab.class, tabNames.get(j)));
            results.add(createTabSet(pane, tabList));
         }
      }

      return results;
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

   private LogicalWindow createTabSet(String persisterName,
                                      ArrayList<Tab> tabs)
   {
      final WindowFrame frame = new WindowFrame();
      final WorkbenchTabPanel tabPanel = new WorkbenchTabPanel(frame);
      for (int i = 0; i < tabs.size(); i++)
      {
         Tab tab = tabs.get(i);
         tabPanel.add(getTab(tab));
         tabToPanel_.put(tab, tabPanel);
         tabToIndex_.put(tab, i);
      }
      frame.setFillWidget(tabPanel);

      String[] labels = new String[tabs.size()];
      for (int i = 0; i < labels.length; i++)
         labels[i] = getTabLabel(tabs.get(i));
      MinimizedModuleTabLayoutPanel minimized =
            new MinimizedModuleTabLayoutPanel(labels);

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

      return new LogicalWindow(frame, minimized);
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

   public boolean isConsoleOnTop()
   {
      return config_.getPanes().get(0).equals("Console");
   }

   public boolean isPlotsOnTop()
   {
      JsArrayString tabSet1 = config_.getTabSet1();
      for (int i = 0; i < tabSet1.length(); i++)
         if (tabSet1.get(i).equals("Plots"))
            return true;
      return false;
   }

   public PaneConfig setConsoleOnTop(boolean onTop)
   {
      PaneConfig newConfig = config_.copy();
      JsArrayString panes = newConfig.getPanes();
      if (onTop != panes.get(0).equals("Console"))
      {
         String tmp = panes.get(0);
         panes.set(0, panes.get(1));
         panes.set(1, tmp);
      }
      return newConfig;
   }

   public PaneConfig setPlotsOnTop(boolean onTop)
   {
      PaneConfig newConfig = config_.copy();

      JsArrayString tabSet1 = JsArrayString.createArray().cast();
      JsArrayString tabSet2 = JsArrayString.createArray().cast();

      tabSet1.push("Workspace");
      tabSet1.push("History");
      tabSet2.push("Files");

      if (onTop)
         tabSet1.push("Plots");
      else
         tabSet2.push("Plots");

      tabSet2.push("Packages");
      tabSet2.push("Help");
      newConfig.setTabSet1(tabSet1);
      newConfig.setTabSet2(tabSet2);

      return newConfig;
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
   private PaneConfig config_;
}
