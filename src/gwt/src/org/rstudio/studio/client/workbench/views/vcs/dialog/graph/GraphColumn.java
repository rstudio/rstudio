/*
 * GraphColumn.java
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
package org.rstudio.studio.client.workbench.views.vcs.dialog.graph;

public class GraphColumn
{
   GraphColumn(String line)
   {
      start = line.indexOf('+') >= 0;
      end = line.indexOf('-') >= 0;
      nexus = line.indexOf('*') >= 0;
      id = Integer.parseInt(
            line.replace('*', ' ').replace('+', ' ').replace('-', ' ').trim());
   }

   /**
    * The id of this column--will be used for coloring
    */
   public int id;

   /**
    * If true, this column starts on this row
    */
   public boolean start;

   /**
    * If true, this column ends on this row
    */
   public boolean end;

   /**
    * If true, this column is the nexus column
    */
   public boolean nexus;
}
