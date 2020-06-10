/*
 * SVNChangelistTable.java
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
package org.rstudio.studio.client.workbench.views.vcs.svn;

import com.google.gwt.text.shared.SafeHtmlRenderer;
import com.google.gwt.user.cellview.client.Column;
import org.rstudio.core.client.StringUtil;
import org.rstudio.studio.client.common.vcs.StatusAndPath;
import org.rstudio.studio.client.workbench.views.vcs.common.ChangelistTable;

import java.util.Comparator;

public class SVNChangelistTable extends ChangelistTable
{
   public SVNChangelistTable()
   {
   }

   public void setChangelistColumnVisible(boolean visible)
   {
      if ((changelistColumn_ != null) != visible)
      {
         if (!visible)
         {
            table_.removeColumn(changelistColumn_);
            changelistColumn_ = null;
         }
         else
         {
            changelistColumn_ = new Column<StatusAndPath, String>(
                  new NotEditingTextCell())
            {
               @Override
               public String getValue(StatusAndPath object)
               {
                  return object.getChangelist();
               }
            };
            changelistColumn_.setSortable(true);
            sortHandler_.setComparator(changelistColumn_, new Comparator<StatusAndPath>()
            {
               @Override
               public int compare(StatusAndPath a,
                                  StatusAndPath b)
               {
                  return StringUtil.notNull(a.getChangelist())
                        .compareToIgnoreCase(
                              b.getChangelist());
               }
            });
            table_.addColumn(changelistColumn_, "Changelist");
         }
      }
   }

   @Override
   protected SafeHtmlRenderer<String> getStatusRenderer()
   {
      return new SVNStatusRenderer();
   }

   private Column<StatusAndPath, String> changelistColumn_;
}
