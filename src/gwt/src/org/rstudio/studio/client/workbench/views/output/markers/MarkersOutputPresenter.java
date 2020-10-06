/*
 * MarkersOutputPresenter.java
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
package org.rstudio.studio.client.workbench.views.output.markers;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.inject.Inject;

import org.rstudio.core.client.CodeNavigationTarget;
import org.rstudio.core.client.FilePosition;
import org.rstudio.core.client.command.CommandBinder;
import org.rstudio.core.client.command.Handler;
import org.rstudio.core.client.events.HasEnsureHiddenHandlers;
import org.rstudio.core.client.events.HasSelectionCommitHandlers;
import org.rstudio.core.client.events.SelectionCommitEvent;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.common.sourcemarkers.SourceMarker;
import org.rstudio.studio.client.common.sourcemarkers.SourceMarkerList;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.views.BasePresenter;
import org.rstudio.studio.client.workbench.views.output.markers.events.MarkersChangedEvent;
import org.rstudio.studio.client.workbench.views.output.markers.model.MarkersServerOperations;
import org.rstudio.studio.client.workbench.views.output.markers.model.MarkersSet;
import org.rstudio.studio.client.workbench.views.output.markers.model.MarkersState;

public class MarkersOutputPresenter extends BasePresenter
{
   public interface Display extends WorkbenchView,
                                    HasEnsureHiddenHandlers
   {
      void ensureVisible(boolean activate);

      void update(MarkersState markerState, int autoSelect);

      HasValueChangeHandlers<String> getMarkerSetList();

      HasSelectionCommitHandlers<CodeNavigationTarget> getMarkerList();

      HasClickHandlers getClearButton();
   }

   public interface Binder extends CommandBinder<Commands, MarkersOutputPresenter> {}

   @Inject
   public MarkersOutputPresenter(Display view,
                                 MarkersServerOperations server,
                                 Commands commands,
                                 Binder binder,
                                 FileTypeRegistry fileTypeRegistry)
   {
      super(view);
      binder.bind(commands, this);
      view_ = view;
      server_ = server;
      commands_ = commands;
      fileTypeRegistry_ = fileTypeRegistry;

      // active marker set changed
      view_.getMarkerSetList().addValueChangeHandler(new ValueChangeHandler<String>() {

         @Override
         public void onValueChange(ValueChangeEvent<String> event)
         {
            server_.updateActiveMarkerSet(event.getValue(),
                                          new VoidServerRequestCallback());
         }

      });

      // clear button
      view_.getClearButton().addClickHandler(event ->
      {
         server_.clearActiveMarkerSet(new VoidServerRequestCallback());
      });

      // source navigation
      view_.getMarkerList().addSelectionCommitHandler(
            (SelectionCommitEvent<CodeNavigationTarget> event) ->
      {
            CodeNavigationTarget target = event.getSelectedItem();
            if (target == null)
               return;

            fileTypeRegistry_.editFile(
                  FileSystemItem.createFile(target.getFile()),
                  target.getPosition());
      });
   }

   public void showInitialMarkers(MarkersState state)
   {
      view_.ensureVisible(false);
      view_.update(state, SourceMarkerList.AUTO_SELECT_NONE);
   }

   public void onMarkersChanged(MarkersChangedEvent event)
   {
      // get the state
      MarkersState state = event.getMarkersState();

      if (state.hasMarkers())
      {
         view_.ensureVisible(true);
         view_.update(event.getMarkersState(), event.getAutoSelect());

         // navigate to auto-selection if requested
         MarkersSet markersSet = state.getMarkersSet();
         if (markersSet != null)
         {
            JsArray<SourceMarker> markers = markersSet.getMarkers();
            if (markers.length() > 0)
            {
               SourceMarker selectMarker = null;
               int autoSelect = event.getAutoSelect();
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
      }
      else
      {
         view_.ensureHidden();
      }
   }

   public void onClosing()
   {
      server_.markersTabClosed(new VoidServerRequestCallback());
   }

   @Handler
   public void onActivateMarkers()
   {
      // Ensure that console pane is not minimized
      commands_.activateConsolePane().execute();
      view_.bringToFront();
   }

   private final Display view_;
   private final MarkersServerOperations server_;
   private final Commands commands_;
   private final FileTypeRegistry fileTypeRegistry_;
}
