/*
 * HTMLPreviewProgressDialog.java
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
package org.rstudio.studio.client.htmlpreview.ui;


import com.google.gwt.aria.client.Roles;
import org.rstudio.core.client.widget.ProgressDialog;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.AriaLiveService;
import org.rstudio.studio.client.application.events.AriaLiveStatusEvent.Severity;
import org.rstudio.studio.client.application.events.AriaLiveStatusEvent.Timing;
import org.rstudio.studio.client.common.compile.CompileOutputBuffer;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.Widget;


public class HTMLPreviewProgressDialog extends ProgressDialog
                                       implements HasClickHandlers

{
   public HTMLPreviewProgressDialog(String caption)
   {
      this(caption, -1);
   }

   public HTMLPreviewProgressDialog(String caption, int maxHeight)
   {
      super(caption, Roles.getDialogRole(), maxHeight);
   }

   @Override
   public HandlerRegistration addClickHandler(ClickHandler handler)
   {
      return stopButton().addClickHandler(handler);
   }

   public void setCaption(String caption)
   {
      setLabel(caption);
   }

   public void showOutput(String output)
   {
      if (!isShowing())
         showModal();

      output_.append(output);
   }

   public void stopProgress()
   {
      hideProgress();
      stopButton().setText("Close");
   }

   public void dismiss()
   {
      closeDialog();
   }

   @Override
   protected Widget createDisplayWidget(Object param)
   {
      SimplePanel panel = new SimplePanel();
      int height = Window.getClientHeight() - 150;
      int maxHeight = (Integer)param;
      if (maxHeight != -1)
         height = Math.min(maxHeight, height);
      panel.getElement().getStyle().setHeight(height, Unit.PX);

      output_ = new CompileOutputBuffer();
      panel.setWidget(output_);
      return panel;
   }

   @Override
   protected void announceCompletion(String message)
   {
      RStudioGinjector.INSTANCE.getAriaLiveService().announce(
            AriaLiveService.PROGRESS_COMPLETION, message, Timing.IMMEDIATE, Severity.STATUS);
   }

   private CompileOutputBuffer output_;
}
