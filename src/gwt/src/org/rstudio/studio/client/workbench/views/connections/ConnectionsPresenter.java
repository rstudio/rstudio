/*
 * ConnectionsPresenter.java
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
package org.rstudio.studio.client.workbench.views.connections;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.inject.Inject;

import org.rstudio.core.client.ListUtil;
import org.rstudio.core.client.ListUtil.FilterPredicate;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.WorkbenchListManager;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.views.BasePresenter;
import org.rstudio.studio.client.workbench.views.connections.model.Connection;
import org.rstudio.studio.client.workbench.views.connections.model.ConnectionList;
import org.rstudio.studio.client.workbench.views.connections.model.ConnectionsServerOperations;

public class ConnectionsPresenter extends BasePresenter 
{
   public interface Display extends WorkbenchView
   {
      void setConnections(List<Connection> connections);
      
      HandlerRegistration addSearchFilterChangedHandler(
                                       ValueChangeHandler<String> handler);
   }
   
   @Inject
   public ConnectionsPresenter(Display display, 
                               ConnectionsServerOperations server,
                               EventBus eventBus,
                               WorkbenchListManager listManager)
   {
      super(display);
      display_ = display;
      server_ = server;
      connectionList_ = new ConnectionList(listManager.getConnectionsList());
      
      ArrayList<Connection> connections = new ArrayList<Connection>();
      
      connections.add(Connection.create("Spark", "localhost:4040"));
      connections.add(Connection.create("Spark", "localhost:4141"));
      connections.add(Connection.create("Spark", "localhost:4242"));
      
      updateConnections(connections);
      
      display_.addSearchFilterChangedHandler(new ValueChangeHandler<String>() {

         @Override
         public void onValueChange(ValueChangeEvent<String> event)
         {
            String query = event.getValue();
            final String[] splat = query.toLowerCase().split("\\s+");
            List<Connection> connections = ListUtil.filter(allConnections_, 
                                         new FilterPredicate<Connection>()
            {
               @Override
               public boolean test(Connection connection)
               {
                  for (String el : splat)
                  {
                     boolean match =
                         connection.getName().toLowerCase().contains(el);
                     if (!match)
                        return false;
                  }
                  return true;
               }
            });
            display_.setConnections(connections);
         }
      });
   }
   
   private void updateConnections(List<Connection> connections)
   {
      allConnections_ = connections;
      display_.setConnections(allConnections_);
   }
   
   
   private final Display display_ ;
   @SuppressWarnings("unused")
   private final ConnectionsServerOperations server_ ;
   
   @SuppressWarnings("unused")
   private ConnectionList connectionList_;

   private List<Connection> allConnections_;
}