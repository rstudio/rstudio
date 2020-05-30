/*
 * DataImportServerOperations.java
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

package org.rstudio.studio.client.workbench.views.environment.dataimport.model;

import org.rstudio.studio.client.server.ServerRequestCallback;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.views.environment.dataimport.DataImportOptions;

public interface DataImportServerOperations
{
   void previewDataImport(DataImportOptions dataImportOptions,
                          int maxCols,
                          int maxFactors,
                          ServerRequestCallback<DataImportPreviewResponse> requestCallback);
   
   void assembleDataImport(DataImportOptions dataImportOptions,
                           ServerRequestCallback<DataImportAssembleResponse> requestCallback);

   void previewDataImportAsync(DataImportOptions dataImportOptions,
                               int maxCols,
                               int maxFactors,
                               ServerRequestCallback<DataImportPreviewResponse> requestCallback);
   
   void interrupt(ServerRequestCallback<Void> requestCallback);
   
   void previewDataImportAsyncAbort(ServerRequestCallback<Void> requestCallback);
   
   void previewDataImportClean(DataImportOptions dataImportOptions, ServerRequestCallback<Void> requestCallback);
}
