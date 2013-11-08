/*
 * AboutDialog.java
 *
 * Copyright (C) 2009-13 by RStudio, Inc.
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
import org.rstudio.core.client.widget.ModalDialogBase;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.application.model.ProductInfo;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Widget;

public class AboutDialog extends ModalDialogBase
{
   public AboutDialog(ProductInfo info)
   {
      ThemedButton OKButton = new ThemedButton("OK", 
         new ClickHandler() 
      {
            @Override
            public void onClick(ClickEvent event) {
               closeDialog();   
            }
      });
      addOkButton(OKButton);
      contents_ = new AboutDialogContents(info);
      setWidth("600px");
   }

   @Override
   protected Widget createMainWidget()
   {
      return contents_;
   }
   
   private AboutDialogContents contents_;
}
