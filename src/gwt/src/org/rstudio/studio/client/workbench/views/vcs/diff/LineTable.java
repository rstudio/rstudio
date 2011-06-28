package org.rstudio.studio.client.workbench.views.vcs.diff;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.CellTable.Resources;
import com.google.gwt.user.cellview.client.RowStyles;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.inject.Inject;
import org.rstudio.studio.client.workbench.views.vcs.diff.Line.Type;

import java.util.List;

import static org.rstudio.studio.client.workbench.views.vcs.diff.Line.Type.Same;

public class LineTable extends CellTable<Line>
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
   public LineTable(final LineTableResources res)
   {
      super(1, res);

      TextColumn<Line> oldCol = new TextColumn<Line>()
      {
         @Override
         public String getValue(Line object)
         {
            return intToString(object.getOldLine());
         }
      };
      addColumn(oldCol);

      TextColumn<Line> newCol = new TextColumn<Line>()
      {
         @Override
         public String getValue(Line object)
         {
            return intToString(object.getNewLine());
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
   }

   private String intToString(Integer value)
   {
      if (value == null)
         return "";
      return value.toString();
   }

   public static void ensureStylesInjected()
   {
      LineTableResources res = GWT.create(LineTableResources.class);
      res.cellTableStyle().ensureInjected();
   }
}
