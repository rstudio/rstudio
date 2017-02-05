/*
 * TerminalSubprocEvent.java
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.terminal.events;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

/**
 * Sent when terminal shell program has (or doesn't have) subprocesses.
 * Terminals can be killed via session suspend when they are all at the
 * command-prompt, but not if any are running programs.
 */
public class TerminalSubprocEvent extends GwtEvent<TerminalSubprocEvent.Handler>
{
   public static class Data extends JavaScriptObject
   {
      protected Data()
      {
      }

      /**
       * @return Terminal handle
       */
      public final native String getHandle() /*-{
            return this.handle;
      }-*/;

      /**
       * @return true if terminal has sub-processes.
       */
      public final native boolean hasSubprocs() /*-{
            return this.subprocs;
      }-*/;
   }

   public interface Handler extends EventHandler
   {
      void onTerminalSubprocs(TerminalSubprocEvent event);
   }

   public TerminalSubprocEvent(Data data)
   {
      data_ = data;
   }

   public String getHandle()
   {
      return data_.getHandle();
   }

   public boolean hasSubprocs()
   {
      return data_.hasSubprocs();
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onTerminalSubprocs(this);
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   private final Data data_;

   public static final Type<Handler> TYPE = new Type<Handler>();
}
