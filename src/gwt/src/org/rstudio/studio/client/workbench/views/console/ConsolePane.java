/*
 * ConsolePane.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.console;

import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import com.google.inject.Provider;
import org.rstudio.core.client.widget.CanFocus;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.ui.ToolbarPane;
import org.rstudio.studio.client.workbench.views.console.shell.Shell;

public class ConsolePane extends ToolbarPane
   implements Console.Display,
              CanFocus
{
   @Inject
   public ConsolePane(Provider<Shell> consoleProvider,
                      EventBus events,
                      Commands commands)
   {
      consoleProvider_ = consoleProvider ;
      
      // console is interacted with immediately so we make sure it
      // is always created during startup
      ensureWidget();

      new Console(this, events, commands);
   }

   public void focus()
   {
      setFocus(true);
   }

   public void setFocus(boolean focused)
   {
      shell_.getDisplay().setFocus(focused);
   }
   
   public int getCharacterWidth()
   {
      return shell_.getDisplay().getCharacterWidth();
   }
      
   @Override
   protected Widget createMainWidget()
   {
      shell_ = consoleProvider_.get() ;
      return (Widget) shell_.getDisplay() ;
   }
   
   private Provider<Shell> consoleProvider_ ;
   private Shell shell_;
}
