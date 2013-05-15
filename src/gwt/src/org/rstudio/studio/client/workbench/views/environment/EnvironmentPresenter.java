/*
 * EnvironmentPresenter.java
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

/* Some examples:
 * 
 * Adding an RPC method
 * https://github.com/rstudio/rstudio/commit/6815944da3e140aa1064a0c9866db7a70731c9d0
 * 
 * Presenter -> Model/View 
 * https://github.com/rstudio/rstudio/commit/0b7ef94ec6d9ad8c0a9385d2d1a8b43edf280f52
 * 
 * Adding a Command
 * https://github.com/rstudio/rstudio/commit/a5eee4b211dc09eac2221a9f825cfcfc3221f144
 * 
 * Raising Events from the Server
 * https://github.com/rstudio/rstudio/commit/6178166bf1c97a338986a85e5694f7278c0bc940
 * 
 */

package org.rstudio.studio.client.workbench.views.environment;

import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.BasePresenter;
import org.rstudio.studio.client.workbench.views.environment.events.ContextDepthChangedEvent;
import org.rstudio.studio.client.workbench.views.environment.events.EnvironmentObjectAssignedEvent;
import org.rstudio.studio.client.workbench.views.environment.events.EnvironmentObjectRemovedEvent;
import org.rstudio.studio.client.workbench.views.environment.events.EnvironmentRefreshEvent;
import org.rstudio.studio.client.workbench.views.environment.model.EnvironmentServerOperations;
import org.rstudio.studio.client.workbench.views.environment.model.EnvironmentState;
import org.rstudio.studio.client.workbench.views.environment.model.RObject;


import com.google.gwt.core.client.JsArray;
import com.google.inject.Inject;

public class EnvironmentPresenter extends BasePresenter
{
   public interface Binder extends CommandBinder<Commands, EnvironmentPresenter> {}
   
   public interface Display extends WorkbenchView
   {
      void addObject(RObject object);
      void clearObjects();
      void setContextDepth(int contextDepth);
      void removeObject(String object);
      void setEnvironmentName(String name);
   }
   
   @Inject
   public EnvironmentPresenter(Display view,
                               EnvironmentServerOperations server,
                               Binder binder,
                               Commands commands,
                               GlobalDisplay globalDisplay,
                               EventBus eventBus)
   {
      super(view);
      binder.bind(commands, this);
      
      view_ = view;
      server_ = server;
      globalDisplay_ = globalDisplay;
      
      refreshView();
      
      eventBus.addHandler(EnvironmentRefreshEvent.TYPE, 
                          new EnvironmentRefreshEvent.Handler()
      {
         @Override
         public void onEnvironmentRefresh(EnvironmentRefreshEvent event)
         {
            refreshView();
         }
      });
      
      eventBus.addHandler(ContextDepthChangedEvent.TYPE, 
                          new ContextDepthChangedEvent.Handler()
      {
         @Override
         public void onContextDepthChanged(ContextDepthChangedEvent event)
         {
            contextDepth_ = event.getContextDepth();
            view_.setContextDepth(contextDepth_);
            view_.setEnvironmentName(event.getFunctionName());
            setViewFromEnvironmentList(event.getEnvironmentList());
         }
      });
      
      eventBus.addHandler(EnvironmentObjectAssignedEvent.TYPE,
                          new EnvironmentObjectAssignedEvent.Handler() 
      {
         @Override
         public void onEnvironmentObjectAssigned(EnvironmentObjectAssignedEvent event)
         {
            view_.addObject(event.getObjectInfo());
         }
      });

      eventBus.addHandler(EnvironmentObjectRemovedEvent.TYPE,
            new EnvironmentObjectRemovedEvent.Handler() 
      {
         @Override
         public void onEnvironmentObjectRemoved(EnvironmentObjectRemovedEvent event)
         {
            view_.removeObject(event.getObjectName());
         }
      });
   }
   
   @Handler
   void onRefreshEnvironment()
   {
      refreshView();
   }
   
   public void initialize(EnvironmentState environmentState)
   {
      environmentState_ = environmentState;
      setContextDepth(environmentState_.contextDepth());
   }
   
   public void setContextDepth(int contextDepth)
   {
      contextDepth_ = contextDepth;
      view_.setContextDepth(contextDepth_);
   }
   
   private void setViewFromEnvironmentList(JsArray<RObject> objects)
   {
      view_.clearObjects();
      for (int i = 0; i<objects.length(); i++)
         view_.addObject(objects.get(i));
   }
    
   private void refreshView()
   {
      server_.listEnvironment(new ServerRequestCallback<JsArray<RObject>>() {

         @Override
         public void onResponseReceived(JsArray<RObject> objects)
         {
            setViewFromEnvironmentList(objects);
         }
         
         @Override
         public void onError(ServerError error)
         {
            globalDisplay_.showErrorMessage("Error Listing Objects", 
                                            error.getUserMessage());
         }
      });
   }
   
   private final Display view_;
   private final EnvironmentServerOperations server_;
   private final GlobalDisplay globalDisplay_;
   private EnvironmentState environmentState_;
   private int contextDepth_;
}
