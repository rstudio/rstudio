/*
 * ChunkOutputGallery.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.source.editors.text;

import java.util.ArrayList;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Widget;

public class ChunkOutputGallery extends Composite
{

   private static ChunkOutputGalleryUiBinder uiBinder = GWT
         .create(ChunkOutputGalleryUiBinder.class);

   interface ChunkOutputGalleryUiBinder
         extends UiBinder<Widget, ChunkOutputGallery>
   {
   }

   public interface GalleryStyle extends CssResource
   {
      String thumbnail();
      String selected();
   }

   public ChunkOutputGallery()
   {
      pages_ = new ArrayList<ChunkOutputPage>();
      initWidget(uiBinder.createAndBindUi(this));
   }
   
   // Public methods ----------------------------------------------------------
   
   public void addPage(ChunkOutputPage page)
   {
      final int index = pages_.size();
      pages_.add(page);
      Widget thumbnail = page.thumbnailWidget();
      thumbnail.addStyleName(style.thumbnail());
      filmstrip_.add(thumbnail);

      DOM.sinkEvents(thumbnail.getElement(), Event.ONCLICK);
      DOM.setEventListener(thumbnail.getElement(), new EventListener()
      {
         @Override
         public void onBrowserEvent(Event evt)
         {
            switch(DOM.eventGetType(evt))
            {
            case Event.ONCLICK:
               setActivePage(index);
               break;
            };
         }
      });
      if (pages_.size() == 1)
         viewer_.add(page.contentWidget());
   }
   
   // Private methods ---------------------------------------------------------
   
   private void setActivePage(int idx)
   {
      if (idx >= pages_.size())
         return;
      viewer_.remove(0);
      viewer_.add(pages_.get(idx).contentWidget());
      // TODO: reduce flicker by keeping content height fixed
   }
   
   private final ArrayList<ChunkOutputPage> pages_;
   
   @UiField GalleryStyle style;
   @UiField FlowPanel filmstrip_;
   @UiField HTMLPanel viewer_;
}
