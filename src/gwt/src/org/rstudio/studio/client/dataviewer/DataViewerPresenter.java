/*
 * DataViewerPresenter.java
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

import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.satellite.Satellite;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.views.source.model.DataItem;

import com.google.gwt.user.client.ui.IsWidget;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class DataViewerPresenter implements 
      IsWidget
      
{
   public interface Binder 
          extends CommandBinder<Commands, DataViewerPresenter>
   {}

   public interface Display extends IsWidget
   {
      void showData(DataItem item);
   }
   
   @Inject
   public DataViewerPresenter(Display view,
                              GlobalDisplay globalDisplay,
                              Binder binder,
                              final Commands commands,
                              EventBus eventBus,
                              Satellite satellite,
                              Session session)
   {
      view_ = view;
      satellite_ = satellite;
      events_ = eventBus;
      globalDisplay_ = globalDisplay;
      session_ = session;
      
      binder.bind(commands, this);  
   }     

   @Override
   public Widget asWidget()
   {
      return view_.asWidget();
   }
   
   public void showData(DataItem item)
   {
      view_.showData(item);
   }
   
   private final Display view_;
   private final Satellite satellite_;
   private final EventBus events_;
   private final GlobalDisplay globalDisplay_;
   private final Session session_;
}