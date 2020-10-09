/*
 * EditorThemeStyleChangedEvent.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.events;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class EditorThemeStyleChangedEvent extends GwtEvent<EditorThemeStyleChangedEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onEditorThemeStyleChanged(EditorThemeStyleChangedEvent event);
   }

   public EditorThemeStyleChangedEvent(Element editorContent, Style style)
   {
      style_ = style;
      editorContent_ = editorContent;
   }

   public Style getStyle()
   {
      return style_;
   }

   public Element getEditorContent()
   {
      return editorContent_;
   }

   private final Style style_;
   private final Element editorContent_;

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onEditorThemeStyleChanged(this);
   }

   public static final Type<Handler> TYPE = new Type<>();
}
