/*
 * DataImportOptionsXls.java
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

package org.rstudio.studio.client.workbench.views.environment.dataimport;

public class DataImportOptionsXls extends DataImportOptions
{
   protected DataImportOptionsXls()
   {
   }
   
   public final static native DataImportOptionsXls create(
      String dataName,
      String sheet,
      int skip,
      boolean colNames,
      String na,
      boolean openDataViewer,
      Integer nMax,
      String range
      ) /*-{
         return {
            "mode": "xls",
            "dataName": dataName,
            "sheet": sheet,
            "skip": skip,
            "columnNames": colNames,
            "na": na,
            "openDataViewer": openDataViewer,
            "nMax": nMax,
            "range": range
       }
   }-*/;
}
