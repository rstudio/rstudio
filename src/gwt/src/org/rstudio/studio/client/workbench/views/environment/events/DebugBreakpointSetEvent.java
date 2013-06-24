/*
 * DebugBreakpointSetEvent.java
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
package org.rstudio.studio.client.workbench.views.environment.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class DebugBreakpointSetEvent
        extends GwtEvent<DebugBreakpointSetEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onDebugBreakpointSet(DebugBreakpointSetEvent event);
   }

   public DebugBreakpointSetEvent(String fileName, int lineNumber)
   {
      fileName_ = fileName;
      lineNumber_ = lineNumber;
   }

   public String getFileName()
   {
      return fileName_;
   }
   
   public int getLineNumber()
   {
      return lineNumber_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onDebugBreakpointSet(this);
   }

   public static final Type<Handler> TYPE = new Type<Handler>();
   
   private String fileName_ = "";
   private int lineNumber_ = 0;
}
