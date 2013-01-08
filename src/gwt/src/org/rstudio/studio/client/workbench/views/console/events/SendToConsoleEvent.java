/*
 * SendToConsoleEvent.java
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
package org.rstudio.studio.client.workbench.views.console.events;

import com.google.gwt.event.shared.GwtEvent;

public class SendToConsoleEvent extends GwtEvent<SendToConsoleHandler>
{
   public static final GwtEvent.Type<SendToConsoleHandler> TYPE =
      new GwtEvent.Type<SendToConsoleHandler>();
  
   public SendToConsoleEvent(String code, boolean execute)
   {
      this(code, execute, false);
   }
   
   public SendToConsoleEvent(String code, boolean execute, boolean focus)
   {
      code_ = code;
      execute_ = execute;
      focus_ = focus;
   }

   public String getCode()
   {
      return code_;
   }

   public boolean shouldExecute()
   {
      return execute_;
   }
   
   public boolean shouldFocus()
   {
      return focus_;
   }
   
   @Override
   public Type<SendToConsoleHandler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(SendToConsoleHandler sendToConsoleHandler)
   {
      sendToConsoleHandler.onSendToConsole(this);
   }

   private final String code_;
   private final boolean execute_;
   private final boolean focus_;
}
