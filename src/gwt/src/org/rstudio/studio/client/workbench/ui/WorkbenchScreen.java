/*
 * WorkbenchScreen.java
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

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Document;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.*;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.name.Named;
import org.rstudio.core.client.SerializedCommand;
import org.rstudio.core.client.SerializedCommandQueue;
import org.rstudio.core.client.Size;
import org.rstudio.core.client.TimeBufferedCommand;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.CommandHandler;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.events.WindowStateChangeEvent;
import org.rstudio.core.client.layout.DualWindowLayoutPanel;
import org.rstudio.core.client.layout.LogicalWindow;
import org.rstudio.core.client.layout.WindowState;
import org.rstudio.core.client.theme.MinimizedModuleTabLayoutPanel;
import org.rstudio.core.client.theme.MinimizedWindowFrame;
import org.rstudio.core.client.theme.PrimaryWindowFrame;
import org.rstudio.core.client.theme.WindowFrame;
import org.rstudio.core.client.widget.FontSizer;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.studio.client.application.events.ChangeFontSizeEvent;
import org.rstudio.studio.client.application.events.ChangeFontSizeHandler;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.ui.appended.ApplicationEndedPopupPanel;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.Value;
import org.rstudio.studio.client.workbench.MRUList;
import org.rstudio.studio.client.workbench.WorkbenchMainView;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.*;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.WorkbenchMetrics;
import org.rstudio.studio.client.workbench.model.helper.BoolStateValue;
import org.rstudio.studio.client.workbench.model.helper.IntStateValue;
import org.rstudio.studio.client.workbench.views.console.ConsoleInterruptButton;
import org.rstudio.studio.client.workbench.views.console.ConsolePane;
import org.rstudio.studio.client.workbench.views.console.events.WorkingDirChangedEvent;
import org.rstudio.studio.client.workbench.views.console.events.WorkingDirChangedHandler;
import org.rstudio.studio.client.workbench.views.edit.Edit;
import org.rstudio.studio.client.workbench.views.edit.events.ShowEditorEvent;
import org.rstudio.studio.client.workbench.views.plots.PlotsTab;
import org.rstudio.studio.client.workbench.views.source.SourceShim;
import org.rstudio.studio.client.workbench.views.source.events.LastSourceDocClosedEvent;
import org.rstudio.studio.client.workbench.views.source.events.LastSourceDocClosedHandler;
import org.rstudio.studio.client.workbench.views.workspace.table.WorkspaceTab;

public class WorkbenchScreen extends Composite 
                             implements WorkbenchMainView,
                                        SelectionHandler<Integer>,
                                        RequiresResize
{
   interface MyCommandBinder extends CommandBinder<Commands, WorkbenchScreen>{}
   static MyCommandBinder commandBinder = GWT.create(MyCommandBinder.class);

   @Inject
   public WorkbenchScreen(EventBus eventBus,
                          Session session,
                          Provider<MainSplitPanel> pSplitPanel,
                          @Named("Console") final Widget consolePane,
                          ConsoleInterruptButton consoleInterrupt,
                          //@Named("Source") final Widget sourcePane,
                          SourceShim source,
                          final WorkspaceTab workspaceTab,
                          @Named("History") final WorkbenchTab historyTab,
                          @Named("Data") final WorkbenchTab dataTab,
                          @Named("Files") final WorkbenchTab filesTab,
                          @Named("Plots") final WorkbenchTab plotsTab,
                          @Named("Packages") final WorkbenchTab packagesTab,
                          @Named("Help") final WorkbenchTab helpTab,
                          final Edit.Shim edit,
                          Commands commands,
                          final GlobalDisplay globalDisplay,
                          final Provider<MRUList> mruList,
                          FontSizeManager fontSizeManager)
   {
      eventBus_ = eventBus;
      session_ = session;

      eventBus_.addHandler(ShowEditorEvent.TYPE, edit);
      eventBus_.addHandler(ChangeFontSizeEvent.TYPE, new ChangeFontSizeHandler()
      {
         public void onChangeFontSize(ChangeFontSizeEvent event)
         {
            FontSizer.setNormalFontSize(Document.get(), event.getFontSize());
            Scheduler.get().scheduleDeferred(new ScheduledCommand()
            {
               public void execute()
               {
                  // Causes the console width to be remeasured
                  doOnPaneSizesChanged();
               }
            });
         }
      });
      FontSizer.setNormalFontSize(Document.get(), fontSizeManager.getSize());

      // create tabsets
      tabsPanel_ = pSplitPanel.get();
      tabsPanel_.setSize("100%", "100%");
      tabsPanel_.addStyleDependentName("Workbench");

      final WindowFrame rightTopFrame = new WindowFrame();
      rightTopTabs_ = new WorkbenchTabPanel(rightTopFrame);
      rightTopTabs_.add(workspaceTab);
      //rightTopTabs_.add(dataTab);
      rightTopTabs_.add(historyTab);
      rightTopFrame.setFillWidget(rightTopTabs_);

      // initialize right tabs
      final WindowFrame rightBottomFrame = new WindowFrame();
      browseTabs_ = new WorkbenchTabPanel(rightBottomFrame);
      browseTabs_.add(filesTab);
      browseTabs_.add(plotsTab_ = plotsTab);
      browseTabs_.add(packagesTab);
      browseTabs_.add(helpTab) ;
      browseTabs_.addSelectionHandler(this);
      rightBottomFrame.setFillWidget(browseTabs_);

      MinimizedModuleTabLayoutPanel minimizedTopModuleTabs = new MinimizedModuleTabLayoutPanel(
            new String[] {"Workspace", /*"Data",*/ "History"});
      minimizedTopModuleTabs.addSelectionHandler(new SelectionHandler<Integer>()
      {
         public void onSelection(SelectionEvent<Integer> integerSelectionEvent)
         {
            int tab = integerSelectionEvent.getSelectedItem().intValue();
            rightTopTabs_.selectTab(tab);
         }
      });
      MinimizedModuleTabLayoutPanel minimizedBottomModuleTabs = new MinimizedModuleTabLayoutPanel(
            new String[] {"Files", "Plots", "Packages", "Help"});
      minimizedBottomModuleTabs.addSelectionHandler(new SelectionHandler<Integer>()
      {
         public void onSelection(SelectionEvent<Integer> integerSelectionEvent)
         {
            int tab = integerSelectionEvent.getSelectedItem().intValue();
            browseTabs_.selectTab(tab);
         }
      });

      LogicalWindow workspaceLogicalWindow = new LogicalWindow(
            rightTopFrame,
            minimizedTopModuleTabs);
      LogicalWindow plotsLogicalWindow = new LogicalWindow(
            rightBottomFrame,
            minimizedBottomModuleTabs);

      initBoolPref("plotsOnTop", plotsOnTop_, commands.plotsOnTop(), session, globalDisplay);
      commands.plotsOnTop().setMenuLabel("Plots on " + (plotsOnTop_.getValue() ? "Bottom" : "Top"));

      LogicalWindow rightTopWindow = plotsOnTop_.getValue() ? plotsLogicalWindow
                                                            : workspaceLogicalWindow;
      LogicalWindow rightBottomWindow = plotsOnTop_.getValue() ? workspaceLogicalWindow
                                                               : plotsLogicalWindow;

      final DualWindowLayoutPanel rightTabs = new DualWindowLayoutPanel(
            eventBus,
            rightTopWindow,
            rightBottomWindow,
            session,
            "right",
            WindowState.NORMAL,
            (int) (Window.getClientHeight()*0.6));

      // initialize left tabs
      consolePane_ = consolePane;
      consoleFrame_ = new PrimaryWindowFrame("Console", consolePane_);
      consoleFrame_.setContextButton(consoleInterrupt,
                                     consoleInterrupt.getWidth(),
                                     consoleInterrupt.getHeight());
      consoleLogicalWindow_ = new LogicalWindow(
            consoleFrame_,
            new MinimizedWindowFrame("Console"));
      WindowFrame sourceFrame = new WindowFrame();
      sourceFrame.setFillWidget(source.toWidget());
      source.forceLoad();
      sourceLogicalWindow_ = new LogicalWindow(
            sourceFrame,
            new MinimizedWindowFrame("Source"));

      initBoolPref("consoleOnTop", consoleOnTop_, commands.consoleOnTop(), session, globalDisplay);
      commands.consoleOnTop().setMenuLabel("Console on " + (consoleOnTop_.getValue() ? "Bottom" : "Top"));

      LogicalWindow leftTopWindow = consoleOnTop_.getValue() ? consoleLogicalWindow_
                                                             : sourceLogicalWindow_;
      LogicalWindow leftBottomWindow = consoleOnTop_.getValue() ? sourceLogicalWindow_
                                                                : consoleLogicalWindow_;

      DualWindowLayoutPanel leftTabs = new DualWindowLayoutPanel(
            eventBus,
            leftTopWindow,
            leftBottomWindow,
            session,
            "left",
            WindowState.HIDE,
            300
      );


      /*
      JsArray<SourceDocument> srcDocs =
                                 session.getSessionInfo().getSourceDocuments();
      if (srcDocs == null || srcDocs.length() < 1)
      {
         if (consoleOnTop_)
            leftTabs.setTopWindowState(WindowState.EXCLUSIVE);
         else
            leftTabs.setTopWindowState(WindowState.HIDE);
      }
      */

      tabsPanel_.initialize(leftTabs, rightTabs);

      // Prevent doOnPaneSizesChanged() from being called more than once
      // every N milliseconds. Note that the act of sending the client metrics
      // to the server also has its own buffer, so this one only needs to
      // minimize the expense of doing the calculation, not of sending data to
      // the server.
      paneSizesChangedCommand_ = new TimeBufferedCommand(200)
      {
         @Override
         protected void performAction(boolean shouldSchedulePassive)
         {
            assert !shouldSchedulePassive;
            doOnPaneSizesChanged();
         }
      };

      final SerializedCommandQueue prefetchQueue = new SerializedCommandQueue();
      eventBus.addHandler(SessionInitEvent.TYPE, new SessionInitHandler()
      {
         public void onSessionInit(SessionInitEvent sie)
         {
            DeferredCommand.addCommand(new Command() {
               public void execute()
               {
                  onPaneSizesChanged();
               }
            });

            DeferredCommand.addCommand(new Command()
            {
               public void execute()
               {
                  WorkbenchTab[] tabs = {
                        filesTab, plotsTab,
                        packagesTab, helpTab, dataTab, historyTab, workspaceTab };
                  for (final WorkbenchTab tab : tabs)
                     prefetchQueue.addCommand(new SerializedCommand()
                     {
                        public void onExecute(Command continuation)
                        {
                           tab.prefetch(continuation);
                        }
                     });
                  prefetchQueue.addCommand(new SerializedCommand()
                  {
                     public void onExecute(Command continuation)
                     {
                        ApplicationEndedPopupPanel.prefetch(continuation);
                     }
                  });
                  prefetchQueue.addCommand(new SerializedCommand()
                  {
                     public void onExecute(Command continuation)
                     {
                        edit.forceLoad(true, continuation);
                     }
                  });
               }
            });

            mruList.get();
         }
      });

      eventBus.addHandler(LastSourceDocClosedEvent.TYPE,
                          new LastSourceDocClosedHandler()
      {
         public void onLastSourceDocClosed(LastSourceDocClosedEvent event)
         {
            sourceLogicalWindow_.onWindowStateChange(
                  new WindowStateChangeEvent(WindowState.HIDE));
         }
      });

      eventBus.addHandler(WorkingDirChangedEvent.TYPE,
                          new WorkingDirChangedHandler()
                          {
                             public void onWorkingDirChanged(
                                   WorkingDirChangedEvent event)
                             {
                                updateWorkingDirectory(event.getPath());
                             }
                          });

      ((PlotsTab)plotsTab_).addResizeHandler(new ResizeHandler()
      {
         public void onResize(ResizeEvent event)
         {
            onPaneSizesChanged();
         }
      });
      tabsPanel_.addSplitterResizedHandler(new SplitterResizedHandler()
      {
         public void onSplitterResized(SplitterResizedEvent event)
         {
            onPaneSizesChanged();
         }
      });

      new IntStateValue("workbenchp",
                        "righttoptab",
                        true,
                        session_.getSessionInfo().getClientState()) {
         @Override
         protected void onInit(Integer value)
         {
            if (value != null)
               rightTopTabs_.selectTab(value.intValue());
         }

         @Override
         protected Integer getValue()
         {
            return rightTopTabs_.getSelectedIndex();
         }
      };

      new IntStateValue("workbenchp",
                        "righttab",
                        true,
                        session_.getSessionInfo().getClientState()) {
         @Override
         protected void onInit(Integer value)
         {
            if (value != null)
               browseTabs_.selectTab(value.intValue());
//            else
//               browseTabs_.selectTab(workspaceTab_);
         }

         @Override
         protected Integer getValue()
         {
            return browseTabs_.getSelectedIndex();
         }
      };

      // init widget
      initWidget(tabsPanel_);

      commandBinder.bind(commands, this);
   }

   private static void initBoolPref(String key,
                                    final Value<Boolean> val,
                                    AppCommand command,
                                    final Session session,
                                    final GlobalDisplay globalDisplay)
   {
      new BoolStateValue("moduleprefs", key, true,
                         session.getSessionInfo().getClientState())
      {
         @Override
         protected void onInit(Boolean value)
         {
            val.setValue(value == null ? false : value);
         }

         @Override
         protected Boolean getValue()
         {
            return val.getValue();
         }
      };
      command.addHandler(new CommandHandler()
      {
         public void onCommand(AppCommand command)
         {
            val.setValue(val.getValue() == null
                         || !val.getValue());
            session.persistClientState();
            globalDisplay.showProgress("Saving preferences...");
            new Timer() {
               @Override
               public void run()
               {
                  Window.Location.reload();
               }
            }.schedule(1500);
         }
      });
   }

   private void updateWorkingDirectory(String path)
   {
      if (!path.endsWith("/"))
         path += "/";
      consoleFrame_.setSubtitle(path);
   }

   public void onResize()
   {
      tabsPanel_.onResize();
      onPaneSizesChanged();
   }
         
   @Override
   protected void onLoad()
   {
      eventBus_.fireEvent(new WorkbenchLoadedEvent());
   }
   

   private void onPaneSizesChanged()
   {
      paneSizesChangedCommand_.nudge();
   }

   private void doOnPaneSizesChanged()
   {
      // console width
      int consoleWidth = ((ConsolePane) consolePane_).getCharacterWidth();

      // plots size (don't allow negative metrics)
      Size deckPanelSize = browseTabs_.getDeckPanelSize();
      Size plotsSize = new Size(
               Math.max(deckPanelSize.width, 0),
               Math.max(deckPanelSize.height - Toolbar.DEFAULT_HEIGHT, 0));
   
      WorkbenchMetrics metrics = WorkbenchMetrics.create(consoleWidth,
                                                         plotsSize.width,
                                                         plotsSize.height);

      // make sure we don't send the same metrics value twice (it is
      // an expensive operation since it
      //  involves at least 2 http req)
      if (!metrics.equalTo(lastMetrics_))
      {
         lastMetrics_ = metrics;
         eventBus_.fireEvent(new WorkbenchMetricsChangedEvent(metrics));
      }

      session_.persistClientState();
   }

   public void onSelection(SelectionEvent<Integer> integerSelectionEvent)
   {
      eventBus_.fireEvent(new PushClientStateEvent());
   }

   @Handler
   void onActivateWorkspace() { rightTopTabs_.selectTab(0); }
   /*
   @Handler
   void onActivateData() { rightTopTabs_.selectTab(1); }
   */
   @Handler
   void onActivateHistory() { rightTopTabs_.selectTab(2); }
   @Handler
   void onActivateFiles() { browseTabs_.selectTab(0); }
   @Handler
   void onActivatePlots() { browseTabs_.selectTab(1); }
   @Handler
   void onActivatePackages() { browseTabs_.selectTab(2); }
   @Handler
   void onActivateHelp() { browseTabs_.selectTab(3); }

   public Widget toWidget()
   {
      return this;
   }

   private TimeBufferedCommand paneSizesChangedCommand_;

   private WorkbenchMetrics lastMetrics_ = WorkbenchMetrics.create(0,0,0);
   
   private final EventBus eventBus_;
   private final Session session_;

   private final MainSplitPanel tabsPanel_ ;
   
   private final Widget consolePane_;
   private final WorkbenchTab plotsTab_;
   
   private final WorkbenchTabPanel rightTopTabs_;
   private final WorkbenchTabPanel browseTabs_;
   private Value<Boolean> consoleOnTop_ = new Value<Boolean>(false);
   private Value<Boolean> plotsOnTop_ = new Value<Boolean>(false);
   private LogicalWindow consoleLogicalWindow_;
   private LogicalWindow sourceLogicalWindow_;
   private PrimaryWindowFrame consoleFrame_;
}
