/*
 * TextEditingTargetJSHelper.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.MessageDisplay;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.common.PreviewResult;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.workbench.views.console.events.SendToConsoleEvent;
import org.rstudio.studio.client.workbench.views.source.editors.EditingTarget;
import org.rstudio.studio.client.workbench.views.source.model.SourceServerOperations;

import com.google.gwt.core.client.GWT;
import com.google.inject.Inject;

public class TextEditingTargetJSHelper
{
   public TextEditingTargetJSHelper(DocDisplay docDisplay)
   {
      docDisplay_ = docDisplay;
      RStudioGinjector.INSTANCE.injectMembers(this);
   }

   @Inject
   void initialize(GlobalDisplay display, EventBus eventBus, SourceServerOperations server)
   {
      display_ = display;
      eventBus_ = eventBus;
      server_ = server;
   }


   public boolean previewJS(EditingTarget editingTarget)
   {
      TextEditingTargetCommentHeaderHelper previewSource = new TextEditingTargetCommentHeaderHelper(
         docDisplay_.getCode(),
         "preview",
         "//"
      );

      if (!previewSource.hasCommentHeader())
         return false;

      if (!previewSource.getFunction().equals("r2d3"))
      {
         display_.showErrorMessage(
                        constants_.previewJSErrorCaption(),
                        constants_.previewJSErrorMessage(previewSource.getFunction()));
      }
      else
      {
         previewSource.setFunction("r2d3::r2d3");

         previewSource.buildCommand(
            editingTarget.getPath(),
            new OperationWithInput<String>()
            {
               @Override
               public void execute(String command)
               {
                  doPreviewJS(command, false);
               }
            }
         );
      }

      return true;
   }

   // Ask the backend whether the built r2d3 command is safe to run. The
   // command comes from the file's '!preview' header, so an untrusted file
   // could otherwise smuggle arbitrary R into the console. The backend
   // requires the command to be a single statement and classifies it; when it
   // is not statically safe the backend returns a "confirm" result; we prompt
   // the user and only run it (in the console, where r2d3 renders the widget)
   // once they consent.
   private void doPreviewJS(final String command, boolean allowUnsafe)
   {
      server_.previewR2d3(command, allowUnsafe, new ServerRequestCallback<PreviewResult>()
      {
         @Override
         public void onResponseReceived(PreviewResult result)
         {
            if (result == null)
               return;

            if (result.isOk())
            {
               eventBus_.fireEvent(new SendToConsoleEvent(command, true));
               return;
            }

            if (result.isConfirm())
            {
               display_.showYesNoMessage(
                     MessageDisplay.MSG_WARNING,
                     constants_.previewRunCodeConfirmCaption(),
                     constants_.previewRunCodeConfirmMessage(result.getExpression()),
                     new Operation()
                     {
                        @Override
                        public void execute()
                        {
                           doPreviewJS(command, true);
                        }
                     },
                     false);
               return;
            }

            if (!StringUtil.isNullOrEmpty(result.getMessage()))
            {
               display_.showErrorMessage(
                     constants_.previewJSErrorCaption(),
                     result.getMessage());
            }
            else
            {
               // an unexpected (or empty) action with no message; log it
               // rather than silently doing nothing
               Debug.log("Unexpected r2d3 preview result action: '" +
                         result.getAction() + "'");
            }
         }

         @Override
         public void onError(ServerError error)
         {
            Debug.logError(error);
         }
      });
   }

   private GlobalDisplay display_;
   private EventBus eventBus_;
   private SourceServerOperations server_;
   private DocDisplay docDisplay_;
   private static final EditorsTextConstants constants_ = GWT.create(EditorsTextConstants.class);
}
