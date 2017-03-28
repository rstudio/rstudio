/*
 * DocsMenu.java
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
package org.rstudio.studio.client.workbench.views.source;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.gwt.user.client.ui.PopupPanel;
import com.google.inject.Inject;

import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.command.AppMenuBar;
import org.rstudio.core.client.command.DisabledMenuItem;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.views.source.events.DocTabsChangedEvent;
import org.rstudio.studio.client.workbench.views.source.events.DocTabsChangedHandler;
import org.rstudio.studio.client.workbench.views.source.events.SwitchToDocEvent;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class DocsMenu extends AppMenuBar
{
   public DocsMenu()
   {
      super(true);
      RStudioGinjector.INSTANCE.injectMembers(this);
   }

   @Inject
   public void initialize(EventBus events)
   {
      assert events_ == null : "DocsMenu.initialize was called more than once";
      events_ = events;
      events_.addHandler(
            DocTabsChangedEvent.TYPE,
            new DocTabsChangedHandler()
            {
               public void onDocTabsChanged(DocTabsChangedEvent event)
               {
                  setDocs(event.getIcons(), event.getNames(), event.getPaths());
               }
            }) ;
   }

   public void setOwnerPopupPanel(PopupPanel panel)
   {
      panel_ = panel;
   }

   public void setDocs(ImageResource[] icons, String[] names, String[] paths)
   {
      clearItems();
      names_.clear();
      menuItems_.clear();
      
      // de-duplicate names
      names = deduplicate(names, paths);

      assert icons.length == names.length && names.length == paths.length;

      if (icons.length == 0)
      {
         addItem(new DisabledMenuItem("(No documents)"));
      }

      for (int i = 0; i < icons.length; i++)
      {
         String label = AppCommand.formatMenuLabel(icons[i],
                                                   names[i] + "\u00A0\u00A0\u00A0",
                                                   null);
         final int tabIndex = i;
         MenuItem item = addItem(label, true, new Command()
         {
            public void execute()
            {
               if (panel_ != null)
                  panel_.hide(false);
               events_.fireEvent(new SwitchToDocEvent(tabIndex));
            }
         });
         item.setTitle(paths[i]);

         names_.add(names[i]);
         menuItems_.add(item);
      }
   }

   public void filter(String criteria)
   {
      for (int i = 0; i < names_.size(); i++)
      {
         menuItems_.get(i).setVisible(shouldShow(criteria, names_.get(i)));
      }
   }

   private boolean shouldShow(String filterCriteria, String value)
   {
      if (filterCriteria == null)
         return true;
      return value.toLowerCase().startsWith(filterCriteria.toLowerCase());
   }
   
   private String[] deduplicate(String[] names, String[] paths)
   {
      Map<String, Integer> counts = new HashMap<String, Integer>();
      
      // initialize map with zeroes
      int n = names.length;
      for (int i = 0; i < n; i++)
         counts.put(names[i], 0);
      
      // generate counts
      for (int i = 0; i < n; i++)
         counts.put(names[i], counts.get(names[i]) + 1);
      
      // de-duplicate names based on path components
      String[] deduped = new String[names.length];
      for (int i = 0; i < n; i++)
      {
         if (counts.get(names[i]) >= 2 && paths[i] != null)
         {
            FileSystemItem item = FileSystemItem.createFile(paths[i]);
            deduped[i] = names[i] + " \u2014 " + item.getParentPath().getName();
         }
         else
         {
            deduped[i] = names[i];
         }
      }
      
      // count duplicates once more
      counts.clear();
      for (int i = 0; i < n; i++)
         counts.put(deduped[i], 0);
      
      for (int i = 0; i < n; i++)
         counts.put(deduped[i], counts.get(deduped[i]) + 1);
      
      // for items that are still duplicated, just print the full parent path
      for (int i = 0; i < n; i++)
      {
         if (counts.get(deduped[i]) >= 2 && paths[i] != null)
         {
            FileSystemItem item = FileSystemItem.createFile(paths[i]);
            deduped[i] = names[i] + " \u2014 " + item.getParentPath().getPath();
         }
      }
      
      return deduped;
   }

   private ArrayList<String> names_ = new ArrayList<String>();
   private ArrayList<MenuItem> menuItems_ = new ArrayList<MenuItem>();
   private EventBus events_;
   private PopupPanel panel_;
}
