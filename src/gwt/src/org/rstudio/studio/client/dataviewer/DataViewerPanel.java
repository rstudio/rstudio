/*
 * DataViewerPanel.java
 *
 * Copyright (C) 2009-14 by RStudio, Inc.
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

package org.rstudio.studio.client.dataviewer;

import com.google.inject.Inject;

import org.rstudio.core.client.widget.RStudioFrame;
import org.rstudio.core.client.widget.SatelliteFramePanel;
import org.rstudio.core.client.widget.Toolbar;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.source.events.DataViewChangedEvent;
import org.rstudio.studio.client.workbench.views.source.model.DataItem;
import org.rstudio.studio.client.application.events.EventBus;

public class DataViewerPanel extends SatelliteFramePanel<RStudioFrame>
                             implements DataViewerPresenter.Display, 
                                        DataViewChangedEvent.Handler,
                                        DataTable.Host
{
   @Inject
   public DataViewerPanel(Commands commands, EventBus events)
   {
      super(commands);
      events_ = events;
      events_.addHandler(DataViewChangedEvent.TYPE, this);
   }
   
   @Override 
   protected void initToolbar(Toolbar toolbar, Commands commands)
   {
      table_ = new DataTable(this);
      table_.initToolbar(toolbar);
   }
   
   @Override
   public void showData(DataItem item)
   {
      item_ = item;
      showUrl(item.getContentUrl());
   }
   
   @Override
   protected RStudioFrame createFrame(String url)
   {
      return new RStudioFrame(url);
   }

   @Override
   public RStudioFrame getDataTableFrame()
   {
      return getFrame();
   }

   @Override
   public void onDataViewChanged(DataViewChangedEvent event)
   {
      if (item_ != null && 
          event.getData().getCacheKey().equals(item_.getCacheKey()))
      {
         table_.refreshData(event.getData().structureChanged(), false);
      }
   } 

   private DataItem item_;
   private DataTable table_;
   private final EventBus events_;
}