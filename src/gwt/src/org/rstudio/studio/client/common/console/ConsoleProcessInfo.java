/*
 * ConsoleProcessInfo.java
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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
package org.rstudio.studio.client.common.console;

import com.google.gwt.core.client.JavaScriptObject;
import org.rstudio.core.client.js.JsObject;

public class ConsoleProcessInfo extends JavaScriptObject
{
   public static final int INTERACTION_NEVER = 0;
   public static final int INTERACTION_POSSIBLE = 1;
   public static final int INTERACTION_ALWAYS = 2;
   
   public static final int DEFAULT_COLS = 80;
   public static final int DEFAULT_ROWS = 25;
   
   public static final int CHANNEL_RPC = 0;
   public static final int CHANNEL_WEBSOCKET = 1;
   public static final int CHANNEL_PIPE = 2;
   
   protected ConsoleProcessInfo() {}

   public final native String getHandle() /*-{
      return this.handle;
   }-*/;

   public final native String getCaption() /*-{
      return this.caption;
   }-*/;

   public final native boolean getShowOnOutput() /*-{
      return this.show_on_output;
   }-*/;
   
   public final native int getInteractionMode()  /*-{
      return this.interaction_mode;
   }-*/;

   public final native int getMaxOutputLines()  /*-{
      return this.max_output_lines;
   }-*/;
   
   public final native String getBufferedOutput() /*-{
      return this.buffered_output;
   }-*/;

   public final Integer getExitCode()
   {
      JsObject self = this.cast();
      return self.getInteger("exit_code");
   }

   public final native int getTerminalSequence() /*-{
      return this.terminal_sequence;
   }-*/;

   public final native boolean getAllowRestart() /*-{
      return this.allow_restart;
   }-*/;

   public final native String getTitle() /*-{
      return this.title;
   }-*/;

   public final native boolean getHasChildProcs() /*-{
      return this.child_procs;
   }-*/;
   
   public final native int getShellType() /*-{
      return this.shell_type;
   }-*/;
   
   public final native int getChannelMode() /*-{
      return this.channel_mode;
   }-*/;
   
   public final native String getChannelId() /*-{
      return this.channel_id;
   }-*/;

   public static final int SEQUENCE_NO_TERMINAL = 0;
   
   public final native boolean isTerminal() /*-{
      return this.terminal_sequence > 0;
   }-*/;
}
