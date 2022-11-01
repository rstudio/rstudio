/*
 * RMarkdownNoParamsDialog.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text.ui;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.*;

import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.widget.ModalDialogBase;
import org.rstudio.core.client.widget.MultiLineLabel;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.core.client.widget.images.MessageDialogImages;
import org.rstudio.studio.client.common.HelpLink;
import org.rstudio.studio.client.workbench.views.source.ViewsSourceConstants;

public class RMarkdownNoParamsDialog extends ModalDialogBase
{  
   public RMarkdownNoParamsDialog()
   {
      super(Roles.getAlertdialogRole());
      setText(constants_.noParametersDefined());
      setButtonAlignment(HasHorizontalAlignment.ALIGN_CENTER);
      
      addOkButton(new ThemedButton(constants_.okFullyCapitalized(), new ClickHandler()
      {
         public void onClick(ClickEvent event)
         {
            closeDialog();
         }
      }));
   }
  

   @Override
   protected Widget createMainWidget()
   {
      VerticalPanel panel = new VerticalPanel();
      
      HorizontalPanel horizontalPanel = new HorizontalPanel();
      horizontalPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_TOP);

      // add image
      MessageDialogImages images = MessageDialogImages.INSTANCE;
      Image image = new Image(new ImageResource2x(images.dialog_warning2x()));
      image.setAltText(MessageDialogImages.DIALOG_WARNING_TEXT);
      horizontalPanel.add(image);

      // add message widget
      VerticalPanel messagePanel = new VerticalPanel();
      Label label = new MultiLineLabel(
            constants_.noParametersDefinedForCurrentRMarkdown());
      label.setStylePrimaryName(
            ThemeResources.INSTANCE.themeStyles().dialogMessage());
      messagePanel.add(label);
      HelpLink helpLink = new HelpLink(constants_.usingRMarkdownParameters(),
                                       "parameterized_reports", 
                                       false);
      Style style = helpLink.getElement().getStyle();
      style.setMarginTop(4, Unit.PX);
      style.setMarginBottom(12, Unit.PX);
      
      messagePanel.add(helpLink);
      
      horizontalPanel.add(messagePanel);
      panel.add(horizontalPanel);
      
      // read the message when dialog is shown
      setARIADescribedBy(label.getElement());
      return panel;
   }
   private static final ViewsSourceConstants constants_ = GWT.create(ViewsSourceConstants.class);
}
