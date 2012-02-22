/*
 * FindTreeViewModel.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.output.find;

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.view.client.SingleSelectionModel;
import com.google.gwt.view.client.TreeViewModel;
import org.rstudio.core.client.SafeHtmlUtil;
import org.rstudio.core.client.StringUtil;

public class FindTreeViewModel implements TreeViewModel
{
   class FileCell extends AbstractCell<FindResultContext.File>
   {
      @Override
      public void render(Context context,
                         FindResultContext.File value,
                         SafeHtmlBuilder sb)
      {
         sb.appendEscaped(value.getPath() + " (" + value.getCount() + ")");
      }
   }

   class MatchCell extends AbstractCell<FindResultContext.Match>
   {
      public MatchCell(String lineClassName)
      {
         lineClassName_ = lineClassName;
      }

      @Override
      public void render(Context context,
                         FindResultContext.Match value,
                         SafeHtmlBuilder sb)
      {
         String padVal = StringUtil.padRight(value.getLine() + "",
                                             context_.getMaxLineWidth());
         SafeHtmlUtil.appendSpan(sb, lineClassName_,
                                 padVal + " ");
         sb.appendEscaped(value.getValue());
      }

      private final String lineClassName_;
   }

   public FindTreeViewModel(FindResultContext context, String lineNumberClassName)
   {
      context_ = context;
      lineNumberClassName_ = lineNumberClassName;
      selectionModel_ = new SingleSelectionModel<Object>();
   }

   public SingleSelectionModel<Object> getSelectionModel()
   {
      return selectionModel_;
   }

   @Override
   public <T> NodeInfo<?> getNodeInfo(T value)
   {
      if (value == null)
      {
         return new DefaultNodeInfo<FindResultContext.File>(
               context_.getDataProvider(),
               new FileCell(),
               selectionModel_,
               null);
      }
      else if (value instanceof FindResultContext.File)
      {
         return new DefaultNodeInfo<FindResultContext.Match>(
               ((FindResultContext.File)value).getDataProvider(),
               new MatchCell(lineNumberClassName_),
               selectionModel_,
               null);
      }
      return null;
   }

   @Override
   public boolean isLeaf(Object value)
   {
      return value instanceof FindResultContext.Match;
   }

   private FindResultContext context_;
   private final String lineNumberClassName_;
   private SingleSelectionModel<Object> selectionModel_;
}
