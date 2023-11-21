/*
 * CopilotEnabledEvent.java
 *
 * Copyright (C) 2023 by Posit Software, PBC
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
package org.rstudio.studio.client.workbench.prefs.views.events;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

// This dummy event is only used to communicate between the global Copilot
// preferences pane and the project Copilot preference pane, so they can
// notify each other of changes (for an improved UI experience)
public class CopilotEnabledEvent extends GwtEvent<CopilotEnabledEvent.Handler>
{
   public static class Data extends JavaScriptObject
   {
      protected Data()
      {
      }

      // Event data accessors ----
      
      
   }

   public CopilotEnabledEvent(boolean isEnabled, boolean isProject)
   {
      isEnabled_ = isEnabled;
      isProject_ = isProject;
   }

   public boolean isEnabled()
   {
      return isEnabled_;
   }
   
   public boolean isProject()
   {
      return isProject_;
   }

   private final boolean isEnabled_;
   private final boolean isProject_;

   // Boilerplate ----

   public interface Handler extends EventHandler
   {
      void onCopilotEnabled(CopilotEnabledEvent event);
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onCopilotEnabled(this);
   }

   public static final Type<Handler> TYPE = new Type<Handler>();
}

