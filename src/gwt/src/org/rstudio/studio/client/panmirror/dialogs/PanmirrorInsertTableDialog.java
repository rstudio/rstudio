/*
 * PanmirrorInsertTableDialog.java
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


package org.rstudio.studio.client.panmirror.dialogs;


import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.NumericValueWidget;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.TextBoxWithCue;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorInsertTableResult;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorTableCapabilities;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;


public class PanmirrorInsertTableDialog extends ModalDialog<PanmirrorInsertTableResult>
{
   public PanmirrorInsertTableDialog(PanmirrorTableCapabilities capabilities, 
                                     OperationWithInput<PanmirrorInsertTableResult> operation)
   {
      super("Insert Table", Roles.getDialogRole(), operation, () -> {
         // cancel returns null
         operation.execute(null);
      });
   
      mainWidget_ = GWT.<Binder>create(Binder.class).createAndBindUi(this);
      
      configureNumeric(rows_);
      configureNumeric(columns_);
      
      header_.setValue(true);
      header_.setVisible(capabilities.headerOptional);
      captionLabel_.setVisible(capabilities.captions);
      caption_.setVisible(capabilities.captions);
      
      header_.getElement().setId(ElementIds.VISUAL_MD_INSERT_TABLE_HEADER);
   }
   
   @Override
   protected Widget createMainWidget()
   {
      return mainWidget_;
   }
   
   @Override
   protected PanmirrorInsertTableResult collectInput()
   {
      PanmirrorInsertTableResult result = new PanmirrorInsertTableResult();
      result.rows = readNumeric(rows_);
      result.cols = readNumeric(columns_);
      result.caption = caption_.getText();
      result.header = header_.getValue();
      return result;
   }
   
   @Override
   protected boolean validate(PanmirrorInsertTableResult result)
   {
      return rows_.validate() && columns_.validate();
   }
   
   private void configureNumeric(NumericValueWidget widget)
   {
      widget.setWidth("100px");
      widget.setLimits(1, NumericValueWidget.NoMaximum);
   }
   
   private int readNumeric(NumericValueWidget widget)
   {
      String value = widget.getValue().trim();
      return value.length() > 0 ? Integer.parseInt(value) : 0;
   }
   
   interface Binder extends UiBinder<Widget, PanmirrorInsertTableDialog> {}
   
   private Widget mainWidget_;

   @UiField NumericValueWidget rows_;
   @UiField NumericValueWidget columns_;
   @UiField Label captionLabel_;
   @UiField TextBoxWithCue caption_;
   @UiField CheckBox header_;
   
}
