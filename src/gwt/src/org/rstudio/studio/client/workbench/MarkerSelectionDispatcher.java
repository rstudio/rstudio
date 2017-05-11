/*
 * MarkerSelectionDispatcher.java
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench;

import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.source.events.SelectMarkerEvent;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class MarkerSelectionDispatcher
{
   public interface Binder extends CommandBinder<Commands, MarkerSelectionDispatcher>
   {
   }
   
   @Inject
   public MarkerSelectionDispatcher(Binder binder,
                                    Commands commands,
                                    EventBus events)
   {
      binder.bind(commands, this);
      events_ = events;
   }
   
   @Handler
   public void onSelectNextMarker()
   {
      onSelectMarker(1, true);
   }
   
   @Handler
   public void onSelectPreviousMarker()
   {
      onSelectMarker(-1, true);
   }
   
   public void selectMarkerRelative(int index)
   {
      onSelectMarker(index, true);
   }
   
   public void onSelectMarker(int index, boolean relative)
   {
      events_.fireEvent(new SelectMarkerEvent(index, relative));
   }

   private final EventBus events_;
}
