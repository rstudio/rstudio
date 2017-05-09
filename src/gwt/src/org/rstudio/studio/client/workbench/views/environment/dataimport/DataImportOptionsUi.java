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

import org.rstudio.studio.client.common.HelpLink;
import org.rstudio.studio.client.workbench.views.environment.dataimport.model.DataImportAssembleResponse;
import org.rstudio.studio.client.workbench.views.environment.dataimport.model.DataImportPreviewResponse;

import com.google.gwt.event.logical.shared.HasValueChangeHandlers;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.TextBox;

public class DataImportOptionsUi extends Composite implements HasValueChangeHandlers<DataImportOptions>
{
   public DataImportOptions getOptions()
   {
      return DataImportOptions.create();
   }

   public HandlerRegistration addValueChangeHandler(ValueChangeHandler<DataImportOptions> handler)
   {
      return handlerManager_.addHandler(
            ValueChangeEvent.getType(),
            handler);
   }
   
   public void setAssembleResponse(DataImportAssembleResponse response)
   {
      
   }
   
   public void setPreviewResponse(DataImportPreviewResponse response)
   {
   
   }
   
   public void clearOptions()
   {
      
   }
   
   public void setImportLocation(String importLocation)
   {
      nameTextBox_.setText("");
   }
   
   void triggerChange()
   {
      ValueChangeEvent.fire(this, getOptions());
   }
   
   private final HandlerManager handlerManager_ = new HandlerManager(this);
   
   @Override
   public void fireEvent(GwtEvent<?> event)
   {
      handlerManager_.fireEvent(event);
   }
   
   public HelpLink getHelpLink()
   {
      return null;
   }
   
   @UiField
   protected TextBox nameTextBox_;
}
