/*
 * HelpTab.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.help;

import com.google.inject.Inject;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.ui.DelayLoadTabShim;
import org.rstudio.studio.client.workbench.ui.DelayLoadWorkbenchTab;
import org.rstudio.studio.client.workbench.views.help.events.ShowHelpEvent;
import org.rstudio.studio.client.workbench.views.help.events.ShowHelpHandler;

public class HelpTab extends DelayLoadWorkbenchTab<Help>
{
   public abstract static class Shim extends DelayLoadTabShim<Help, HelpTab>
                                     implements ShowHelpHandler
   {
      @Handler public abstract void onHelpHome();
   }
   
   public interface Binder extends CommandBinder<Commands, HelpTab.Shim> {}

   @Inject
   public HelpTab(Shim shim,
                  Binder binder,
                  Commands commands,
                  EventBus events)
   {
      super("Help", shim);
      binder.bind(commands, shim);
      events.addHandler(ShowHelpEvent.TYPE, shim);
   }
}
