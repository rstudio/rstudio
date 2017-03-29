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

import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.inject.Inject;

import org.rstudio.core.client.ListUtil;
import org.rstudio.core.client.ListUtil.FilterPredicate;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.studio.client.application.ApplicationInterrupt;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.DelayedProgressRequestCallback;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.GlobalProgressDelayer;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.WorkbenchListManager;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.model.ClientState;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.model.helper.JSObjectStateValue;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.views.BasePresenter;
import org.rstudio.studio.client.workbench.views.connections.events.ActiveConnectionsChangedEvent;
import org.rstudio.studio.client.workbench.views.connections.events.ConnectionListChangedEvent;
import org.rstudio.studio.client.workbench.views.connections.events.ConnectionOpenedEvent;
import org.rstudio.studio.client.workbench.views.connections.events.ConnectionUpdatedEvent;
import org.rstudio.studio.client.workbench.views.connections.events.ExecuteConnectionActionEvent;
import org.rstudio.studio.client.workbench.views.connections.events.ExploreConnectionEvent;
import org.rstudio.studio.client.workbench.views.connections.events.PerformConnectionEvent;
import org.rstudio.studio.client.workbench.views.connections.events.ViewConnectionDatasetEvent;
import org.rstudio.studio.client.workbench.views.connections.model.Connection;
import org.rstudio.studio.client.workbench.views.connections.model.ConnectionId;
import org.rstudio.studio.client.workbench.views.connections.model.ConnectionOptions;
import org.rstudio.studio.client.workbench.views.connections.model.ConnectionsServerOperations;
import org.rstudio.studio.client.workbench.views.connections.model.NewConnectionContext;
import org.rstudio.studio.client.workbench.views.connections.ui.NewConnectionWizard;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;
import org.rstudio.studio.client.workbench.views.source.events.NewDocumentWithCodeEvent;
import org.rstudio.studio.client.workbench.views.source.model.SourcePosition;

public class ConnectionsPresenter extends BasePresenter  
                                  implements PerformConnectionEvent.Handler,
                                             ViewConnectionDatasetEvent.Handler
{
   public interface Display extends WorkbenchView
   {
      void showConnectionsList(boolean animate);
      
      void setConnections(List<Connection> connections);
      void setActiveConnections(List<ConnectionId> connections);
           
      String getSearchFilter();
      
      HandlerRegistration addSearchFilterChangeHandler(
                                       ValueChangeHandler<String> handler);
      
      HandlerRegistration addExploreConnectionHandler(
                                       ExploreConnectionEvent.Handler handler); 
      
      HandlerRegistration addExecuteConnectionActionHandler(
            ExecuteConnectionActionEvent.Handler handler);
      
      void showConnectionExplorer(Connection connection, String connectVia);
      void setExploredConnection(Connection connection);
      
      void updateExploredConnection(String hint);
      
      HasClickHandlers backToConnectionsButton();
      
      String getConnectVia();
      String getConnectCode();
      
      void showConnectionProgress();
   }
   
   public interface Binder extends CommandBinder<Commands, ConnectionsPresenter> {}
   
   @Inject
   public ConnectionsPresenter(Display display, 
                               ConnectionsServerOperations server,
                               GlobalDisplay globalDisplay,
                               EventBus eventBus,
                               UIPrefs uiPrefs,
                               Binder binder,
                               final Commands commands,
                               WorkbenchListManager listManager,
                               Session session,
                               ApplicationInterrupt applicationInterrupt)
   {
      super(display);
      binder.bind(commands, this);
      display_ = display;
      commands_ = commands;
      server_ = server;
      uiPrefs_ = uiPrefs;
      globalDisplay_ = globalDisplay;
      eventBus_ = eventBus;
      applicationInterrupt_ = applicationInterrupt;
         
      // search filter
      display_.addSearchFilterChangeHandler(new ValueChangeHandler<String>() {

         @Override
         public void onValueChange(ValueChangeEvent<String> event)
         {
            display_.setConnections(filteredConnections());
         }
      });
      
      display_.addExploreConnectionHandler(new ExploreConnectionEvent.Handler()
      {   
         @Override
         public void onExploreConnection(ExploreConnectionEvent event)
         {
            exploreConnection(event.getConnection());
         }
      });
      
      display_.backToConnectionsButton().addClickHandler(new ClickHandler() {

         @Override
         public void onClick(ClickEvent event)
         {
            showAllConnections(true);
         }
      });
      
      
      display_.addExecuteConnectionActionHandler(
            new ExecuteConnectionActionEvent.Handler()
      {
         
         @Override
         public void onExecuteConnectionAction(
               ExecuteConnectionActionEvent event)
         {
            server_.connectionExecuteAction(event.getConnectionId(), 
                  event.getAction(), new VoidServerRequestCallback());
         }
      });
      
      // events
      eventBus_.addHandler(PerformConnectionEvent.TYPE, this);
      eventBus_.addHandler(ViewConnectionDatasetEvent.TYPE, this);
      
      // set connections      
      final SessionInfo sessionInfo = session.getSessionInfo();
      updateConnections(sessionInfo.getConnectionList());  
      updateActiveConnections(sessionInfo.getActiveConnections());
           
      // make the explored connection persistent
      new JSObjectStateValue(MODULE_CONNECTIONS, 
                             KEY_EXPLORED_CONNECTION, 
                             ClientState.PERSISTENT, 
                             session.getSessionInfo().getClientState(), 
                             false)
      {
         @Override
         protected void onInit(JsObject value)
         {
            // get the value
            if (value != null)
               exploredConnection_ = value.cast();
            else
               exploredConnection_ = null;
                 
            lastExploredConnection_ = exploredConnection_;
            
            // if there is an an explored connection then explore it
            // (but delay to allow for the panel to be laid out)
            if (exploredConnection_ != null)
               exploreConnection(exploredConnection_);
         }

         @Override
         protected JsObject getValue()
         {
            if (exploredConnection_ != null)
               return exploredConnection_.cast();
            else
               return null;
         }

         @Override
         protected boolean hasChanged()
         {
            if (lastExploredConnection_ != exploredConnection_)
            {
               lastExploredConnection_ = exploredConnection_;
               return true;
            }
            else
            {
               return false;
            }
         }
      };
   }
   
   public void activate()
   {
      display_.bringToFront();
   }
   
   public void onConnectionOpened(ConnectionOpenedEvent event)
   {
      if (exploredConnection_ == null || 
          !exploredConnection_.getId().equalTo(event.getConnection().getId()))
      {
         exploreConnection(event.getConnection());
      }
      activate();
   }
   
   public void onConnectionUpdated(ConnectionUpdatedEvent event)
   {  
      if (exploredConnection_ == null)
         return;
      
      if (!exploredConnection_.getId().equalTo(event.getConnectionId()))
         return;
      
      display_.updateExploredConnection(event.getHint());
   }
   
   public void onConnectionListChanged(ConnectionListChangedEvent event)
   {
      updateConnections(event.getConnectionList());
   }
   
   public void onActiveConnectionsChanged(ActiveConnectionsChangedEvent event)
   {
      updateActiveConnections(event.getActiveConnections());
   }

   private void showError(String errorMessage)
   {
      globalDisplay_.showErrorMessage("Error", errorMessage);
   }
   
   public void onNewConnection()
   {
      // if r session bussy, fail
      if (commands_.interruptR().isEnabled()) {
        showError(
          "The R session is currently busy. Wait for completion or " +
          "interrupt the current session and retry.");
        return;
      }

      // get the context
      server_.getNewConnectionContext(
         new DelayedProgressRequestCallback<NewConnectionContext>(
                                                   "New Connection...") {
   
            @Override
            protected void onSuccess(final NewConnectionContext context)
            {
                // show dialog
               NewConnectionWizard newConnectionWizard = new NewConnectionWizard(
                 context,
                 new ProgressOperationWithInput<ConnectionOptions>() {
                    @Override
                    public void execute(ConnectionOptions result,
                                        ProgressIndicator indicator)
                    {
                       indicator.onCompleted();

                       eventBus_.fireEvent(new PerformConnectionEvent(
                          result.getConnectVia(),
                          result.getConnectCode())
                       );
                    }
                 }
               );
               
               newConnectionWizard.showModal();
            }
         });      
   }
   
   @Override
   public void onPerformConnection(PerformConnectionEvent event)
   {
      String connectVia = event.getConnectVia();
      String connectCode = event.getConnectCode();
     
      if (connectVia.equals(
            ConnectionOptions.CONNECT_COPY_TO_CLIPBOARD))
      {
         DomUtils.copyCodeToClipboard(connectCode);
      }
      else if (connectVia.equals(ConnectionOptions.CONNECT_R_CONSOLE))
      {
         eventBus_.fireEvent(
               new SendToConsoleEvent(connectCode, true));
         
         display_.showConnectionProgress();
      }
      else if (connectVia.equals(ConnectionOptions.CONNECT_NEW_R_SCRIPT) ||
               connectVia.equals(ConnectionOptions.CONNECT_NEW_R_NOTEBOOK))
      {
         String type;
         String code = connectCode;
         SourcePosition cursorPosition = null;
         if (connectVia.equals(ConnectionOptions.CONNECT_NEW_R_SCRIPT))
         {
            type = NewDocumentWithCodeEvent.R_SCRIPT;
            code = code + "\n\n";
         }
         else
         {
            type = NewDocumentWithCodeEvent.R_NOTEBOOK; 
            int codeLength = code.split("\n").length;
            code = "---\n" +
                   "title: \"R Notebook\"\n" +
                   "output: html_notebook\n" +
                   "---\n" +
                   "\n" +
                   "```{r setup, include=FALSE}\n" +
                   code + "\n" +
                   "```\n" +
                   "\n" +
                   "```{r}\n" +
                   "\n" +
                   "```\n";
            cursorPosition = SourcePosition.create(9 + codeLength, 0);      
         }
        
         eventBus_.fireEvent(
            new NewDocumentWithCodeEvent(type, code, cursorPosition, true));
         
         display_.showConnectionProgress();
      }
   }
   
   @Override
   public void onViewConnectionDataset(ViewConnectionDatasetEvent event)
   {
      if (exploredConnection_ == null)
         return;
      
      GlobalProgressDelayer progress = new GlobalProgressDelayer(
                              globalDisplay_, 100, "Previewing table...");
      
      server_.connectionPreviewObject(
         exploredConnection_.getId(), 
         event.getDataset().createSpecifier(),
         new VoidServerRequestCallback(progress.getIndicator())); 
   }
   
   @Handler
   public void onRemoveConnection()
   {
      if (exploredConnection_ == null)
         return;
      
      globalDisplay_.showYesNoMessage(
         MessageDialog.QUESTION,
         "Remove Connection",
         "Are you sure you want to remove this connection from the connection history?",
         new Operation() {

            @Override
            public void execute()
            {
               server_.removeConnection(
                 exploredConnection_.getId(), new VoidServerRequestCallback()); 
               disconnectConnection(false);
               showAllConnections(true);
            }
         },
         true);
     
   }
   
   @Handler
   public void onDisconnectConnection()
   {
      disconnectConnection(true);
   }
  
   private void disconnectConnection(boolean prompt)
   {
      if (exploredConnection_ == null)
         return;
      
      // define connect operation
      final Operation connectOperation = new Operation() {
         @Override
         public void execute()
         {
            server_.connectionDisconnect(exploredConnection_.getId(), 
                  new VoidServerRequestCallback());
         }  
      };
      
      if (prompt)
      {
         StringBuilder builder = new StringBuilder();
         builder.append("Are you sure you want to disconnect?");
         globalDisplay_.showYesNoMessage(
               MessageDialog.QUESTION,
               "Disconnect",
               builder.toString(),
               connectOperation,
               true);  
      }
      else
      {
         connectOperation.execute();
      }
   }
   
   
   @Handler
   public void onRefreshConnection()
   {
      if (exploredConnection_ == null)
         return;
      
      display_.updateExploredConnection("");
   }
   
   private void showAllConnections(boolean animate)
   {
      exploredConnection_ = null;
      display_.showConnectionsList(animate);
   }
   
   private void updateConnections(JsArray<Connection> connections)
   {
      // update all connections
      allConnections_.clear();
      for (int i = 0; i<connections.length(); i++)
         allConnections_.add(connections.get(i)); 
      
      // set filtered connections
      display_.setConnections(filteredConnections());
      
      // update explored connection
      if (exploredConnection_ != null)
      {
         for (int i = 0; i<connections.length(); i++)
         {
            if (connections.get(i).getId().equals(exploredConnection_.getId()))
            {
               exploredConnection_ = connections.get(i);
               display_.setExploredConnection(exploredConnection_);
               break;
            }
         }
      }
   }
   
   private void updateActiveConnections(JsArray<ConnectionId> connections)
   {
      activeConnections_.clear();
      for (int i = 0; i<connections.length(); i++)
         activeConnections_.add(connections.get(i));  
      display_.setActiveConnections(activeConnections_);
      manageUI();
   }
   
   private void exploreConnection(Connection connection)
   {
      exploredConnection_ = connection;
      display_.showConnectionExplorer(connection, uiPrefs_.connectionsConnectVia().getValue());
      manageUI();
   }
   
   private void manageUI()
   {
      if (exploredConnection_ != null)
      {
         boolean connected = isConnected(exploredConnection_.getId());
         commands_.removeConnection().setVisible(!connected);
         commands_.disconnectConnection().setVisible(connected);
         // TODO: show connection actions
         commands_.refreshConnection().setVisible(connected);
      }
      else
      {
         commands_.removeConnection().setVisible(false);
         commands_.disconnectConnection().setVisible(false);
         // TODO: hide connection actions
         commands_.refreshConnection().setVisible(false);
      }
   }
   
   private boolean isConnected(ConnectionId id)
   {
      for (int i=0; i<activeConnections_.size(); i++)
         if (activeConnections_.get(i).equalTo(id))
            return true;
      return false;
   }
   
   private List<Connection> filteredConnections()
   {
      String query = display_.getSearchFilter();
      final String[] splat = query.toLowerCase().split("\\s+");
      return ListUtil.filter(allConnections_, 
                                   new FilterPredicate<Connection>()
      {
         @Override
         public boolean test(Connection connection)
         {
            for (String el : splat)
            {
               boolean match =
                   connection.getHost().toLowerCase().contains(el);
               if (!match)
                  return false;
            }
            return true;
         }
      });
   }
   
   private final GlobalDisplay globalDisplay_;
   
   private final Display display_ ;
   private final EventBus eventBus_;
   private final Commands commands_;
   private UIPrefs uiPrefs_;
   private final ConnectionsServerOperations server_ ;
   @SuppressWarnings("unused") private final ApplicationInterrupt applicationInterrupt_;
   
   // client state
   public static final String MODULE_CONNECTIONS = "connections-pane";
   private static final String KEY_EXPLORED_CONNECTION = "exploredConnections";
   private Connection exploredConnection_;
   private Connection lastExploredConnection_;
   
   private ArrayList<Connection> allConnections_ = new ArrayList<Connection>();
   private ArrayList<ConnectionId> activeConnections_ = new ArrayList<ConnectionId>();
   
}