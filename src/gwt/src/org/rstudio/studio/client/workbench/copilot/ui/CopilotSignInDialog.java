/*
 * CopilotSignInDialog.java
 *
 * Copyright (C) 2023 by Posit Software, PBC
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
package org.rstudio.studio.client.workbench.copilot.ui;

import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.widget.ElementPanel;
import org.rstudio.core.client.widget.ModalDialogBase;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.images.MessageDialogImages;
import org.rstudio.studio.client.workbench.copilot.CopilotUIConstants;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.AnchorElement;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.DivElement;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;

public class CopilotSignInDialog extends ModalDialogBase
{

   private static CopilotSignInDialogUiBinder uiBinder =
         GWT.create(CopilotSignInDialogUiBinder.class);

   interface CopilotSignInDialogUiBinder extends UiBinder<DivElement, CopilotSignInDialog>
   {
   }

   public CopilotSignInDialog(String verificationUri,
                              String verificationCode)
   {
      super(Roles.getDialogRole());
      setText(constants_.copilotSignInDialogTitle());

      ui_ = uiBinder.createAndBindUi(this);
      progress_ = addProgressIndicator(true, false);
      verificationUri_.setInnerText(verificationUri);
      verificationUri_.setHref(verificationUri);
      verificationCode_.setInnerText(verificationCode);
      verificationCode_.getStyle().setProperty("userSelect", "all");
      
      Event.sinkEvents(verificationUri_, Event.ONCLICK | Event.ONMOUSEUP | Event.ONKEYDOWN);
      Event.setEventListener(verificationUri_, (event) ->
      {
         if (BrowserEvents.KEYDOWN.equals(event.getType()) && event.getKeyCode() != KeyCodes.KEY_ENTER)
            return;
         
         if (!progressShowing_)
         {
            progressShowing_ = true;
            progress_.onProgress(constants_.copilotAuthenticating());
         }
      });

      addCancelButton();
   }
   
   @Override
   protected Widget createMainWidget()
   {
      HorizontalPanel panel = new HorizontalPanel();
      panel.add(new Image(new ImageResource2x(MessageDialogImages.INSTANCE.dialog_info2x())));
      panel.add(new ElementPanel(ui_));
      return panel;
   }

   @UiField AnchorElement verificationUri_;
   @UiField SpanElement verificationCode_;
   
   private boolean progressShowing_ = false;
   private final DivElement ui_;
   private final ProgressIndicator progress_;

   private static final CopilotUIConstants constants_ = GWT.create(CopilotUIConstants.class);
}
