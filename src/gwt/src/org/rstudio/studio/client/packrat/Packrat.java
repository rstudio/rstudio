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
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.workbench.commands.Commands;

import com.google.inject.Inject;
import com.google.inject.Singleton;

// Packrat is responsible for managing top-level metadata concerning
// debug sessions (both function and top-level) and for processing the basic
// debug commands (run, step, etc) in the appropriate context.
@Singleton
public class Packrat {
   public interface Binder extends CommandBinder<Commands, Packrat> {
   }

   @Inject
   public Packrat(Binder binder, Commands commands, GlobalDisplay display) {
      display_ = display;
      binder.bind(commands, this);

   }

   @Handler
   public void onPackratHelp() {
      display_.openRStudioLink("packrat");
   }

   private GlobalDisplay display_;

}
