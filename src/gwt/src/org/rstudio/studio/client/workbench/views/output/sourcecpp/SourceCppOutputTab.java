/*
 * SourceCppOutputTab.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.output.sourcecpp;

import com.google.inject.Inject;

import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.ui.DelayLoadTabShim;
import org.rstudio.studio.client.workbench.ui.DelayLoadWorkbenchTab;

import org.rstudio.studio.client.workbench.views.output.sourcecpp.events.SourceCppCompletedEvent;
import org.rstudio.studio.client.workbench.views.output.sourcecpp.events.SourceCppStartedEvent;

public class SourceCppOutputTab extends DelayLoadWorkbenchTab<SourceCppOutputPresenter>
{
   public abstract static class Shim extends
                DelayLoadTabShim<SourceCppOutputPresenter, SourceCppOutputTab>
      implements SourceCppStartedEvent.Handler,
                 SourceCppCompletedEvent.Handler
   {
   }

   @Inject
   public SourceCppOutputTab(Shim shim, EventBus events)
   {
      super("Source Cpp", shim);
      events.addHandler(SourceCppStartedEvent.TYPE, shim);
      events.addHandler(SourceCppCompletedEvent.TYPE, shim);
   }

   @Override
   public boolean closeable()
   {
      return true;
   }
}
