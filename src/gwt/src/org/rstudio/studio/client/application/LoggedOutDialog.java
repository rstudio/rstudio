/*
 * LoggedOutDialog.java
 *
 * Copyright (C) 2024 by Posit Software, PBC
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
package org.rstudio.studio.client.application;

import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.widget.ModalDialogBase;
import org.rstudio.core.client.widget.MultiLineLabel;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.core.client.widget.images.MessageDialogImages;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public class LoggedOutDialog extends ModalDialogBase 
{

   private static final LoggedOutDialog INSTANCE = new LoggedOutDialog();

   public static void setUnauthorized() 
   {
      INSTANCE.doSetUnauthorized();
   }

   public static void setAuthorized() 
   {
      INSTANCE.doSetAuthorized();
   }

   protected LoggedOutDialog() 
   {
      super(Roles.getAlertdialogRole());

      setText("Posit Workbench Login Required");
      addActionButton(createLoginButton());
      setGlassEnabled(true);
   }

   @Override
   protected Widget createMainWidget() 
   {
      HorizontalPanel horizontalPanel = new HorizontalPanel();
      horizontalPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_TOP);


      MessageDialogImages images = MessageDialogImages.INSTANCE;
      Image image = new Image(new ImageResource2x(images.dialog_info2x()));;
      String imageText = MessageDialogImages.DIALOG_INFO_TEXT;


      horizontalPanel.add(image);
      if (image != null)
         image.setAltText(imageText);


      Label label = new MultiLineLabel("Login expired or signed out from another window.\nSelect 'Login' for a new login tab. Return here to resume session.");
      label.setStylePrimaryName(ThemeResources.INSTANCE.themeStyles().dialogMessage());
      horizontalPanel.add(label);

      return horizontalPanel;
   }

   private ThemedButton createLoginButton() 
   {
      ThemedButton loginButton = new ThemedButton("Login", clickEvent -> 
      {
         String url = ApplicationUtils.getHostPageBaseURLWithoutContext(true) + "auth-sign-in";
         Window.open(url, "_blank", "");
      });
      return loginButton;
   }

   private void doSetUnauthorized() 
   {
      if (!visible_) {
         visible_ = true;
         showModal();
      }
   }

   private void doSetAuthorized() 
   {
      if (visible_) {
         visible_ = false;
         hide();
      }
   }

   private boolean visible_;
}
