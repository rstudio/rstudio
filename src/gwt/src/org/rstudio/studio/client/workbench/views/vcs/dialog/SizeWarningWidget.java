/*
 * SizeWarningWidget.java
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.vcs.dialog;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.HasClickHandlers;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.workbench.views.vcs.ViewVcsConstants;

public class SizeWarningWidget extends Composite implements HasClickHandlers
{
   interface Binder extends UiBinder<Widget, SizeWarningWidget>
   {}

   public SizeWarningWidget(String subject)
   {
      showDiffButton_ = new ThemedButton(constants_.showDiffCapitalized());

      initWidget(GWT.<Binder>create(Binder.class).createAndBindUi(this));

      subject_.setInnerText(subject);
   }

   public void setSize(long size)
   {
      size_.setInnerText(StringUtil.formatFileSize(size));
   }
   private static final ViewVcsConstants constants_ = GWT.create(ViewVcsConstants.class);

   @Override
   public HandlerRegistration addClickHandler(ClickHandler handler)
   {
      return showDiffButton_.addClickHandler(handler);
   }

   @UiField(provided = true)
   ThemedButton showDiffButton_;
   @UiField
   SpanElement subject_;
   @UiField
   SpanElement size_;
}
