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

import org.rstudio.core.client.widget.DirectoryChooserTextBox;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.rmarkdown.RmdTemplateDiscovery;
import org.rstudio.studio.client.rmarkdown.model.RmdChosenTemplate;
import org.rstudio.studio.client.rmarkdown.model.RmdDiscoveredTemplate;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiFactory;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.TextBox;
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
   
   public void populateTemplates(final Operation onCompleted)
   {
      if (state_ != STATE_EMPTY)
         return;
      
      discovery_ = RStudioGinjector.INSTANCE.getRmdTemplateDiscovery();
      discovery_.discoverTemplates(
         new OperationWithInput<RmdDiscoveredTemplate>()
         {
            @Override
            public void execute(RmdDiscoveredTemplate template)
            {
               listTemplates_.addItem(template.getName(), template.getPath());
            }
         },
         new Operation()
         {
            @Override
            public void execute()
            {
               state_ = STATE_POPULATED;
               completeDiscovery();
               onCompleted.execute();
            }
         });
      state_ = STATE_POPULATING;
   }
   
   @UiFactory
   public DirectoryChooserTextBox makeDirectoryChooserTextbox()
   {
      return new DirectoryChooserTextBox("", null);
   }
   
   public RmdChosenTemplate getChosenTemplate()
   {
      return new RmdChosenTemplate(getSelectedTemplatePath(), 
                                   getFileName(), 
                                   getDirectory(), 
                                   createDirectory());
   }
   
   public int getState()
   {
      return state_;
   }
   
   // Private methods ---------------------------------------------------------
   
   private void completeDiscovery()
   {
      if (listTemplates_.getItemCount() == 0)
      {
         listTemplates_.setVisible(false);
         noTemplatesFound_.setVisible(true);
         txtName_.setEnabled(false);
         dirLocation_.setEnabled(false);
         chkCreate_.setEnabled(false);
      }
   }

   private String getSelectedTemplatePath()
   {
      int idx = listTemplates_.getSelectedIndex();
      if (idx > 0)
      {
         return listTemplates_.getValue(idx);
      }
      return null;
   }
   
   private String getFileName()
   {
      return txtName_.getText();
   }
   
   private String getDirectory()
   {
      return dirLocation_.getText();
   }
   
   private boolean createDirectory()
   {
      return chkCreate_.getValue();
   }
   
   private RmdTemplateDiscovery discovery_;
   private int state_;
   
   @UiField ListBox listTemplates_;
   @UiField TextBox txtName_;
   @UiField DirectoryChooserTextBox dirLocation_;
   @UiField CheckBox chkCreate_;
   @UiField HTMLPanel noTemplatesFound_;
   
   public final static int STATE_EMPTY = 0;
   public final static int STATE_POPULATING = 1;
   public final static int STATE_POPULATED = 2;
}
