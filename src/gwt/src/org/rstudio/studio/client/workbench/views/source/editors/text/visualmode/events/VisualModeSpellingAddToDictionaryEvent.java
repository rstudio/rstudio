/*
 * VisualModeSpellingAddToDictionaryEvent.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.visualmode.events;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class VisualModeSpellingAddToDictionaryEvent extends GwtEvent<VisualModeSpellingAddToDictionaryEvent.Handler>
{
   public VisualModeSpellingAddToDictionaryEvent(String word)
   {
      word_ = word;
   }

   public String getWord()
   {
      return word_;
   }

   private final String word_;

   // Boilerplate ----

   public interface Handler extends EventHandler
   {
      void onVisualModeSpellingAddToDictionary(VisualModeSpellingAddToDictionaryEvent event);
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onVisualModeSpellingAddToDictionary(this);
   }

   public static final Type<Handler> TYPE = new Type<>();
}
