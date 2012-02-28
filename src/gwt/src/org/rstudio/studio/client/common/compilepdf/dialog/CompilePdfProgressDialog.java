/*
 * CompilePdfProgressDialog.java
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

/*   
 *   - Refactor ConsoleProgressDialog so we can share its UI
 *  
 *   - Satellite subscribes to the CompilePdfStarted event and
 *     shows the dialog when that happens -- the dialog then 
 *     subscribes to the Output, Errors, and Completed events
 * 
 *   - When hitting the Compile PDF button if a compile is already running
 *     then it is a no-op (reactivate the tab)
 * 
 *   - Attempting to close the satellite window while a compilation
 *     is actively running results in a prompt to terminate the compile
 */

package org.rstudio.studio.client.common.compilepdf.dialog;

import org.rstudio.core.client.HandlerRegistrations;
import org.rstudio.core.client.widget.ModalDialogBase;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;

import org.rstudio.studio.client.common.compilepdf.events.CompilePdfCompletedEvent;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

public class CompilePdfProgressDialog extends ModalDialogBase
   implements CompilePdfCompletedEvent.Handler
{
   public CompilePdfProgressDialog()
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      setText("Compile PDF Progress");
      
      addCancelButton().setText("Close");
      
      registrations_ = new HandlerRegistrations();
      registrations_.add(
            eventBus_.addHandler(CompilePdfCompletedEvent.TYPE, this));
   }
   
   @Inject
   void initialize(EventBus eventBus)
   {
      eventBus_ = eventBus;
   }

   @Override
   protected Widget createMainWidget()
   {
      Label label = new Label("Main Widget");
      label.setWidth("300px;");
      return label;
   }
   
   @Override
   public void onCompilePdfCompleted(CompilePdfCompletedEvent event)
   {
      closeDialog();
   }
   
   @Override
   protected void onUnload()
   {
      super.onUnload();
      registrations_.removeHandler();
   }
   
   private EventBus eventBus_;
   private HandlerRegistrations registrations_;
   

}
