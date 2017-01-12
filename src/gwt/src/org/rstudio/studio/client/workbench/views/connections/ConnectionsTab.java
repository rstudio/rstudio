/*
 * ConnectionsTab.java
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

import com.google.inject.Inject;

import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.ReloadWithLastChanceSaveEvent;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.SessionInitEvent;
import org.rstudio.studio.client.workbench.events.SessionInitHandler;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.model.SessionInfo;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;
import org.rstudio.studio.client.workbench.ui.DelayLoadTabShim;
import org.rstudio.studio.client.workbench.ui.DelayLoadWorkbenchTab;
import org.rstudio.studio.client.workbench.views.connections.events.ActiveConnectionsChangedEvent;
import org.rstudio.studio.client.workbench.views.connections.events.ConnectionListChangedEvent;
import org.rstudio.studio.client.workbench.views.connections.events.ConnectionOpenedEvent;
import org.rstudio.studio.client.workbench.views.connections.events.ConnectionUpdatedEvent;
import org.rstudio.studio.client.workbench.views.connections.events.EnableConnectionsEvent;

public class ConnectionsTab extends DelayLoadWorkbenchTab<ConnectionsPresenter>
                            implements EnableConnectionsEvent.Handler
{
   public abstract static class Shim 
        extends DelayLoadTabShim<ConnectionsPresenter, ConnectionsTab>
        implements ConnectionUpdatedEvent.Handler,
                   ConnectionOpenedEvent.Handler,
                   ConnectionListChangedEvent.Handler,
                   ActiveConnectionsChangedEvent.Handler {
      
      @Handler
      public abstract void onNewConnection();
      
      public abstract void activate();
      
   }
   
   public interface Binder extends CommandBinder<Commands, ConnectionsTab.Shim> {}


   @Inject
   public ConnectionsTab(final Shim shim, 
                         Binder binder,
                         Commands commands,
                         EventBus eventBus,
                         Session session, 
                         UIPrefs uiPrefs)
   {
      super("Connections", shim);
      binder.bind(commands, shim);
      session_ = session;
      eventBus_ = eventBus;
      eventBus.addHandler(ConnectionUpdatedEvent.TYPE, shim);
      eventBus.addHandler(ConnectionOpenedEvent.TYPE, shim);
      eventBus.addHandler(ConnectionListChangedEvent.TYPE, shim);
      eventBus.addHandler(ActiveConnectionsChangedEvent.TYPE, shim);
      eventBus.addHandler(EnableConnectionsEvent.TYPE, this);
      
      eventBus.addHandler(SessionInitEvent.TYPE, new SessionInitHandler() {
         public void onSessionInit(SessionInitEvent sie)
         {
            SessionInfo sessionInfo = session_.getSessionInfo();
            if (sessionInfo.getConnectionsEnabled() && 
                sessionInfo.getActivateConnections())
            {
               shim.activate();
            }
         }
      });
   }
   
   @Override
   public boolean isSuppressed()
   {
      return !session_.getSessionInfo().getConnectionsEnabled();
   }
   
   @Override
   public void onEnableConnections(EnableConnectionsEvent event)
   {
      if (isSuppressed())
         eventBus_.fireEvent(new ReloadWithLastChanceSaveEvent());
   }
   
   private Session session_;
   private EventBus eventBus_;
}