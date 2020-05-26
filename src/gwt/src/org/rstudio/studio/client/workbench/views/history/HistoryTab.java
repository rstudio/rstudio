/*
 * HistoryTab.java
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
package org.rstudio.studio.client.workbench.views.history;

import com.google.inject.Inject;

import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.ui.DelayLoadTabShim;
import org.rstudio.studio.client.workbench.ui.DelayLoadWorkbenchTab;

public class HistoryTab extends DelayLoadWorkbenchTab<History>
{
   public interface Binder extends CommandBinder<Commands, HistoryTab.Shim> {}
   
   public abstract static class Shim
         extends DelayLoadTabShim<History, HistoryTab>
   {
      @Handler
      public abstract void onLoadHistory();
      
      @Handler
      public abstract void onSaveHistory();
   }

   @Inject
   public HistoryTab(Shim shim, Binder binder, Commands commands)
   {
      super("History", shim);
      binder.bind(commands, shim);
   }
}
