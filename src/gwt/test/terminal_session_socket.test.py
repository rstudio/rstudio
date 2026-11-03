#!/usr/bin/env python3
# Copyright (C) 2026 by Posit Software, PBC
# Licensed under the GNU Affero General Public License, version 3.

"""Session-level terminal regressions without a browser or a running rsession.

Compile the production TerminalSessionSocket, TerminalWebSocket, and packet
classes unchanged. Small doubles replace only GWT/UI, socket, and RPC dependencies
so tests can order late network events and reentrant input callbacks explicitly.
The separate TerminalWebSocketTests exercise the real Websocket listener wrapper.
These tests cover session coordination, not browser/network integration.

Run from src/gwt with `ant test-terminal-session-socket`, or directly with:
    python test/terminal_session_socket.test.py --java-home /path/to/jdk

Requires Python 3 and JDK 17 or newer. Generated sources/classes live under bin/.
"""
from pathlib import Path
import argparse
import os
import re
import shutil
import subprocess

parser = argparse.ArgumentParser(description=__doc__)
parser.add_argument('--java-home', default=os.environ.get('JAVA_HOME'))
parser.add_argument('--build-dir', type=Path)
options = parser.parse_args()
gwt = Path(__file__).resolve().parents[1]
repo = gwt.parents[1]
root = (options.build_dir or gwt / 'bin' / 'terminal-session-socket-test').resolve()
root.mkdir(parents=True, exist_ok=True)
sources = []


def java_tool(name):
    if options.java_home:
        suffix = '.exe' if os.name == 'nt' else ''
        tool = Path(options.java_home) / 'bin' / (name + suffix)
        if tool.is_file():
            return str(tool)
        parser.error(f'Cannot find {name} under {options.java_home}')
    tool = shutil.which(name)
    if not tool:
        parser.error(f'Cannot find {name}; pass --java-home with a JDK 17+ directory')
    return tool


terminal = 'org.rstudio.studio.client.workbench.views.terminal'
core = 'org.rstudio.core.client'
console = 'org.rstudio.studio.client.common.console'
server = 'org.rstudio.studio.client.server'
websocket = 'com.sksamuel.gwt.websockets'

def write(name, body):
    package, _, cls = name.rpartition('.')
    path = root / (name.replace('.', '/') + '.java')
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text('package ' + package + ';\n' + body, encoding='utf-8')
    sources.append(path)

source_dir = repo / 'src/gwt/src' / terminal.replace('.', '/')
source = (source_dir / 'TerminalSessionSocket.java').read_text(encoding='utf-8')
for name in ['TerminalSessionSocket', 'TerminalSocketPacket', 'TerminalWebSocket']:
    target = root / terminal.replace('.', '/') / (name + '.java')
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_bytes((source_dir / (name + '.java')).read_bytes())
    sources.append(target)

write(core + '.Debug', 'public class Debug { public static void log(String s) {} }')
write(core + '.StringUtil', '''public class StringUtil {
 public static String substring(String s, int i) { return s.substring(i); }
 public static boolean equals(String a, String b) { return java.util.Objects.equals(a,b); }
}''')
write('com.google.gwt.event.shared.HandlerRegistration', 'public interface HandlerRegistration { void removeHandler(); }')
write(core + '.HandlerRegistrations', '''public class HandlerRegistrations {
 public void add(com.google.gwt.event.shared.HandlerRegistration h) {}
 public void removeHandler() {}
}''')
write(core + '.regex.Pattern', '''public class Pattern {
 public static Pattern create(String a, String b) { return new Pattern(); }
 public boolean test(String s) { return false; }
}''')
write('org.rstudio.studio.client.application.Desktop', 'public class Desktop { public static boolean isDesktop() { return false; } }')
write('com.google.gwt.core.client.GWT', '''public class GWT {
 public static String getHostPageBaseURL() { return "http://localhost/"; }
 public static <T> T create(Class<T> c) { return c.cast(java.lang.reflect.Proxy.newProxyInstance(
   c.getClassLoader(), new Class<?>[] {c}, (p,m,a) -> m.getName())); }
}''')
write('com.google.gwt.user.client.Timer', '''public abstract class Timer {
 public static java.util.List<Timer> timers = new java.util.ArrayList<>();
 public boolean active;
 public Timer() { timers.add(this); }
 public abstract void run();
 public void schedule(int ms) { active = true; }
 public void scheduleRepeating(int ms) { active = true; }
 public void cancel() { active = false; }
}''')
write(server + '.ServerError', '''public class ServerError {
 public static final int CONNECTION=1;
 public int getCode() { return CONNECTION; }
 public String getMessage() { return "error"; }
 public String getUserMessage() { return getMessage(); }
 public String getRedirectUrl() { return null; }
 public ServerErrorCause getCause() { return null; }
 public com.google.gwt.json.client.JSONValue getClientInfo() { return null; }
}''')
write(server + '.ServerErrorCause', 'public class ServerErrorCause {}')
write('com.google.gwt.json.client.JSONValue', 'public class JSONValue {}')
write(server + '.VoidResponse', 'public class VoidResponse {}')
write(server + '.ServerRequestCallback', '''public class ServerRequestCallback<T> {
 public void onResponseReceived(T response) {}
 public void onError(ServerError error) {}
}''')
write(server + '.VoidServerRequestCallback', 'public class VoidServerRequestCallback extends ServerRequestCallback<VoidResponse> {}')
write(console + '.ConsoleOutputEvent', '''public class ConsoleOutputEvent {
 private String text;
 public ConsoleOutputEvent(String s) { text = s; }
 public String getOutput() { return text; }
 public interface Handler { void onConsoleOutput(ConsoleOutputEvent e); }
}''')
write(console + '.ConsoleProcessInfo', '''public class ConsoleProcessInfo {
 public static final int CHANNEL_RPC=0, CHANNEL_WEBSOCKET=1, CHANNEL_PIPE=2;
 public boolean getZombie() { return false; }
 public String getHandle() { return "test"; }
 public String getChannelId() { return "1234"; }
}''')
write('org.rstudio.studio.client.common.shell.ShellInput', '''public class ShellInput {
 public String text; public static ShellInput create(int n, String s, boolean echo) { ShellInput i = new ShellInput(); i.text=s; return i; }
}''')
write(console + '.ConsoleProcess', '''import org.rstudio.studio.client.server.*;
public class ConsoleProcess {
 public int mode = ConsoleProcessInfo.CHANNEL_WEBSOCKET, fallbacks=0, rpcWrites=0;
 public ServerRequestCallback<VoidResponse> pendingRpc;
 public java.util.List<String> inputs = new java.util.ArrayList<>();
 public ConsoleProcessInfo getProcessInfo() { return new ConsoleProcessInfo(); }
 public int getChannelMode() { return mode; }
 public com.google.gwt.event.shared.HandlerRegistration addConsoleOutputHandler(ConsoleOutputEvent.Handler h) { return () -> {}; }
 public void useRpcMode(ServerRequestCallback<VoidResponse> cb) {
  fallbacks++; mode = ConsoleProcessInfo.CHANNEL_RPC; pendingRpc=cb;
 }
 public void writeStandardInput(org.rstudio.studio.client.common.shell.ShellInput s, VoidServerRequestCallback cb) {
  rpcWrites++; inputs.add(s.text); cb.onResponseReceived(null);
 }
}''')
write(terminal + '.events.TerminalDataInputEvent', '''public class TerminalDataInputEvent {
 public String getData() { return "x"; }
 public interface Handler { void onTerminalDataInput(TerminalDataInputEvent e); }
}''')
write(terminal + '.xterm.XTermWidget', '''public class XTermWidget {
 public com.google.gwt.event.shared.HandlerRegistration addTerminalDataInputHandler(
   org.rstudio.studio.client.workbench.views.terminal.events.TerminalDataInputEvent.Handler h) { return () -> {}; }
 public void accept(String s) {}
}''')
write(terminal + '.TerminalLocalEcho', '''public class TerminalLocalEcho {
 public TerminalLocalEcho(xterm.XTermWidget x) {}
 public void echo(String s) {} public void clear() {} public void pause(int ms) {}
 public boolean isEmpty() { return true; } public void write(String s) {}
 public void resetDiagnostics() {} public String getDiagnostics() { return ""; }
}'''.replace('xterm.XTermWidget', terminal + '.xterm.XTermWidget'))
write(terminal + '.TerminalDiagnostics', '''public class TerminalDiagnostics {
 public void log(String s) {} public void resetLog() {} public String getLog() { return ""; }
}''')
methods = sorted(set(re.findall(r'constants_\.(\w+)\(', source)))
write(terminal + '.TerminalConstants', 'public interface TerminalConstants {\n' + '\n'.join('String '+m+'();' for m in methods) + '\n}')
write(websocket + '.CloseEvent', 'public class CloseEvent {}')
write(websocket + '.WebsocketListenerExt', '''public abstract class WebsocketListenerExt {
 public abstract void onClose(CloseEvent e); public abstract void onMessage(String s);
 public abstract void onOpen(); public abstract void onError();
}''')
write(websocket + '.Websocket', '''public class Websocket {
 public static java.util.List<Websocket> sockets = new java.util.ArrayList<>();
 public static Websocket latest() { return sockets.get(sockets.size() - 1); }
 public WebsocketListenerExt listener;
 public java.util.List<String> sent = new java.util.ArrayList<>();
 public boolean closeRequested; public int state=1; public int getState() { return state; }
 public Websocket(String url) { sockets.add(this); }
 public void addListener(WebsocketListenerExt l) { listener = l; }
 public void open() {} public void close() { closeRequested = true; state=3; }
 public void send(String s) { sent.add(s); }
}''')
write(terminal + '.TerminalSessionSocketRegression', r'''import com.sksamuel.gwt.websockets.*;
import com.google.gwt.user.client.Timer;
import org.rstudio.studio.client.common.console.ConsoleProcess;
import org.rstudio.studio.client.server.*;
import org.rstudio.studio.client.workbench.views.terminal.xterm.XTermWidget;
public class TerminalSessionSocketRegression {
 static class Session implements TerminalSessionSocket.Session {
  int disconnected=0; public void receivedInput(String s) {} public void receivedOutput(String s) {}
  public void connectionDisconnected() { disconnected++; }
 }
 static class Callback implements TerminalSessionSocket.ConnectCallback {
  int connected=0, errors=0; public void onConnected() { connected++; } public void onError(String s) { errors++; }
 }
 static class Ack extends VoidServerRequestCallback {
  int count=0, errors=0; public void onResponseReceived(VoidResponse r) { count++; }
  public void onError(ServerError e) { errors++; }
 }
 static void check(boolean b, String s) { if (!b) throw new AssertionError(s); }
 static void tick() {
  for (Timer timer : new java.util.ArrayList<>(Timer.timers)) {
   if (timer.active) { timer.active=false; timer.run(); return; }
  }
  throw new AssertionError("No active timer");
 }
 public static void main(String[] args) {
  Session session=new Session(); Callback cb=new Callback(); ConsoleProcess proc=new ConsoleProcess();
  TerminalSessionSocket c=new TerminalSessionSocket(session,new XTermWidget(),10,10);
  c.connect(proc,cb); Websocket old=Websocket.latest(); old.listener.onOpen();
  c.dispatchInput(0,"already sent",false,new Ack());
  for(int i=0;i<4;i++) tick();
  check(proc.fallbacks==1 && old.closeRequested,"liveness fallback");
  Ack a=new Ack(), b=new Ack();
  c.dispatchInput(1,"queued A",false,a); c.dispatchInput(2,"queued B",false,b);
  check(proc.rpcWrites==0 && a.count==0 && b.count==0,"input must wait for RPC confirmation");
  proc.pendingRpc.onResponseReceived(null);
  check(proc.inputs.equals(java.util.List.of("queued A","queued B")),"ordered delivery without replay");
  check(a.count==1 && b.count==1 && cb.connected==1,"callbacks exactly once after fallback");
  old.listener.onOpen(); old.listener.onError(); old.listener.onClose(new CloseEvent());
  check(cb.connected==1 && proc.fallbacks==1 && session.disconnected==0,"stale events after fallback");
  System.out.println("PASS: live fallback queues new input, preserves order, does not replay sent input or repeat connection completion.");

  c.disconnect(false); ConsoleProcess newer=new ConsoleProcess(); Callback cb2=new Callback(); c.connect(newer,cb2);
  Websocket next=Websocket.latest(); next.listener.onOpen(); old.listener.onClose(new CloseEvent());
  check(cb2.connected==1 && session.disconnected==0,"old close cannot disconnect replacement");
  next.state=2; Ack unsent=new Ack(); c.dispatchInput(3,"not accepted by closing socket",false,unsent);
  check(newer.fallbacks==1 && unsent.count==0,"failed send not acknowledged");
  newer.pendingRpc.onResponseReceived(null);
  check(unsent.count==1 && newer.inputs.size()==1,"unsent input retried through RPC");
  System.out.println("PASS: replacement ignores old close; a rejected send is delivered by RPC after confirmation.");

  c.disconnect(false); ConsoleProcess pending=new ConsoleProcess(); Callback cb3=new Callback(); c.connect(pending,cb3);
  tick(); Websocket abandoned=Websocket.latest();
  c.disconnect(false); ConsoleProcess current=new ConsoleProcess(); Callback cb4=new Callback(); c.connect(current,cb4);
  pending.pendingRpc.onResponseReceived(null); abandoned.listener.onOpen();
  check(cb3.connected==0 && cb4.connected==0,"stale RPC response completed a new connection");
  Websocket.latest().listener.onOpen(); check(cb4.connected==1,"new connection completion");
  c.disconnect(false);
  System.out.println("PASS: disconnected RPC attempt and late open cannot complete another connection.");

  ConsoleProcess failed=new ConsoleProcess(); Callback cb5=new Callback(); c.connect(failed,cb5);
  Websocket.latest().listener.onOpen(); Websocket.latest().state=2;
  Ack failAck=new Ack(); c.dispatchInput(4,"failed",false,failAck);
  failed.pendingRpc.onError(new ServerError());
  check(failAck.errors==1 && failAck.count==0 && session.disconnected==1,"fallback error must surface");
  c.disconnect(false);
  System.out.println("PASS: fallback errors report failure for queued input and disconnect the session.");

  for (boolean permanent : new boolean[] {false, true}) {
   ConsoleProcess cancelled=new ConsoleProcess(); c.connect(cancelled,new Callback());
   Websocket.latest().listener.onOpen(); Websocket.latest().state=2;
   Ack first=new Ack(), second=new Ack();
   c.dispatchInput(5,"cancel A",false,first); c.dispatchInput(6,"cancel B",false,second);
   c.disconnect(permanent);
   check(first.errors==1 && second.errors==1 && first.count==0 && second.count==0,"disconnect cancellation");
   cancelled.pendingRpc.onResponseReceived(null);
   cancelled.pendingRpc.onError(new ServerError());
   c.disconnect(permanent);
   check(first.errors==1 && second.errors==1 && cancelled.inputs.isEmpty(),"late response must not complete cancelled input again");
  }
  System.out.println("PASS: temporary and permanent disconnect cancel queued input exactly once; late fallback replies are ignored.");

  ConsoleProcess replaced=new ConsoleProcess(); c.connect(replaced,new Callback());
  Websocket.latest().listener.onOpen(); Websocket.latest().state=2;
  Ack replacedAck=new Ack(); c.dispatchInput(7,"cancel on reconnect",false,replacedAck);
  c.connect(new ConsoleProcess(),new Callback());
  check(replacedAck.errors==1 && replacedAck.count==0,"reconnect must cancel old queue");
  c.disconnect(false);
  System.out.println("PASS: reconnect reports cancellation for queued input from the previous connection.");

  ConsoleProcess reentrant=new ConsoleProcess(); c.connect(reentrant,new Callback());
  Websocket.latest().listener.onOpen(); Websocket.latest().state=2;
  ConsoleProcess replacement=new ConsoleProcess(); Ack replacementAck=new Ack();
  Ack reconnectingAck=new Ack() {
   public void onError(ServerError e) {
    super.onError(e);
    c.connect(replacement,new Callback());
    Websocket.latest().listener.onOpen(); Websocket.latest().state=2;
    c.dispatchInput(8,"keep replacement input",false,replacementAck);
   }
  };
  Ack trailingAck=new Ack();
  c.dispatchInput(9,"old A",false,reconnectingAck); c.dispatchInput(10,"old B",false,trailingAck);
  c.disconnect(true);
  check(reconnectingAck.errors==1 && trailingAck.errors==1,"all detached old input must be cancelled");
  check(replacementAck.errors==0 && replacementAck.count==0,"reentrant replacement queue must survive");
  reentrant.pendingRpc.onResponseReceived(null);
  reentrant.pendingRpc.onError(new ServerError());
  check(replacementAck.count==0,"old RPC reply must not flush replacement input");
  replacement.pendingRpc.onResponseReceived(null);
  check(replacementAck.count==1 && replacement.inputs.equals(java.util.List.of("keep replacement input")),"replacement queue must flush normally");
  c.disconnect(false);
  System.out.println("PASS: a cancellation callback can reconnect and queue input without that new input being cancelled.");

  ConsoleProcess original=new ConsoleProcess(); c.connect(original,new Callback());
  Websocket.latest().listener.onOpen(); Websocket.latest().state=2;
  ConsoleProcess winner=new ConsoleProcess(); Callback winnerCallback=new Callback();
  Websocket[] winningSocket=new Websocket[1];
  Ack connectDuringCancellation=new Ack() {
   public void onError(ServerError e) {
    super.onError(e);
    c.connect(winner,winnerCallback);
    winningSocket[0]=Websocket.latest();
   }
  };
  c.dispatchInput(11,"old queued input",false,connectDuringCancellation);
  int beforeReplacement=Websocket.sockets.size(); Callback supersededCallback=new Callback();
  c.connect(new ConsoleProcess(),supersededCallback);
  check(Websocket.sockets.size()==beforeReplacement+1,"superseded connect installed a second socket after cancellation reconnected");
  winningSocket[0].listener.onOpen();
  Ack winnerAck=new Ack(); c.dispatchInput(12,"winner input",false,winnerAck);
  check(winnerCallback.connected==1 && supersededCallback.connected==0,"only the winning connection can complete");
  check(winningSocket[0].sent.contains("awinner input") && winnerAck.count==1,"input must use the winning connection");
  original.pendingRpc.onResponseReceived(null);
  original.pendingRpc.onError(new ServerError());
  check(winnerCallback.connected==1,"old RPC completion must stay ignored");
  c.disconnect(false);
  check(winningSocket[0].closeRequested,"the winning socket must remain owned and get closed");
  System.out.println("PASS: reconnect from cancellation supersedes the outer connect without leaking or overwriting its socket.");


 }
}''')

source_args = root / 'sources.txt'
source_args.write_text(
    '\n'.join('"' + str(path).replace('\\', '/') + '"' for path in sources),
    encoding='utf-8',
)
classes = root / 'classes'
print('Compiling production terminal classes and controlled dependencies...', flush=True)
subprocess.run([
    java_tool('javac'), '--release', '17', '-d', str(classes), '@' + str(source_args)
], check=True)
subprocess.run([
    java_tool('java'), '-cp', str(classes), terminal + '.TerminalSessionSocketRegression'
], check=True)
