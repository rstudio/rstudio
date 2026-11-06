/*
 * TerminalSocketTestDoubles.java
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

import java.util.ArrayList;
import java.util.List;

import com.sksamuel.gwt.websockets.Websocket;

import junit.framework.Assert;

/**
 * Controlled stand-ins for the browser WebSocket and its timers, so tests can
 * order network events and timer expiry explicitly.
 */
class TerminalSocketTestDoubles
{
   /** One-shot timers that run only when a test ticks them. */
   static class Clock implements TerminalWebSocket.Scheduler
   {
      static class Task
      {
         Task(Runnable command)
         {
            this.command = command;
         }

         final Runnable command;
         boolean cancelled;
      }

      @Override
      public Runnable schedule(int delayMs, Runnable command)
      {
         Assert.assertTrue(delayMs > 0);
         Task task = new Task(command);
         tasks.add(task);
         return () -> task.cancelled = true;
      }

      /** Runs the oldest timer that has not been cancelled. */
      void tick()
      {
         while (!tasks.isEmpty())
         {
            Task task = tasks.remove(0);
            if (!task.cancelled)
            {
               task.command.run();
               return;
            }
         }
         Assert.fail("No scheduled task");
      }

      final List<Task> tasks = new ArrayList<>();
   }

   /** A WebSocket whose events the test raises through the listener API. */
   static class Socket extends Websocket
   {
      Socket(String url)
      {
         super(url);
      }

      @Override
      public void open()
      {
      }

      @Override
      public void close()
      {
         closes++;
         state = CLOSED;
      }

      @Override
      public int getState()
      {
         return state;
      }

      @Override
      public void send(String message)
      {
         if (throwOnSend)
            throw new IllegalStateException("send failed");
         sent.add(message);
      }

      void opened()
      {
         state = OPEN;
         onOpen();
      }

      void message(String message)
      {
         onMessage(message);
      }

      void error()
      {
         onError();
      }

      void closed()
      {
         state = CLOSED;
         onClose((short) 1000, "", true);
      }

      static final int OPEN = 1;
      static final int CLOSING = 2;
      static final int CLOSED = 3;

      int state;
      int closes;
      boolean throwOnSend;
      final List<String> sent = new ArrayList<>();
   }
}
