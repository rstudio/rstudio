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
import com.google.gwt.view.client.SelectionChangeEvent;
import com.google.inject.Inject;
import org.rstudio.core.client.SafeHtmlUtil;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.dom.DomUtils.NodePredicate;
import org.rstudio.core.client.theme.RStudioCellTableStyle;
import org.rstudio.core.client.widget.FontSizer;
import org.rstudio.core.client.widget.MultiSelectCellTable;
import org.rstudio.studio.client.common.vcs.VCSServerOperations.PatchMode;
import org.rstudio.studio.client.workbench.views.vcs.diff.Line.Type;
import org.rstudio.studio.client.workbench.views.vcs.diff.LineTablePresenter.Display;
import org.rstudio.studio.client.workbench.views.vcs.events.DiffChunkActionEvent;
import org.rstudio.studio.client.workbench.views.vcs.events.DiffChunkActionEvent.Action;
import org.rstudio.studio.client.workbench.views.vcs.events.DiffChunkActionHandler;
import org.rstudio.studio.client.workbench.views.vcs.events.DiffLineActionEvent;
import org.rstudio.studio.client.workbench.views.vcs.events.DiffLineActionHandler;

import java.util.ArrayList;
import java.util.HashSet;

public class LineTableView extends MultiSelectCellTable<ChunkOrLine> implements Display
{
   public interface LineTableViewCellTableResources extends CellTable.Resources
   {
      @Source({RStudioCellTableStyle.RSTUDIO_DEFAULT_CSS,
               "LineTableViewCellTableStyle.css"})
      LineTableViewCellTableStyle cellTableStyle();
   }

   public interface LineTableViewCellTableStyle extends CellTable.Style
   {
      String header();
      String same();
      String insertion();
      String deletion();
      String comment();

      String actions();
      String lineActions();
      String chunkActions();

      String start();
      String end();

      String stageMode();
      String workingMode();
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
         if ("click".equals(event.getType()) && parent.isOrHasChild(event.getEventTarget().<Node>cast()))
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

               Action action = Action.valueOf(el.getAttribute("data-action"));

               if (value.getChunk() != null)
                  fireEvent(new DiffChunkActionEvent(action, value.getChunk()));
               else
                  fireEvent(new DiffLineActionEvent(action, value.getLine()));
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
            if (showActions_ && value.getLine().getType() != Line.Type.Same)
               renderActionButtons(sb,
                                   RES.cellTableStyle().lineActions(),
                                   " line");
         }
         else
         {
            sb.appendEscaped(UnifiedEmitter.createChunkString(value.getChunk()));
            if (showActions_)
            {
               renderActionButtons(sb,
                                   RES.cellTableStyle().chunkActions(),
                                   " chunk");
            }
         }
      }

      private void renderActionButtons(SafeHtmlBuilder sb,
                                       String className,
                                       String labelSuffix)
      {
         sb.append(SafeHtmlUtil.createOpenTag(
               "div",
               "style", "float: right",
               "class", RES.cellTableStyle().actions() + " " + className));
         renderActionButton(sb, Action.Unstage, labelSuffix);
         renderActionButton(sb, Action.Stage, labelSuffix);
         renderActionButton(sb, Action.Discard, labelSuffix);
         sb.appendHtmlConstant("</div>");
      }

      private void renderActionButton(SafeHtmlBuilder sb,
                                      Action action,
                                      String labelSuffix)
      {
         sb.append(SafeHtmlUtil.createOpenTag(
               "a",
               "href", "javascript:void",
               "data-action", action.name()));
         sb.appendEscaped(action.name() + labelSuffix);
         sb.appendHtmlConstant("</a>");
      }
   }

   private class SwitchableSelectionModel<T> extends MultiSelectionModel<T>
   {
      private SwitchableSelectionModel()
      {
      }

      private SwitchableSelectionModel(ProvidesKey<T> keyProvider)
      {
         super(keyProvider);
      }

      @Override
      public void setSelected(T object, boolean selected)
      {
         if (!enabled_)
            return;

         super.setSelected(object, selected);
      }

      @SuppressWarnings("unused")
      public boolean isEnabled()
      {
         return enabled_;
      }

      public void setEnabled(boolean enabled)
      {
         this.enabled_ = enabled;
      }

      private boolean enabled_ = true;
   }

   public LineTableView()
   {
      this(GWT.<LineTableViewCellTableResources>create(LineTableViewCellTableResources.class));
   }

   @Inject
   public LineTableView(final LineTableViewCellTableResources res)
   {
      super(1, res);

      FontSizer.applyNormalFontSize(this);

      TextColumn<ChunkOrLine> oldCol = new TextColumn<ChunkOrLine>()
      {
         @Override
         public String getValue(ChunkOrLine object)
         {
            Line line = object.getLine();
            return line == null ? "" :
                   line.getType() == Type.Insertion ? "" :
                   line.getType() == Type.Comment ? "" :
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
                   line.getType() == Type.Deletion ? "" :
                   line.getType() == Type.Comment ? "" :
                   intToString(line.getNewLine());
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
            {
               return res.cellTableStyle().header();
            }
            else
            {
               String prefix = "";
               if (startRows_.contains(rowIndex))
                  prefix += res.cellTableStyle().start() + " ";
               if (endRows_.contains(rowIndex))
                  prefix += res.cellTableStyle().end() + " ";

               switch (line.getType())
               {
                  case Same:
                     return prefix + res.cellTableStyle().same();
                  case Insertion:
                     return prefix + res.cellTableStyle().insertion();
                  case Deletion:
                     return prefix + res.cellTableStyle().deletion();
                  case Comment:
                     return prefix + res.cellTableStyle().comment();
                  default:
                     return "";
               }
            }

         }
      });

      selectionModel_ = new SwitchableSelectionModel<ChunkOrLine>(new ProvidesKey<ChunkOrLine>()
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
            if (object.getLine() != null && object.getLine().getType() != Line.Type.Same)
               super.setSelected(object, selected);
         }
      };
      setSelectionModel(selectionModel_);

      setData(new ArrayList<ChunkOrLine>(), PatchMode.Working);
   }

   private String intToString(Integer value)
   {
      if (value == null)
         return "";
      return value.toString();
   }

   public boolean isShowActions()
   {
      return showActions_;
   }

   public void setShowActions(boolean showActions)
   {
      showActions_ = showActions;
      selectionModel_.setEnabled(showActions);
   }

   @Override
   public void setData(ArrayList<ChunkOrLine> diffData, PatchMode patchMode)
   {
      removeStyleName(RES.cellTableStyle().stageMode());
      removeStyleName(RES.cellTableStyle().workingMode());
      switch (patchMode)
      {
         case Stage:
            addStyleName(RES.cellTableStyle().stageMode());
            break;
         case Working:
            addStyleName(RES.cellTableStyle().workingMode());
            break;
      }

      lines_ = diffData;
      setPageSize(diffData.size());
      selectionModel_.clear();
      setRowData(diffData);

      startRows_.clear();
      endRows_.clear();

      Line.Type state = Line.Type.Same;
      boolean suppressNextStart = true; // Suppress at start to avoid 2px border
      for (int i = 0; i < lines_.size(); i++)
      {
         ChunkOrLine chunkOrLine = lines_.get(i);
         Line line = chunkOrLine.getLine();
         boolean isChunk = line == null;
         Line.Type newState = isChunk ? Line.Type.Same : line.getType();

         // Edge case: last line is a diff line
         if (newState != Line.Type.Same && i == lines_.size() - 1)
            endRows_.add(i);

         if (newState != state)
         {
            // Note: endRows_ doesn't include the borders between insertions and
            // deletions, or vice versa. This is to avoid 2px borders between
            // these regions when just about everything else is 1px.
            if (state != Line.Type.Same && newState == Line.Type.Same)
               endRows_.add(i-1);
            if (!suppressNextStart && newState != Line.Type.Same)
               startRows_.add(i);

            state = newState;
         }

         suppressNextStart = isChunk;
      }
   }

   @Override
   protected boolean canSelectVisibleRow(int visibleRow)
   {
      if (visibleRow < 0 || visibleRow >= lines_.size())
         return false;

      Line line = lines_.get(visibleRow).getLine();
      return line != null && (line.getType() == Type.Insertion
                              || line.getType() == Type.Deletion);
   }

   @Override
   public void clear()
   {
      setData(new ArrayList<ChunkOrLine>(), PatchMode.Working);
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

   @Override
   public HandlerRegistration addDiffLineActionHandler(DiffLineActionHandler handler)
   {
      return addHandler(handler, DiffLineActionEvent.TYPE);
   }

   @Override
   public HandlerRegistration addSelectionChangeHandler(SelectionChangeEvent.Handler handler)
   {
      return selectionModel_.addSelectionChangeHandler(handler);
   }

   public static void ensureStylesInjected()
   {
      RES.cellTableStyle().ensureInjected();
   }

   private boolean showActions_ = true;
   private ArrayList<ChunkOrLine> lines_;
   private SwitchableSelectionModel<ChunkOrLine> selectionModel_;
   private HashSet<Integer> startRows_ = new HashSet<Integer>();
   private HashSet<Integer> endRows_ = new HashSet<Integer>();
   private static final LineTableViewCellTableResources RES = GWT.create(LineTableViewCellTableResources.class);
}
