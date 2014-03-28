/*
 * RmdTemplateChooser.java
 *
 * Copyright (C) 2009-14 by RStudio, Inc.
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

import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.rmarkdown.RmdTemplateDiscovery;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Widget;

public class RmdTemplateChooser extends Composite
{
   private static RmdTemplateChooserUiBinder uiBinder = GWT
         .create(RmdTemplateChooserUiBinder.class);

   interface RmdTemplateChooserUiBinder extends
         UiBinder<Widget, RmdTemplateChooser>
   {
   }

   public RmdTemplateChooser()
   {
      initWidget(uiBinder.createAndBindUi(this));
   }
   
   public void populateTemplates()
   {
      if (isPopulated_)
         return;
      
      discovery_ = RStudioGinjector.INSTANCE.getRmdTemplateDiscovery();
      discovery_.discoverTemplates(
         new OperationWithInput<String>()
         {
            @Override
            public void execute(String input)
            {
               listTemplates_.addItem(input);
            }
         },
         new Operation()
         {
            @Override
            public void execute()
            {
               // TODO Auto-generated method stub
               
            }
            
         });
      isPopulated_ = true;
   }
   
   private RmdTemplateDiscovery discovery_;
   private boolean isPopulated_;

   @UiField ListBox listTemplates_;
}
