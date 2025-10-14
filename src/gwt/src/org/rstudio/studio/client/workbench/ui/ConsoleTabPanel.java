/*
 * ConsoleTabPanel.java
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

import org.rstudio.core.client.layout.LogicalWindow;
import org.rstudio.core.client.theme.PrimaryWindowFrame;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.views.console.ConsoleClearButton;
import org.rstudio.studio.client.workbench.views.console.ConsoleInterpreterVersion;
import org.rstudio.studio.client.workbench.views.console.ConsoleInterruptButton;
import org.rstudio.studio.client.workbench.views.console.ConsoleInterruptProfilerButton;
import org.rstudio.studio.client.workbench.views.console.ConsolePane;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleRestartRCompletedEvent;
import org.rstudio.studio.client.workbench.views.console.events.WorkingDirChangedEvent;
import org.rstudio.studio.client.workbench.views.output.find.FindOutputTab;
import org.rstudio.studio.client.workbench.views.output.markers.MarkersOutputTab;

import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;

public class ConsoleTabPanel extends WorkbenchTabPanel
{
   public static final String RSTUDIO_CONSOLE_WIDGET_LAYOUT = "rstudio-console-widget-layout";
   public static final String RSTUDIO_CONSOLE_HEADER_LAYOUT = "rstudio-console-header-layout";
   public static final String RSTUDIO_CONSOLE_MINIMIZE_LAYOUT = "rstudio-console-minimize-layout";
   public static final String RSTUDIO_CONSOLE_MAXIMIZE_LAYOUT = "rstudio-console-maximize-layout";
   
   @Inject
   public void initialize(ConsoleInterruptButton consoleInterrupt,
                          ConsoleInterruptProfilerButton consoleInterruptProfiler,
                          ConsoleClearButton consoleClearButton,
                          ConsoleInterpreterVersion consoleInterpreterVersion,
                          UserPrefs uiPrefs,
                          Session session)
   {
      consoleInterrupt_ = consoleInterrupt;
      consoleInterruptProfiler_ = consoleInterruptProfiler;
      consoleClearButton_ = consoleClearButton;
      consoleInterpreterVersion_ = consoleInterpreterVersion;
      userPrefs_ = uiPrefs;
      session_ = session;
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
                          EventBus events,
                          ToolbarButton goToWorkingDirButton,
                          WorkbenchTab testsTab,
                          WorkbenchTab dataTab,
                          WorkbenchTab backgroundJobsTab,
                          WorkbenchTab workbenchJobsTab)
   {
      super(owner, parentWindow, "ConsoleTabSet", null);
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
      testsTab_ = testsTab;
      dataTab_ = dataTab;
      backgroundJobsTab_ = backgroundJobsTab;
      workbenchJobsTab_ = workbenchJobsTab;
      
      RStudioGinjector.INSTANCE.injectMembers(this);

      compilePdfTab.addEnsureVisibleHandler(ensureVisibleEvent ->
      {
         compilePdfTabVisible_ = true;
         managePanels();
         if (ensureVisibleEvent.getActivate())
            selectTab(compilePdfTab_);
      });
      compilePdfTab.addEnsureHiddenHandler(ensureHiddenEvent ->
      {
         compilePdfTabVisible_ = false;
         managePanels();
         if (!consoleOnly_)
            selectTab(0);
      });

      findResultsTab.addEnsureVisibleHandler(ensureVisibleEvent ->
      {
         findResultsTabVisible_ = true;
         managePanels();
         if (ensureVisibleEvent.getActivate())
            selectTab(findResultsTab_);
      });
      findResultsTab.addEnsureHiddenHandler(ensureHiddenEvent ->
      {
         findResultsTab_.onDismiss();
         findResultsTabVisible_ = false;
         managePanels();
         if (!consoleOnly_)
            selectTab(0);
      });
      
      sourceCppTab.addEnsureVisibleHandler(ensureVisibleEvent ->
      {
         sourceCppTabVisible_ = true;
         managePanels();
         if (ensureVisibleEvent.getActivate())
            selectTab(sourceCppTab_);
      });
      sourceCppTab.addEnsureHiddenHandler(ensureHiddenEvent ->
      {
         sourceCppTabVisible_ = false;
         managePanels();
         if (!consoleOnly_)
            selectTab(0);
      });

      renderRmdTab.addEnsureVisibleHandler(ensureVisibleEvent ->
      {
         renderRmdTabVisible_ = true;
         managePanels();
         if (ensureVisibleEvent.getActivate())
            selectTab(renderRmdTab_);
      });
      renderRmdTab.addEnsureHiddenHandler(ensureHiddenEvent ->
      {
         renderRmdTabVisible_ = false;
         managePanels();
         if (!consoleOnly_)
            selectTab(0);
      });

      testsTab.addEnsureVisibleHandler(ensureVisibleEvent ->
      {
         testsTabVisible_ = true;
         managePanels();
         if (ensureVisibleEvent.getActivate())
            selectTab(testsTab_);
      });
      testsTab.addEnsureHiddenHandler(ensureHiddenEvent ->
      {
         testsTabVisible_ = false;
         managePanels();
         if (!consoleOnly_)
            selectTab(0);
      });

      dataTab.addEnsureVisibleHandler(ensureVisibleEvent ->
      {
         dataTabVisible_ = true;
         managePanels();
         if (ensureVisibleEvent.getActivate())
            selectTab(dataTab_);
      });
      dataTab.addEnsureHiddenHandler(ensureHiddenEvent ->
      {
         dataTabVisible_ = false;
         managePanels();
         if (!consoleOnly_)
            selectTab(0);
      });

      deployContentTab.addEnsureVisibleHandler(ensureVisibleEvent ->
      {
         deployContentTabVisible_ = true;
         managePanels();
         if (ensureVisibleEvent.getActivate())
            selectTab(deployContentTab_);
      });
      deployContentTab.addEnsureHiddenHandler(ensureHiddenEvent ->
      {
         deployContentTabVisible_ = false;
         managePanels();
         if (!consoleOnly_)
            selectTab(0);
      });
      
      markersTab.addEnsureVisibleHandler(ensureVisibleEvent ->
      {
         markersTabVisible_ = true;
         managePanels();
         if (ensureVisibleEvent.getActivate())
            selectTab(markersTab_);
      });
      markersTab.addEnsureHiddenHandler(ensureHiddenEvent ->
      {
         markersTab_.onDismiss();
         markersTabVisible_ = false;
         managePanels();
         if (!consoleOnly_)
            selectTab(0);
      });

      terminalTab.addEnsureVisibleHandler(ensureVisibleEvent ->
      {
         terminalTabVisible_ = true;
         managePanels();
         if (ensureVisibleEvent.getActivate())
            selectTab(terminalTab_);
      });
      terminalTab.addEnsureHiddenHandler(ensureHiddenEvent ->
      {
         terminalTabVisible_ = false;
         managePanels();
         if (!consoleOnly_)
            selectTab(0);
      });

      backgroundJobsTab.addEnsureVisibleHandler(ensureVisibleEvent ->
      {
         backgroundJobsTabVisible_ = true;
         managePanels();
         if (ensureVisibleEvent.getActivate())
            selectTab(backgroundJobsTab_);
      });
      backgroundJobsTab.addEnsureHiddenHandler(ensureHiddenEvent ->
      {
         backgroundJobsTabVisible_ = false;
         managePanels();
         if (!consoleOnly_)
            selectTab(0);
      });

      workbenchJobsTab.addEnsureVisibleHandler(ensureVisibleEvent ->
      {
         workbenchJobsTabVisible_ = true;
         managePanels();
         if (ensureVisibleEvent.getActivate())
            selectTab(workbenchJobsTab_);
      });
      workbenchJobsTab.addEnsureHiddenHandler(ensureHiddenEvent ->
      {
         workbenchJobsTabVisible_ = false;
         managePanels();
         if (!consoleOnly_)
            selectTab(0);
      });
      
      events.addHandler(WorkingDirChangedEvent.TYPE, workingDirChangedEvent ->
      {
         String path = workingDirChangedEvent.getPath();
         if (!path.endsWith("/"))
            path += "/";
         consolePane_.setWorkingDirectory(path);
         owner.setSubtitle(path);
      });

      // listen for R session restarts, and update ConsoleInterpreterVersion after event completes
      events.addHandler(ConsoleRestartRCompletedEvent.TYPE, consoleRestartRCompletedEvent ->
      {
         consolePane_.updateConsoleInterpreterVersion();
      });

      // Determine initial visibility of terminal tab
      terminalTabVisible_ = userPrefs_.showTerminalTab().getValue();
      if (terminalTabVisible_ && !session_.getSessionInfo().getAllowShell())
      {
         terminalTabVisible_ = false;
      }
      
      // Determine initial visibility of background jobs and workbench jobs tabs
      String backgroundJobsTabVisibilitySetting = userPrefs_.jobsTabVisibility().getValue();
      Command showWorkbenchJobsTab = () ->
      {
         workbenchJobsTabVisible_ = userPrefs_.showLauncherJobsTab().getValue();

         // By default, we don't show the Background Jobs tab when Workbench Jobs tab is visible.
         // However, if user has explicitly shown or hidden Background Jobs, we'll
         // honor that independent of Workbench Jobs tabs visibility.
         switch (backgroundJobsTabVisibilitySetting)
         {
            default:
            case UserPrefs.JOBS_TAB_VISIBILITY_DEFAULT:
               backgroundJobsTabVisible_ = !workbenchJobsTabVisible_;
               break;

            case UserPrefs.JOBS_TAB_VISIBILITY_CLOSED:
               backgroundJobsTabVisible_ = false;
               break;

            case UserPrefs.JOBS_TAB_VISIBILITY_SHOWN:
               backgroundJobsTabVisible_ = true;
               break;
         }
      };


      if (session_.getSessionInfo().getWorkbenchJobsEnabled())
      {
         showWorkbenchJobsTab.execute();
      }
      else
      {
         Command hideWorkbenchJobsTab = () ->
         {
            workbenchJobsTabVisible_ = false;
            switch (backgroundJobsTabVisibilitySetting)
            {
               default:
               case UserPrefs.JOBS_TAB_VISIBILITY_DEFAULT:
               case UserPrefs.JOBS_TAB_VISIBILITY_SHOWN:
                  backgroundJobsTabVisible_ = true;
                  break;

               case UserPrefs.JOBS_TAB_VISIBILITY_CLOSED:
                  backgroundJobsTabVisible_ = false;
                  break;
            }
         };

         if (Desktop.hasDesktopFrame())
         {
            // if there are session servers defined, we will show the Workbench Jobs tab
            Desktop.getFrame().getSessionServers(servers ->
            {
               if (servers.length() > 0)
               {
                  showWorkbenchJobsTab.execute();
                  managePanels();
               }
               else
               {
                  hideWorkbenchJobsTab.execute();
                  managePanels();
               }
            });
         }
         else
         {
            hideWorkbenchJobsTab.execute();
         }
      }

      // This ensures the logic in managePanels() works whether starting
      // up with terminal tab on by default or not.
      consoleOnly_ = terminalTabVisible_ || backgroundJobsTabVisible_ || workbenchJobsTabVisible_;
      managePanels();
   }

   private void managePanels()
   {
      boolean consoleOnly = !terminalTabVisible_ &&
                            !compilePdfTabVisible_ && 
                            !findResultsTabVisible_ &&
                            !sourceCppTabVisible_ &&
                            !renderRmdTabVisible_ &&
                            !deployContentTabVisible_ &&
                            !markersTabVisible_ &&
                            !testsTabVisible_ &&
                            !dataTabVisible_ &&
                            !backgroundJobsTabVisible_ &&
                            !workbenchJobsTabVisible_;

      consolePane_.setIsTabPanel(!consoleOnly);;
      if (consoleOnly)
      {
         owner_.addStyleName(ThemeResources.INSTANCE.themeStyles().consoleOnlyWindowFrame());
         owner_.getTitleWidget().getElement().removeAttribute("aria-hidden");
         owner_.getSubtitleWidget().getElement().removeAttribute("aria-hidden");
         goToWorkingDirButton_.getElement().removeAttribute("aria-hidden");
      }
      else
      {
         owner_.removeStyleName(ThemeResources.INSTANCE.themeStyles().consoleOnlyWindowFrame());
         owner_.getTitleWidget().getElement().setAttribute("aria-hidden", "true");
         owner_.getSubtitleWidget().getElement().setAttribute("aria-hidden", "true");
         goToWorkingDirButton_.getElement().setAttribute("aria-hidden", "true");
      }
      
      if (!consoleOnly)
      {
         ArrayList<WorkbenchTab> tabs = new ArrayList<>();
         tabs.add(consolePane_);
         if (terminalTabVisible_)
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
         if (testsTabVisible_)
            tabs.add(testsTab_);
         if (dataTabVisible_)
            tabs.add(dataTab_);
         if (backgroundJobsTabVisible_)
            tabs.add(backgroundJobsTab_);
         if (workbenchJobsTabVisible_)
            tabs.add(workbenchJobsTab_);

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
            
            owner_.setContextButton(consoleClearButton_,
                                    consoleClearButton_.getWidth(),
                                    consoleClearButton_.getHeight(),
                                    0);
            owner_.setContextButton(consoleInterrupt_,
                                    consoleInterrupt_.getWidth(),
                                    consoleInterrupt_.getHeight(),
                                    1);
            owner_.setContextButton(consoleInterruptProfiler_,
                                    consoleInterruptProfiler_.getWidth(),
                                    consoleInterruptProfiler_.getHeight(),
                                    2);
            
                  
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
            owner_.setContextButton(null, 0, 0, 2);
         }
      }
      
      addLayoutStyles(owner_.getElement());
   }
   
   public void addLayoutStyles(Element parent)
   {
      // In order to be able to style the actual layout div that GWT uses internally
      // to construct the WindowFrame layout, we need to assign it ourselves.
      for (Element e = parent.getFirstChildElement(); e != null; e = e.getNextSiblingElement()) {
         boolean hasWidgetClass = false;
         boolean hasHeaderClass = false;
         boolean hasMinimizeClass = false;
         boolean hasMaximizeClass = false;
         
         for (Element c = e.getFirstChildElement(); c != null; c = c.getNextSiblingElement()) {
            if (c.hasClassName(ThemeResources.INSTANCE.themeStyles().windowFrameWidget()))
               hasWidgetClass = true;
            
            if (c.hasClassName(ThemeResources.INSTANCE.themeStyles().primaryWindowFrameHeader()))
               hasHeaderClass = true;
            
            if (c.hasClassName(ThemeResources.INSTANCE.themeStyles().minimize()))
               hasMinimizeClass = true;
            
            if (c.hasClassName(ThemeResources.INSTANCE.themeStyles().maximize()))
               hasMaximizeClass = true;
         }
         
         if (hasWidgetClass)
         {
            e.addClassName(ThemeResources.INSTANCE.themeStyles().consoleWidgetLayout());
            e.addClassName(RSTUDIO_CONSOLE_WIDGET_LAYOUT);
         }
         
         if (hasHeaderClass)
         {
            e.addClassName(ThemeResources.INSTANCE.themeStyles().consoleHeaderLayout());
            e.addClassName(RSTUDIO_CONSOLE_HEADER_LAYOUT);
         }
         
         if (hasMinimizeClass)
         {
            e.addClassName(ThemeResources.INSTANCE.themeStyles().consoleMinimizeLayout());
            e.addClassName(RSTUDIO_CONSOLE_MINIMIZE_LAYOUT);
         }
         
         if (hasMaximizeClass)
         {
            e.addClassName(ThemeResources.INSTANCE.themeStyles().consoleMaximizeLayout());
            e.addClassName(RSTUDIO_CONSOLE_MAXIMIZE_LAYOUT);
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
   private boolean terminalTabVisible_;
   private ConsoleInterruptButton consoleInterrupt_;
   private ConsoleInterruptProfilerButton consoleInterruptProfiler_;
   private ConsoleClearButton consoleClearButton_;
   @SuppressWarnings("unused")
   private ConsoleInterpreterVersion consoleInterpreterVersion_;
   private final ToolbarButton goToWorkingDirButton_;
   private boolean findResultsTabVisible_;
   private boolean consoleOnly_;
   private UserPrefs userPrefs_;
   private Session session_;
   private final WorkbenchTab testsTab_;
   private boolean testsTabVisible_;
   private final WorkbenchTab dataTab_;
   private boolean dataTabVisible_;
   private final WorkbenchTab backgroundJobsTab_;
   private boolean backgroundJobsTabVisible_;
   private final WorkbenchTab workbenchJobsTab_;
   private boolean workbenchJobsTabVisible_;
}
