/*
 * SourceWindowUnloadingEvent.java
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

import org.rstudio.core.client.js.JavaScriptSerializable;
import org.rstudio.studio.client.application.events.CrossWindowEvent;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

// Fired to the main window by a source window that is unloading without
// having been able to warn about its unsaved documents (the browser withholds
// the beforeunload prompt from a window the user never interacted with). The
// main window keeps the listed documents open if the window turns out to be
// closing rather than reloading.
@JavaScriptSerializable
public class SourceWindowUnloadingEvent
             extends CrossWindowEvent<SourceWindowUnloadingEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onSourceWindowUnloading(SourceWindowUnloadingEvent event);
   }

   // An unsaved document; 'contents' carries edits the window had not yet
   // backed up to the server, and is null when the server copy is current.
   public static class UnsavedDoc extends JavaScriptObject
   {
      protected UnsavedDoc() {}

      public static final native UnsavedDoc create(String id, String contents) /*-{
         return { id: id, contents: contents };
      }-*/;

      public final native String getId() /*-{ return this.id; }-*/;
      public final native String getContents() /*-{ return this.contents; }-*/;
   }

   public static final GwtEvent.Type<SourceWindowUnloadingEvent.Handler> TYPE = new GwtEvent.Type<>();

   public SourceWindowUnloadingEvent()
   {
   }

   public SourceWindowUnloadingEvent(JsArray<UnsavedDoc> unsavedDocs)
   {
      unsavedDocs_ = unsavedDocs;
   }

   public JsArray<UnsavedDoc> getUnsavedDocs()
   {
      return unsavedDocs_;
   }

   @Override
   protected void dispatch(SourceWindowUnloadingEvent.Handler handler)
   {
      handler.onSourceWindowUnloading(this);
   }

   @Override
   public GwtEvent.Type<SourceWindowUnloadingEvent.Handler> getAssociatedType()
   {
      return TYPE;
   }

   private JsArray<UnsavedDoc> unsavedDocs_;
}
