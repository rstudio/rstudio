/*
 * CompilePdfOutputPresenter.java
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
package org.rstudio.studio.client.workbench.views.output.compilepdf;

import com.google.inject.Inject;
import org.rstudio.core.client.events.HasEnsureHiddenHandlers;
import org.rstudio.studio.client.workbench.WorkbenchView;
import org.rstudio.studio.client.workbench.events.CompilePdfOutputEvent;
import org.rstudio.studio.client.workbench.views.BasePresenter;

public class CompilePdfOutputPresenter extends BasePresenter
   implements CompilePdfOutputEvent.Handler
{
   public interface Display extends WorkbenchView, HasEnsureHiddenHandlers
   {

   }

   @Inject
   public CompilePdfOutputPresenter(Display view)
   {
      super(view);
      view_ = view;
   }

   @Override
   public void onCompilePdfOutput(CompilePdfOutputEvent event)
   {
      view_.bringToFront();
   }

   private final Display view_;
}
