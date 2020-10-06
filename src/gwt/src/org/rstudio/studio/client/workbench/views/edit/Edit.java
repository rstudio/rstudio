/*
 * Edit.java
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
package org.rstudio.studio.client.workbench.views.edit;

import com.google.inject.Inject;
import org.rstudio.core.client.AsyncShim;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.studio.client.server.VoidServerRequestCallback;
import org.rstudio.studio.client.workbench.views.edit.events.ShowEditorEvent;
import org.rstudio.studio.client.workbench.views.edit.model.EditServerOperations;


public class Edit implements ShowEditorEvent.Handler
{
   public abstract static class Shim extends AsyncShim<Edit>
         implements ShowEditorEvent.Handler
   {
      public abstract void onShowEditor(ShowEditorEvent event);
   }

   public interface Display
   {
      void show(String text,
                boolean isRCode,
                boolean lineWrapping,
                ProgressOperationWithInput<String> operation);
   }

   @Inject
   public Edit(Display view,
               EditServerOperations server)
   {
      view_ = view;
      server_ = server;
   }

   public void onShowEditor(ShowEditorEvent event)
   {
      view_.show(event.getContent(),
                 event.isRCode(),
                 event.getLineWrapping(),
                 new ProgressOperationWithInput<String>() {

         public void execute(final String input,
                             final ProgressIndicator progress)
         {
            if (input != null)
            {
               progress.onProgress("Saving...");
               server_.editCompleted(input,
                                    new VoidServerRequestCallback(progress));
            }
            else
            {
               progress.onProgress("Cancelling...");
               server_.editCompleted(null,
                                    new VoidServerRequestCallback(progress));
            }
         }
      });

   }

   private final Display view_;
   private final EditServerOperations server_;
}
