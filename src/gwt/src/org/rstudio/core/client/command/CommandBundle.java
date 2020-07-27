/*
 * CommandBundle.java
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
package org.rstudio.core.client.command;

import java.util.HashMap;

/**
 * Marker interface for declaring sets of commands
 */
public abstract class CommandBundle
{
   public AppCommand getCommandById(String commandId)
   {
      return commandsById_.get(commandId);
   }

   public void addCommand(String id, AppCommand command)
   {
      if (commandsById_.containsKey(id))
         throw new IllegalStateException("Command " + id + " already exists");

      commandsById_.put(id, command);
   }

   public HashMap<String, AppCommand> getCommands()
   {
      return commandsById_;
   }

   private final HashMap<String, AppCommand> commandsById_ =
         new HashMap<String, AppCommand>();
}
