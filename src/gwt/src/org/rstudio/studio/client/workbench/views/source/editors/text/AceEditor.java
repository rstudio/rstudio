package org.rstudio.studio.client.workbench.views.source.editors.text;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.IFrameElement;
import com.google.gwt.dom.client.PreElement;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.codemirror.client.events.EditorFocusHandler;
import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.dom.IFrameElementEx;
import org.rstudio.core.client.events.NativeKeyDownHandler;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.core.client.widget.FontSizer.Size;
import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.model.ChangeTracker;
import org.rstudio.studio.client.workbench.model.EventBasedChangeTracker;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.EditSession;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Position;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;

public class AceEditor implements DocDisplay
{
   private AceEditor(AceEditorWidget widget)
   {
      widget_ = widget;
      widget_.addValueChangeHandler(new ValueChangeHandler<Void>()
      {
         public void onValueChange(ValueChangeEvent<Void> evt)
         {
            ValueChangeEvent.fire(AceEditor.this, null);
         }
      });
   }

   public static void create(final CommandWithArg<AceEditor> callback)
   {
      AceEditorWidget.create(new CommandWithArg<AceEditorWidget>()
      {
         public void execute(AceEditorWidget arg)
         {
            callback.execute(new AceEditor(arg));
         }
      });
   }

   public void setFileType(TextFileType fileType)
   {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   public String getCode()
   {
      return getSession().getValue();
   }

   public void setCode(String code)
   {
      widget_.setCode(code);
   }

   public void insertCode(String code, boolean blockMode)
   {
      // TODO: implement block mode
      getSession().replace(
            getSession().getSelection().getRange(), code);
   }

   public void focus()
   {
      widget_.getEditor().focus();
   }

   public void print()
   {
      String code = getCode();
      IFrameElementEx iframe = Document.get().createIFrameElement().cast();
      iframe.getStyle().setPosition(com.google.gwt.dom.client.Style.Position.ABSOLUTE);
      iframe.getStyle().setLeft(-5000, Unit.PX);
      Document.get().getBody().appendChild(iframe);
      Document childDoc = iframe.getContentWindow().getDocument();
      PreElement pre = childDoc.createPreElement();
      pre.setInnerText(code);
      pre.getStyle().setProperty("whiteSpace", "pre-wrap");
      childDoc.getBody().appendChild(pre);
      iframe.getContentWindow().print();
      iframe.removeFromParent();
   }

   public String getSelection()
   {
      return getSession().getTextRange(
            getSession().getSelection().getRange());
   }

   public String getCurrentLine()
   {
      int row = widget_.getEditor()
            .getSession()
            .getSelection()
            .getRange()
            .getStart()
            .getRow();
      return getSession().getLine(row);
   }

   public void replaceSelection(String code)
   {
      Range selRange = getSession().getSelection().getRange();
      Position position = getSession().replace(selRange, code);
      Range range = Range.fromPoints(selRange.getStart(), position);
      getSession().getSelection().setSelectionRange(range);
   }

   public boolean moveSelectionToNextLine()
   {
      int curRow = getSession().getSelection().getCursor().getRow();
      if (curRow < getSession().getLength() - 1)
      {
         String line = getSession().getLine(curRow + 1);
         Pattern pattern = Pattern.create("[^\\s]");
         Match match = pattern.match(line, 0);
         int col =  (match != null) ? match.getIndex() : 0;
         getSession().getSelection().moveCursorTo(curRow+1, col, false);
         return true;
      }
      return false;
   }

   public ChangeTracker getChangeTracker()
   {
      return new EventBasedChangeTracker<Void>(this);
   }

   public void setTextWrapping(boolean wrap)
   {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   public HandlerRegistration addEditorFocusHandler(EditorFocusHandler handler)
   {
      return null;  //To change body of implemented methods use File | Settings | File Templates.
   }

   public HandlerRegistration addNativeKeyDownHandler(NativeKeyDownHandler handler)
   {
      return null;  //To change body of implemented methods use File | Settings | File Templates.
   }

   public void markScrollPosition()
   {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   public void restoreScrollPosition()
   {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   public void fitSelectionToLines(boolean expand)
   {
      Range range = getSession().getSelection().getRange();
      Position start = range.getStart();
      Position newStart = start;

      if (start.getColumn() > 0)
      {
         if (expand)
         {
            newStart = Position.create(start.getRow(), 0);
         }
         else
         {
            String firstLine = getSession().getLine(start.getRow());
            if (firstLine.substring(0, start.getColumn()).trim().length() == 0)
               newStart = Position.create(start.getRow(), 0);
         }
      }

      Position end = range.getEnd();
      Position newEnd = end;
      if (expand)
      {
         int endRow = end.getRow();
         if (endRow == newStart.getRow() || end.getColumn() > 0)
         {
            // If selection ends at the start of a line, keep the selection
            // there--unless that means less than one line will be selected
            // in total.
            newEnd = Position.create(
                  endRow, getSession().getLine(endRow).length());
         }
      }
      else
      {
         while (newEnd.getRow() != newStart.getRow())
         {
            String line = getSession().getLine(newEnd.getRow());
            if (line.substring(0, newEnd.getColumn()).trim().length() != 0)
               break;

            int prevRow = newEnd.getRow() - 1;
            int len = getSession().getLine(prevRow).length();
            newEnd = Position.create(prevRow, len);
         }
      }

      getSession().getSelection().setSelectionRange(
            Range.fromPoints(newStart, newEnd));
   }

   public int getSelectionOffset(boolean start)
   {
      Range range = getSession().getSelection().getRange();
      if (start)
         return range.getStart().getColumn();
      else
         return range.getEnd().getColumn();
   }

   public void updateBodyMinHeight()
   {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   public void setFontSize(Size size)
   {
      //To change body of implemented methods use File | Settings | File Templates.
   }

   public HandlerRegistration addValueChangeHandler(
         ValueChangeHandler<Void> handler)
   {
      return handlers_.addHandler(ValueChangeEvent.getType(), handler);
   }

   public void fireEvent(GwtEvent<?> event)
   {
      handlers_.fireEvent(event);
   }

   public Widget toWidget()
   {
      return widget_;
   }

   private EditSession getSession()
   {
      return widget_.getEditor().getSession();
   }

   private final HandlerManager handlers_ = new HandlerManager(this);
   private final AceEditorWidget widget_;
}
