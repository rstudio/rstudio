/*
 * VisualModeNavigation.java
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


package org.rstudio.studio.client.workbench.views.source.editors.text.visualmode;

import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.panmirror.PanmirrorNavigation;
import org.rstudio.studio.client.panmirror.PanmirrorNavigationType;
import org.rstudio.studio.client.panmirror.PanmirrorWidget;
import org.rstudio.studio.client.panmirror.location.PanmirrorEditingLocation;
import org.rstudio.studio.client.workbench.views.source.events.SourceNavigationEvent;
import org.rstudio.studio.client.workbench.views.source.model.SourceNavigation;
import org.rstudio.studio.client.workbench.views.source.model.SourcePosition;

import com.google.inject.Inject;


public class VisualModeNavigation
{
   interface Context
   {
      String getId();
      String getPath();
      PanmirrorWidget panmirror();
   }
   
   public VisualModeNavigation(Context context)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      context_ = context;
   }
   
   @Inject
   void initialize(EventBus events)
   {
      events_ = events;
   }
   
   
   public void onNavigated(PanmirrorNavigation navigation)
   {
      if (navigation.prevPos != -1)
         events_.fireEvent(new SourceNavigationEvent(createSourceNavigation(navigation.prevPos)));
      events_.fireEvent(new SourceNavigationEvent(createSourceNavigation(navigation.pos)));
   }
   
   public boolean isVisualModePosition(SourcePosition position)
   {
      return kPanmirrorContext.equals(position.getContext());
   }

   public void navigate(SourcePosition position, boolean recordCurrentPosition)
   {
      int pos = (kRowLength * position.getRow()) + position.getColumn();
      context_.panmirror().navigate(PanmirrorNavigationType.Pos, Integer.toString(pos), recordCurrentPosition);
   }
   
   public void navigateToXRef(String xref, boolean recordCurrentPosition)
   {
      context_.panmirror().navigate(PanmirrorNavigationType.XRef, xref, recordCurrentPosition);
   }
   
   public void recordCurrentNavigationPosition()
   {
      events_.fireEvent(new SourceNavigationEvent(createSourceNavigation(getSourcePosition())));            
   }
   
   public SourcePosition getSourcePosition()
   {
      PanmirrorWidget panmirror = context_.panmirror();
      if (panmirror != null && panmirror.isAttached())
      {
         PanmirrorEditingLocation editingLocation = panmirror.getEditingLocation();
         return createSourcePosition(editingLocation.pos);
      }
      else
      { 
         return createSourcePosition(0);
      }
   }
   
   private SourceNavigation createSourceNavigation(int pos)
   {
      return createSourceNavigation(createSourcePosition(pos));
   }
   
   private SourceNavigation createSourceNavigation(SourcePosition pos)
   {
      return SourceNavigation.create(context_.getId(), context_.getPath(), pos);
   }
   
   private SourcePosition createSourcePosition(int pos)
   {
      // create 'virtual' rows based on 50 character chunks (this is used for 
      // detecting duplicates in the navigation history, and 50 characters is
      // hardly worth a navigation (source mode uses actual editor rows for this)
      int row = pos / kRowLength;
      int col = pos % kRowLength;
      
      // create the position
      return SourcePosition.create(kPanmirrorContext, row, col, -1);
   }
  
   private Context context_;
     
   private EventBus events_;
   
   private final static String kPanmirrorContext = "panmirror";
   private final static int kRowLength = 50;
   
   
}
