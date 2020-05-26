/*
 * ShinyDocumentWarning.java
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
package org.rstudio.studio.client.rmarkdown.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.Widget;

public class ShinyDocumentWarning extends Composite
{
   private static ShinyDocumentWarningUiBinder uiBinder = GWT
         .create(ShinyDocumentWarningUiBinder.class);

   interface ShinyDocumentWarningUiBinder extends
         UiBinder<Widget, ShinyDocumentWarning>
   {
   }

   public ShinyDocumentWarning()
   {
      initWidget(uiBinder.createAndBindUi(this));
   }

   public Element getMessageElement()
   {
      return dialogMessage_.getElement();
   }

   @UiField HTML dialogMessage_;
}
