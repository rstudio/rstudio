/*
 * AboutOpenSourceDialog.java
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
package org.rstudio.studio.client.application.ui;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.ScrollPanel;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.Size;
import org.rstudio.core.client.dom.DomMetrics;
import org.rstudio.core.client.widget.FontSizer;
import org.rstudio.core.client.widget.ModalDialogBase;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.application.model.ProductNotice;

public class AboutOpenSourceDialog extends ModalDialogBase
{
   interface Binder extends UiBinder<Widget, AboutOpenSourceDialog> {}

   public AboutOpenSourceDialog(ProductNotice notice)
   {
      super(Roles.getDialogRole());
      setText("Open Source Components");

      mainWidget_ = GWT.<Binder>create(Binder.class).createAndBindUi(this);

      setButtonAlignment(HasHorizontalAlignment.ALIGN_CENTER);
      ThemedButton closeButton = new ThemedButton("Close", event -> closeDialog());
      addOkButton(closeButton);

      noticeHTML_.setHTML("<pre>" + notice.notice + "</pre>");
      FontSizer.applyNormalFontSize(noticeHTML_);
      Roles.getDocumentRole().set(noticeHTML_.getElement());
      Roles.getDocumentRole().setAriaLabelProperty(noticeHTML_.getElement(),
                                                   "Open Source Components");
      noticeHTML_.getElement().setTabIndex(0);

      // compute the widget size and set it
      Size preferredSize = new Size(500, 350);
      Size minimumSize = new Size(300, 300);
      preferredSize = DomMetrics.adjustedElementSize(
            preferredSize,
            minimumSize,
            100,   // pad
            100); // client margin
      noticeScroll_.setSize(preferredSize.width + "px", preferredSize.height + "px");
      setWidth("575px");
   }

   @Override
   protected Widget createMainWidget()
   {
      return mainWidget_;
   }

   @UiField ScrollPanel noticeScroll_;
   @UiField HTML noticeHTML_;

   private final Widget mainWidget_;
}
