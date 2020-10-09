/*
 * SnippetsChangedEvent.java
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
package org.rstudio.studio.client.workbench.snippets.model;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class SnippetsChangedEvent extends GwtEvent<SnippetsChangedEvent.Handler>
{
   public static class Data extends JsArray<SnippetData>
   {
      protected Data() {}
   }

   public interface Handler extends EventHandler
   {
      void onSnippetsChanged(SnippetsChangedEvent event);
   }

   public SnippetsChangedEvent(Data data)
   {
      data_ = data;
   }

   public JsArray<SnippetData> getData()
   {
      return data_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onSnippetsChanged(this);
   }

   public static final Type<Handler> TYPE = new Type<>();

   private final Data data_;
}
