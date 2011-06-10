/*
 * HelpAtCursorEvent.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.console.events;

import com.google.gwt.event.shared.GwtEvent;

public class HelpAtCursorEvent extends GwtEvent<HelpAtCursorHandler>
{
   public static final Type<HelpAtCursorHandler> TYPE = new Type<HelpAtCursorHandler>();

   public HelpAtCursorEvent(String term)
   {
      this(term, term.length());
   }

   public HelpAtCursorEvent(String line, int cursorPos)
   {
      this.line = line;
      this.cursorPos = cursorPos;
   }

   public String getLine()
   {
      return line;
   }

   public int getCursorPos()
   {
      return cursorPos;
   }

   @Override
   public Type<HelpAtCursorHandler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(HelpAtCursorHandler handler)
   {
      handler.onHelpAtCursor(this);
   }

   private final String line;
   private final int cursorPos;
}
