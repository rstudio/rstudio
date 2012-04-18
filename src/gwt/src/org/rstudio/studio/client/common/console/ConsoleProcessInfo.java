/*
 * ConsoleProcessInfo.java
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
package org.rstudio.studio.client.common.console;

import com.google.gwt.core.client.JavaScriptObject;
import org.rstudio.core.client.js.JsObject;

public class ConsoleProcessInfo extends JavaScriptObject
{
   public static final int INTERACTION_NEVER = 0;
   public static final int INTERACTION_POSSIBLE = 1;
   public static final int INTERACTION_ALWAYS = 2;
   
   protected ConsoleProcessInfo() {}

   public final native String getHandle() /*-{
      return this.handle;
   }-*/;

   public final native String getCaption() /*-{
      return this.caption;
   }-*/;

   public final native boolean isDialog() /*-{
      return this.dialog;
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
}
