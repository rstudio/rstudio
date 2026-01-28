/*
 * DocSelectionChangedEvent.java
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

package org.rstudio.studio.client.workbench.views.source.events;

import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

/**
 * Event fired when the selection changes in a document editor.
 * Used to notify the chat backend of the current selection so it can
 * provide context-aware assistance.
 */
public class DocSelectionChangedEvent extends GwtEvent<DocSelectionChangedEvent.Handler>
{
   /**
    * Helper class to create selection JSON objects in the format expected by the Chat backend.
    * Format: { startLine, startCharacter, endLine, endCharacter, text }
    */
   public static class Selection extends JavaScriptObject
   {
      protected Selection() {}

      public static native Selection create(Range range, String text) /*-{
         return {
            "startLine": range.start.row,
            "startCharacter": range.start.column,
            "endLine": range.end.row,
            "endCharacter": range.end.column,
            "text": text || ""
         };
      }-*/;
   }

   public interface Handler extends EventHandler
   {
      void onDocSelectionChanged(DocSelectionChangedEvent event);
   }

   public static final GwtEvent.Type<DocSelectionChangedEvent.Handler> TYPE = new GwtEvent.Type<>();

   public DocSelectionChangedEvent(String id, JsArray<JavaScriptObject> selections)
   {
      id_ = id;
      selections_ = selections;
   }

   public String getId()
   {
      return id_;
   }

   public JsArray<JavaScriptObject> getSelections()
   {
      return selections_;
   }

   @Override
   protected void dispatch(DocSelectionChangedEvent.Handler handler)
   {
      handler.onDocSelectionChanged(this);
   }

   @Override
   public GwtEvent.Type<DocSelectionChangedEvent.Handler> getAssociatedType()
   {
      return TYPE;
   }

   private final String id_;
   private final JsArray<JavaScriptObject> selections_;
}
