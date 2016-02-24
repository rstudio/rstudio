/*
 * DataImportOptionsUiXml.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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

package org.rstudio.studio.client.workbench.views.environment.dataimport;

import org.rstudio.studio.client.workbench.views.environment.dataimport.model.DataImportAssembleResponse;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;

public class DataImportOptionsUiXml extends DataImportOptionsUi
{

   private static DataImportOptionsUiXmlUiBinder uiBinder = GWT
         .create(DataImportOptionsUiXmlUiBinder.class);

   interface DataImportOptionsUiXmlUiBinder
         extends UiBinder<Widget, DataImportOptionsUiXml>
   {
   }

   public DataImportOptionsUiXml()
   {
      initWidget(uiBinder.createAndBindUi(this));
   }

   @Override
   public DataImportOptionsXml getOptions()
   {
      return DataImportOptionsXml.create(
         nameTextBox_.getValue()
      );
   }
   
   @Override
   public void setAssembleResponse(DataImportAssembleResponse response)
   {
      nameTextBox_.setText(response.getDataName());
   }
   
   @Override
   public void clearOptions()
   {
      nameTextBox_.setText("");
   }
   
   @UiField
   TextBox nameTextBox_;
}
