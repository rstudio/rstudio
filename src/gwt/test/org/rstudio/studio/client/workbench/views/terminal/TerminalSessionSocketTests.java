/*
 * TerminalSessionSocketTests.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
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
import java.util.Arrays;
import java.util.List;

import org.rstudio.studio.client.common.console.ConsoleOutputEvent;
import org.rstudio.studio.client.common.console.ConsoleProcessInfo;
import org.rstudio.studio.client.common.shell.ShellInput;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerErrorCause;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.VoidResponse;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.views.terminal.TerminalSocketTestDoubles.Clock;
import org.rstudio.studio.client.workbench.views.terminal.TerminalSocketTestDoubles.Socket;
import org.rstudio.studio.client.workbench.views.terminal.events.TerminalDataInputEvent;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.json.client.JSONValue;
import com.google.gwt.junit.client.GWTTestCase;

import junit.framework.Assert;

public class TerminalSessionSocketTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   public void testCommonPasswordPrompts()
   {
      Assert.assertFalse(
            TerminalSessionSocket.PASSWORD_PATTERN.test("Nothing to see here"));
      Assert.assertTrue(
            TerminalSessionSocket.PASSWORD_PATTERN.test("Password:"));
      Assert.assertTrue(
            TerminalSessionSocket.PASSWORD_PATTERN.test("password:"));
      Assert.assertFalse(
            TerminalSessionSocket.PASSWORD_PATTERN.test("Password"));
      Assert.assertFalse(
            TerminalSessionSocket.PASSWORD_PATTERN.test("password"));
      Assert.assertTrue(
            TerminalSessionSocket.PASSWORD_PATTERN.test("Passphrase:"));
      Assert.assertTrue(
            TerminalSessionSocket.PASSWORD_PATTERN.test("passphrase:"));
      Assert.assertFalse(
            TerminalSessionSocket.PASSWORD_PATTERN.test("Passphrase"));
      Assert.assertFalse(
            TerminalSessionSocket.PASSWORD_PATTERN.test("passphrase"));
   }

   public void testPasswordPromptsWithExtraGunk()
   {
      Assert.assertTrue(
            TerminalSessionSocket.PASSWORD_PATTERN.test("My HappyPassword:  "));
      Assert.assertTrue(
            TerminalSessionSocket.PASSWORD_PATTERN.test("\n\rpassword:::"));
      Assert.assertTrue(
            TerminalSessionSocket.PASSWORD_PATTERN.test("The Passphrase: is here"));
      Assert.assertTrue(
            TerminalSessionSocket.PASSWORD_PATTERN.test("passphrasepasswordpassphrase: "));
   }

   // The remaining tests drive the production TerminalSessionSocket and
   // TerminalWebSocket with controlled socket events, timers and RPC
   // completions, so late network events and reentrant input callbacks can be
   // ordered explicitly. They cover connection and input lifecycles, not the
   // browser or the network.

   private static class Session implements TerminalSessionSocket.Session
   {
      @Override
      public void receivedInput(String input)
      {
      }

      @Override
      public void receivedOutput(String output)
      {
      }

      @Override
      public void connectionDisconnected()
      {
         disconnected++;
      }

      int disconnected;
   }

   private static class Emulator implements TerminalSessionSocket.Emulator
   {
      @Override
      public HandlerRegistration addTerminalDataInputHandler(TerminalDataInputEvent.Handler handler)
      {
         return () -> {};
      }

      @Override
      public void accept(String output)
      {
      }
   }

   private static class Callback implements TerminalSessionSocket.ConnectCallback
   {
      @Override
      public void onConnected()
      {
         connected++;
      }

      @Override
      public void onError(String message)
      {
         errors++;
      }

      int connected;
      int errors;
   }

   private static class Ack extends VoidServerRequestCallback
   {
      @Override
      public void onResponseReceived(VoidResponse response)
      {
         count++;
      }

      @Override
      public void onError(ServerError error)
      {
         errors++;
      }

      int count;
      int errors;
   }

   /** A console process whose RPC completions the test delivers explicitly. */
   private static class Process implements TerminalSessionSocket.Process
   {
      @Override
      public ConsoleProcessInfo getProcessInfo()
      {
         return ConsoleProcessInfo.createNewTerminalInfo(false, "");
      }

      @Override
      public int getChannelMode()
      {
         return mode;
      }

      @Override
      public HandlerRegistration addConsoleOutputHandler(ConsoleOutputEvent.Handler handler)
      {
         return () -> {};
      }

      @Override
      public void useRpcMode(ServerRequestCallback<VoidResponse> callback)
      {
         fallbacks++;
         mode = ConsoleProcessInfo.CHANNEL_RPC;
         pendingRpc = callback;
      }

      @Override
      public void writeStandardInput(ShellInput input, ServerRequestCallback<VoidResponse> callback)
      {
         inputs.add(input.getText());
         sequences.add(input.getSequence());
         if (answerWrites)
            callback.onResponseReceived(null);
         else
            pendingWrites.add(callback);
      }

      int mode = ConsoleProcessInfo.CHANNEL_WEBSOCKET;
      int fallbacks;
      boolean answerWrites = true;
      ServerRequestCallback<VoidResponse> pendingRpc;
      final List<ServerRequestCallback<VoidResponse>> pendingWrites = new ArrayList<>();
      final List<String> inputs = new ArrayList<>();
      final List<Integer> sequences = new ArrayList<>();
   }

   private static class Harness
   {
      Harness()
      {
         socket = new TerminalSessionSocket(session, new Emulator(), 10, 10, (listener, ping, timeout) ->
               new TerminalWebSocket(listener, ping, timeout, url ->
               {
                  Socket created = new Socket(url);
                  sockets.add(created);
                  return created;
               }, clock), writeClock);
      }

      Process connect(Callback callback)
      {
         Process process = new Process();
         socket.connect(process, callback);
         return process;
      }

      /** Connects and opens the socket, then puts it in the CLOSING state so the next send is rejected. */
      Process connectClosing(Callback callback)
      {
         Process process = connect(callback);
         latest().opened();
         latest().state = Socket.CLOSING;
         return process;
      }

      Socket latest()
      {
         return sockets.get(sockets.size() - 1);
      }

      final Session session = new Session();
      final Clock clock = new Clock();
      final Clock writeClock = new Clock();
      final List<Socket> sockets = new ArrayList<>();
      final TerminalSessionSocket socket;
   }

   private static ServerError rpcError()
   {
      return new ServerError()
      {
         @Override
         public int getCode()
         {
            return ServerError.EXECUTION;
         }

         @Override
         public String getMessage()
         {
            return "error";
         }

         @Override
         public String getUserMessage()
         {
            return "error";
         }

         @Override
         public String getRedirectUrl()
         {
            return null;
         }

         @Override
         public ServerErrorCause getCause()
         {
            return null;
         }

         @Override
         public JSONValue getClientInfo()
         {
            return null;
         }
      };
   }

   public void testLiveFallbackQueuesNewInputWithoutReplay()
   {
      Harness h = new Harness();
      Callback callback = new Callback();
      Process process = h.connect(callback);
      Socket old = h.latest();
      old.opened();
      assertEquals(1, callback.connected);
      h.socket.dispatchInput("already sent", false, new Ack());
      assertEquals("aalready sent", old.sent.get(0));

      // three unanswered keep-alives, then expiry
      for (int i = 0; i < 4; i++)
         h.clock.tick();
      assertEquals(1, process.fallbacks);
      assertEquals(1, old.closes);
      assertTrue(h.socket.getConnectionDiagnostics().contains(constants().keepAliveExpiredMessage()));

      Ack a = new Ack();
      Ack b = new Ack();
      h.socket.dispatchInput("queued A", false, a);
      h.socket.dispatchInput("queued B", false, b);
      assertTrue("input must wait for RPC confirmation", process.inputs.isEmpty());
      assertEquals(0, a.count);
      assertEquals(0, b.count);

      process.pendingRpc.onResponseReceived(null);
      assertEquals("the burst is one write", Arrays.asList("queued Aqueued B"), process.inputs);
      assertEquals(1, a.count);
      assertEquals(1, b.count);
      assertEquals("connection completed only once", 1, callback.connected);

      // stale events from the abandoned socket
      old.opened();
      old.error();
      old.closed();
      assertEquals(1, callback.connected);
      assertEquals(1, process.fallbacks);
      assertEquals(0, h.session.disconnected);
   }

   public void testReplacementIgnoresOldCloseAndRetriesRejectedSend()
   {
      Harness h = new Harness();
      h.connect(new Callback());
      Socket old = h.latest();
      old.opened();

      h.socket.disconnect(false);
      Callback callback = new Callback();
      Process process = h.connect(callback);
      Socket next = h.latest();
      next.opened();
      old.closed();
      assertEquals(1, callback.connected);
      assertEquals("old close cannot disconnect replacement", 0, h.session.disconnected);

      next.state = Socket.CLOSING;
      Ack unsent = new Ack();
      h.socket.dispatchInput("not accepted by closing socket", false, unsent);
      assertEquals(1, process.fallbacks);
      assertEquals("failed send not acknowledged", 0, unsent.count);
      assertTrue(h.socket.getConnectionDiagnostics().contains(constants().sendUserInputMessage()));

      process.pendingRpc.onResponseReceived(null);
      assertEquals(1, unsent.count);
      assertEquals(Arrays.asList("not accepted by closing socket"), process.inputs);
   }

   public void testStaleRpcResponseAndLateOpenCannotCompleteNewConnection()
   {
      Harness h = new Harness();
      Callback pendingCallback = new Callback();
      Process pending = h.connect(pendingCallback);
      h.clock.tick(); // connect timeout
      Socket abandoned = h.latest();
      assertEquals(1, pending.fallbacks);
      assertTrue(h.socket.getConnectionDiagnostics().contains(constants().timeoutConnectingMessage()));

      h.socket.disconnect(false);
      Callback currentCallback = new Callback();
      h.connect(currentCallback);
      pending.pendingRpc.onResponseReceived(null);
      abandoned.opened();
      assertEquals(0, pendingCallback.connected);
      assertEquals("stale RPC response completed a new connection", 0, currentCallback.connected);

      h.latest().opened();
      assertEquals(1, currentCallback.connected);
   }

   public void testFallbackErrorSurfacesForQueuedInputAndDisconnects()
   {
      Harness h = new Harness();
      Process process = h.connectClosing(new Callback());
      Ack ack = new Ack();
      h.socket.dispatchInput("failed", false, ack);
      process.pendingRpc.onError(rpcError());
      assertEquals(1, ack.errors);
      assertEquals(0, ack.count);
      assertEquals(1, h.session.disconnected);
   }

   public void testDisconnectCancelsQueuedInputOnce()
   {
      for (boolean permanent : new boolean[] { false, true })
      {
         Harness h = new Harness();
         Process process = h.connectClosing(new Callback());
         Ack first = new Ack();
         Ack second = new Ack();
         h.socket.dispatchInput("cancel A", false, first);
         h.socket.dispatchInput("cancel B", false, second);

         h.socket.disconnect(permanent);
         assertEquals("one failure for the burst", 1, first.errors);
         assertEquals(0, second.errors);
         assertEquals(0, first.count);
         assertEquals(0, second.count);

         // late fallback replies must not complete cancelled input
         process.pendingRpc.onResponseReceived(null);
         process.pendingRpc.onError(rpcError());
         h.socket.disconnect(permanent);
         assertEquals(1, first.errors);
         assertEquals(0, second.errors);
         assertTrue(process.inputs.isEmpty());
      }
   }

   public void testReconnectCancelsQueuedInput()
   {
      Harness h = new Harness();
      h.connectClosing(new Callback());
      Ack ack = new Ack();
      h.socket.dispatchInput("cancel on reconnect", false, ack);
      h.connect(new Callback());
      assertEquals(1, ack.errors);
      assertEquals(0, ack.count);
   }

   public void testCancellationCallbackCanReconnectAndQueueInput()
   {
      Harness h = new Harness();
      Process old = h.connectClosing(new Callback());
      Process[] replacement = new Process[1];
      Ack replacementAck = new Ack();
      Ack reconnectingAck = new Ack()
      {
         @Override
         public void onError(ServerError error)
         {
            super.onError(error);
            replacement[0] = h.connectClosing(new Callback());
            h.socket.dispatchInput("keep replacement input", false, replacementAck);
         }
      };
      Ack trailingAck = new Ack();
      h.socket.dispatchInput("old A", false, reconnectingAck);
      h.socket.dispatchInput("old B", false, trailingAck);

      h.socket.disconnect(true);
      assertEquals(1, reconnectingAck.errors);
      assertEquals("later input in the burst is dropped without a second error", 0, trailingAck.errors);
      assertEquals("reentrant replacement queue must survive", 0, replacementAck.errors);
      assertEquals(0, replacementAck.count);

      old.pendingRpc.onResponseReceived(null);
      old.pendingRpc.onError(rpcError());
      assertEquals("old RPC reply must not flush replacement input", 0, replacementAck.count);

      replacement[0].pendingRpc.onResponseReceived(null);
      assertEquals(1, replacementAck.count);
      assertEquals(Arrays.asList("keep replacement input"), replacement[0].inputs);
   }

   public void testReconnectFromCancellationSupersedesOuterConnect()
   {
      Harness h = new Harness();
      Process original = h.connectClosing(new Callback());
      Callback winnerCallback = new Callback();
      Socket[] winningSocket = new Socket[1];
      Ack connectDuringCancellation = new Ack()
      {
         @Override
         public void onError(ServerError error)
         {
            super.onError(error);
            h.connect(winnerCallback);
            winningSocket[0] = h.latest();
         }
      };
      h.socket.dispatchInput("old queued input", false, connectDuringCancellation);

      int socketsBefore = h.sockets.size();
      Callback supersededCallback = new Callback();
      h.connect(supersededCallback);
      assertEquals("superseded connect must not install a second socket", socketsBefore + 1, h.sockets.size());

      winningSocket[0].opened();
      Ack winnerAck = new Ack();
      h.socket.dispatchInput("winner input", false, winnerAck);
      assertEquals(1, winnerCallback.connected);
      assertEquals("only the winning connection can complete", 0, supersededCallback.connected);
      assertTrue(winningSocket[0].sent.contains("awinner input"));
      assertEquals(1, winnerAck.count);

      original.pendingRpc.onResponseReceived(null);
      original.pendingRpc.onError(rpcError());
      assertEquals(1, winnerCallback.connected);

      h.socket.disconnect(false);
      assertEquals("the winning socket must remain owned and get closed", 1, winningSocket[0].closes);
   }

   public void testRejectedInputIsWrittenInOrderWithLaterInput()
   {
      Harness h = new Harness();
      Process process = h.connectClosing(new Callback());
      h.socket.dispatchInput("rejected", false, new Ack());
      h.socket.dispatchInput("typed during switch", false, new Ack());
      process.pendingRpc.onResponseReceived(null);
      h.socket.dispatchInput("typed after switch", false, new Ack());

      assertEquals(Arrays.asList("rejectedtyped during switch", "typed after switch"), process.inputs);
      assertEquals(Arrays.asList(ShellInput.IGNORE_SEQUENCE, ShellInput.IGNORE_SEQUENCE), process.sequences);
   }

   public void testQueuedInputIsWrittenAsOneBurstAndLaterInputWaitsForIt()
   {
      Harness h = new Harness();
      Callback callback = new Callback();
      Process process = h.connectClosing(callback);
      process.answerWrites = false;
      Ack a = new Ack();
      Ack b = new Ack();
      Ack c = new Ack();
      h.socket.dispatchInput("rejected", false, a);
      h.socket.dispatchInput(" and typed during the switch", false, b);
      process.pendingRpc.onResponseReceived(null);
      assertEquals(Arrays.asList("rejected and typed during the switch"), process.inputs);
      assertEquals(0, a.count);
      assertEquals(0, b.count);

      // typed while the burst is in flight: written after it, not raced with it
      h.socket.dispatchInput("typed during the burst", false, c);
      assertEquals(1, process.inputs.size());

      process.pendingWrites.remove(0).onResponseReceived(null);
      assertEquals(1, a.count);
      assertEquals(1, b.count);
      assertEquals(0, c.count);
      assertEquals(Arrays.asList("rejected and typed during the switch", "typed during the burst"), process.inputs);

      process.pendingWrites.remove(0).onResponseReceived(null);
      assertEquals(1, c.count);
      assertEquals("connection completed only once", 1, callback.connected);

      // the switch is complete: input goes straight out
      h.socket.dispatchInput("after the switch", false, new Ack());
      assertEquals(3, process.inputs.size());
      assertEquals(1, process.pendingWrites.size());
   }

   public void testFailedBurstIsReportedOnceAndLaterInputStillWritten()
   {
      Harness h = new Harness();
      Process process = h.connectClosing(new Callback());
      process.answerWrites = false;
      Ack a = new Ack();
      Ack b = new Ack();
      Ack c = new Ack();
      h.socket.dispatchInput("lost A", false, a);
      h.socket.dispatchInput("lost B", false, b);
      process.pendingRpc.onResponseReceived(null);
      h.socket.dispatchInput("typed during the burst", false, c);

      process.pendingWrites.remove(0).onError(rpcError());
      assertEquals("one failure for the burst", 1, a.errors);
      assertEquals(0, b.errors);
      assertEquals(0, a.count);
      assertEquals(0, b.count);
      assertEquals(Arrays.asList("lost Alost B", "typed during the burst"), process.inputs);

      process.pendingWrites.remove(0).onResponseReceived(null);
      assertEquals(1, c.count);
      assertEquals(0, c.errors);
      assertEquals(0, h.session.disconnected);
   }

   // One write in flight at a time, so nothing can overtake anything
   // (#19542); keystrokes typed meanwhile are sent as one write once it
   // completes. Writes carry no sequence number: the order is the client's.
   public void testRpcWritesAreSerializedAndUnsequenced()
   {
      Harness h = new Harness();
      Process process = h.connect(new Callback());
      process.mode = ConsoleProcessInfo.CHANNEL_RPC;
      process.answerWrites = false;
      Ack a = new Ack();
      Ack b = new Ack();
      Ack c = new Ack();
      h.socket.dispatchInput("a", false, a);
      h.socket.dispatchInput("b", false, b);
      h.socket.dispatchInput("c", false, c);
      assertEquals(Arrays.asList("a"), process.inputs);

      process.pendingWrites.remove(0).onResponseReceived(null);
      assertEquals(1, a.count);
      assertEquals(0, b.count);
      assertEquals(Arrays.asList("a", "bc"), process.inputs);

      process.pendingWrites.remove(0).onResponseReceived(null);
      assertEquals(1, b.count);
      assertEquals(1, c.count);

      // nothing in flight: input is written at once, as its own request
      h.socket.dispatchInput("d", false, new Ack());
      assertEquals(Arrays.asList("a", "bc", "d"), process.inputs);
      process.pendingWrites.remove(0).onResponseReceived(null);
      h.socket.dispatchInput("e", false, new Ack());
      assertEquals(Arrays.asList("a", "bc", "d", "e"), process.inputs);

      for (int sequence : process.sequences)
         assertEquals(ShellInput.IGNORE_SEQUENCE, sequence);
   }

   // A failed write is reported and the input held behind it still goes out.
   public void testFailedWriteDoesNotHoldLaterInput()
   {
      Harness h = new Harness();
      Process process = h.connect(new Callback());
      process.mode = ConsoleProcessInfo.CHANNEL_RPC;
      process.answerWrites = false;
      Ack a = new Ack();
      Ack b = new Ack();
      h.socket.dispatchInput("a", false, a);
      h.socket.dispatchInput("b", false, b);

      process.pendingWrites.remove(0).onError(rpcError());
      assertEquals(1, a.errors);
      assertEquals(Arrays.asList("a", "b"), process.inputs);

      process.pendingWrites.remove(0).onResponseReceived(null);
      assertEquals(1, b.count);
      h.socket.dispatchInput("c", false, new Ack());
      assertEquals(Arrays.asList("a", "b", "c"), process.inputs);
   }

   // RPC requests have no timeout, so a write whose response never arrives
   // must not hold later input forever.
   public void testWriteThatNeverCompletesStopsHoldingInput()
   {
      Harness h = new Harness();
      Process process = h.connect(new Callback());
      process.mode = ConsoleProcessInfo.CHANNEL_RPC;
      process.answerWrites = false;
      Ack a = new Ack();
      Ack b = new Ack();
      h.socket.dispatchInput("a", false, a);
      h.socket.dispatchInput("b", false, b);
      h.socket.dispatchInput("c", false, new Ack());
      assertEquals(Arrays.asList("a"), process.inputs);

      h.writeClock.tick();
      assertEquals(Arrays.asList("a", "bc"), process.inputs);

      // the abandoned write completing late is reported but is not counted
      // twice: the write now in flight still holds later input
      ServerRequestCallback<VoidResponse> lateA = process.pendingWrites.remove(0);
      lateA.onResponseReceived(null);
      assertEquals(1, a.count);
      h.socket.dispatchInput("d", false, new Ack());
      assertEquals(Arrays.asList("a", "bc"), process.inputs);

      process.pendingWrites.remove(0).onResponseReceived(null);
      assertEquals(1, b.count);
      assertEquals(Arrays.asList("a", "bc", "d"), process.inputs);
   }

   public void testCompletedOrDisconnectedWriteLeavesNoTimerBehind()
   {
      Harness h = new Harness();
      Process process = h.connect(new Callback());
      process.mode = ConsoleProcessInfo.CHANNEL_RPC;
      process.answerWrites = false;
      h.socket.dispatchInput("a", false, new Ack());
      assertEquals(1, h.writeClock.pending());
      process.pendingWrites.remove(0).onResponseReceived(null);
      assertEquals(0, h.writeClock.pending());

      h.socket.dispatchInput("b", false, new Ack());
      assertEquals(1, h.writeClock.pending());
      process.pendingWrites.remove(0).onError(rpcError());
      assertEquals(0, h.writeClock.pending());

      h.socket.dispatchInput("c", false, new Ack());
      h.socket.disconnect(false);
      assertEquals(0, h.writeClock.pending());
   }

   // Writes of a previous connection are not waited for (their completions
   // may never arrive), and a late old completion changes nothing for the
   // replacement.
   public void testReconnectDoesNotWaitForOldWrites()
   {
      Harness h = new Harness();
      Process old = h.connect(new Callback());
      old.mode = ConsoleProcessInfo.CHANNEL_RPC;
      old.answerWrites = false;
      Ack a = new Ack();
      h.socket.dispatchInput("a", false, a);

      h.socket.disconnect(false);
      Process next = h.connect(new Callback());
      next.mode = ConsoleProcessInfo.CHANNEL_RPC;
      next.answerWrites = false;
      h.socket.dispatchInput("c", false, new Ack());
      h.socket.dispatchInput("d", false, new Ack());
      assertEquals(Arrays.asList("c"), next.inputs);

      old.pendingWrites.remove(0).onError(rpcError());
      assertEquals(1, a.errors);
      assertEquals("old completion is ignored", Arrays.asList("c"), next.inputs);
      next.pendingWrites.remove(0).onResponseReceived(null);
      assertEquals(Arrays.asList("c", "d"), next.inputs);
   }

   // GWT.create() is only legal once the test runs in the GWT module, not in a
   // static initializer, which the JUnit runner evaluates in the JVM.
   private static TerminalConstants constants()
   {
      return GWT.create(TerminalConstants.class);
   }
}
