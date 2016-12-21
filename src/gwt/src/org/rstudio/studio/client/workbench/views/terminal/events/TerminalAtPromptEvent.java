/*
 * TerminalAtPromptEvent.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.terminal.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

/**
 * Sent when terminal is, or isn't, at the command-prompt. The purpose: allow
 * terminals to be killed via session suspend when they are all at the
 * command-prompt, but not if any are running programs.
 */
public class TerminalAtPromptEvent extends GwtEvent<TerminalAtPromptEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onTerminalAtPrompt(TerminalAtPromptEvent event);
   }

   public TerminalAtPromptEvent(boolean atPrompt)
   {
      atPrompt_ = atPrompt;
   }

   public boolean atPrompt()
   {
      return atPrompt_;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onTerminalAtPrompt(this);
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   private final boolean atPrompt_;

   public static final Type<Handler> TYPE = new Type<Handler>();
}
