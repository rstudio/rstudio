/*
 * DataEditingTarget.java
 *
 * Copyright (C) 2009-20 by RStudio, PBC
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

import com.google.gwt.aria.client.Roles;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.widget.SimplePanelWithProgress;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.filetypes.FileIcon;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.source.editors.urlcontent.UrlContentEditingTarget;
import org.rstudio.studio.client.workbench.views.source.events.DataViewChangedEvent;
import org.rstudio.studio.client.workbench.views.source.events.PopoutDocEvent;
import org.rstudio.studio.client.workbench.views.source.model.DataItem;
import org.rstudio.studio.client.workbench.views.source.model.SourceServerOperations;

import java.util.HashMap;

public class DataEditingTarget extends UrlContentEditingTarget
                               implements DataViewChangedEvent.Handler
{
   enum QueuedRefreshType
   {
      NoRefresh,
      StructureRefresh
   }

   @Inject
   public DataEditingTarget(SourceServerOperations server,
                            Commands commands,
                            GlobalDisplay globalDisplay,
                            EventBus events)
   {
      super(server, commands, globalDisplay, events);
      events_ = events;
      isActive_ = true;
      events.addHandler(DataViewChangedEvent.TYPE, this);
   }

   @Override
   protected Display createDisplay()
   {
      progressPanel_ = new SimplePanelWithProgress();
      progressPanel_.setSize("100%", "100%");
      Roles.getTabpanelRole().set(progressPanel_.getElement());
      setAccessibleName(null);
      reloadDisplay();
      return new Display()
      {
         public void print()
         {
            ((Display)progressPanel_.getWidget()).print();
         }

         public void setAccessibleName(String accessibleName)
         {
            DataEditingTarget.this.setAccessibleName(accessibleName);
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
         queuedRefresh_ = QueuedRefreshType.StructureRefresh;
         // perform the refresh immediately if the tab is active; otherwise,
         // leave it in the queue and it'll be run when the tab is activated
         if (isActive_)
         {
            doQueuedRefresh();
         }
      }
   }

   private void setAccessibleName(String accessibleName)
   {
      if (StringUtil.isNullOrEmpty(accessibleName))
         accessibleName = "Untitled Data Browser";
      Roles.getTabpanelRole().setAriaLabelProperty(progressPanel_.getElement(), accessibleName + 
            " Data Browser");
   }

   @Override
   public void onActivate()
   {
      super.onActivate();
      if (view_ != null)
      {
         // the data change while the window wasn't active, so refresh it,
         if (queuedRefresh_ != QueuedRefreshType.NoRefresh)
            doQueuedRefresh();
         else
            view_.onActivate();
      }
   }

   @Override
   public void onDeactivate()
   {
      super.onDeactivate();
      view_.onDeactivate();
      isActive_ = false;
   }

   @Override
   public void onDismiss(int dismissType)
   {
      // explicitly avoid calling super method as we don't
      // have an associated content URL to clean up
   }
   
   private void doQueuedRefresh()
   {
      view_.refreshData();
      queuedRefresh_ = QueuedRefreshType.NoRefresh;
   }

   private void clearDisplay()
   {
      progressPanel_.showProgress(1);
   }

   private void reloadDisplay()
   {
      view_ = new DataEditingTargetWidget(
            "Data Browser",
            commands_,
            events_,
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
   public FileIcon getIcon()
   {
      return FileIcon.CSV_ICON;
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
      events_.fireEvent(new PopoutDocEvent(getId(), null));
   }

   @Override
   public String getCurrentStatus()
   {
      return "Data Browser displayed";
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

   private SimplePanelWithProgress progressPanel_;
   private DataEditingTargetWidget view_;
   private final EventBus events_;
   private boolean isActive_;
   private QueuedRefreshType queuedRefresh_;
}
