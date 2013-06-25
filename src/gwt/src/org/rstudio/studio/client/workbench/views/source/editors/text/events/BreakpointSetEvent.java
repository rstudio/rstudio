/*
 * BreakpointSetEvent.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class BreakpointSetEvent extends GwtEvent<BreakpointSetEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onBreakpointSet(BreakpointSetEvent event);
   }

   public BreakpointSetEvent(int lineNumber, boolean set)
   {
      lineNumber_ = lineNumber;
      set_ = set;
   }
   
   public int getLineNumber()
   {
      return lineNumber_;
   }
   
   public boolean isSet()
   {
      return set_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onBreakpointSet(this);
   }

   public static final Type<Handler> TYPE = new Type<Handler>();
   
   private int lineNumber_;
   private boolean set_;
}
