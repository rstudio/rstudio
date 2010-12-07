/*
 * EditorFocusedEvent.java
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
package org.rstudio.codemirror.client.events;

import com.google.gwt.event.shared.GwtEvent;

public class EditorFocusedEvent extends GwtEvent<EditorFocusedHandler>
{
   public static Type<EditorFocusedHandler> TYPE =
         new Type<EditorFocusedHandler>();

   public EditorFocusedEvent()
   {
   }

   @Override
   public Type<EditorFocusedHandler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(EditorFocusedHandler handler)
   {
      handler.onEditorFocused(this);
   }
}
