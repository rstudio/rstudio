/*
 * DataOutputPresenter.java
 *
 * Copyright (C) 2009-18 by RStudio, Inc.
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

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.Command;
import com.google.inject.Inject;

import org.rstudio.core.client.CodeNavigationTarget;
import org.rstudio.core.client.events.HasEnsureHiddenHandlers;
import org.rstudio.core.client.events.HasSelectionCommitHandlers;
import org.rstudio.core.client.events.SelectionCommitEvent;
import org.rstudio.core.client.events.SelectionCommitHandler;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.application.events.RestartStatusEvent;
import org.rstudio.studio.client.common.DelayedProgressRequestCallback;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.ui.PaneManager;
import org.rstudio.studio.client.workbench.ui.PaneManager.Tab;
import org.rstudio.studio.client.workbench.views.BasePresenter;
import org.rstudio.studio.client.workbench.views.BusyPresenter;
import org.rstudio.studio.client.workbench.views.output.data.events.DataOutputCompletedEvent;

public class DataOutputPresenter extends BasePresenter
   implements DataOutputCompletedEvent.Handler
{
   public interface Display extends WorkbenchView, HasEnsureHiddenHandlers
   {
      void ensureVisible(boolean activate);
   }
   
   @Inject
   public DataOutputPresenter(GlobalDisplay globalDisplay,
                              PaneManager paneManager,
                              Commands commands,
                              EventBus events,
                              Display view)
   {
      super(view);
      view_ = (DataOutputPane) view;
      
      paneManager_ = paneManager;

      
      globalDisplay_ = globalDisplay;
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
   
   private final DataOutputPane view_;
   private final GlobalDisplay globalDisplay_;
   private final PaneManager paneManager_;
}