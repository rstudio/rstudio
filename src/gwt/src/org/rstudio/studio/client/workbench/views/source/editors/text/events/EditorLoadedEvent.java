/*
 * EditorLoadedEvent.java
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

import com.google.gwt.event.shared.GwtEvent;

import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceEditorNative;

public class EditorLoadedEvent extends GwtEvent<EditorLoadedHandler>
{
   public EditorLoadedEvent(AceEditorNative editor)
   {
      editor_ = editor;
   }
   
   public AceEditorNative getEditor()
   {
      return editor_;
   }
   
   private final AceEditorNative editor_;
   
   public static final Type<EditorLoadedHandler> TYPE = new Type<EditorLoadedHandler>();

   @Override
   public Type<EditorLoadedHandler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(EditorLoadedHandler handler)
   {
      handler.onEditorLoaded(this);
   }
}
