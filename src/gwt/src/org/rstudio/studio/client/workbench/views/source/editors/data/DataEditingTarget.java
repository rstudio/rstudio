/*
 * DataEditingTarget.java
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
package org.rstudio.studio.client.workbench.views.source.editors.data;

import com.google.gwt.resources.client.ImageResource;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.Size;
import org.rstudio.core.client.widget.SimplePanelWithProgress;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.filetypes.FileIconResources;
import org.rstudio.studio.client.common.satellite.SatelliteManager;
import org.rstudio.studio.client.dataviewer.DataViewerSatellite;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.source.editors.urlcontent.UrlContentEditingTarget;
import org.rstudio.studio.client.workbench.views.source.events.DataViewChangedEvent;
import org.rstudio.studio.client.workbench.views.source.model.DataItem;
import org.rstudio.studio.client.workbench.views.source.model.SourceServerOperations;

import java.util.HashMap;

public class DataEditingTarget extends UrlContentEditingTarget
                               implements DataViewChangedEvent.Handler
{
   enum QueuedRefreshType
   {
      NoRefresh,
      DataRefresh,
      StructureRefresh
   }

   @Inject
   public DataEditingTarget(SourceServerOperations server,
                            Commands commands,
                            GlobalDisplay globalDisplay,
                            EventBus events,
                            SatelliteManager satelliteManager)
   {
      super(server, commands, globalDisplay, events);
      satelliteManager_ = satelliteManager;
      isActive_ = true;
      events.addHandler(DataViewChangedEvent.TYPE, this);
   }

   @Override
   protected Display createDisplay()
   {
      progressPanel_ = new SimplePanelWithProgress();
      progressPanel_.setSize("100%", "100%");
      reloadDisplay();
      return new Display()
      {
         public void print()
         {
            ((Display)progressPanel_.getWidget()).print();
         }

         public Widget asWidget()
         {
            return progressPanel_;
         }
      };
   }

   @Override
   public void onDataViewChanged(DataViewChangedEvent event)
   {
      if (event.getData().getCacheKey().equals(getDataItem().getCacheKey()))
      {
         // figure out what kind of refresh we need--if we already have a full
         // (structural) refresh queued, it trumps other refresh types
         if (queuedRefresh_ != QueuedRefreshType.StructureRefresh)
         {
            queuedRefresh_ = event.getData().structureChanged() ? 
                  QueuedRefreshType.StructureRefresh : 
                  QueuedRefreshType.DataRefresh;
         }

         // perform the refresh immediately if the tab is active; otherwise,
         // leave it in the queue and it'll be run when the tab is activated
         if (isActive_)
         {
            doQueuedRefresh(false);
         }
      }
   }

   @Override
   public void onActivate()
   {
      super.onActivate();
      if (view_ != null)
      {
         if (queuedRefresh_ != QueuedRefreshType.NoRefresh)
         {
            // the data change while the window wasn't active, so refresh it,
            // and recompute the size when finished
            doQueuedRefresh(true);
         }
         else
         {
            view_.onActivate();
         }
      }
   }

   @Override
   public void onDeactivate()
   {
      super.onDeactivate();
      view_.onDeactivate();
      isActive_ = false;
   }
   
   private void doQueuedRefresh(boolean onActivate)
   {
      if (queuedRefresh_ == QueuedRefreshType.DataRefresh)
         view_.refreshData(false, onActivate);
      else if (queuedRefresh_ == QueuedRefreshType.StructureRefresh)
         view_.refreshData(true, onActivate);
      queuedRefresh_ = QueuedRefreshType.NoRefresh;
   }

   private void clearDisplay()
   {
      progressPanel_.showProgress(1);
   }

   private void reloadDisplay()
   {
      view_ = new DataEditingTargetWidget(
            commands_,
            getDataItem());
      view_.setSize("100%", "100%");
      progressPanel_.setWidget(view_);
   }
   
   @Override
   public String getPath()
   {
      return getDataItem().getURI();
   }

   @Override
   public ImageResource getIcon()
   {
      return FileIconResources.INSTANCE.iconCsv();
   }

   private DataItem getDataItem()
   {
      return doc_.getProperties().cast();
   }

   @Override
   protected String getContentTitle()
   {
      return getDataItem().getCaption();
   }

   @Override
   protected String getContentUrl()
   {
      return getDataItem().getContentUrl();
   }

   @Override
   public void popoutDoc()
   {
      DataItem item = getDataItem();
      if (item.getCacheKey() != null && item.getCacheKey().length() > 0)
      {
         // if we have a cache key, duplicate it
         server_.duplicateDataView(item.getCaption(), item.getEnvironment(), 
                                   item.getObject(), item.getCacheKey(), 
               new ServerRequestCallback<DataItem>() {
                  @Override
                  public void onResponseReceived(DataItem item)
                  {
                     satelliteManager_.openSatellite(DataViewerSatellite.NAME, item, 
                                                     new Size(750, 850));
                  }

                  @Override
                  public void onError(ServerError error)
                  {
                     globalDisplay_.showErrorMessage("View Failed", 
                           error.getMessage());
                  }
         });
      }
      else
      {
         // no cache key, just show the content directly
         globalDisplay_.showHtmlFile(item.getContentUrl());
      }
   }

   protected String getCacheKey()
   {
      return getDataItem().getCacheKey();
   }

   public void updateData(final DataItem data)
   {
      final Widget originalWidget = progressPanel_.getWidget();

      clearDisplay();
      
      final String oldCacheKey = getCacheKey();

      HashMap<String, String> props = new HashMap<String, String>();
      data.fillProperties(props);
      server_.modifyDocumentProperties(
            doc_.getId(),
            props,
            new SimpleRequestCallback<Void>("Error")
            {
               @Override
               public void onResponseReceived(Void response)
               {
                  server_.removeCachedData(
                        oldCacheKey,
                        new ServerRequestCallback<Void>() {

                           @Override
                           public void onError(ServerError error)
                           {
                              Debug.logError(error);
                           }
                        });

                  data.fillProperties(doc_.getProperties());
                  reloadDisplay();
               }

               @Override
               public void onError(ServerError error)
               {
                  super.onError(error);
                  progressPanel_.setWidget(originalWidget);
               }
            });
   }

   @Override
   public void onDismiss()
   {
      server_.removeCachedData(getCacheKey(),
                               new ServerRequestCallback<org.rstudio.studio.client.server.Void>()
                               {
                                  @Override
                                  public void onError(ServerError error)
                                  {
                                     Debug.logError(error);
                                  }
                               });
   }

   private SimplePanelWithProgress progressPanel_;
   private DataEditingTargetWidget view_;
   private final SatelliteManager satelliteManager_;
   private boolean isActive_;
   private QueuedRefreshType queuedRefresh_;
}
