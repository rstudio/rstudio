/*
 * ConsoleTabPanel.java
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

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.events.*;
import org.rstudio.core.client.theme.PrimaryWindowFrame;
import org.rstudio.core.client.theme.WindowFrame;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.views.console.ConsolePane;
import org.rstudio.studio.client.workbench.views.console.events.WorkingDirChangedEvent;
import org.rstudio.studio.client.workbench.views.console.events.WorkingDirChangedHandler;

import java.util.ArrayList;

public class ConsoleTabPanel extends WorkbenchTabPanel
{
   public ConsoleTabPanel(final PrimaryWindowFrame owner,
                          ConsolePane consolePane,
                          WorkbenchTab compilePdfTab,
                          WorkbenchTab findResultsTab,
                          EventBus events)
   {
      super(owner);
      owner_ = owner;
      consolePane_ = consolePane;
      compilePdfTab_ = compilePdfTab;
      findResultsTab_ = findResultsTab;

      compilePdfTab.addEnsureVisibleHandler(new EnsureVisibleHandler()
      {
         @Override
         public void onEnsureVisible(EnsureVisibleEvent event)
         {
            compilePdfTabVisible_ = true;
            managePanels();
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
            selectTab(findResultsTab_);
         }
      });
      findResultsTab.addEnsureHiddenHandler(new EnsureHiddenHandler()
      {
         @Override
         public void onEnsureHidden(EnsureHiddenEvent event)
         {
            findResultsTabVisible_ = false;
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

      consoleOnly_ = false;
      managePanels();
   }

   private void managePanels()
   {
      boolean consoleOnly = !compilePdfTabVisible_ && !findResultsTabVisible_;

      if (!consoleOnly)
      {
         ArrayList<WorkbenchTab> tabs = new ArrayList<WorkbenchTab>();
         tabs.add(consolePane_);
         if (compilePdfTabVisible_)
            tabs.add(compilePdfTab_);
         if (findResultsTabVisible_)
            tabs.add(findResultsTab_);

         setTabs(tabs);
      }

      if (consoleOnly != consoleOnly_)
      {
         consoleOnly_ = consoleOnly;

         consolePane_.setMainToolbarVisible(!consoleOnly);
         if (consoleOnly)
         {
            owner_.setMainWidget(consolePane_);
            consolePane_.onBeforeSelected();
            consolePane_.onSelected();
            consolePane_.setVisible(true);
         }
         else
         {
            consolePane_.onBeforeUnselected();
            owner_.setFillWidget(this);
         }
      }
   }

   private final WindowFrame owner_;
   private final ConsolePane consolePane_;
   private final WorkbenchTab compilePdfTab_;
   private boolean compilePdfTabVisible_;
   private final WorkbenchTab findResultsTab_;
   private boolean findResultsTabVisible_;
   private boolean consoleOnly_;
}
