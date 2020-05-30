/*
 * LineTableView.java
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
package org.rstudio.studio.client.workbench.views.vcs.common.diff;

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
import com.google.gwt.view.client.SelectionChangeEvent.Handler;
import com.google.inject.Inject;
import org.rstudio.core.client.SafeHtmlUtil;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.dom.DomUtils.NodePredicate;
import org.rstudio.core.client.theme.RStudioCellTableStyle;
import org.rstudio.core.client.widget.FontSizer;
import org.rstudio.core.client.widget.MultiSelectCellTable;
import org.rstudio.studio.client.common.vcs.GitServerOperations.PatchMode;
import org.rstudio.studio.client.workbench.views.vcs.common.diff.Line.Type;
import org.rstudio.studio.client.workbench.views.vcs.common.diff.LineTablePresenter.Display;
import org.rstudio.studio.client.workbench.views.vcs.common.events.DiffChunkActionEvent;
import org.rstudio.studio.client.workbench.views.vcs.common.events.DiffChunkActionEvent.Action;
import org.rstudio.studio.client.workbench.views.vcs.common.events.DiffChunkActionHandler;
import org.rstudio.studio.client.workbench.views.vcs.common.events.DiffLinesActionEvent;
import org.rstudio.studio.client.workbench.views.vcs.common.events.DiffLinesActionHandler;

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
      String info();

      String lineNumber();
      String lastLineNumber();

      String actions();
      String lineActions();
      String chunkActions();

      String start();
      String end();

      String stageMode();
      String workingMode();
      String noStageMode();
   }

   public class LineContentCell extends AbstractCell<ChunkOrLine>
   {
      public LineContentCell()
      {
         super("mousedown");
      }

      @Override
      public void onBrowserEvent(Context context,
                                 Element parent,
                                 ChunkOrLine value,
                                 NativeEvent event,
                                 ValueUpdater<ChunkOrLine> chunkOrLineValueUpdater)
      {
         if ("mousedown".equals(event.getType())
             && event.getButton() == NativeEvent.BUTTON_LEFT
             && parent.isOrHasChild(event.getEventTarget().<Node>cast()))
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
                               ((Element) n).hasAttribute("data-action");
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
                  fireEvent(new DiffLinesActionEvent(action));
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
            if (showActions_
                && value.getLine().getType() != Line.Type.Same
                && value.getLine().getType() != Line.Type.Info
                && value == firstSelectedLine_)
            {
               renderActionButtons(
                     sb,
                     RES.cellTableStyle().lineActions(),
                     selectionModel_.getSelectedSet().size() > 1
                     ? " selection"
                     : " line");
            }
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
         if (action == Action.Stage)
         {
            blueButtonRenderer_.render(
                  sb, action.name() + labelSuffix, action.name());
         }
         else
         {
            grayButtonRenderer_.render(
                  sb, action.name() + labelSuffix, action.name());
         }
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

   public LineTableView(int filesCompared)
   {
      this(filesCompared,
           GWT.<LineTableViewCellTableResources>create(LineTableViewCellTableResources.class));
   }

   @Inject
   public LineTableView(final LineTableViewCellTableResources res)
   {
      this(2, res);
   }

   public LineTableView(int filesCompared,
                        final LineTableViewCellTableResources res)
   {
      super(1, res);

      FontSizer.applyNormalFontSize(this);
      addStyleName("rstudio-fixed-width-font");

      for (int i = 0; i < filesCompared; i++)
      {
         final int index = i;

         TextColumn<ChunkOrLine> col = new TextColumn<ChunkOrLine>()
         {
            @Override
            public String getValue(ChunkOrLine object)
            {
               Line line = object.getLine();
               if (line == null)
                  return "\u00A0";

               if (!line.getAppliesTo()[index])
                  return "\u00A0";

               return intToString(line.getLines()[index]);
            }
         };
         col.setHorizontalAlignment(TextColumn.ALIGN_RIGHT);
         addColumn(col);
         setColumnWidth(col, 100, Unit.PX);
         addColumnStyleName(i, res.cellTableStyle().lineNumber());
         if (i == filesCompared - 1)
            addColumnStyleName(i, res.cellTableStyle().lastLineNumber());
      }

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
                  case Info:
                     return prefix + res.cellTableStyle().info();
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
               return item.getChunk().getDiffIndex();
            else
               return item.getLine().getDiffIndex();
         }
      }) {
         @Override
         public void setSelected(ChunkOrLine object, boolean selected)
         {
            if (object.getLine() != null &&
                object.getLine().getType() != Line.Type.Same &&
                object.getLine().getType() != Line.Type.Info)
            {
               super.setSelected(object, selected);
            }
         }
      };
      selectionModel_.addSelectionChangeHandler(new Handler()
      {
         @Override
         public void onSelectionChange(SelectionChangeEvent event)
         {
            ChunkOrLine newFirstSelectedLine = null;
            for (ChunkOrLine value : selectionModel_.getSelectedSet())
            {
               if (value.getLine() != null &&
                   (newFirstSelectedLine == null || newFirstSelectedLine.getLine().compareTo(value.getLine()) > 0))
               {
                  newFirstSelectedLine = value;
               }
            }

            if (newFirstSelectedLine != null)
               refreshValue(newFirstSelectedLine);
            if (firstSelectedLine_ != newFirstSelectedLine)
            {
               if (firstSelectedLine_ != null)
                  refreshValue(firstSelectedLine_);
            }

            firstSelectedLine_ = newFirstSelectedLine;
         }
      });
      setSelectionModel(selectionModel_);

      setData(new ArrayList<ChunkOrLine>(), PatchMode.Working);
   }

   private void refreshValue(ChunkOrLine value)
   {
      int index = lines_.indexOf(value);
      if (index >= 0)
      {
         ArrayList<ChunkOrLine> list = new ArrayList<ChunkOrLine>();
         list.add(value);
         setRowData(index, list);
      }

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

   public void hideStageCommands()
   {
      addStyleName(RES.cellTableStyle().noStageMode());
   }

   public void setUseStartBorder(boolean useStartBorder)
   {
      useStartBorder_ = useStartBorder;
   }

   public void setUseEndBorder(boolean useEndBorder)
   {
      useEndBorder_ = useEndBorder;
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
      firstSelectedLine_ = null;
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

         if (useStartBorder_ && i == 0)
            startRows_.add(i);

         // Edge case: last line is a diff line
         if (useEndBorder_ && i == lines_.size() - 1)
            endRows_.add(i);

         if (newState != state)
         {
            // Note: endRows_ doesn't include the borders between insertions and
            // deletions, or vice versa. This is to avoid 2px borders between
            // these regions when just about everything else is 1px.
            if (state != Line.Type.Same && newState == Line.Type.Same && !isChunk)
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
   public HandlerRegistration addDiffLineActionHandler(DiffLinesActionHandler handler)
   {
      return addHandler(handler, DiffLinesActionEvent.TYPE);
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
   private boolean useStartBorder_ = false;
   private boolean useEndBorder_ = true;
   // Keep explicit track of the first selected line so we can render it differently
   private ChunkOrLine firstSelectedLine_;
   private static final LineTableViewCellTableResources RES = GWT.create(LineTableViewCellTableResources.class);
   private static final LineActionButtonRenderer blueButtonRenderer_ = LineActionButtonRenderer.createBlue();
   private static final LineActionButtonRenderer grayButtonRenderer_ = LineActionButtonRenderer.createGray();
}
