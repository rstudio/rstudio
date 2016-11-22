/*
 * TerminalSessionStartedEvent.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

import org.rstudio.studio.client.workbench.views.terminal.events.TerminalSessionStartedEvent.Handler;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.user.client.ui.Widget;

public class TerminalSessionStartedEvent extends GwtEvent<Handler>
{
   public interface Handler extends EventHandler
   {
      void onTerminalSessionStarted(TerminalSessionStartedEvent event);
   }
   
   public interface HasHandlers extends com.google.gwt.event.shared.HasHandlers
   {
      void addTerminalSessionStartedHandler(Handler handler);
   }
   
   public TerminalSessionStartedEvent(String terminalName, Widget terminalWidget)
   {
      terminalName_ = terminalName;
      terminalWidget_ = terminalWidget;
   }

   @Override
   public com.google.gwt.event.shared.GwtEvent.Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onTerminalSessionStarted(this);
   }
   
   public String getTerminalName()
   {
      return terminalName_;
   }
   
   public Widget getTerminalWidget()
   {
      return terminalWidget_;
   }
  
   private String terminalName_;
   private Widget terminalWidget_;
   
   public static final Type<Handler> TYPE = new Type<Handler>();
}
