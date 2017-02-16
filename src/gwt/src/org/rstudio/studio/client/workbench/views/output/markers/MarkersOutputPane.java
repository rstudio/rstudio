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


import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

import org.rstudio.core.client.CodeNavigationTarget;
import org.rstudio.core.client.events.EnsureVisibleEvent;
import org.rstudio.core.client.events.HasSelectionCommitHandlers;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.core.client.widget.*;
import org.rstudio.studio.client.common.sourcemarkers.SourceMarkerList;
import org.rstudio.studio.client.workbench.commands.Commands;
import org.rstudio.studio.client.workbench.ui.WorkbenchPane;
import org.rstudio.studio.client.workbench.views.output.markers.model.MarkersSet;
import org.rstudio.studio.client.workbench.views.output.markers.model.MarkersState;

public class MarkersOutputPane extends WorkbenchPane
      implements MarkersOutputPresenter.Display
{
   @Inject
   public MarkersOutputPane(Commands commands)
   {
      super("Markers");
      markerSetsToolbarButton_ = new MarkerSetsToolbarButton();
      markerList_ = new SourceMarkerList();
      clearButton_ = new ToolbarButton(commands.clearPlots().getImageResource(),
                                       null);
      ensureWidget();
   }
   
   @Override
   public void update(MarkersState markerState, int autoSelect)
   {
      // update list and toolbar button
      markerList_.clear();
      markerSetsToolbarButton_.updateActiveMarkerSet(null);
      markerSetsToolbarButton_.updateAvailableMarkerSets(new String[]{});
      
      if (markerState.hasMarkers())
      {
         MarkersSet markersSet = markerState.getMarkersSet();
     
         markerList_.showMarkers(null,
                                 markersSet.getBasePath(),
                                 markersSet.getMarkers(), 
                                 autoSelect);
              
         markerSetsToolbarButton_.updateAvailableMarkerSets(
               JsUtil.toStringArray(markerState.getMarkersSetNames()));
         
         markerSetsToolbarButton_.updateActiveMarkerSet(markersSet.getName());
      }
   }


   @Override
   protected Toolbar createMainToolbar()
   {
      Toolbar toolbar = new Toolbar();
      toolbar.addLeftWidget(markerSetsToolbarButton_);
      
      toolbar.addRightWidget(clearButton_);

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
   public HasValueChangeHandlers<String> getMarkerSetList()
   {
      return markerSetsToolbarButton_;
   }

   @Override
   public HasSelectionCommitHandlers<CodeNavigationTarget> getMarkerList()
   {
      return markerList_;
   }
   
   @Override
   public HasClickHandlers getClearButton()
   {
      return clearButton_;
   }
   
   @Override
   public void onSelected()
   {
      super.onSelected();
      markerList_.focus();
      markerList_.ensureSelection();
   }
   
   private SourceMarkerList markerList_;
   private MarkerSetsToolbarButton markerSetsToolbarButton_;
   private ToolbarButton clearButton_;
}
