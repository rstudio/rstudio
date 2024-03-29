/*
 * DataImportOptionsCsv.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

package org.rstudio.studio.client.workbench.views.environment.dataimport;

public class DataImportOptionsCsv extends DataImportOptions
{
   protected DataImportOptionsCsv()
   {
   }

   public final static native DataImportOptionsCsv create(String dataName,
                                                          String delimiter,
                                                          String quotes,
                                                          boolean escapeBackslash,
                                                          boolean escapeDouble,
                                                          boolean columnNames,
                                                          boolean trimSpaces,
                                                          DataImportOptionsCsvLocale locale,
                                                          String na,
                                                          String comments,
                                                          int skip,
                                                          boolean openDataViewer) /*-{
      return {
         "mode": "text",
         "dataName": dataName,
         "delimiter": delimiter,
         "quotes": quotes,
         "escapeBackslash": escapeBackslash,
         "escapeDouble": escapeDouble,
         "columnNames": columnNames,
         "trimSpaces": trimSpaces,
         "locale": locale,
         "na": na,
         "comments": comments,
         "skip": skip > 0 ? skip : null,
         "openDataViewer": openDataViewer
      }
   }-*/;
}
