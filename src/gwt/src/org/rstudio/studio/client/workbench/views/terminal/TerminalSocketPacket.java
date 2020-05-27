/*
 * TerminalSocketPacket.java
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

package org.rstudio.studio.client.workbench.views.terminal;

import org.rstudio.core.client.StringUtil;

/**
 * Super-simple packet format for Terminal Websocket payloads. Not using JSON to keep
 * overhead to absolute minimum, and to make server-side parsing trivial on a worker
 * thread.
 *
 * First character is a method indicator, as follows:
 *    "a" = send text, e.g. "aHello"
 *    "b" = ping/pong, e.g. "b"
 *
 * Only the "send text" method has a payload (everything after the "a").
 *
 * See SessionConsoleProcessSocketPacket in session code for C++ side of this sophisticated
 * wire format.
 */
public class TerminalSocketPacket
{
   public static String textPacket(String text)
   {
      return textPrefix + text;
   }

   public static String keepAlivePacket()
   {
      return keepAlivePrefix;
   }

   public static boolean isKeepAlive(String text)
   {
      return StringUtil.equals(text, keepAlivePrefix);
   }

   public static String getMessage(String text)
   {
      if (text.startsWith(textPrefix))
      {
         return text.substring(1);
      }
      return "";
   }

   private static final String keepAlivePrefix = "b";
   private static final String textPrefix = "a";
}
