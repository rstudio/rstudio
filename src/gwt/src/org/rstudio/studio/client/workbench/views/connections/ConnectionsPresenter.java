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
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;

import org.rstudio.core.client.ListUtil;
import org.rstudio.core.client.ListUtil.FilterPredicate;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.js.JsObject;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.DelayedProgressRequestCallback;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.SimpleRequestCallback;
import org.rstudio.studio.client.common.console.ConsoleProcess;
import org.rstudio.studio.client.common.console.ProcessExitEvent;
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
import org.rstudio.studio.client.workbench.views.connections.events.ConnectionUpdatedEvent;
import org.rstudio.studio.client.workbench.views.connections.events.ExploreConnectionEvent;
import org.rstudio.studio.client.workbench.views.connections.model.Connection;
import org.rstudio.studio.client.workbench.views.connections.model.ConnectionId;
import org.rstudio.studio.client.workbench.views.connections.model.ConnectionOptions;
import org.rstudio.studio.client.workbench.views.connections.model.ConnectionsServerOperations;
import org.rstudio.studio.client.workbench.views.connections.model.NewSparkConnectionContext;
import org.rstudio.studio.client.workbench.views.connections.model.SparkVersion;
import org.rstudio.studio.client.workbench.views.connections.ui.InstallInfoPanel;
import org.rstudio.studio.client.workbench.views.connections.ui.ComponentsNotInstalledDialogs;
import org.rstudio.studio.client.workbench.views.connections.ui.NewSparkConnectionDialog;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;
import org.rstudio.studio.client.workbench.views.source.events.NewDocumentWithCodeEvent;
import org.rstudio.studio.client.workbench.views.source.model.SourcePosition;
import org.rstudio.studio.client.workbench.views.vcs.common.ConsoleProgressDialog;

public class ConnectionsPresenter extends BasePresenter 
{
   public interface Display extends WorkbenchView
   {
      void showConnectionsList();
      
      void setConnections(List<Connection> connections);
      void setActiveConnections(List<ConnectionId> connections);
      
      boolean isConnected(ConnectionId id);
           
      String getSearchFilter();
      
      HandlerRegistration addSearchFilterChangeHandler(
                                       ValueChangeHandler<String> handler);
      
      HandlerRegistration addExploreConnectionHandler(
                                       ExploreConnectionEvent.Handler handler); 
      
      void showConnectionExplorer(Connection connection, String connectVia);
      
      HasClickHandlers backToConnectionsButton();
      
      String getConnectVia();
      
      void addToConnectionExplorer(String item);
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
                               Session session)
   {
      super(display);
      binder.bind(commands, this);
      display_ = display;
      commands_ = commands;
      server_ = server;
      uiPrefs_ = uiPrefs;
      globalDisplay_ = globalDisplay;
      eventBus_ = eventBus;
         
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
            //display_.ensureHeight(EnsureHeightEvent.MAXIMIZED);
         }
      });
      
      display_.backToConnectionsButton().addClickHandler(new ClickHandler() {

         @Override
         public void onClick(ClickEvent event)
         {
            showAllConnections();
         }
         
      });
      
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
   
   public void onConnectionUpdated(ConnectionUpdatedEvent event)
   {     
      display_.addToConnectionExplorer(event.getHint());
   }
   
   public void onConnectionListChanged(ConnectionListChangedEvent event)
   {
      updateConnections(event.getConnectionList());
   }
   
   public void onActiveConnectionsChanged(ActiveConnectionsChangedEvent event)
   {
      updateActiveConnections(event.getActiveConnections());
   }
   
   public void onNewConnection()
   {
      // get the context
      server_.getNewSparkConnectionContext(
         new DelayedProgressRequestCallback<NewSparkConnectionContext>(
                                                   "New Connection...") {
   
            @Override
            protected void onSuccess(NewSparkConnectionContext context)
            {
               // prompt for no java installed
               if (!context.isJavaInstalled())
               {
                  ComponentsNotInstalledDialogs.showJavaNotInstalled(context.getJavaInstallUrl());
               }
               
               // prompt for no spark installed
               else if (context.getSparkVersions().length() == 0)
               {
                  ComponentsNotInstalledDialogs.showSparkNotInstalled();
               }
               
               // otherwise proceed with connecting
               else
               {
                  // show dialog
                  new NewSparkConnectionDialog(
                   context,
                   new OperationWithInput<ConnectionOptions>() {
                     @Override
                     public void execute(final ConnectionOptions result)
                     {
                        withRequiredSparkInstallation(
                              result.getSparkVersion(),
                              result.getRemote(),
                              new Command() {
                                 @Override
                                 public void execute()
                                 {
                                    performConnection(result.getConnectVia(),
                                                      result.getConnectCode());
                                 }
                                 
                              });
                     }
                  }).showModal();
               }
            }
         });      
   }
   
   
   private void withRequiredSparkInstallation(final SparkVersion sparkVersion,
                                              boolean remote,
                                              final Command command)
   {
      if (!sparkVersion.isInstalled())
      {
         globalDisplay_.showYesNoMessage(
            MessageDialog.QUESTION, 
            "Install Spark Components",
            InstallInfoPanel.getInfoText(sparkVersion, remote, true),
            false,
            new Operation() {  public void execute() {
               server_.installSpark(
                 sparkVersion.getSparkVersionNumber(),
                 sparkVersion.getHadoopVersionNumber(),
                 new SimpleRequestCallback<ConsoleProcess>(){

                    @Override
                    public void onResponseReceived(ConsoleProcess process)
                    {
                       final ConsoleProgressDialog dialog = 
                             new ConsoleProgressDialog(process, server_);
                       dialog.showModal();
           
                       process.addProcessExitHandler(
                          new ProcessExitEvent.Handler()
                          {
                             @Override
                             public void onProcessExit(ProcessExitEvent event)
                             {
                                if (event.getExitCode() == 0)
                                {
                                   dialog.hide();
                                   command.execute();
                                } 
                             }
                          }); 
                    }

               });   
            }},
            null,
            null,
            "Install",
            "Cancel",
            true);
      }
      else
      {
         command.execute();
      }
   }
   
  
   
   private void performConnection(String connectVia, String connectCode)
   {
      if (connectVia.equals(
            ConnectionOptions.CONNECT_COPY_TO_CLIPBOARD))
      {
         DomUtils.copyCodeToClipboard(connectCode);
      }
      else if (connectVia.equals(ConnectionOptions.CONNECT_R_CONSOLE))
      {
         eventBus_.fireEvent(
               new SendToConsoleEvent(connectCode, true));
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
            code = "---\n" +
                   "title: \"R Notebook\"\n" +
                   "output: html_notebook\n" +
                   "---\n" +
                   "\n" +
                   "```{r connect}\n" +
                   code + "\n" +
                   "```\n" +
                   "\n" +
                   "```{r}\n" +
                   "\n" +
                   "```\n";
            cursorPosition = SourcePosition.create(11, 0);      
         }
        
         eventBus_.fireEvent(
            new NewDocumentWithCodeEvent(type, code, cursorPosition, true));
      }
   }
   
   @Handler
   public void onRemoveConnection()
   {
      if (exploredConnection_ != null)
      {
         globalDisplay_.showYesNoMessage(
            MessageDialog.QUESTION,
            "Remove Connection",
            "Are you sure you want to remove the selected connection?",
            new Operation() {
   
               @Override
               public void execute()
               {
                  server_.removeConnection(
                    exploredConnection_.getId(), new VoidServerRequestCallback());   
                  showAllConnections();
               }
            },
            true);
      }
      else
      {
         globalDisplay_.showErrorMessage(
           "Remove Connection", "No connection currently selected.");
      }
   }
  
   @Handler
   public void onConnectConnection()
   {
      if (exploredConnection_ != null)
      {
         String connectVia = display_.getConnectVia();
         String connectCode = exploredConnection_.getConnectCode();
         performConnection(connectVia, connectCode);
         
         uiPrefs_.connectionsConnectVia().setGlobalValue(connectVia);
         uiPrefs_.writeUIPrefs();
      }
   }
   
   @Handler
   public void onDisconnectConnection()
   {
      if (exploredConnection_ != null)
      {
         server_.getDisconnectCode(exploredConnection_, 
                                   new SimpleRequestCallback<String>() {
            @Override
            public void onResponseReceived(String disconnectCode)
            {
               eventBus_.fireEvent(new SendToConsoleEvent(disconnectCode, true));
            }
         });
      }
   }
   
   @Handler
   public void onSparkLog()
   {
      if (exploredConnection_ != null)
      {
         server_.showSparkLog(exploredConnection_, 
                              new VoidServerRequestCallback());
      }
   }
   
   @Handler
   public void onSparkUI()
   {
      if (exploredConnection_ != null)
      {
         server_.showSparkUI(exploredConnection_, 
                             new VoidServerRequestCallback());
      }
   }
   
   
   private void showAllConnections()
   {
      exploredConnection_ = null;
      display_.showConnectionsList();
      //display_.ensureHeight(EnsureHeightEvent.NORMAL);
   }
   
   private void updateConnections(JsArray<Connection> connections)
   {
      // update all connections
      allConnections_.clear();
      for (int i = 0; i<connections.length(); i++)
         allConnections_.add(connections.get(i)); 
      
      // set filtered connections
      display_.setConnections(filteredConnections());
   }
   
   private void updateActiveConnections(JsArray<ConnectionId> connections)
   {
      activeConnections_.clear();
      for (int i = 0; i<connections.length(); i++)
         activeConnections_.add(connections.get(i));  
      display_.setActiveConnections(activeConnections_);
      manageCommands();
   }
   
   private void exploreConnection(Connection connection)
   {
      exploredConnection_ = connection;
      display_.showConnectionExplorer(connection, uiPrefs_.connectionsConnectVia().getValue());
      manageCommands();
   }
   
   private void manageCommands()
   {
      if (exploredConnection_ != null)
      {
         boolean connected = display_.isConnected(exploredConnection_.getId());
         commands_.connectConnection().setVisible(!connected);
         commands_.disconnectConnection().setVisible(connected);
         commands_.sparkLog().setVisible(connected);
         commands_.sparkUI().setVisible(connected);
      }
      else
      {
         commands_.disconnectConnection().setVisible(false);
         commands_.connectConnection().setVisible(false);
         commands_.sparkLog().setVisible(false);
         commands_.sparkUI().setVisible(false);
      }
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
   
   // client state
   public static final String MODULE_CONNECTIONS = "connections-pane";
   private static final String KEY_EXPLORED_CONNECTION = "exploredConnection";
   private Connection exploredConnection_;
   private Connection lastExploredConnection_;
   
   private ArrayList<Connection> allConnections_ = new ArrayList<Connection>();
   private ArrayList<ConnectionId> activeConnections_ = new ArrayList<ConnectionId>();
}