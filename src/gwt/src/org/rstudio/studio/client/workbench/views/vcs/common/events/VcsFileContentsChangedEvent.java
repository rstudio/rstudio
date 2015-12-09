/*
 * VcsFileContentsChangedEvent.java
 *
 * Copyright (C) 2009-15 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.vcs.common.events;

import java.util.ArrayList;

import org.rstudio.core.client.JsArrayUtil;
import org.rstudio.core.client.js.JavaScriptSerializable;
import org.rstudio.studio.client.application.events.CrossWindowEvent;

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.event.shared.EventHandler;

@JavaScriptSerializable
public class VcsFileContentsChangedEvent 
             extends CrossWindowEvent<VcsFileContentsChangedEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onVcsFileContentsChanged(VcsFileContentsChangedEvent event);
   }
   
   public VcsFileContentsChangedEvent()
   {
   }

   public VcsFileContentsChangedEvent(ArrayList<String> paths)
   {
      paths_ = JsArrayUtil.toJsArrayString(paths);
   }
   
   public JsArrayString getPaths()
   {
      return paths_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onVcsFileContentsChanged(this);
   }

   public static final Type<Handler> TYPE = new Type<Handler>();
   
   private JsArrayString paths_;
}
