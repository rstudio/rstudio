/*
 * EditorThemeChangedEvent.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.source.editors.text.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

import org.rstudio.studio.client.workbench.views.source.editors.text.themes.AceTheme;

public class EditorThemeChangedEvent extends GwtEvent<EditorThemeChangedEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onEditorThemeChanged(EditorThemeChangedEvent event);
   }

   public EditorThemeChangedEvent(AceTheme theme)
   {
      theme_ = theme;
   }

   public AceTheme getTheme()
   {
      return theme_;
   }

   private final AceTheme theme_;

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onEditorThemeChanged(this);
   }

   public static final Type<Handler> TYPE = new Type<>();
}
