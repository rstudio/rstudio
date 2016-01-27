/*
 * DataImportOptionsUiSav.java
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

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.ui.Widget;

public class DataImportOptionsUiSav extends DataImportOptionsUi
{

   private static DataImportOptionsUiSavUiBinder uiBinder = GWT
         .create(DataImportOptionsUiSavUiBinder.class);

   interface DataImportOptionsUiSavUiBinder
         extends UiBinder<Widget, DataImportOptionsUiSav>
   {
   }

   public DataImportOptionsUiSav()
   {
      initWidget(uiBinder.createAndBindUi(this));
   }
   
   @Override
   public DataImportOptionsSav getOptions()
   {
      return DataImportOptionsSav.create("test");
   }
}
