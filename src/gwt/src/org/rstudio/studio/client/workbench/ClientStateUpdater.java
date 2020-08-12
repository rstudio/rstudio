/*
 * ClientStateUpdater.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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
package org.rstudio.studio.client.workbench;

import com.google.inject.Inject;

import org.rstudio.core.client.Barrier.Token;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.TimeBufferedCommand;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.events.*;
import org.rstudio.studio.client.workbench.model.ClientState;
import org.rstudio.studio.client.workbench.model.WorkbenchServerOperations;

public class ClientStateUpdater extends TimeBufferedCommand
{
   /**
    * "Client state" collectively refers to the set of stuff that the
    * client would like to remember for the next time we connect.
    *
    * Components that would like to participate in saving client state should
    * add a handler to SaveClientStateEvent on EventBus, and in
    * that handler, add values to the ClientState if they have changed.
    * Values that are unchanged since the last update need not be added,
    * as the values that are put in ClientState will be merged with previous
    * values.
    *
    * The SaveClientStateEvent fires either passively (on a timer) or actively
    * (on request [though in fact also on a timer, just a shorter one]). Any
    * component can request that SaveClientStateEvent be fired: simply fire
    * PushClientStateEvent on the EventBus.
    */
   @Inject
   public ClientStateUpdater(EventBus events,
                             WorkbenchServerOperations server)
   {
      super(INITIAL_INTERVAL_MILLIS, PASSIVE_INTERVAL_MILLIS, ACTIVE_INTERVAL_MILLIS);
      events_ = events;
      server_ = server;

      events_.addHandler(PushClientStateEvent.TYPE, pushClientStateEvent -> reschedule());

      events_.addHandler(LastChanceSaveEvent.TYPE, lastChanceSaveEvent ->
      {
         // We're quitting. Save client state one more time.
         barrierToken_ = lastChanceSaveEvent.acquire();
         try
         {
            nudge();
         }
         catch(Exception ex)
         {
            Debug.log("Exception on scheduling client state save for shutdown: ");
            Debug.logException(ex);
            if (barrierToken_ != null)
               barrierToken_.release();
         }
      });
   }

   public void pauseSendingUpdates()
   {
      pauseSendingUpdates_ = true;
   }

   public void resumeSendingUpdates()
   {
      pauseSendingUpdates_ = false;
   }

   @Override
   protected void performAction(final boolean shouldSchedulePassive)
   {
      ClientState state = ClientState.create();
      try
      {
         events_.fireEvent(new SaveClientStateEvent(state));
      }
      catch (Exception e)
      {
         onComplete(shouldSchedulePassive);
         return;
      }

      if (state.isEmpty())
      {
         onComplete(shouldSchedulePassive);
         return;
      }

      try
      {
         if (pauseSendingUpdates_)
         {
            onComplete(shouldSchedulePassive);
            return;
         }

         server_.updateClientState(
               state.getTemporaryData(),
               state.getPersistentData(),
               state.getProjectPersistentData(),
               new ServerRequestCallback<Void>() {
                  @Override
                  public void onError(ServerError error)
                  {
                     onComplete(shouldSchedulePassive);
                  }

                  @Override
                  public void onResponseReceived(Void response)
                  {
                     onComplete(shouldSchedulePassive);
                  }
               });
      }
      catch (Exception ex)
      {
         Debug.log("Exception updating client state: ");
         Debug.logException(ex);

         // complete (ensure barrier token is released)
         onComplete(shouldSchedulePassive);
      }
   }

   private void onComplete(boolean shouldSchedulePassive)
   {
      if (barrierToken_ != null)
         barrierToken_.release();

      if (shouldSchedulePassive)
         reschedule();
   }

   private static final int INITIAL_INTERVAL_MILLIS = 2000;
   private static final int PASSIVE_INTERVAL_MILLIS = 5000;
   private static final int ACTIVE_INTERVAL_MILLIS = Desktop.isDesktop()
                                                     ? 100
                                                     : 350;
   private final EventBus events_;
   private final WorkbenchServerOperations server_;
   private Token barrierToken_;
   private boolean pauseSendingUpdates_ = false;
}
