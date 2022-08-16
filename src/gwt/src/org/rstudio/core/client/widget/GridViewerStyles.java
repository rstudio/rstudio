/*
 * GridViewerStyles.java
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */

package org.rstudio.core.client.widget;

import org.rstudio.core.client.theme.ThemeColors;

public class GridViewerStyles
{
   public static String getCustomStyle()
   {
      return
         ".rstudio-themes-default #rsGridData_info,\n" +
         ".rstudio-themes-default table.dataTable thead th,\n" +
         ".rstudio-themes-default table.dataTable thead td,\n" +
         ".rstudio-themes-default table.dataTable tbody td {\n" +
         "   border-color: " + ThemeColors.defaultBorder + ";\n" +
         "}\n" +
         "\n" +
         ".rstudio-themes-dark-grey #rsGridData_info,\n" +
         ".rstudio-themes-dark-grey table.dataTable thead th,\n" +
         ".rstudio-themes-dark-grey table.dataTable thead td,\n" +
         ".rstudio-themes-dark-grey table.dataTable tbody td {\n" +
         "   border-color: " + ThemeColors.darkGreyBorder + ";\n" +
         "}\n" +
         "\n" +
         ".rstudio-themes-alternate #rsGridData_info,\n" +
         ".rstudio-themes-alternate table.dataTable thead th,\n" +
         ".rstudio-themes-alternate table.dataTable thead td,\n" +
         ".rstudio-themes-alternate table.dataTable tbody td {\n" +
         "   border-color: " + ThemeColors.alternateBorder + ";\n" +
         "}\n" +
         "\n" +
         ".rstudio-themes-default #rsGridData_info,\n" +
         ".rstudio-themes-default th,\n" +
         ".rstudio-themes-default td.first-child,\n" +
         ".rstudio-themes-default table.dataTable thead td {\n" +
         "   background-color: " + ThemeColors.defaultBackground + ";\n" +
         "}\n" +
         "\n" +
         ".rstudio-themes-dark-grey #rsGridData_info,\n" +
         ".rstudio-themes-dark-grey th,\n" +
         ".rstudio-themes-dark-grey td.first-child,\n" +
         ".rstudio-themes-dark-grey table.dataTable thead th {\n" +
         "   background-color: " + ThemeColors.darkGreyBackground + ";\n" +
         "}\n" +
         "\n" +
         ".rstudio-themes-alternate #rsGridData_info,\n" +
         ".rstudio-themes-alternate th,\n" +
         ".rstudio-themes-alternate td.first-child,\n" +
         ".rstudio-themes-alternate table.dataTable thead th {\n" +
         "   background-color: " + ThemeColors.alternateBackground + ";\n" +
         "}\n" +
         "\n";
   }
}
