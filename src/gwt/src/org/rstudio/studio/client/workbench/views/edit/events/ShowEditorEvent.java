/*
 * ShowEditorEvent.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.edit.events;

import com.google.gwt.event.shared.GwtEvent;

public class ShowEditorEvent extends GwtEvent<ShowEditorHandler>
{
   public static final GwtEvent.Type<ShowEditorHandler> TYPE =
      new GwtEvent.Type<ShowEditorHandler>();
   
   public ShowEditorEvent(String content)
   {
      content_ = content;
   }
   
   public String getContent()
   {
      return content_;
   }
   
   @Override
   protected void dispatch(ShowEditorHandler handler)
   {
      handler.onShowEditor(this);
   }

   @Override
   public GwtEvent.Type<ShowEditorHandler> getAssociatedType()
   {
      return TYPE;
   }
   
   private String content_;
}
