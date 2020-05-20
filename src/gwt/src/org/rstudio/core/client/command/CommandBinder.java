/*
 * CommandBinder.java
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

import com.google.gwt.event.shared.HandlerRegistration;

/**
 * Provides a mechanism for declaratively hooking up command handler methods
 * to the relevant commands.
 *
 * 1) Create a method for each command to be handled. It should have a return
 *    type of void and no parameters. The method should* be named "on[Command]",
 *    for example if the command is named "saveChanges" then the method should
 *    be named "onSaveChanges". The method should have public or package level
 *    accessibility.
 * 2) Annotate each handler method with @Handler.
 * 3) Declare a subinterface for CommandBinder that specializes the type
 *    arguments with the appropriate values.
 * 4) Use GWT.create() to create an instance of your new subinterface, and
 *    call bind().
 *
 * [*] Or you can use a different method name, if you provide the command name
 *     to the @Handler attribute.
 *
 * Example:
 *
 * interface MyCommands extends CommandBundle {
 *    public AppCommand saveChanges(); 
 * }
 *
 * class MyObject {
 *   interface MyBinder extends CommandBinder<MyCommands, MyObject> {}
 *
 *   public MyObject(Commands commands) {
 *     ((MyBinder)GWT.create(MyBinder.class)).bind(commands, this);
 *   }
 *
 *   @Handler
 *   void onSaveChanges();
 * }
 *
 * @param <TCommands> The subtype of CommandBundle that will be used to
 *    find the relevant commands
 * @param <THandlers> The type of the object that contains the handler methods 
 */
public interface CommandBinder<TCommands extends CommandBundle, THandlers>
{
   HandlerRegistration bind(TCommands commands, THandlers handlers);
}
