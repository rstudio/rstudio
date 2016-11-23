/*
 * ConsoleTabPanel.java
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

import org.rstudio.core.client.events.*;
import org.rstudio.core.client.layout.LogicalWindow;
import org.rstudio.core.client.theme.PrimaryWindowFrame;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.views.console.ConsoleInterruptButton;
import org.rstudio.studio.client.workbench.views.console.ConsoleInterruptProfilerButton;
import org.rstudio.studio.client.workbench.views.console.ConsolePane;
import org.rstudio.studio.client.workbench.views.console.events.WorkingDirChangedEvent;
import org.rstudio.studio.client.workbench.views.console.events.WorkingDirChangedHandler;
import org.rstudio.studio.client.workbench.views.output.find.FindOutputTab;
import org.rstudio.studio.client.workbench.views.output.markers.MarkersOutputTab;

import com.google.inject.Inject;

import java.util.ArrayList;

public class ConsoleTabPanel extends WorkbenchTabPanel
{
   @Inject
   public void initialize(ConsoleInterruptButton consoleInterrupt,
                          ConsoleInterruptProfilerButton consoleInterruptProfiler)
   {
      consoleInterrupt_ = consoleInterrupt;
      consoleInterruptProfiler_ = consoleInterruptProfiler;
   }
   
   public ConsoleTabPanel(final PrimaryWindowFrame owner,
                          final LogicalWindow parentWindow,
                          ConsolePane consolePane,
                          WorkbenchTab compilePdfTab,
                          FindOutputTab findResultsTab,
                          WorkbenchTab sourceCppTab,
                          WorkbenchTab renderRmdTab, 
                          WorkbenchTab deployContentTab,
                          MarkersOutputTab markersTab,
                          WorkbenchTab terminalTab,
                          boolean enableTerminalTab,
                          EventBus events,
                          ToolbarButton goToWorkingDirButton)
   {
      super(owner, parentWindow);
      owner_ = owner;
      consolePane_ = consolePane;
      compilePdfTab_ = compilePdfTab;
      findResultsTab_ = findResultsTab;
      sourceCppTab_ = sourceCppTab;
      goToWorkingDirButton_ = goToWorkingDirButton;
      renderRmdTab_ = renderRmdTab;
      deployContentTab_ = deployContentTab;
      markersTab_ = markersTab;
      terminalTab_ = terminalTab;
      enableTerminalTab_ = enableTerminalTab;
      
      RStudioGinjector.INSTANCE.injectMembers(this);

      compilePdfTab.addEnsureVisibleHandler(new EnsureVisibleHandler()
      {
         @Override
         public void onEnsureVisible(EnsureVisibleEvent event)
         {
            compilePdfTabVisible_ = true;
            managePanels();
            if (event.getActivate())
               selectTab(compilePdfTab_);
         }
      });
      compilePdfTab.addEnsureHiddenHandler(new EnsureHiddenHandler()
      {
         @Override
         public void onEnsureHidden(EnsureHiddenEvent event)
         {
            compilePdfTabVisible_ = false;
            managePanels();
            if (!consoleOnly_)
               selectTab(0);
         }
      });

      findResultsTab.addEnsureVisibleHandler(new EnsureVisibleHandler()
      {
         @Override
         public void onEnsureVisible(EnsureVisibleEvent event)
         {
            findResultsTabVisible_ = true;
            managePanels();
            if (event.getActivate())
               selectTab(findResultsTab_);
         }
      });
      findResultsTab.addEnsureHiddenHandler(new EnsureHiddenHandler()
      {
         @Override
         public void onEnsureHidden(EnsureHiddenEvent event)
         {
            findResultsTab_.onDismiss();
            findResultsTabVisible_ = false;
            managePanels();
            if (!consoleOnly_)
               selectTab(0);
         }
      });
      
      sourceCppTab.addEnsureVisibleHandler(new EnsureVisibleHandler()
      {
         @Override
         public void onEnsureVisible(EnsureVisibleEvent event)
         {
            sourceCppTabVisible_ = true;
            managePanels();
            if (event.getActivate())
               selectTab(sourceCppTab_);
         }
      });
      sourceCppTab.addEnsureHiddenHandler(new EnsureHiddenHandler()
      {
         @Override
         public void onEnsureHidden(EnsureHiddenEvent event)
         {
            sourceCppTabVisible_ = false;
            managePanels();
            if (!consoleOnly_)
               selectTab(0);
         }
      });

      renderRmdTab.addEnsureVisibleHandler(new EnsureVisibleHandler()
      {
         @Override
         public void onEnsureVisible(EnsureVisibleEvent event)
         {
            renderRmdTabVisible_ = true;
            managePanels();
            if (event.getActivate())
               selectTab(renderRmdTab_);
         }
      });
      renderRmdTab.addEnsureHiddenHandler(new EnsureHiddenHandler()
      {
         @Override
         public void onEnsureHidden(EnsureHiddenEvent event)
         {
            renderRmdTabVisible_ = false;
            managePanels();
            if (!consoleOnly_)
               selectTab(0);
         }
      });

      deployContentTab.addEnsureVisibleHandler(new EnsureVisibleHandler()
      {
         @Override
         public void onEnsureVisible(EnsureVisibleEvent event)
         {
            deployContentTabVisible_ = true;
            managePanels();
            if (event.getActivate())
               selectTab(deployContentTab_);
         }
      });
      deployContentTab.addEnsureHiddenHandler(new EnsureHiddenHandler()
      {
         @Override
         public void onEnsureHidden(EnsureHiddenEvent event)
         {
            deployContentTabVisible_ = false;
            managePanels();
            if (!consoleOnly_)
               selectTab(0);
         }
      });
      
      markersTab.addEnsureVisibleHandler(new EnsureVisibleHandler()
      {
         @Override
         public void onEnsureVisible(EnsureVisibleEvent event)
         {
            markersTabVisible_ = true;
            managePanels();
            if (event.getActivate())
               selectTab(markersTab_);
         }
      });
      markersTab.addEnsureHiddenHandler(new EnsureHiddenHandler()
      {
         @Override
         public void onEnsureHidden(EnsureHiddenEvent event)
         {
            markersTab_.onDismiss();
            markersTabVisible_ = false;
            managePanels();
            if (!consoleOnly_)
               selectTab(0);
         }
      });
      
      events.addHandler(WorkingDirChangedEvent.TYPE, new WorkingDirChangedHandler()
      {
         @Override
         public void onWorkingDirChanged(WorkingDirChangedEvent event)
         {
            String path = event.getPath();
            if (!path.endsWith("/"))
               path += "/";
            consolePane_.setWorkingDirectory(path);
            owner.setSubtitle(path);
         }
      });

      consoleOnly_ = enableTerminalTab_; 
      managePanels();
   }

   private void managePanels()
   {
      boolean consoleOnly = !enableTerminalTab_ &&
                            !compilePdfTabVisible_ && 
                            !findResultsTabVisible_ &&
                            !sourceCppTabVisible_ &&
                            !renderRmdTabVisible_ &&
                            !deployContentTabVisible_ &&
                            !markersTabVisible_;

      if (!consoleOnly)
      {
         ArrayList<WorkbenchTab> tabs = new ArrayList<WorkbenchTab>();
         tabs.add(consolePane_);
         if (enableTerminalTab_)
            tabs.add(terminalTab_);
         if (compilePdfTabVisible_)
            tabs.add(compilePdfTab_);
         if (findResultsTabVisible_)
            tabs.add(findResultsTab_);
         if (sourceCppTabVisible_)
            tabs.add(sourceCppTab_);
         if (renderRmdTabVisible_)
            tabs.add(renderRmdTab_);
         if (deployContentTabVisible_)
            tabs.add(deployContentTab_);
         if (markersTabVisible_)
            tabs.add(markersTab_);

         setTabs(tabs);
      }

      if (consoleOnly != consoleOnly_)
      {
         consoleOnly_ = consoleOnly;

         consolePane_.setMainToolbarVisible(!consoleOnly);
         if (consoleOnly)
         {
            owner_.setMainWidget(consolePane_);
            owner_.addLeftWidget(goToWorkingDirButton_);
            owner_.setContextButton(consoleInterrupt_,
                                    consoleInterrupt_.getWidth(),
                                    consoleInterrupt_.getHeight(),
                                    0);
            owner_.setContextButton(consoleInterruptProfiler_,
                  consoleInterruptProfiler_.getWidth(),
                  consoleInterruptProfiler_.getHeight(),
                  1);
            consolePane_.onBeforeSelected();
            consolePane_.onSelected();
            consolePane_.setVisible(true);
         }
         else
         {
            consolePane_.onBeforeUnselected();
            owner_.setFillWidget(this);
            owner_.setContextButton(null, 0, 0, 0);
            owner_.setContextButton(null, 0, 0, 1);
         }
      }
   }

   private final PrimaryWindowFrame owner_;
   private final ConsolePane consolePane_;
   private final WorkbenchTab compilePdfTab_;
   private boolean compilePdfTabVisible_;
   private final FindOutputTab findResultsTab_;
   private final WorkbenchTab sourceCppTab_;
   private boolean sourceCppTabVisible_;
   private final WorkbenchTab renderRmdTab_;
   private boolean renderRmdTabVisible_;
   private final WorkbenchTab deployContentTab_;
   private boolean deployContentTabVisible_;
   private final MarkersOutputTab markersTab_;
   private boolean markersTabVisible_;
   private final WorkbenchTab terminalTab_;
   private boolean enableTerminalTab_;
   private ConsoleInterruptButton consoleInterrupt_;
   private ConsoleInterruptProfilerButton consoleInterruptProfiler_;
   private final ToolbarButton goToWorkingDirButton_;
   private boolean findResultsTabVisible_;
   private boolean consoleOnly_;
}
