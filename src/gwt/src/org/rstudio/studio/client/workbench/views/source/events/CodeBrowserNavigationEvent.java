/*
 * CodeBrowserNavigationEvent.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
import org.rstudio.studio.client.workbench.codesearch.model.SearchPathFunctionDefinition;

import com.google.gwt.event.shared.GwtEvent;

public class CodeBrowserNavigationEvent extends GwtEvent<CodeBrowserNavigationHandler>
{
   public static final GwtEvent.Type<CodeBrowserNavigationHandler> TYPE =
      new GwtEvent.Type<CodeBrowserNavigationHandler>();
   
   public CodeBrowserNavigationEvent(SearchPathFunctionDefinition function)
   {
      this(function, null, false);
   }
   
   public CodeBrowserNavigationEvent(SearchPathFunctionDefinition function,
                                     DebugFilePosition debugPosition,
                                     boolean executing)
   {
      function_ = function;
      debugPosition_ = debugPosition;
      executing_ = executing;
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
   
   @Override
   protected void dispatch(CodeBrowserNavigationHandler handler)
   {
      handler.onCodeBrowserNavigation(this);
   }

   @Override
   public GwtEvent.Type<CodeBrowserNavigationHandler> getAssociatedType()
   {
      return TYPE;
   }

   final DebugFilePosition debugPosition_;
   final SearchPathFunctionDefinition function_;
   final boolean executing_;
}

