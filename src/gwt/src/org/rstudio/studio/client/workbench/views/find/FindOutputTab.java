/*
 * FindOutputTab.java
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
package org.rstudio.studio.client.workbench.views.find;

import com.google.gwt.core.client.GWT;
import com.google.inject.Inject;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.events.FindInFilesResultEvent;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.ui.DelayLoadTabShim;
import org.rstudio.studio.client.workbench.ui.DelayLoadWorkbenchTab;

public class FindOutputTab extends DelayLoadWorkbenchTab<FindOutputPresenter>
{
   public abstract static class Shim extends DelayLoadTabShim<FindOutputPresenter, FindOutputTab>
      implements FindInFilesResultEvent.Handler
   {
      @Handler
      abstract void onFindInFiles();
   }

   static interface Binder extends CommandBinder<Commands, Shim>
   {}

   @Inject
   public FindOutputTab(Shim shim,
                        EventBus events,
                        Commands commands,
                        Session session)
   {
      super("Find", shim);

      if (!session.getSessionInfo().isFindInFilesEnabled())
         commands.findInFiles().setVisible(false);

      events.addHandler(FindInFilesResultEvent.TYPE, shim);

      GWT.<Binder>create(Binder.class).bind(commands, shim);
   }

   @Override
   public boolean closeable()
   {
      return true;
   }
}
