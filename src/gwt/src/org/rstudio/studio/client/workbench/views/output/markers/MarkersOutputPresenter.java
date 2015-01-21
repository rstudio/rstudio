/*
 * MarkersOutputPresenter.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.output.markers;

import com.google.inject.Inject;

import org.rstudio.core.client.CodeNavigationTarget;
import org.rstudio.core.client.events.HasEnsureHiddenHandlers;
import org.rstudio.core.client.events.HasSelectionCommitHandlers;
import org.rstudio.core.client.events.SelectionCommitEvent;
import org.rstudio.core.client.events.SelectionCommitHandler;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.views.BasePresenter;
import org.rstudio.studio.client.workbench.views.output.markers.events.ShowMarkersEvent;
import org.rstudio.studio.client.workbench.views.output.markers.model.MarkersServerOperations;
import org.rstudio.studio.client.workbench.views.output.markers.model.MarkersState;


public class MarkersOutputPresenter extends BasePresenter
{
   public interface Display extends WorkbenchView,
                                    HasSelectionCommitHandlers<CodeNavigationTarget>,
                                    HasEnsureHiddenHandlers
   {
      void ensureVisible(boolean activate);
   }

   @Inject
   public MarkersOutputPresenter(Display view,
                                 MarkersServerOperations server,
                                 EventBus events,
                                 final FileTypeRegistry fileTypeRegistry)
   {
      super(view);
      view_ = view;
      server_ = server;

      events.addHandler(ShowMarkersEvent.TYPE, new ShowMarkersEvent.Handler()
      {
         @Override
         public void onShowMarkers(ShowMarkersEvent event)
         {
            view_.ensureVisible(true);
         }
      });
      
      view_.addSelectionCommitHandler(new SelectionCommitHandler<CodeNavigationTarget>()
      {
         @Override
         public void onSelectionCommit(SelectionCommitEvent<CodeNavigationTarget> event)
         {
            CodeNavigationTarget target = event.getSelectedItem();
            if (target == null)
               return;

            fileTypeRegistry.editFile(
                  FileSystemItem.createFile(target.getFile()),
                  target.getPosition());
         }
      });
   }

   public void initialize(MarkersState state)
   {
      if (state.isVisible())
         view_.ensureVisible(false);
   }
   
   public void onDismiss()
   {
      server_.clearAllMarkers(new VoidServerRequestCallback());
   }
  
   private final Display view_;
   private final MarkersServerOperations server_;
}
