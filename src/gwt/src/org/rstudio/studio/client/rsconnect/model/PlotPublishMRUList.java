/*
 * PlotPublishMRUList.java
 *
 * Copyright (C) 2009-15 by RStudio, Inc.
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
package org.rstudio.studio.client.rsconnect.model;

import java.util.ArrayList;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.AppCommand;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ToolbarPopupMenu;
import org.rstudio.studio.client.rsconnect.ui.RSConnectResources;
import org.rstudio.studio.client.workbench.WorkbenchList;
import org.rstudio.studio.client.workbench.WorkbenchListManager;
import org.rstudio.studio.client.workbench.events.ListChangedEvent;
import org.rstudio.studio.client.workbench.events.ListChangedHandler;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.MenuItem;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class PlotPublishMRUList
{
   public static class Entry 
   {
      public Entry(String accountIn, String serverIn, String nameIn, 
            String titleIn)
      {
         account = accountIn;
         server = serverIn;
         name = nameIn;
         title = titleIn;
      }
      
      public String asText()
      {
         return account + "|" + server + "|" + name + 
               (StringUtil.isNullOrEmpty(title) ? "" : "|" + title);
      }
      
      public static Entry fromText(String text)
      {
         String[] pieces = text.split("\\|");
         if (pieces.length < 3)
            return null;
         
         return new Entry(pieces[0], pieces[1], pieces[2],
               pieces.length > 3 ? pieces[3] : "");
      }

      public final String account;
      public final String server;
      public final String name;
      public final String title;
   }

   @Inject 
   public PlotPublishMRUList(WorkbenchListManager listManager)
   {
      plotMru_ = listManager.getPlotPublishMruList();
      plotMru_.addListChangedHandler(new ListChangedHandler()
      {
         @Override
         public void onListChanged(ListChangedEvent event)
         {
            plotMruList_ = event.getList();
         }
      });
   }
   
   public void addPlotMruEntries(ToolbarPopupMenu menu, 
         final OperationWithInput<Entry> onSelected)
   {
      for (String entry: plotMruList_)
      {
         final Entry mruEntry = Entry.fromText(entry);
         if (entry == null)
            continue;
         
         // format the display name: pick title if specified, name if not
         String displayName = StringUtil.isNullOrEmpty(mruEntry.title) ?
               mruEntry.name : mruEntry.title;
         displayName += " (" + mruEntry.server + ")";
         
         menu.addItem(new MenuItem(AppCommand.formatMenuLabel(
               RSConnectResources.INSTANCE.republishPlot2x(), 
               displayName, null), true, 
               new Command() 
         {
            @Override
            public void execute()
            {
               onSelected.execute(mruEntry);
            }
         }));
      }
   }
   
   public void addPlotMruEntry(String account, String server, String name,
         String title)
   {
      Entry mruEntry = new Entry(account, server, name, title);
      plotMru_.prepend(mruEntry.asText());
   }
   
   private WorkbenchList plotMru_;
   private ArrayList<String> plotMruList_ = new ArrayList<String>();
}
