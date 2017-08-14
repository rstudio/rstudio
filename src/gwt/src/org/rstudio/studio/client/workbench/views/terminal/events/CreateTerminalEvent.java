
/*
 * CreateTerminalEvent.java
 *
 * Copyright (C) 2009-14 by RStudio, Inc.
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

import org.rstudio.studio.client.application.events.CrossWindowEvent;

import com.google.gwt.event.shared.EventHandler;

/**
 * Event to trigger creation of a terminal session, with optional text to insert
 * in terminal after creation.
 */
public class CreateTerminalEvent extends CrossWindowEvent<CreateTerminalEvent.Handler>
{  
   public interface Handler extends EventHandler
   {
      /**
       * Event sent to trigger creation of a terminal session
       * @param event empty event
       */
      void onCreateTerminal(CreateTerminalEvent event);
   }

   public CreateTerminalEvent()
   {
      postCreateText_ = null;
   }

   public CreateTerminalEvent(String postCreateText)
   {
      postCreateText_ = postCreateText;
   }
     
   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onCreateTerminal(this);
   }
   
   /**
    * @return text to insert in terminal after creation, may be null
    */
   public String getPostCreateText()
   {
      return postCreateText_;
   }
   
   private final String postCreateText_;

   public static final Type<Handler> TYPE = new Type<Handler>();
}