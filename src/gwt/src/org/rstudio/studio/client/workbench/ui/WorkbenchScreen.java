/*
 * WorkbenchScreen.java
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

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Document;
import com.google.gwt.event.logical.shared.ResizeEvent;
import com.google.gwt.event.logical.shared.ResizeHandler;
import com.google.gwt.event.logical.shared.SelectionEvent;
import com.google.gwt.event.logical.shared.SelectionHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.*;
import com.google.inject.Inject;
import com.google.inject.Provider;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.SerializedCommand;
import org.rstudio.core.client.SerializedCommandQueue;
import org.rstudio.core.client.Size;
import org.rstudio.core.client.TimeBufferedCommand;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.theme.ModuleTabLayoutPanel;
import org.rstudio.core.client.widget.FontSizer;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.studio.client.application.events.ChangeFontSizeEvent;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.ui.appended.ApplicationEndedPopupPanel;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.workbench.FileMRUList;
import org.rstudio.studio.client.workbench.WorkbenchMainView;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.*;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.WorkbenchMetrics;
import org.rstudio.studio.client.workbench.prefs.views.AccessibilityPreferencesPane;
import org.rstudio.studio.client.workbench.prefs.views.AppearancePreferencesPane;
import org.rstudio.studio.client.workbench.prefs.views.CompilePdfPreferencesPane;
import org.rstudio.studio.client.workbench.prefs.views.ConsolePreferencesPane;
import org.rstudio.studio.client.workbench.prefs.views.EditingPreferencesPane;
import org.rstudio.studio.client.workbench.prefs.views.PackagesPreferencesPane;
import org.rstudio.studio.client.workbench.prefs.views.PublishingPreferencesPane;
import org.rstudio.studio.client.workbench.prefs.views.PythonPreferencesPane;
import org.rstudio.studio.client.workbench.prefs.views.RMarkdownPreferencesPane;
import org.rstudio.studio.client.workbench.prefs.views.SourceControlPreferencesPane;
import org.rstudio.studio.client.workbench.prefs.views.SpellingPreferencesPane;
import org.rstudio.studio.client.workbench.prefs.views.TerminalPreferencesPane;
import org.rstudio.studio.client.workbench.ui.PaneManager.Tab;
import org.rstudio.studio.client.workbench.views.console.ConsoleResources;
import org.rstudio.studio.client.workbench.views.edit.Edit;
import org.rstudio.studio.client.workbench.views.edit.Edit.Shim;
import org.rstudio.studio.client.workbench.views.edit.events.ShowEditorEvent;
import org.rstudio.studio.client.workbench.views.help.events.ActivateHelpEvent;
import org.rstudio.studio.client.workbench.views.plots.PlotsTab;
import org.rstudio.studio.client.workbench.views.source.events.LastSourceDocClosedEvent;
import org.rstudio.studio.client.workbench.views.source.events.LastSourceDocClosedHandler;

public class WorkbenchScreen extends Composite
                             implements WorkbenchMainView,
                                        SelectionHandler<Integer>,
                                        ActivatePaneEvent.Handler,
                                        RequiresResize
{
   interface MyCommandBinder extends CommandBinder<Commands, WorkbenchScreen>{}
   static MyCommandBinder commandBinder = GWT.create(MyCommandBinder.class);

   @Inject
   public WorkbenchScreen(GlobalDisplay globalDisplay,
                          EventBus eventBus,
                          Session session,
                          Provider<PaneManager> pPaneManager,
                          final Edit.Shim edit,
                          Commands commands,
                          final Provider<FileMRUList> mruList,
                          FontSizeManager fontSizeManager,
                          OptionsLoader.Shim optionsLoader)
   {
      globalDisplay_ = globalDisplay;
      eventBus_ = eventBus;
      session_ = session;
      edit_ = edit;
      optionsLoader_ = optionsLoader;

      if (!BrowseCap.isMacintoshDesktop())
      {
         commands.macPreferences().remove();
         commands.toggleFullScreen().remove();
      }

      eventBus_.addHandler(ActivatePaneEvent.TYPE, this);
      eventBus_.addHandler(ShowEditorEvent.TYPE, edit);
      eventBus_.addHandler(ChangeFontSizeEvent.TYPE, changeFontSizeEvent ->
      {
         FontSizer.setNormalFontSize(Document.get(), changeFontSizeEvent.getFontSize());
         Scheduler.get().scheduleDeferred(() ->
         {
            // Causes the console width to be remeasured
            doOnPaneSizesChanged();
         });
      });
      FontSizer.setNormalFontSize(Document.get(), fontSizeManager.getSize());

      paneManager_ = pPaneManager.get();
      tabsPanel_ = paneManager_.getPanel();
      tabsPanel_.setSize("100%", "100%");
      tabsPanel_.addStyleDependentName("Workbench");
      Roles.getMainRole().set(tabsPanel_.getElement());
      Roles.getMainRole().setAriaLabelProperty(tabsPanel_.getElement(), "Workbench");

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

      eventBus.addHandler(SessionInitEvent.TYPE, (SessionInitEvent sie) ->
      {
         prefetch();
         mruList.get();
      });

      eventBus.addHandler(LastSourceDocClosedEvent.TYPE,
                          new LastSourceDocClosedHandler()
      {
         public void onLastSourceDocClosed(LastSourceDocClosedEvent event)
         {
            paneManager_.closeSourceWindow(event.getName());
         }
      });

      ((PlotsTab) paneManager_.getTab(Tab.Plots)).addResizeHandler(new ResizeHandler()
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

      // init widget
      initWidget(tabsPanel_);

      commandBinder.bind(commands, this);
   }

   private void prefetch()
   {
      final SerializedCommandQueue prefetchQueue = new SerializedCommandQueue();
      Scheduler.get().scheduleDeferred(new ScheduledCommand() {
         public void execute()
         {
            onPaneSizesChanged();
         }
      });

      Scheduler.get().scheduleDeferred(new ScheduledCommand()
      {
         public void execute()
         {
            for (final WorkbenchTab tab : paneManager_.getAllTabs())
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
                  edit_.forceLoad(true, continuation);
               }
            });
            prefetchQueue.addCommand(new SerializedCommand()
            {
               public void onExecute(Command continuation)
               {
                  optionsLoader_.forceLoad(true, continuation);
               }
            });
         }
      });
   }

   public void onResize()
   {
      tabsPanel_.onResize();
      onPaneSizesChanged();
   }

   @Override
   protected void onLoad()
   {
      super.onLoad();
      eventBus_.fireEvent(new WorkbenchLoadedEvent());
   }


   private void onPaneSizesChanged()
   {
      paneSizesChangedCommand_.nudge();
   }

   private void doOnPaneSizesChanged()
   {
      // console width
      int consoleWidth = paneManager_.getConsole().getCharacterWidth();

      // if the console is hidden then just use its last value. if there
      // has never been a valid value then then lastMetrics_ console width
      // will be 0, which the server will know to ignore
      if (paneManager_.getConsole().getOffsetWidth() <= 0)
         consoleWidth = lastMetrics_.getConsoleWidth();

      // build console width
      WorkbenchTabPanel buildPane = paneManager_.getOwnerTabPanel(Tab.Build);
      int buildConsoleWidth =
         buildPane == null ?  0 :
            DomUtils.getCharacterWidth(buildPane.getElement(),
               ConsoleResources.INSTANCE.consoleStyles().console());

      // if the build console is hidden then just use its last value. if there
      // has never been a valid value then then lastMetrics_ build console width
      // will be 0, which the server will know to ignore
      if (buildConsoleWidth <= 0)
         buildConsoleWidth = lastMetrics_.getBuildConsoleWidth();

      // plots size (don't allow negative metrics)
      WorkbenchTabPanel plotPanel = paneManager_.getOwnerTabPanel(Tab.Plots);
      Size deckPanelSize = plotPanel == null ? new Size(0, 0) :
         new Size(
            plotPanel.getOffsetWidth(),
            plotPanel.getOffsetHeight() - ModuleTabLayoutPanel.BAR_HEIGHT);

      Size plotsSize = new Size(
               Math.max(deckPanelSize.width, 0),
               Math.max(deckPanelSize.height - Toolbar.DEFAULT_HEIGHT, 0));

      double devicePixelRatio = 1.0;
      if (BrowseCap.isMacintoshDesktop())
         devicePixelRatio = BrowseCap.devicePixelRatio();
      WorkbenchMetrics metrics = WorkbenchMetrics.create(consoleWidth,
                                                         buildConsoleWidth,
                                                         plotsSize.width,
                                                         plotsSize.height,
                                                         devicePixelRatio);

      // make sure we don't send very similar metrics values twice (it is
      // an expensive operation since it involves at least 2 http requests)
      if (!metrics.closeEnoughToPrevious(lastMetrics_))
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

   @Override
   public void onActivatePane(ActivatePaneEvent event)
   {
      paneManager_.activateTab(event.getPane());
   }

   private void fireEventDelayed(final GwtEvent<?> event, int delayMs)
   {
      new Timer()
      {
         @Override
         public void run()
         {
            eventBus_.fireEvent(event);
         }
      }.schedule(delayMs);
   }

   @Handler
   void onActivateEnvironment() { paneManager_.activateTab(Tab.Environment); }
   @Handler
   void onActivateHistory() { paneManager_.activateTab(Tab.History); }
   @Handler
   void onActivateFiles() { paneManager_.activateTab(Tab.Files); }
   @Handler
   void onActivatePlots() { paneManager_.activateTab(Tab.Plots); }
   @Handler
   void onActivatePackages() { paneManager_.activateTab(Tab.Packages); }
   @Handler
   void onActivateHelp()
   {
      paneManager_.activateTab(Tab.Help);
      fireEventDelayed(new ActivateHelpEvent(), 200);
   }
   @Handler
   void onActivateVcs() { paneManager_.activateTab(Tab.VCS); }
   @Handler
   void onActivateBuild() { paneManager_.activateTab(Tab.Build); }
   @Handler
   void onActivatePresentation() { paneManager_.activateTab(Tab.Presentation); }
   @Handler
   void onActivateViewer() { paneManager_.activateTab(Tab.Viewer); }
   @Handler
   void onActivateConnections() { paneManager_.activateTab(Tab.Connections); }
   @Handler
   void onActivateTutorial() { paneManager_.activateTab(Tab.Tutorial); }


   @Handler
   void onLayoutZoomEnvironment() { paneManager_.zoomTab(Tab.Environment); }
   @Handler
   void onLayoutZoomHistory() { paneManager_.zoomTab(Tab.History); }
   @Handler
   void onLayoutZoomFiles() { paneManager_.zoomTab(Tab.Files); }
   @Handler
   void onLayoutZoomPlots() { paneManager_.zoomTab(Tab.Plots); }
   @Handler
   void onLayoutZoomPackages() { paneManager_.zoomTab(Tab.Packages); }
   @Handler
   void onLayoutZoomHelp()
   {
      paneManager_.zoomTab(Tab.Help);
      fireEventDelayed(new ActivateHelpEvent(), 200);
   }
   @Handler
   void onLayoutZoomVcs() { paneManager_.zoomTab(Tab.VCS); }
   @Handler
   void onLayoutZoomBuild() { paneManager_.zoomTab(Tab.Build); }
   @Handler
   void onLayoutZoomViewer() { paneManager_.zoomTab(Tab.Viewer); }
   @Handler
   void onLayoutZoomConnections() { paneManager_.zoomTab(Tab.Connections); }
   @Handler
   void onLayoutZoomTutorial() { paneManager_.zoomTab(Tab.Tutorial); }

   @Handler
   void onLayoutZoomLeftColumn() { paneManager_.zoomColumn(PaneManager.LEFT_COLUMN); }

   @Handler
   void onLayoutZoomRightColumn() { paneManager_.zoomColumn(PaneManager.RIGHT_COLUMN); }

   @Handler
   void onMacPreferences()
   {
      onShowOptions();
   }

   @Handler
   void onShowOptions()
   {
      optionsLoader_.showOptions();
   }

   @Handler
   void onShowCodeOptions()
   {
      optionsLoader_.showOptions(EditingPreferencesPane.class, true);
   }

   @Handler
   void onShowConsoleOptions()
   {
      optionsLoader_.showOptions(ConsolePreferencesPane.class, true);
   }

   @Handler
   void onShowAppearanceOptions()
   {
      optionsLoader_.showOptions(AppearancePreferencesPane.class, true);
   }

   @Handler
   void onShowPackagesOptions()
   {
      optionsLoader_.showOptions(PackagesPreferencesPane.class, true);
   }

   @Handler
   void onShowRMarkdownOptions()
   {
      optionsLoader_.showOptions(RMarkdownPreferencesPane.class, true);
   }

   @Handler
   void onShowSweaveOptions()
   {
      optionsLoader_.showOptions(CompilePdfPreferencesPane.class, true);
   }

   @Handler
   void onShowSpellingOptions()
   {
      optionsLoader_.showOptions(SpellingPreferencesPane.class, true);
   }

   @Handler
   void onShowVcsOptions()
   {
      optionsLoader_.showOptions(SourceControlPreferencesPane.class, true);
   }

   @Handler
   void onShowPublishingOptions()
   {
      optionsLoader_.showOptions(PublishingPreferencesPane.class, true);
   }

   @Handler
   void onShowPythonOptions()
   {
      optionsLoader_.showOptions(PythonPreferencesPane.class, true);
   }

   @Handler
   void onShowAccessibilityOptions()
   {
      optionsLoader_.showOptions(AccessibilityPreferencesPane.class, false);
   }

   @Handler
   void onShowTerminalOptions()
   {
      optionsLoader_.showOptions(TerminalPreferencesPane.class, true);
   }

   @Handler
   void onVersionControlHelp()
   {
      globalDisplay_.openRStudioLink("using_version_control");
   }

   public Widget asWidget()
   {
      return this;
   }

   private final TimeBufferedCommand paneSizesChangedCommand_;

   private WorkbenchMetrics lastMetrics_ = WorkbenchMetrics.create(0,0,0,0,1.0);

   private final GlobalDisplay globalDisplay_;
   private final EventBus eventBus_;
   private final Session session_;
   private final Shim edit_;
   private final org.rstudio.studio.client.workbench.ui.OptionsLoader.Shim optionsLoader_;

   private final MainSplitPanel tabsPanel_;
   private final PaneManager paneManager_;
}
