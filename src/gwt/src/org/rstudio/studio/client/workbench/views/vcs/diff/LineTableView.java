/*
 * LineTableView.java
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
package org.rstudio.studio.client.workbench.views.vcs.diff;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.RowStyles;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.view.client.MultiSelectionModel;
import com.google.gwt.view.client.ProvidesKey;
import com.google.inject.Inject;
import org.rstudio.studio.client.workbench.views.vcs.diff.Line.Type;
import org.rstudio.studio.client.workbench.views.vcs.diff.LineTablePresenter.Display;

import java.util.ArrayList;

public class LineTableView extends CellTable<Line> implements Display
{
   public interface LineTableResources extends CellTable.Resources
   {
      @Source("cellTableStyle.css")
      TableStyle cellTableStyle();
   }

   public interface TableStyle extends CellTable.Style
   {
      String same();
      String insertion();
      String deletion();
   }

   @Inject
   public LineTableView(final LineTableResources res)
   {
      super(1, res);

      TextColumn<Line> oldCol = new TextColumn<Line>()
      {
         @Override
         public String getValue(Line object)
         {
            return object.getType() == Type.Insertion
                   ? ""
                   : intToString(object.getOldLine());
         }
      };
      addColumn(oldCol);

      TextColumn<Line> newCol = new TextColumn<Line>()
      {
         @Override
         public String getValue(Line object)
         {
            return object.getType() == Type.Deletion
                   ? ""
                   : intToString(object.getNewLine());
         }
      };
      addColumn(newCol);

      TextColumn<Line> textCol = new TextColumn<Line>()
      {
         @Override
         public String getValue(Line object)
         {
            return object.getText();
         }
      };
      addColumn(textCol);

      setColumnWidth(oldCol, 100, Unit.PX);
      setColumnWidth(newCol, 100, Unit.PX);
      setColumnWidth(textCol, 100, Unit.PCT);

      setRowStyles(new RowStyles<Line>()
      {
         @Override
         public String getStyleNames(Line row, int rowIndex)
         {
            switch (row.getType())
            {
               case Same:
                  return res.cellTableStyle().same();
               case Insertion:
                  return res.cellTableStyle().insertion();
               case Deletion:
                  return res.cellTableStyle().deletion();
               default:
                  return "";
            }
         }
      });

      selectionModel_ = new MultiSelectionModel<Line>(new ProvidesKey<Line>()
      {
         @Override
         public Object getKey(Line item)
         {
            return item.getOldLine() + "," + item.getNewLine();
         }
      });
      setSelectionModel(selectionModel_);

      setData(new ArrayList<Line>());
   }

   private String intToString(Integer value)
   {
      if (value == null)
         return "";
      return value.toString();
   }

   @Override
   public void setData(ArrayList<Line> diffData)
   {
      lines_ = diffData;
      setPageSize(diffData.size());
      selectionModel_.clear();
      setRowData(diffData);
   }

   @Override
   public void clear()
   {
      setData(new ArrayList<Line>());
   }

   @Override
   public ArrayList<Line> getSelectedLines()
   {
      ArrayList<Line> selected = new ArrayList<Line>();
      for (Line line : lines_)
         if (selectionModel_.isSelected(line))
            selected.add(line);
      return selected;
   }

   @Override
   public ArrayList<Line> getAllLines()
   {
      ArrayList<Line> selected = new ArrayList<Line>();
      for (Line line : lines_)
         selected.add(line);
      return selected;
   }

   public static void ensureStylesInjected()
   {
      LineTableResources res = GWT.create(LineTableResources.class);
      res.cellTableStyle().ensureInjected();
   }

   private ArrayList<Line> lines_;
   private MultiSelectionModel<Line> selectionModel_;
}
