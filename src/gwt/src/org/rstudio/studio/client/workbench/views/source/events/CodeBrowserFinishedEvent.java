/*
 * CodeBrowserFinishedEvent.java
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

package org.rstudio.studio.client.workbench.views.source.events;


import org.rstudio.core.client.js.JavaScriptSerializable;
import org.rstudio.studio.client.application.events.CrossWindowEvent;
import org.rstudio.studio.client.workbench.codesearch.model.SearchPathFunctionDefinition;

import com.google.gwt.event.shared.GwtEvent;

@JavaScriptSerializable
public class CodeBrowserFinishedEvent extends 
             CrossWindowEvent<CodeBrowserFinishedHandler>
{
   public static final GwtEvent.Type<CodeBrowserFinishedHandler> TYPE =
      new GwtEvent.Type<CodeBrowserFinishedHandler>();
   
   public CodeBrowserFinishedEvent()
   {
      this(null);
   }
   
   public CodeBrowserFinishedEvent(SearchPathFunctionDefinition function)
   {
      function_ = function;
   }
   
   public SearchPathFunctionDefinition getFunction()
   {
      return function_;
   }
   
   @Override
   protected void dispatch(CodeBrowserFinishedHandler handler)
   {
      handler.onCodeBrowserFinished(this);
   }

   @Override
   public GwtEvent.Type<CodeBrowserFinishedHandler> getAssociatedType()
   {
      return TYPE;
   }
   
   @Override
   public boolean forward()
   {
      return false;
   }
   
   private SearchPathFunctionDefinition function_;
}
