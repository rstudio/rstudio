/*
 * TerminalWebSocketTests.java
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

import com.sksamuel.gwt.websockets.CloseEvent;
import com.sksamuel.gwt.websockets.Websocket;
import com.sksamuel.gwt.websockets.WebsocketListenerExt;

import junit.framework.TestCase;

/** Uses real listener dispatch with controlled network events and one-shot timers. */
public class TerminalWebSocketTests extends TestCase
{
   private static class Clock implements TerminalWebSocket.Scheduler
   {
      static class Task
      {
         Task(Runnable command) { this.command = command; }
         final Runnable command;
         boolean cancelled;
      }

      @Override
      public Runnable schedule(int delayMs, Runnable command)
      {
         assertTrue(delayMs > 0);
         Task task = new Task(command);
         tasks.add(task);
         return () -> task.cancelled = true;
      }

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
         fail("No scheduled task");
      }

      final List<Task> tasks = new ArrayList<>();
   }

   private static class Socket extends Websocket
   {
      Socket(String url) { super(url); }
      @Override public void open() { }
      @Override public void close() { closes++; state = 3; }
      @Override public int getState() { return state; }
      @Override public void send(String message)
      {
         if (throwOnSend)
            throw new IllegalStateException("send failed");
         sent.add(message);
      }
      void opened() { state = 1; onOpen(); }
      void message(String message) { onMessage(message); }
      void error() { onError(); }
      void closed() { state = 3; onClose((short)1000, "", true); }
      int state;
      int closes;
      boolean throwOnSend;
      final List<String> sent = new ArrayList<>();
   }

   private static class Listener implements WebsocketListenerExt
   {
      @Override public void onOpen() { opens++; }
      @Override public void onClose(CloseEvent event) { closes++; }
      @Override public void onError() { errors++; }
      @Override public void onMessage(String message) { messages.add(message); }
      int opens;
      int closes;
      int errors;
      final List<String> messages = new ArrayList<>();
   }

   private class Harness
   {
      Harness(int pingInterval, int connectTimeout)
      {
         transport = new TerminalWebSocket(listener, pingInterval, connectTimeout, url ->
         {
            Socket socket = new Socket(url);
            sockets.add(socket);
            return socket;
         }, clock);
         transport.open("ws://localhost/terminal/test/");
      }

      Socket socket() { return sockets.get(sockets.size() - 1); }
      final Clock clock = new Clock();
      final Listener listener = new Listener();
      final List<Socket> sockets = new ArrayList<>();
      final TerminalWebSocket transport;
   }

   public void testOpenAndInputOutput()
   {
      Harness h = new Harness(10, 10);
      assertFalse(h.transport.send("too early"));
      h.socket().opened();
      h.socket().opened();
      assertEquals(1, h.listener.opens);
      assertTrue(h.transport.send("ainput"));
      assertEquals("ainput", h.socket().sent.get(0));
      h.socket().message("aoutput");
      assertEquals("aoutput", h.listener.messages.get(0));
   }

   public void testTimeoutClosesSocketAndIgnoresLateEvents()
   {
      Harness h = new Harness(10, 10);
      h.clock.tick();
      assertEquals(1, h.listener.errors);
      assertEquals(1, h.socket().closes);
      h.socket().opened();
      h.socket().message("alate output");
      h.socket().error();
      h.socket().closed();
      assertEquals(0, h.listener.opens);
      assertEquals(0, h.listener.closes);
      assertEquals(1, h.listener.errors);
      assertTrue(h.listener.messages.isEmpty());
   }

   public void testReplacedSocketCannotAffectCurrentConnection()
   {
      Harness h = new Harness(10, 10);
      Socket old = h.socket();
      Clock.Task oldTimeout = h.clock.tasks.get(0);
      h.transport.open("ws://localhost/terminal/test/");
      h.socket().opened();
      old.opened();
      old.closed();
      old.error();
      old.message("astale");
      oldTimeout.command.run(); // even an already-queued timeout is harmless
      assertEquals(1, h.listener.opens);
      assertEquals(0, h.listener.closes);
      assertEquals(0, h.listener.errors);
      assertTrue(h.listener.messages.isEmpty());
      assertTrue(h.transport.send("anew"));
      assertEquals("anew", h.socket().sent.get(0));
   }

   public void testCancelledConnectTimeoutCannotExpireOpenSocket()
   {
      Harness h = new Harness(10, 10);
      Clock.Task timeout = h.clock.tasks.get(0);
      h.socket().opened();
      timeout.command.run();
      assertEquals(0, h.listener.errors);
      assertTrue(h.transport.send("astill connected"));
   }

   public void testUnansweredKeepAlivesTriggerFallbackOnce()
   {
      Harness h = new Harness(10, 10);
      h.socket().opened();
      for (int i = 0; i < 3; i++)
         h.clock.tick();
      assertEquals(3, h.socket().sent.size());
      assertEquals(0, h.listener.errors);
      h.clock.tick();
      assertEquals(1, h.listener.errors);
      assertEquals(1, h.socket().closes);
      h.socket().error();
      assertEquals(1, h.listener.errors);
   }

   public void testPongResetsUnansweredProbeCount()
   {
      Harness h = new Harness(10, 10);
      h.socket().opened();
      for (int i = 0; i < 20; i++)
      {
         h.clock.tick();
         h.clock.tick();
         h.socket().message(TerminalSocketPacket.keepAlivePacket());
      }
      assertEquals(0, h.listener.errors);
      assertTrue(h.listener.messages.isEmpty());
      assertTrue(h.transport.send("astill connected"));
   }

   public void testCloseCancelsAllTimers()
   {
      Harness h = new Harness(10, 10);
      h.socket().opened();
      h.transport.close();
      h.transport.close();
      assertEquals(1, h.socket().closes);
      for (Clock.Task task : h.clock.tasks)
      {
         assertTrue(task.cancelled);
         task.command.run();
      }
      assertEquals(0, h.listener.errors);
      assertTrue(h.socket().sent.isEmpty());
   }

   public void testDisabledTimers()
   {
      Harness h = new Harness(0, 0);
      h.socket().opened();
      assertTrue(h.clock.tasks.isEmpty());
      assertTrue(h.transport.send("ainput"));
   }

   public void testClosedSocketDoesNotAcceptInput()
   {
      Harness h = new Harness(10, 10);
      h.socket().opened();
      h.socket().state = 2; // closing, before the asynchronous close event
      assertFalse(h.transport.send("anot sent"));
      assertTrue(h.socket().sent.isEmpty());
   }

   public void testSendExceptionDoesNotAcceptInput()
   {
      Harness h = new Harness(10, 10);
      h.socket().opened();
      h.socket().throwOnSend = true;
      assertFalse(h.transport.send("anot sent"));
      h.clock.tick();
      assertEquals(1, h.listener.errors);
   }

   public void testCloseEventOnlyNotifiesOnce()
   {
      Harness h = new Harness(10, 10);
      h.socket().opened();
      h.socket().closed();
      h.socket().closed();
      assertEquals(1, h.listener.closes);
      assertEquals(0, h.listener.errors);
   }
}
