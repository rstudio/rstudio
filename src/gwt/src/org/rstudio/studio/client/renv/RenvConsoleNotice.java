/*
 * RenvConsoleNotice.java
 *
 * Copyright (C) 2026 by Posit Software, PBC
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
package org.rstudio.studio.client.renv;

import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.widget.DecorativeImage;
import org.rstudio.core.client.widget.ImageButton;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.core.client.widget.images.MessageDialogImages;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.Label;

/** A non-blocking project setup notice, shown above the console. */
public class RenvConsoleNotice extends Composite
{
   public RenvConsoleNotice(boolean severe, String text, String actionLabel,
                            Command onAction, String secondaryActionLabel,
                            Command onSecondaryAction, Command onDismiss)
   {
      RES.styles().ensureInjected();

      FlowPanel panel = new FlowPanel();
      panel.setStyleName(RES.styles().notice());

      DecorativeImage icon = new DecorativeImage(
            new ImageResource2x(severe ? MessageDialogImages.INSTANCE.dialog_error2x()
                                      : MessageDialogImages.INSTANCE.dialog_info2x()));
      icon.setStyleName(RES.styles().icon());
      icon.setSize("auto", "24px");
      panel.add(icon);

      Label message = new Label(text);
      message.setStyleName(RES.styles().message());
      panel.add(message);

      FlowPanel actions = new FlowPanel();
      actions.setStyleName(RES.styles().actions());
      addAction(actions, actionLabel, onAction);
      addAction(actions, secondaryActionLabel, onSecondaryAction);

      ImageButton dismiss = new ImageButton(constants_.renvDismissNotice(),
            new ImageResource2x(ThemeResources.INSTANCE.closeTab2x()));
      dismiss.addStyleName(RES.styles().dismiss());
      dismiss.setTitle(constants_.renvDismissNotice());
      dismiss.addClickHandler(event -> onDismiss.execute());
      actions.add(dismiss);
      panel.add(actions);

      initWidget(panel);
   }

   private void addAction(FlowPanel actions, String label, Command onAction)
   {
      if (onAction == null)
         return;

      ThemedButton action = new ThemedButton(label, event -> onAction.execute());
      action.setThemeAware(true);
      action.addStyleName(RES.styles().action());
      actions.add(action);
   }

   interface Styles extends CssResource
   {
      String notice();
      String icon();
      String message();
      String actions();
      String action();
      String dismiss();
   }

   interface Resources extends ClientBundle
   {
      @Source("RenvConsoleNotice.css")
      Styles styles();
   }

   private static final Resources RES = GWT.create(Resources.class);
   private static final RenvConstants constants_ = GWT.create(RenvConstants.class);
}
