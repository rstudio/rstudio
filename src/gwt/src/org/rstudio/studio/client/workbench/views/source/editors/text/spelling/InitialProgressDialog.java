/*
 * InitialProgressDialog.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.spelling;

import com.google.gwt.event.dom.client.HasClickHandlers;
import org.rstudio.core.client.widget.MessageDialog;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.workbench.views.source.editors.text.spelling.CheckSpelling.ProgressDisplay;

public class InitialProgressDialog implements ProgressDisplay
{
   public InitialProgressDialog()
   {
      dialog_ = new MessageDialog(MessageDialog.INFO,
                                  "Check Spelling",
                                  "Spell check in progress...");
      cancel_ = dialog_.addButton("Cancel", (Operation) null, true, true);
   }

   @Override
   public void show()
   {
      dialog_.showModal();
   }

   @Override
   public void hide()
   {
      dialog_.closeDialog();
   }

   @Override
   public boolean isShowing()
   {
      return dialog_.isShowing();
   }

   @Override
   public HasClickHandlers getCancelButton()
   {
      return cancel_;
   }

   private final MessageDialog dialog_;
   private ThemedButton cancel_;
}
