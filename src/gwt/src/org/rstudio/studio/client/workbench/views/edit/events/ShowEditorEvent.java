/*
 * ShowEditorEvent.java
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
package org.rstudio.studio.client.workbench.views.edit.events;

import com.google.gwt.event.shared.EventHandler;
import org.rstudio.studio.client.workbench.views.edit.model.ShowEditorData;

import com.google.gwt.event.shared.GwtEvent;

public class ShowEditorEvent extends GwtEvent<ShowEditorEvent.Handler>
{
   public static final Type<Handler> TYPE = new Type<>();

   public interface Handler extends EventHandler
   {
      void onShowEditor(ShowEditorEvent event);
   }

   public ShowEditorEvent(ShowEditorData data)
   {
      content_ = data.getContent();
      isRCode_ = data.isRCode();
      lineWrapping_ = data.getLineWrapping();
   }

   public String getContent()
   {
      return content_;
   }

   public boolean isRCode()
   {
      return isRCode_;
   }

   public boolean getLineWrapping()
   {
      return lineWrapping_;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onShowEditor(this);
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   private final String content_;
   private final boolean isRCode_;
   private final boolean lineWrapping_;
}
