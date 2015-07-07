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

import org.rstudio.studio.client.application.events.EventSerializer;
import org.rstudio.studio.client.application.events.SerializableEvent;

import com.google.gwt.core.shared.GWT;
import com.google.gwt.event.shared.GwtEvent;

public class SendToConsoleEvent extends SerializableEvent<
   SendToConsoleEvent.Serializer, SendToConsoleHandler>
{
   public interface Serializer extends EventSerializer<SendToConsoleEvent>{};

   public static final GwtEvent.Type<SendToConsoleHandler> TYPE =
      new GwtEvent.Type<SendToConsoleHandler>();
  
   public SendToConsoleEvent(String code, boolean execute)
   {
      this(code, execute, false);
   }
   
   public SendToConsoleEvent(String code, boolean execute, boolean focus)
   {
      this(code, execute, focus, false);
   }
   
   public SendToConsoleEvent(String code, 
                             boolean execute, 
                             boolean focus,
                             boolean animate)
   {
      super((Serializer)GWT.create(Serializer.class));
      code_ = code;
      execute_ = execute;
      focus_ = focus;
      animate_ = animate;
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
   
   public boolean shouldAnimate()
   {
      return animate_;
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

   protected final String code_;
   protected final boolean execute_;
   protected final boolean focus_;
   protected final boolean animate_;
}
