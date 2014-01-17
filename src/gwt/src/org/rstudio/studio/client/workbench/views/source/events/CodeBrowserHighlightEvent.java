/*
 * CodeBrowserHighlightEvent.java
 *
 * Copyright (C) 2009-14 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.source.events;

import org.rstudio.core.client.DebugFilePosition;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class CodeBrowserHighlightEvent 
   extends GwtEvent<CodeBrowserHighlightEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onCodeBrowserHighlight(CodeBrowserHighlightEvent event);
   }

   public static final GwtEvent.Type<CodeBrowserHighlightEvent.Handler> TYPE =
      new GwtEvent.Type<CodeBrowserHighlightEvent.Handler>();
   
   public CodeBrowserHighlightEvent(DebugFilePosition debugPosition)
   {
      debugPosition_ = debugPosition;
   }
   
   public DebugFilePosition getDebugPosition()
   {
      return debugPosition_;
   }
   
   
   @Override
   protected void dispatch(CodeBrowserHighlightEvent.Handler handler)
   {
      handler.onCodeBrowserHighlight(this);
   }

   @Override
   public GwtEvent.Type<CodeBrowserHighlightEvent.Handler> getAssociatedType()
   {
      return TYPE;
   }

   final DebugFilePosition debugPosition_;
}
