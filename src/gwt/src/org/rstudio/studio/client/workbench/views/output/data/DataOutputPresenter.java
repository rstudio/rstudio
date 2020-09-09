/*
 * DataOutputPresenter.java
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

package org.rstudio.studio.client.workbench.views.output.data;

import com.google.gwt.user.client.Command;
import com.google.inject.Inject;

import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.events.HasEnsureHiddenHandlers;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.BasePresenter;
import org.rstudio.studio.client.workbench.views.output.data.events.DataOutputCompletedEvent;

public class DataOutputPresenter extends BasePresenter
   implements DataOutputCompletedEvent.Handler
{
   public interface Display extends WorkbenchView, HasEnsureHiddenHandlers
   {
      void ensureVisible(boolean activate);
   }
   public interface Binder extends CommandBinder<Commands, DataOutputPresenter>
   {}

   @Inject
   public DataOutputPresenter(Binder binder,
                              Commands commands,
                              EventBus events,
                              Display view)
   {
      super(view);
      view_ = (DataOutputPane) view;
      commands_ = commands;
      binder.bind(commands, this);
   }

   public void initialize()
   {
   }

   public void confirmClose(final Command onConfirmed)
   {
      onConfirmed.execute();
   }

   @Override
   public void onDataOutputCompleted(DataOutputCompletedEvent event)
   {
      view_.ensureVisible(true);

      view_.outputCompleted(event);
   }

   @Handler
   public void onActivateSQLResults()
   {
      // Ensure that console pane is not minimized
      commands_.activateConsolePane().execute();
      view_.bringToFront();
   }

   private final DataOutputPane view_;
   private final Commands commands_;
}
