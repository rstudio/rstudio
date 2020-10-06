/*
 * CompilePdfErrorsEvent.java
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
package org.rstudio.studio.client.common.compilepdf.events;

import org.rstudio.studio.client.common.sourcemarkers.SourceMarker;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class CompilePdfErrorsEvent extends GwtEvent<CompilePdfErrorsEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onCompilePdfErrors(CompilePdfErrorsEvent event);
   }

   public CompilePdfErrorsEvent(JsArray<SourceMarker> errors)
   {
      errors_ = errors;
   }

   public JsArray<SourceMarker> getErrors()
   {
      return errors_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onCompilePdfErrors(this);
   }

   private JsArray<SourceMarker> errors_;

   public static final Type<Handler> TYPE = new Type<>();
}
