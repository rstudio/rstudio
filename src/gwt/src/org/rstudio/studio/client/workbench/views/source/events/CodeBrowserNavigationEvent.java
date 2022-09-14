/*
 * CodeBrowserNavigationEvent.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.source.events;

import com.google.gwt.event.shared.EventHandler;
import org.rstudio.core.client.DebugFilePosition;
import org.rstudio.core.client.js.JavaScriptSerializable;
import org.rstudio.studio.client.application.events.CrossWindowEvent;
import org.rstudio.studio.client.workbench.codesearch.model.SearchPathFunctionDefinition;


@JavaScriptSerializable
public class CodeBrowserNavigationEvent
             extends CrossWindowEvent<CodeBrowserNavigationEvent.Handler>
{
   public static final Type<Handler> TYPE = new Type<>();

   public interface Handler extends EventHandler
   {
      void onCodeBrowserNavigation(CodeBrowserNavigationEvent event);
   }

   public CodeBrowserNavigationEvent()
   {
      this(null);
   }

   public CodeBrowserNavigationEvent(SearchPathFunctionDefinition function)
   {
      this(function, null, false, false);
   }

   public CodeBrowserNavigationEvent(SearchPathFunctionDefinition function,
                                     DebugFilePosition debugPosition,
                                     boolean executing,
                                     boolean serverDispatched)
   {
      function_ = function;
      debugPosition_ = debugPosition;
      executing_ = executing;
      serverDispatched_ = serverDispatched;
   }

   public SearchPathFunctionDefinition getFunction()
   {
      return function_;
   }

   public DebugFilePosition getDebugPosition()
   {
      return debugPosition_;
   }

   public boolean getExecuting()
   {
      return executing_;
   }

   public boolean serverDispatched()
   {
      return serverDispatched_;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onCodeBrowserNavigation(this);
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   public boolean forward()
   {
      return false;
   }

   DebugFilePosition debugPosition_;
   SearchPathFunctionDefinition function_;
   boolean executing_;
   boolean serverDispatched_;
}

