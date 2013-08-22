/*
 * ContextDepthChangedEvent.java
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
package org.rstudio.studio.client.workbench.views.environment.events;

import org.rstudio.studio.client.workbench.views.environment.model.CallFrame;
import org.rstudio.studio.client.workbench.views.environment.model.EnvironmentContextData;
import org.rstudio.studio.client.workbench.views.environment.model.RObject;

import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class ContextDepthChangedEvent extends
      GwtEvent<ContextDepthChangedEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onContextDepthChanged(ContextDepthChangedEvent event);
   }

   public ContextDepthChangedEvent(EnvironmentContextData data)
   {
      contextData_ = data;
   }
   
   public int getContextDepth() 
   {
      return contextData_.contextDepth();
   }
   
   public JsArray<RObject> getEnvironmentList()
   {
      return contextData_.environmentList();
   }

   public JsArray<CallFrame> getCallFrames()
   {
      return contextData_.callFrames();
   }

   public String getFunctionName()
   {
      return contextData_.functionName();
   }
   
   public String getFunctionCode()
   {
      return contextData_.functionCode();
   }
   
   public boolean useProvidedSource()
   {
      return contextData_.useProvidedSource();
   }
   
   public String getEnvironmentName()
   {
      return contextData_.environmentName();
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onContextDepthChanged(this);
   }
  
   public static final Type<Handler> TYPE = new Type<Handler>();
   private final EnvironmentContextData contextData_;
}
