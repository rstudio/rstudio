/*
 * ShowContentEvent.java
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
package org.rstudio.studio.client.workbench.views.source.events;

import com.google.gwt.event.shared.GwtEvent;
import org.rstudio.studio.client.workbench.views.source.model.ContentItem;

public class ShowContentEvent extends GwtEvent<ShowContentHandler>
{
   public static final GwtEvent.Type<ShowContentHandler> TYPE =
      new GwtEvent.Type<ShowContentHandler>();
   
   public ShowContentEvent(ContentItem content)
   {
      content_ = content;
   }
   
   public ContentItem getContent()
   {
      return content_;
   }
   
   @Override
   protected void dispatch(ShowContentHandler handler)
   {
      handler.onShowContent(this);
   }

   @Override
   public GwtEvent.Type<ShowContentHandler> getAssociatedType()
   {
      return TYPE;
   }
   
   private ContentItem content_;
}

