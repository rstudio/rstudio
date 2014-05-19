/*
 * Packrat.java
 *
 * Copyright (C) 2014 by RStudio, Inc.
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

package org.rstudio.studio.client.packrat;

import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.ConsoleDispatcher;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class Packrat {
   public interface Binder extends CommandBinder<Commands, Packrat> {
   }

   @Inject
   public Packrat(
         Binder binder,
         Commands commands,
         EventBus eventBus,
         GlobalDisplay display) {
      eventBus_ = eventBus;
      display_ = display;
      binder.bind(commands, this);
   }

   @Handler
   public void onPackratHelp() {
      display_.openRStudioLink("packrat");
   }

   @Handler
   public void onPackratSnapshot() {
      eventBus_.fireEvent(
         new SendToConsoleEvent("packrat::snapshot()", true, false)
      );
   }

   @Handler
   public void onPackratRestore() {
      eventBus_.fireEvent(
         new SendToConsoleEvent("packrat::restore()", true, false)
      );
   }

   @Handler
   public void onPackratClean() {
      eventBus_.fireEvent(
         new SendToConsoleEvent("packrat::clean()", true, false)
      );
   }

   @Handler
   public void onPackratBundle() {
      eventBus_.fireEvent(
         new SendToConsoleEvent("packrat::bundle()", true, false)
      );
   }


   private GlobalDisplay display_;
   private EventBus eventBus_;

}
