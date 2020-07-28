/*
 * BuildOutputEvent.java
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
package org.rstudio.studio.client.workbench.views.buildtools.events;

import org.rstudio.studio.client.common.compile.CompileOutput;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class BuildOutputEvent extends GwtEvent<BuildOutputEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onBuildOutput(BuildOutputEvent event);
   }

   public BuildOutputEvent(CompileOutput output)
   {
      output_ = output;
   }

   public CompileOutput getOutput()
   {
      return output_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onBuildOutput(this);
   }

   private final CompileOutput output_;

   public static final Type<Handler> TYPE = new Type<>();
}
