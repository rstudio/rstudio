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
import org.rstudio.studio.client.panmirror.PanmirrorWidget;
import org.rstudio.studio.client.panmirror.location.PanmirrorEditingLocation;
import org.rstudio.studio.client.workbench.views.source.events.SourceNavigationEvent;
import org.rstudio.studio.client.workbench.views.source.model.DocUpdateSentinel;
import org.rstudio.studio.client.workbench.views.source.model.SourceNavigation;
import org.rstudio.studio.client.workbench.views.source.model.SourcePosition;

import com.google.inject.Inject;


public class VisualModeNavigation
{
   public VisualModeNavigation(DocUpdateSentinel docUpdateSentinel)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      docUpdateSentinel_ = docUpdateSentinel;
      
   }
   
   @Inject
   void initialize(EventBus events)
   {
      events_ = events;
   }
   
   
   public void onNavigated(PanmirrorNavigation navigation)
   {
      events_.fireEvent(new SourceNavigationEvent(toSourceNavigation(navigation)));
   }
   
   public boolean isVisualModePosition(SourcePosition position)
   {
      return PanmirrorNavigation.isPanmirrorPosition(position);
   }

   
   public void navigate(PanmirrorWidget panmirror, SourcePosition position)
   {
      PanmirrorNavigation navigation = toPanmirrorNavigation(position);
      if (navigation != null)
         panmirror.navigate(navigation);
   }
   
   public void recordCurrentNavigationPosition(PanmirrorWidget panmirror)
   {
      events_.fireEvent(new SourceNavigationEvent(SourceNavigation.create(
         docUpdateSentinel_.getId(), 
         docUpdateSentinel_.getPath(),
         getSourcePosition(panmirror)
      )));            
   }
   
   public SourcePosition getSourcePosition(PanmirrorWidget panmirror)
   {
      if (panmirror != null)
      {
         PanmirrorEditingLocation editingLocation = panmirror.getEditingLocation();
         return PanmirrorNavigation.toSourcePosition(PanmirrorNavigation.pos(editingLocation.pos));
      }
      else
      { 
         return PanmirrorNavigation.toSourcePosition(PanmirrorNavigation.top());
      }
   }
   
   private PanmirrorNavigation toPanmirrorNavigation(SourcePosition position)
   {
      if (isVisualModePosition(position))
      {
         return PanmirrorNavigation.fromSourcePosition(position);
      }
      else
      {
         return null;
      }
   }
   
   private SourceNavigation toSourceNavigation(PanmirrorNavigation navigation)
   {
      return SourceNavigation.create(docUpdateSentinel_.getId(), 
                                     docUpdateSentinel_.getPath(),
                                     PanmirrorNavigation.toSourcePosition(navigation));  
   }
   
   private final DocUpdateSentinel docUpdateSentinel_;
   
   private EventBus events_;
   
}
