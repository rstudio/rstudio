/*
 * BreakpointSavedEvent.java
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
package org.rstudio.studio.client.common.debugging.events;

import java.util.ArrayList;

import org.rstudio.studio.client.common.debugging.model.Breakpoint;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class BreakpointsSavedEvent
        extends GwtEvent<BreakpointsSavedEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onBreakpointsSaved(BreakpointsSavedEvent event);
   }

   public BreakpointsSavedEvent(
         ArrayList<Breakpoint> breakpoints, boolean successful)
   {
      breakpoints_ = breakpoints;
      successful_ = successful;
   }

   public boolean successful()
   {
      return successful_;
   }

   public ArrayList<Breakpoint> breakpoints()
   {
      return breakpoints_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onBreakpointsSaved(this);
   }

   public static final Type<Handler> TYPE = new Type<>();

   private ArrayList<Breakpoint> breakpoints_;
   private boolean successful_;
}
