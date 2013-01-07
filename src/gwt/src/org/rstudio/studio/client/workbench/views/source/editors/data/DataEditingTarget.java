/*
 * DataEditingTarget.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
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
import org.rstudio.core.client.widget.SimplePanelWithProgress;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.filetypes.FileIconResources;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.source.editors.urlcontent.UrlContentEditingTarget;
import org.rstudio.studio.client.workbench.views.source.model.DataItem;
import org.rstudio.studio.client.workbench.views.source.model.SourceServerOperations;

import java.util.HashMap;

public class DataEditingTarget extends UrlContentEditingTarget
{
   @Inject
   public DataEditingTarget(SourceServerOperations server,
                            Commands commands,
                            GlobalDisplay globalDisplay,
                            EventBus events)
   {
      super(server, commands, globalDisplay, events);
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

   private void clearDisplay()
   {
      progressPanel_.showProgress(1);
   }

   private void reloadDisplay()
   {
      DataEditingTargetWidget view = new DataEditingTargetWidget(
            commands_,
            getDataItem());
      view.setSize("100%", "100%");
      progressPanel_.setWidget(view);
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

   public void updateData(final DataItem data)
   {
      final Widget originalWidget = progressPanel_.getWidget();

      clearDisplay();
      
      final String oldContentUrl = getContentUrl();

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
                  server_.removeContentUrl(
                        oldContentUrl,
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
}
