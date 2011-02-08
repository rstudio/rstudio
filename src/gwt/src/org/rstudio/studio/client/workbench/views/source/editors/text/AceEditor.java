package org.rstudio.studio.client.workbench.views.source.editors.text;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.PreElement;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.BlurHandler;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.KeyCodeEvent;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.event.shared.EventHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;
import org.rstudio.codemirror.client.events.EditorFocusHandler;
import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.Rectangle;
import org.rstudio.core.client.dom.IFrameElementEx;
import org.rstudio.core.client.events.NativeKeyDownHandler;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.core.client.widget.FontSizer.Size;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.codetools.CodeToolsServerOperations;
import org.rstudio.studio.client.common.filetypes.TextFileType;
import org.rstudio.studio.client.server.Void;
import org.rstudio.studio.client.workbench.model.ChangeTracker;
import org.rstudio.studio.client.workbench.model.EventBasedChangeTracker;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionManager;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionManager.InitCompletionFilter;
import org.rstudio.studio.client.workbench.views.console.shell.assist.CompletionPopupPanel;
import org.rstudio.studio.client.workbench.views.console.shell.assist.NullCompletionManager;
import org.rstudio.studio.client.workbench.views.console.shell.assist.RCompletionManager;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorDisplay;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorSelection;
import org.rstudio.studio.client.workbench.views.source.editors.text.TextEditingTarget.DocDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.*;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Renderer.ScreenCoordinates;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.EditorLoadedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.EditorLoadedHandler;

public class AceEditor implements DocDisplay, InputEditorDisplay
{
   /**
    * jcheng 2010-03-09: This exists merely to let me use the nice accessors on
    * KeyCodeEvent that decode NativeEvent info. Previously I would use
    * DomEvent.fireNativeEvent but that causes assertions sometimes under
    * OOPHM.
    */
   private class FakeKeyCodeEvent extends KeyCodeEvent<EventHandler>
   {
      public FakeKeyCodeEvent(NativeEvent nativeEvent)
      {
         super();
         setNativeEvent(nativeEvent);
      }

      @Override
      public Type<EventHandler> getAssociatedType()
      {
         assert false;
         return null;
      }

      @Override
      protected void dispatch(EventHandler handler)
      {
         assert false;
      }
   }

   private class Filter implements InitCompletionFilter
   {
      public boolean shouldComplete(NativeEvent event)
      {
         Range range = getSession().getSelection().getRange();
         if (!range.isEmpty())
            return false;

         int col = range.getStart().getColumn();
         if (col == 0)
            return false;

         String row = getSession().getLine(range.getStart().getRow());

         return row.substring(0, col).trim().length() != 0;
      }
   }

   private AceEditor(AceEditorWidget widget)
   {
      widget_ = widget;
      completionManager_ = new NullCompletionManager();
      RStudioGinjector.INSTANCE.injectMembers(this);

      widget_.addValueChangeHandler(new ValueChangeHandler<Void>()
      {
         public void onValueChange(ValueChangeEvent<Void> evt)
         {
            ValueChangeEvent.fire(AceEditor.this, null);
         }
      });

      widget_.addEditorLoadedHandler(new EditorLoadedHandler()
      {
         public void onEditorLoaded(EditorLoadedEvent event)
         {
            updateLanguage();
         }
      });
   }

   @Inject
   void initialize(CodeToolsServerOperations server)
   {
      server_ = server;
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
      fileType_ = fileType;
      if (isEditorLoaded())
         updateLanguage();
   }

   private void updateLanguage()
   {
      if (fileType_ == null)
         return;
      
      if (fileType_.canExecuteCode())
      {
         completionManager_ = new RCompletionManager(this,
                                                     new CompletionPopupPanel(),
                                                     server_,
                                                     new Filter());
      }
      else
         completionManager_ = new NullCompletionManager();

      widget_.getEditor().setKeyboardHandler(
            new AceCompletionAdapter(completionManager_).getKeyboardHandler());

      getSession().setEditorMode(fileType_.getEditorLanguage().getParserName());
      getSession().setUseWrapMode(fileType_.getWordWrap());
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

   public String getText()
   {
      return getSession().getLine(
            getSession().getSelection().getCursor().getRow());
   }

   public void setText(String string)
   {
      setCode(string);
   }

   public boolean hasSelection()
   {
      return true;
   }

   public InputEditorSelection getSelection()
   {
      Range selection = getSession().getSelection().getRange();
      return new InputEditorSelection(
            new AceInputEditorPosition(getSession(), selection.getStart()),
            new AceInputEditorPosition(getSession(), selection.getEnd()));

   }

   public String getSelectionValue()
   {
      return getSession().getTextRange(
            getSession().getSelection().getRange());
   }

   public void beginSetSelection(InputEditorSelection selection,
                                 Command callback)
   {
      AceInputEditorPosition start = (AceInputEditorPosition)selection.getStart();
      AceInputEditorPosition end = (AceInputEditorPosition)selection.getEnd();
      getSession().getSelection().setSelectionRange(Range.fromPoints(
            start.getValue(), end.getValue()));
      if (callback != null)
         callback.execute();
   }

   public Rectangle getCursorBounds()
   {
      Range range = getSession().getSelection().getRange();
      Renderer renderer = widget_.getEditor().getRenderer();
      ScreenCoordinates start = renderer.textToScreenCoordinates(
                  range.getStart().getRow(),
                  range.getStart().getColumn());
      ScreenCoordinates end = renderer.textToScreenCoordinates(
                  range.getEnd().getRow(),
                  range.getEnd().getColumn());
      // TODO: Use actual width and height
      return new Rectangle(start.getPageX(),
                           start.getPageY(),
                           end.getPageX() - start.getPageX(),
                           9);
   }

   public Rectangle getBounds()
   {
      return new Rectangle(
            widget_.getAbsoluteLeft(),
            widget_.getAbsoluteTop(),
            widget_.getOffsetWidth(),
            widget_.getOffsetHeight());
   }

   public void setFocus(boolean focused)
   {
      if (focused)
         widget_.getEditor().focus();
      else
         widget_.getEditor().blur();
   }

   public String replaceSelection(String value, boolean collapseSelection)
   {
      Selection selection = getSession().getSelection();
      String oldValue = getSession().getTextRange(selection.getRange());

      replaceSelection(value);

      if (collapseSelection)
      {
         collapseSelection(false);
      }

      return oldValue;
   }

   public boolean isSelectionCollapsed()
   {
      return getSession().getSelection().isEmpty();
   }

   public void clear()
   {
      setCode("");
   }

   public void collapseSelection(boolean collapseToStart)
   {
      Selection selection = getSession().getSelection();
      Range rng = selection.getRange();
      Position pos = collapseToStart ? rng.getStart() : rng.getEnd();
      selection.setSelectionRange(Range.fromPoints(pos, pos));
   }

   public InputEditorSelection getStart()
   {
      return new InputEditorSelection(
            new AceInputEditorPosition(getSession(), Position.create(0, 0)));
   }

   public InputEditorSelection getEnd()
   {
      EditSession session = getSession();
      int rows = session.getLength();
      Position end = Position.create(rows, session.getLine(rows).length());
      return new InputEditorSelection(new AceInputEditorPosition(session, end));
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
      return widget_.addNativeKeyDownHandler(handler);
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
      widget_.onResize();
   }

   public void setFontSize(Size size)
   {
      // No change needed--the AceEditorWidget uses the "normalSize" style 
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

   private boolean isEditorLoaded()
   {
      return widget_.getEditor() != null;
   }
   
   public EditSession getSession()
   {
      return widget_.getEditor().getSession();
   }

   public HandlerRegistration addBlurHandler(BlurHandler handler)
   {
      return widget_.addBlurHandler(handler);
   }

   public HandlerRegistration addClickHandler(ClickHandler handler)
   {
      return widget_.addClickHandler(handler);
   }

   public HandlerRegistration addFocusHandler(FocusHandler handler)
   {
      return widget_.addFocusHandler(handler);
   }

   public Widget getWidget()
   {
      return widget_;
   }

   private final HandlerManager handlers_ = new HandlerManager(this);
   private final AceEditorWidget widget_;
   private CompletionManager completionManager_;
   private CodeToolsServerOperations server_;
   private TextFileType fileType_;
}
