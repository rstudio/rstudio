/*
 * InitialProgressDialog.java
 *
 * Copyright (C) 2022 by RStudio, PBC
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
package org.rstudio.studio.client.workbench.views.source.editors.text.spelling;

import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.user.client.Timer;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.workbench.views.source.ViewsSourceConstants;
import org.rstudio.studio.client.workbench.views.source.editors.text.spelling.CheckSpelling.ProgressDisplay;

public class InitialProgressDialog implements ProgressDisplay
{
   public InitialProgressDialog(int delayShowMs)
   {
      delayShowMs_ = delayShowMs;
      dialog_ = new MessageDialog(MessageDialog.INFO,
                                  constants_.checkSpelling(),
                                  constants_.spellCheckInProgress());
      cancel_ = dialog_.addButton(constants_.cancel(), ElementIds.DIALOG_CANCEL_BUTTON, (Operation) null, true, true);

      delayShowTimer_ = new Timer()
      {
         @Override
         public void run()
         {
            dialog_.showModal();
         }
      };
   }

   @Override
   public void show()
   {
      if (running_)
         return;

      running_ = true;
      delayShowTimer_.schedule(delayShowMs_);
   }

   @Override
   public void hide()
   {
      if (running_)
      {
         delayShowTimer_.cancel();
         if (dialog_.isShowing())
            dialog_.closeDialog();
      }
   }

   @Override
   public boolean isShowing()
   {
      return running_;
   }

   @Override
   public HasClickHandlers getCancelButton()
   {
      return cancel_;
   }

   private final MessageDialog dialog_;
   private ThemedButton cancel_;
   private final Timer delayShowTimer_;
   private final int delayShowMs_;
   private boolean running_;
   private static final ViewsSourceConstants constants_ = GWT.create(ViewsSourceConstants.class);
}
