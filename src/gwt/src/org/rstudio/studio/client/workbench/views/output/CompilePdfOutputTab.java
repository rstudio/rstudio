/*
 * CompilePdfOutputTab.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.output;

import com.google.inject.Inject;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.events.CompilePdfOutputEvent;
import org.rstudio.studio.client.workbench.ui.DelayLoadTabShim;
import org.rstudio.studio.client.workbench.ui.DelayLoadWorkbenchTab;

public class CompilePdfOutputTab extends DelayLoadWorkbenchTab<CompilePdfOutputPresenter>
{
   public abstract static class Shim extends
                DelayLoadTabShim<CompilePdfOutputPresenter, CompilePdfOutputTab>
      implements CompilePdfOutputEvent.Handler
   {
   }

   @Inject
   public CompilePdfOutputTab(Shim shim,
                              EventBus events)
   {
      super("Compile PDF", shim);

      events.addHandler(CompilePdfOutputEvent.TYPE, shim);
   }

   @Override
   public boolean closeable()
   {
      return true;
   }
}
