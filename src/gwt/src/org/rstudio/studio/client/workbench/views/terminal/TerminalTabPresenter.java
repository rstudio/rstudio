/*
 * TerminalTabPresenter.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

package org.rstudio.studio.client.workbench.views.terminal;

import com.google.gwt.user.client.Command;
import com.google.inject.Inject;

import org.rstudio.core.client.widget.Operation;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.workbench.views.BusyPresenter;

public class TerminalTabPresenter extends BusyPresenter
{
   @Inject
   public TerminalTabPresenter(GlobalDisplay globalDisplay)
   {
      super(new TerminalPane("Terminal"));
      globalDisplay_ = globalDisplay;
   }
   
   public void initialize()
   {
   }

   public void confirmClose(final Command onConfirmed)
   {
      if (isBusy())
      {
        globalDisplay_.showYesNoMessage(GlobalDisplay.MSG_QUESTION, 
              "Close Terminal", 
              "Are you sure you want to exit the shell? Any running jobs " +
              "will be terminated.", false, 
              new Operation()
              {
                 @Override
                 public void execute()
                 {
                    // TODO: close PTY on server end
                    onConfirmed.execute();
                 }
              }, null, null, "Exit", "Cancel", true);
      }
      else
      {
        onConfirmed.execute();
      }
   }

   private final GlobalDisplay globalDisplay_;
}