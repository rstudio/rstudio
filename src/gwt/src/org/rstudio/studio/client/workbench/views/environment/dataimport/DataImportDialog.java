/*
 * DataImportDialog.java
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

import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.RStudioGinjector;

import com.google.gwt.user.client.ui.Widget;

public class DataImportDialog extends ModalDialog<DataImportOptions>
{
   private DataImport dataImport_;
   
   public DataImportDialog(DataImportOptionsUi dataImportOptionsUi,
                           String caption,
                           OperationWithInput<DataImportOptions> operation)
   {
      super(caption, operation);
      
      dataImport_ = new DataImport(dataImportOptionsUi, addProgressIndicator(false));
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      setOkButtonCaption("Import");
      setEnterDisabled(true);
   }

   @Override
   protected DataImportOptions collectInput()
   {
      return dataImport_.getOptions();
   }

   @Override
   protected Widget createMainWidget()
   {
      return dataImport_;
   }
}
