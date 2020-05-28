/*
 * MessageDialog.java
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
package org.rstudio.core.client.widget;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.user.client.ui.*;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.widget.images.MessageDialogImages;

public class MessageDialog extends ModalDialogBase
{
   public final static int INFO = 1;
   public final static int WARNING = 2;
   public final static int ERROR = 3;
   public final static int QUESTION = 4;
   public final static int POPUP_BLOCKED = 0;

   public MessageDialog(int type, String caption, String message)
   {
      this(type, caption, labelForMessage(message));
   }

   public MessageDialog(int type, String caption, Widget message)
   {
      super(Roles.getAlertdialogRole());
      type_ = type;
      setText(caption);
      messageWidget_ = message;
      setButtonAlignment(HasHorizontalAlignment.ALIGN_CENTER);

      // read the message when dialog is shown
      setARIADescribedBy(messageWidget_.getElement());
   }

   public ThemedButton addButton(String label,
                                 String elementId,
                                 final Operation operation,
                                 boolean isDefault,
                                 boolean isCancel)
   {
      ThemedButton button = new ThemedButton(label, clickEvent ->
      {
         if (operation != null)
            operation.execute();
         closeDialog();
      });

      addButton(button, elementId, isDefault, isCancel);

      return button;
   }

   public ThemedButton addButton(String label,
                                 String elementId,
                                 final ProgressOperation operation,
                                 boolean isDefault,
                                 boolean isCancel)
   {
      if (operation != null && progress_ == null)
         progress_ = addProgressIndicator();

      ThemedButton button = new ThemedButton(label, clickEvent ->
      {
         if (operation != null)
            operation.execute(progress_);
         else
            closeDialog();
      });

      addButton(button, elementId, isDefault, isCancel);

      return button;
   }

   private void addButton(ThemedButton button,
                          String elementId,
                          boolean isDefault,
                          boolean isCancel)
   {
      if (isDefault)
         addOkButton(button, elementId);
      else if (isCancel)
         addCancelButton(button, elementId);
      else
         addButton(button, elementId);
   }

   @Override
   protected Widget createMainWidget()
   {
      HorizontalPanel horizontalPanel = new HorizontalPanel();
      horizontalPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_TOP);

      // add image
      MessageDialogImages images = MessageDialogImages.INSTANCE;
      Image image = null;
      String imageText = null;
      switch(type_)
      {
      case INFO:
         image = new Image(new ImageResource2x(images.dialog_info2x()));
         imageText = MessageDialogImages.DIALOG_INFO_TEXT;
         break;
      case WARNING:
         image = new Image(new ImageResource2x(images.dialog_warning2x()));
         imageText = MessageDialogImages.DIALOG_WARNING_TEXT;
         break;
      case ERROR:
         image = new Image(new ImageResource2x(images.dialog_error2x()));
         imageText = MessageDialogImages.DIALOG_ERROR_TEXT;
         break;
      case QUESTION:
         image = new Image(new ImageResource2x(images.dialog_question2x()));
         imageText = MessageDialogImages.DIALOG_QUESTION_TEXT;
         break;
      case POPUP_BLOCKED:
         image = new Image(new ImageResource2x(images.dialog_popup_blocked2x()));
         imageText = MessageDialogImages.DIALOG_POPUP_BLOCKED_TEXT;
         break;
      }
      horizontalPanel.add(image);
      if (image != null)
         image.setAltText(imageText);

      // add message widget
      horizontalPanel.add(messageWidget_);

      return horizontalPanel;
   }

   public static Label labelForMessage(String message)
   {
      Label label = new MultiLineLabel(StringUtil.notNull(message));
      label.setStylePrimaryName(ThemeResources.INSTANCE.themeStyles().dialogMessage());
      return label;
   }

   @Override
   protected void focusInitialControl()
   {
      focusOkButton();
   }

   private final int type_;
   private final Widget messageWidget_;
   private ProgressIndicator progress_;
}
