/*
 * GitExpandDiffEvent.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
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
package org.rstudio.studio.client.workbench.views.vcs.git;

import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;

public class GitExpandDiffEvent extends GwtEvent<GitExpandDiffEvent.Handler>
{
   public interface Handler extends EventHandler
   {
      void onExpandDiff(GitExpandDiffEvent event);
   }

   public GitExpandDiffEvent(int lineNumber)
   {
      lineNumber_ = lineNumber;
   }

   public int getLineNumber()
   {
      return lineNumber_;
   }

   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler handler)
   {
      handler.onExpandDiff(this);
   }

   public static final Type<Handler> TYPE = new Type<>();
   
   private int lineNumber_;
}
