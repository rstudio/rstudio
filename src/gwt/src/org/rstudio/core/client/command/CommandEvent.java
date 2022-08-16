/*
 * CommandEvent.java
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client.command;

import com.google.gwt.event.shared.GwtEvent;

public class CommandEvent extends GwtEvent<CommandHandler>
{
   public static final Type<CommandHandler> TYPE = new Type<>();

   public CommandEvent(AppCommand command)
   {
      command_ = command;
   }

   public AppCommand getCommand()
   {
      return command_;
   }

   @Override
   public Type<CommandHandler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(CommandHandler handler)
   {
      handler.onCommand(command_);
   }

   private final AppCommand command_;
}
