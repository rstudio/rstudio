/*
 * UpdateGutterMarkersEvent.java
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
package org.rstudio.studio.client.workbench.views.output.lint.events;

import org.rstudio.studio.client.workbench.views.output.lint.model.LintItem;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class LintEvent extends GwtEvent<LintEvent.Handler>
{
   static public class Data extends JavaScriptObject
   {
      protected Data()
      {
      }

      public native final JsArray<LintItem> getLint() /*-{
         return this["lint"];
      }-*/;

      public native final String getDocumentId() /*-{
         return this["document_id"];
      }-*/;

   }

   public interface Handler extends EventHandler
   {
      void onLintEvent(LintEvent event);
   }

   public LintEvent(Data data)
   {
      data_ = data;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onLintEvent(this);
   }

   public JsArray<LintItem> getLint()
   {
      return data_.getLint();
   }

   public String getDocumentId()
   {
      return data_.getDocumentId();
   }

   public static final Type<Handler> TYPE = new Type<>();

   private final Data data_;
}
