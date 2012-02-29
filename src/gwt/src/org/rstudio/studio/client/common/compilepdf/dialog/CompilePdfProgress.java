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

import org.rstudio.core.client.CodeNavigationTarget;
import org.rstudio.core.client.events.SelectionCommitEvent;
import org.rstudio.core.client.events.SelectionCommitHandler;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.compilepdf.events.CompilePdfStartedEvent;
import org.rstudio.studio.client.common.filetypes.FileTypeRegistry;
import org.rstudio.studio.client.workbench.model.Session;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class CompilePdfProgress
{
   @Inject
   public CompilePdfProgress(EventBus eventBus,
                             Session session,
                             final FileTypeRegistry fileTypeRegistry)
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
               final CompilePdfProgressDialog compilePdfDialog 
                                    = new CompilePdfProgressDialog();
               
               
               compilePdfDialog.addClickHandler(new ClickHandler() {

                  @Override
                  public void onClick(ClickEvent event)
                  {
                      compilePdfDialog.closeDialog();
                     
                  }
                  
               });
               
               compilePdfDialog.addSelectionCommitHandler(
                           new SelectionCommitHandler<CodeNavigationTarget>() {

                  @Override
                  public void onSelectionCommit(
                        SelectionCommitEvent<CodeNavigationTarget> event)
                  {
                     CodeNavigationTarget target = event.getSelectedItem();
                     fileTypeRegistry.editFile(
                              FileSystemItem.createFile(target.getFile()), 
                              target.getPosition());
                     compilePdfDialog.closeDialog();
                  }
                  
               });
               
               compilePdfDialog.showModal();
            }
         });
         */
      }
   }
}
