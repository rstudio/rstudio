/*
 * CompilePdfProgress.java
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
package org.rstudio.studio.client.common.compilepdf.dialog;

import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.workbench.model.Session;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class CompilePdfProgress
{
   @Inject
   public CompilePdfProgress(EventBus eventBus,
                             Session session)
   {
      if (session.getSessionInfo().isInternalPdfPreviewEnabled())
      {
         /*
         eventBus.addHandler(CompilePdfStartedEvent.TYPE, 
                             new CompilePdfStartedEvent.Handler()
         {
            @Override
            public void onCompilePdfStarted(CompilePdfStartedEvent event)
            {
               new CompilePdfProgressDialog().showModal();
            }
         });
         */
      }
   }
}
