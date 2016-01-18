/*
 * DataImportOptionsUi.java
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

import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Composite;

public class DataImportOptionsUi extends Composite implements HasValueChangeHandlers<DataImportOptions>
{
   public DataImportOptions getOptions()
   {
      return new DataImportOptions();
   }

   public HandlerRegistration addValueChangeHandler(ValueChangeHandler<DataImportOptions> handler)
   {
      return null;
   }
   
   public void setAssembleResponse(DataImportAssembleResponse response)
   {
      
   }
   
   public void clearDataName()
   {
      
   }
}
