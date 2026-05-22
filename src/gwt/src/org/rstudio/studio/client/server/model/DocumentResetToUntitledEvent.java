/*
 * DocumentResetToUntitledEvent.java
 *
 * Copyright (C) 2026 by Posit Software, PBC
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
package org.rstudio.studio.client.server.model;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

/**
 * Reset the source pane to exactly one untitled document, discarding any
 * unsaved changes in other tabs. If an untitled document is already open it
 * is kept; otherwise a fresh one is created before the others are closed, so
 * the source pane never transitions through the zero-tab (HIDE) state.
 */
public class DocumentResetToUntitledEvent extends GwtEvent<DocumentResetToUntitledEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onDocumentResetToUntitled(DocumentResetToUntitledEvent event);
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onDocumentResetToUntitled(this);
   }

   public static final Type<Handler> TYPE = new Type<>();
}
