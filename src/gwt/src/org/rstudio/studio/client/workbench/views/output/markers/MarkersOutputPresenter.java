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

import com.google.gwt.core.client.JsArray;
import com.google.inject.Inject;

import org.rstudio.core.client.CodeNavigationTarget;
import org.rstudio.core.client.FilePosition;
import org.rstudio.core.client.events.HasEnsureHiddenHandlers;
import org.rstudio.core.client.events.HasSelectionCommitHandlers;
import org.rstudio.core.client.events.SelectionCommitEvent;
import org.rstudio.core.client.events.SelectionCommitHandler;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.sourcemarkers.SourceMarker;
import org.rstudio.studio.client.common.sourcemarkers.SourceMarkerList;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.views.BasePresenter;
import org.rstudio.studio.client.workbench.views.output.markers.events.ShowMarkersEvent;
import org.rstudio.studio.client.workbench.views.output.markers.model.MarkersServerOperations;
import org.rstudio.studio.client.workbench.views.output.markers.model.MarkersSet;
import org.rstudio.studio.client.workbench.views.output.markers.model.MarkersState;


public class MarkersOutputPresenter extends BasePresenter
{
   public interface Display extends WorkbenchView,
                                    HasSelectionCommitHandlers<CodeNavigationTarget>,
                                    HasEnsureHiddenHandlers
   {
      void ensureVisible(boolean activate);
      
      void initialize(MarkersState markerState);
      
      void showMarkersSet(MarkersSet markerSet);
   }

   @Inject
   public MarkersOutputPresenter(Display view,
                                 MarkersServerOperations server,
                                 EventBus events,
                                 FileTypeRegistry fileTypeRegistry)
   {
      super(view);
      view_ = view;
      server_ = server;
      fileTypeRegistry_ = fileTypeRegistry;

      view_.addSelectionCommitHandler(new SelectionCommitHandler<CodeNavigationTarget>()
      {
         @Override
         public void onSelectionCommit(SelectionCommitEvent<CodeNavigationTarget> event)
         {
            CodeNavigationTarget target = event.getSelectedItem();
            if (target == null)
               return;

            fileTypeRegistry_.editFile(
                  FileSystemItem.createFile(target.getFile()),
                  target.getPosition());
         }
      });
   }

   public void initialize(MarkersState state)
   {
      view_.initialize(state);
   }
   
   public void onShowMarkers(ShowMarkersEvent event)
   {
      // show tab and marker list
      view_.ensureVisible(true);
      view_.showMarkersSet(event.getMarkersSet());
      
      // select/navigate if requested
      JsArray<SourceMarker> markers = event.getMarkersSet().getMarkers();
      if (markers.length() > 0)
      {
         SourceMarker selectMarker = null;
         int autoSelect = event.getMarkersSet().getAutoSelect();
         if (autoSelect == SourceMarkerList.AUTO_SELECT_FIRST)
            selectMarker = markers.get(0);
         else if (autoSelect == SourceMarkerList.AUTO_SELECT_FIRST_ERROR)
            selectMarker = SourceMarker.getFirstError(markers);
         
         if (selectMarker != null)
         {
            fileTypeRegistry_.editFile(
              FileSystemItem.createFile(selectMarker.getPath()),
                 FilePosition.create(selectMarker.getLine(),
                                     selectMarker.getColumn()));
         }
      }
   }
   
   
   public void onDismiss()
   {
      server_.markersTabClosed(new VoidServerRequestCallback());
   }
  
   private final Display view_;
   private final MarkersServerOperations server_;
   private final FileTypeRegistry fileTypeRegistry_;
}
