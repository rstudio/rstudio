/*
 * MarkersOutputPane.java
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


import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

import org.rstudio.core.client.CodeNavigationTarget;
import org.rstudio.core.client.events.EnsureVisibleEvent;
import org.rstudio.core.client.events.SelectionCommitHandler;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.core.client.widget.*;
import org.rstudio.studio.client.common.sourcemarkers.SourceMarkerList;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.output.markers.model.MarkersSet;
import org.rstudio.studio.client.workbench.views.output.markers.model.MarkersState;

public class MarkersOutputPane extends WorkbenchPane
      implements MarkersOutputPresenter.Display
{
   @Inject
   public MarkersOutputPane()
   {
      super("Markers");
      markerSetsToolbarButton_ = new MarkerSetsToolbarButton();
      markerList_ = new SourceMarkerList();
      ensureWidget();
   }
   
   @Override
   public void update(MarkersState markerState, int autoSelect)
   {
      // update list and toolbar button
      MarkersSet markersSet = markerState.getMarkersSet();
      markerList_.clear();
      if (markersSet != null)
      {
         markerList_.showMarkers(null,
                                 markersSet.getBasePath(),
                                 markersSet.getMarkers(), 
                                 autoSelect);
         
         markerSetsToolbarButton_.updateActiveMarkerSet(markersSet.getName());
      }
      else
      {
         markerSetsToolbarButton_.updateActiveMarkerSet(null);
      }
      
      // update underlying set of choices
      markerSetsToolbarButton_.updateAvailableMarkerSets(
            JsUtil.toStringArray(markerState.getMarkersSetNames()));
   }


   @Override
   protected Toolbar createMainToolbar()
   {
      Toolbar toolbar = new Toolbar();
      toolbar.addLeftWidget(new ToolbarLabel("Showing:"));
      toolbar.addLeftWidget(markerSetsToolbarButton_);

      return toolbar;
   }

   @Override
   protected Widget createMainWidget()
   {
      return markerList_;
   }

   @Override
   public void ensureVisible(boolean activate)
   {
      fireEvent(new EnsureVisibleEvent(activate));
   }
   
   @Override
   public HandlerRegistration addValueChangeHandler(ValueChangeHandler<String> handler)
   {
      return markerSetsToolbarButton_.addValueChangeHandler(handler);
   }

   @Override
   public HandlerRegistration addSelectionCommitHandler(SelectionCommitHandler<CodeNavigationTarget> handler)
   {
      return markerList_.addSelectionCommitHandler(handler);
   }
   
   private SourceMarkerList markerList_;
   private MarkerSetsToolbarButton markerSetsToolbarButton_;
}
