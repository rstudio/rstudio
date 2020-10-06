/*
 * TerminalDeckPanel.java
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

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.ui.DeckLayoutPanel;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.common.console.ConsoleProcessInfo;
import org.rstudio.studio.client.workbench.views.terminal.xterm.XTermOptions;

/**
 * DeckPanel that shows one terminal at a time. It holds TerminalSession widgets, which in
 * turn contain xterm terminal emulator.
 *
 * This code assumes only TerminalSession widgets are added to the Deck.
 */
public class TerminalDeckPanel extends DeckLayoutPanel
{
   /**
    * Create, add, and display a new panel to host a terminal
    */
   public void addNewTerminalPanel(
         ConsoleProcessInfo procInfo,
         XTermOptions options,
         boolean tabMovesFocus,
         boolean showWebLinks,
         boolean createdByApi,
         CommandWithArg<TerminalSession> callback)
   {
      TerminalSession session = new TerminalSession(procInfo, options, tabMovesFocus, showWebLinks, createdByApi);
      add(session);
      showWidget(session);
      Scheduler.get().scheduleDeferred(() ->  callback.execute(session));
   }

   /**
    * Number of terminal widgets in the deck
    * @return
    */
   public int getTerminalCount()
   {
      return getWidgetCount();
   }

   /**
    * Show existing widget for given session, otherwise do nothing
    * @param session
    */
   public void showTerminal(TerminalSession session)
   {
      for (int i = 0; i < getTerminalCount(); i++)
      {
         if (session == getTerminalAtIndex(i))
         {
            showWidget(i);
            break;
         }
      }
   }

   /**
    * Return TerminalSession at given index of deck
    */
   public TerminalSession getTerminalAtIndex(int i)
   {
      return (TerminalSession) getWidget(i);
   }

   /**
    * Return TerminalSession with given caption, or null if not found
    */
   public TerminalSession getTerminalWithCaption(String caption)
   {
      for (int i = 0; i < getWidgetCount(); i++)
      {
         TerminalSession t = getTerminalAtIndex(i);
         if (t != null && StringUtil.equals(t.getCaption(), caption))
         {
            return t;
         }
      }
      return null;
   }

   /**
    * Remove given session from deck; does nothing if not found
    */
   public void removeTerminal(TerminalSession session)
   {
      int index = getTerminalIndex(session);
      if (index != -1)
         remove(index);
   }

   /**
    * Remove all terminal widgets currently loaded in the deck
    */
   public void removeAllTerminals()
   {
      while (getWidgetCount() > 0)
         remove(0);
   }

   /**
    * Return the currently displayed terminal, if any
    */
   public TerminalSession getVisibleTerminal()
   {
      Widget visibleWidget = getVisibleWidget();
      if (visibleWidget != null)
      {
         return (TerminalSession) visibleWidget;
      }
      return null;
   }

   /**
    * Find index of a terminal, or -1 if not in Deck
    */
   public int getTerminalIndex(TerminalSession session)
   {
      for (int i = 0; i < getTerminalCount(); i++)
      {
         if (session == getTerminalAtIndex(i))
         {
            return i;
         }
      }
      return -1;
   }
}
