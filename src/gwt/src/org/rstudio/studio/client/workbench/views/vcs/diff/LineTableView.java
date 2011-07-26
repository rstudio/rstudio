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

import com.google.gwt.cell.client.AbstractCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.user.cellview.client.CellTable;
import com.google.gwt.user.cellview.client.Column;
import com.google.gwt.user.cellview.client.RowStyles;
import com.google.gwt.user.cellview.client.TextColumn;
import com.google.gwt.view.client.MultiSelectionModel;
import com.google.gwt.view.client.ProvidesKey;
import com.google.inject.Inject;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.dom.DomUtils.NodePredicate;
import org.rstudio.studio.client.workbench.views.vcs.diff.Line.Type;
import org.rstudio.studio.client.workbench.views.vcs.diff.LineTablePresenter.Display;
import org.rstudio.studio.client.workbench.views.vcs.events.DiffChunkActionEvent;
import org.rstudio.studio.client.workbench.views.vcs.events.DiffChunkActionEvent.Action;
import org.rstudio.studio.client.workbench.views.vcs.events.DiffChunkActionHandler;

import java.util.ArrayList;

public class LineTableView extends CellTable<ChunkOrLine> implements Display
{
   public interface LineTableResources extends CellTable.Resources
   {
      @Source("cellTableStyle.css")
      TableStyle cellTableStyle();
   }

   public interface TableStyle extends CellTable.Style
   {
      String header();
      String same();
      String insertion();
      String deletion();
   }

   public class LineContentCell extends AbstractCell<ChunkOrLine>
   {
      public LineContentCell()
      {
         super("click");
      }

      @Override
      public void onBrowserEvent(Context context,
                                 Element parent,
                                 ChunkOrLine value,
                                 NativeEvent event,
                                 ValueUpdater<ChunkOrLine> chunkOrLineValueUpdater)
      {
         if ("click".equals(event.getType()))
         {
            Element el = (Element) DomUtils.findNodeUpwards(
                  event.getEventTarget().<Node>cast(),
                  parent,
                  new NodePredicate()
                  {
                     @Override
                     public boolean test(Node n)
                     {
                        return n.getNodeType() == Node.ELEMENT_NODE &&
                               ((Element) n).getTagName().equalsIgnoreCase("a");
                     }
                  });

            if (el != null)
            {
               event.preventDefault();
               event.stopPropagation();

               fireEvent(new DiffChunkActionEvent(
                     Action.valueOf(el.getAttribute("data-action")),
                     value.getChunk()));
            }
         }

         super.onBrowserEvent(context,
                              parent,
                              value,
                              event,
                              chunkOrLineValueUpdater);
      }

      @Override
      public void render(Context context, ChunkOrLine value, SafeHtmlBuilder sb)
      {
         if (value.getLine() != null)
         {
            sb.appendEscaped(value.getLine().getText());
         }
         else
         {
            sb.appendEscaped(UnifiedEmitter.createChunkString(value.getChunk()));
            sb.appendHtmlConstant("<div style=\"float: right\">");
            sb.appendHtmlConstant(" <a href=\"javascript:void\" data-action=\"" + Action.Unstage.name() + "\">Unstage</a>");
            sb.appendHtmlConstant(" <a href=\"javascript:void\" data-action=\"" + Action.Stage.name() + "\">Stage</a>");
            sb.appendHtmlConstant(" <a href=\"javascript:void\" data-action=\"" + Action.Discard.name() + "\">Discard</a>");
            sb.appendHtmlConstant("</div>");
         }
      }
   }

   @Inject
   public LineTableView(final LineTableResources res)
   {
      super(1, res);

      TextColumn<ChunkOrLine> oldCol = new TextColumn<ChunkOrLine>()
      {
         @Override
         public String getValue(ChunkOrLine object)
         {
            Line line = object.getLine();
            return line == null ? "" :
                   line.getType() == Type.Insertion ? "" :
                   intToString(line.getOldLine());
         }
      };
      addColumn(oldCol);

      TextColumn<ChunkOrLine> newCol = new TextColumn<ChunkOrLine>()
      {
         @Override
         public String getValue(ChunkOrLine object)
         {
            Line line = object.getLine();
            return line == null ? "" :
                   line.getType() == Type.Deletion ? ""
                   : intToString(line.getNewLine());
         }
      };
      addColumn(newCol);

      Column<ChunkOrLine, ChunkOrLine> textCol =
            new Column<ChunkOrLine, ChunkOrLine>(new LineContentCell())
            {
               @Override
               public ChunkOrLine getValue(ChunkOrLine object)
               {
                  return object;
               }
            };
      addColumn(textCol);

      setColumnWidth(oldCol, 100, Unit.PX);
      setColumnWidth(newCol, 100, Unit.PX);
      setColumnWidth(textCol, 100, Unit.PCT);

      setRowStyles(new RowStyles<ChunkOrLine>()
      {
         @Override
         public String getStyleNames(ChunkOrLine chunkOrLine, int rowIndex)
         {
            Line line = chunkOrLine.getLine();

            if (line == null)
               return res.cellTableStyle().header();

            switch (line.getType())
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

      selectionModel_ = new MultiSelectionModel<ChunkOrLine>(new ProvidesKey<ChunkOrLine>()
      {
         @Override
         public Object getKey(ChunkOrLine item)
         {
            if (item.getChunk() != null)
            {
               DiffChunk chunk = item.getChunk();
               return chunk.oldRowStart + "," + chunk.oldRowCount + "," +
                     chunk.newRowStart + "," + chunk.newRowCount;
            }

            Line line = item.getLine();
            return line.getOldLine() + "," + line.getNewLine();
         }
      }) {
         @Override
         public void setSelected(ChunkOrLine object, boolean selected)
         {
            if (object.getLine() != null)
               super.setSelected(object, selected);
         }
      };
      setSelectionModel(selectionModel_);

      setData(new ArrayList<ChunkOrLine>());
   }

   private String intToString(Integer value)
   {
      if (value == null)
         return "";
      return value.toString();
   }

   @Override
   public void setData(ArrayList<ChunkOrLine> diffData)
   {
      lines_ = diffData;
      setPageSize(diffData.size());
      selectionModel_.clear();
      setRowData(diffData);
   }

   @Override
   public void clear()
   {
      setData(new ArrayList<ChunkOrLine>());
   }

   @Override
   public ArrayList<Line> getSelectedLines()
   {
      ArrayList<Line> selected = new ArrayList<Line>();
      for (ChunkOrLine line : lines_)
         if (line.getLine() != null && selectionModel_.isSelected(line))
            selected.add(line.getLine());
      return selected;
   }

   @Override
   public ArrayList<Line> getAllLines()
   {
      ArrayList<Line> selected = new ArrayList<Line>();
      for (ChunkOrLine line : lines_)
         if (line.getLine() != null)
            selected.add(line.getLine());
      return selected;
   }

   @Override
   public HandlerRegistration addDiffChunkActionHandler(DiffChunkActionHandler handler)
   {
      return addHandler(handler, DiffChunkActionEvent.TYPE);
   }

   public static void ensureStylesInjected()
   {
      LineTableResources res = GWT.create(LineTableResources.class);
      res.cellTableStyle().ensureInjected();
   }

   private ArrayList<ChunkOrLine> lines_;
   private MultiSelectionModel<ChunkOrLine> selectionModel_;
}
