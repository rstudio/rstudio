/*
 * TerminalWebSocket.java
 *
 * Copyright (C) 2026 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.terminal;

import java.util.function.Function;

import com.google.gwt.user.client.Timer;
import com.sksamuel.gwt.websockets.CloseEvent;
import com.sksamuel.gwt.websockets.Websocket;
import com.sksamuel.gwt.websockets.WebsocketListenerExt;

/** Owns a terminal WebSocket and the timers and callbacks for that connection. */
class TerminalWebSocket
{
   interface Scheduler
   {
      Runnable schedule(int delayMs, Runnable command);
   }

   TerminalWebSocket(WebsocketListenerExt listener, int pingInterval, int connectTimeout)
   {
      this(listener, pingInterval, connectTimeout, Websocket::new, (delay, command) ->
      {
         Timer timer = new Timer()
         {
            @Override
            public void run()
            {
               command.run();
            }
         };
         timer.schedule(delay);
         return timer::cancel;
      });
   }

   TerminalWebSocket(WebsocketListenerExt listener, int pingInterval, int connectTimeout,
                     Function<String, Websocket> factory, Scheduler scheduler)
   {
      listener_ = listener;
      pingIntervalMs_ = pingInterval * 1000;
      connectTimeoutMs_ = connectTimeout * 1000;
      factory_ = factory;
      scheduler_ = scheduler;
   }

   void open(String url)
   {
      close();
      final Websocket socket = factory_.apply(url);
      socket_ = socket;
      socket.addListener(new WebsocketListenerExt()
      {
         @Override
         public void onOpen()
         {
            if (socket_ != socket || opened_)
               return;
            opened_ = true;
            cancelConnect_.run();
            scheduleKeepAlive(socket);
            listener_.onOpen();
         }

         @Override
         public void onMessage(String message)
         {
            if (socket_ != socket)
               return;
            if (TerminalSocketPacket.keepAlivePacket().equals(message))
               unansweredPings_ = 0;
            else
               listener_.onMessage(message);
         }

         @Override
         public void onClose(CloseEvent event)
         {
            if (socket_ != socket)
               return;
            // Invalidate before notifying the session, which may reconnect.
            close();
            listener_.onClose(event);
         }

         @Override
         public void onError()
         {
            fail(socket);
         }
      });

      if (connectTimeoutMs_ > 0)
         cancelConnect_ = scheduler_.schedule(connectTimeoutMs_, () ->
         {
            if (!opened_)
               fail(socket);
         });
      socket.open();
   }

   // False means this input was not accepted for sending. The caller can send
   // it through RPC; input accepted by WebSocket must never be replayed because
   // it may already have executed at the shell.
   boolean send(String message)
   {
      if (socket_ == null || !opened_ || socket_.getState() != OPEN)
         return false;
      try
      {
         socket_.send(message);
         return true;
      }
      catch (RuntimeException error)
      {
         return false;
      }
   }

   void close()
   {
      Websocket socket = socket_;
      socket_ = null;
      opened_ = false;
      unansweredPings_ = 0;
      cancelConnect_.run();
      cancelKeepAlive_.run();
      cancelConnect_ = () -> {};
      cancelKeepAlive_ = () -> {};
      if (socket != null)
         socket.close();
   }

   private void fail(Websocket socket)
   {
      if (socket_ != socket)
         return;
      close();
      listener_.onError();
   }

   private void scheduleKeepAlive(Websocket socket)
   {
      if (pingIntervalMs_ <= 0)
         return;
      cancelKeepAlive_ = scheduler_.schedule(pingIntervalMs_, () ->
      {
         if (socket_ != socket)
            return;
         if (unansweredPings_ >= MAX_UNANSWERED_PINGS)
         {
            fail(socket);
            return;
         }
         // Count probes actually sent, not wall time. Browser suspension or
         // background-tab timer throttling alone must not expire a connection.
         unansweredPings_++;
         if (!send(TerminalSocketPacket.keepAlivePacket()))
         {
            fail(socket);
            return;
         }
         scheduleKeepAlive(socket);
      });
   }

   private static final int OPEN = 1;
   private static final int MAX_UNANSWERED_PINGS = 3;
   private final WebsocketListenerExt listener_;
   private final int pingIntervalMs_;
   private final int connectTimeoutMs_;
   private final Function<String, Websocket> factory_;
   private final Scheduler scheduler_;
   private Websocket socket_;
   private boolean opened_;
   private int unansweredPings_;
   private Runnable cancelConnect_ = () -> {};
   private Runnable cancelKeepAlive_ = () -> {};
}
