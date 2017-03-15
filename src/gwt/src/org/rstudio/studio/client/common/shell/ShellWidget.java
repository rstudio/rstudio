/*
 * ShellWidget.java
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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
package org.rstudio.studio.client.common.shell;

import java.util.Map;
import java.util.TreeMap;

import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.TimeBufferedCommand;
import org.rstudio.core.client.VirtualConsole;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.jsonrpc.RpcObjectList;
import org.rstudio.core.client.widget.BottomScrollPanel;
import org.rstudio.core.client.widget.FontSizer;
import org.rstudio.core.client.widget.PreWidget;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.debugging.model.UnhandledError;
import org.rstudio.studio.client.common.debugging.ui.ConsoleError;
import org.rstudio.studio.client.workbench.model.ConsoleAction;
import org.rstudio.studio.client.workbench.views.console.ConsoleResources;
import org.rstudio.studio.client.workbench.views.console.events.RunCommandWithDebugEvent;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor.NewLineMode;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.CursorChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.CursorChangedHandler;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.PasteEvent;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.FocusHandler;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class ShellWidget extends Composite implements ShellDisplay,
                                                      RequiresResize,
                                                      ConsoleError.Observer
{
   public ShellWidget(AceEditor editor, EventBus events)
   {
      styles_ = ConsoleResources.INSTANCE.consoleStyles();
      events_ = events;
      
      SelectInputClickHandler secondaryInputHandler = new SelectInputClickHandler();

      output_ = new PreWidget();
      output_.setStylePrimaryName(styles_.output());
      output_.addClickHandler(secondaryInputHandler);
      ElementIds.assignElementId(output_.getElement(), 
                                 ElementIds.CONSOLE_OUTPUT);
      output_.addPasteHandler(secondaryInputHandler);

      pendingInput_ = new PreWidget();
      pendingInput_.setStyleName(styles_.output());
      pendingInput_.addClickHandler(secondaryInputHandler);

      prompt_ = new HTML() ;
      prompt_.setStylePrimaryName(styles_.prompt()) ;
      prompt_.addStyleName(KEYWORD_CLASS_NAME);

      input_ = editor ;
      input_.setShowLineNumbers(false);
      input_.setShowPrintMargin(false);
      if (!Desktop.isDesktop())
         input_.setNewLineMode(NewLineMode.Unix);
      input_.setUseWrapMode(true);
      input_.setPadding(0);
      input_.autoHeight();
      final Widget inputWidget = input_.asWidget();
      ElementIds.assignElementId(inputWidget.getElement(),
                                 ElementIds.CONSOLE_INPUT);
      input_.addClickHandler(secondaryInputHandler) ;
      inputWidget.addStyleName(styles_.input());
      input_.addCursorChangedHandler(new CursorChangedHandler()
      {
         public void onCursorChanged(CursorChangedEvent event)
         {
            Scheduler.get().scheduleDeferred(new ScheduledCommand()
            {
               @Override
               public void execute()
               {
                  input_.scrollToCursor(scrollPanel_, 8, 60);
               }
            });
         }
      });
      input_.addCapturingKeyDownHandler(new KeyDownHandler()
      {
         @Override
         public void onKeyDown(KeyDownEvent event)
         {
            // Don't capture keys when a completion popup is visible.
            if (input_.isPopupVisible())
               return;
            
            // If the user hits Page-Up from inside the console input, we need
            // to simulate pageup because focus is not contained in the scroll
            // panel (it's in the hidden textarea that Ace uses under the
            // covers).

            int keyCode = event.getNativeKeyCode();
            switch (keyCode)
            {
               case KeyCodes.KEY_PAGEUP:
                  event.stopPropagation();
                  event.preventDefault();

                  // Can't scroll any further up. Return before we change focus.
                  if (scrollPanel_.getVerticalScrollPosition() == 0)
                     return;

                  scrollPanel_.focus();
                  int newScrollTop = scrollPanel_.getVerticalScrollPosition() -
                                     scrollPanel_.getOffsetHeight() + 40;
                  scrollPanel_.setVerticalScrollPosition(Math.max(0, newScrollTop));
                  break;
            }
         }
      });
      input_.addFocusHandler(new FocusHandler()
      {
         @Override
         public void onFocus(FocusEvent event)
         {
            scrollToBottom();
         }
      });

      inputLine_ = new DockPanel();
      inputLine_.setHorizontalAlignment(DockPanel.ALIGN_LEFT);
      inputLine_.setVerticalAlignment(DockPanel.ALIGN_TOP);
      inputLine_.add(prompt_, DockPanel.WEST);
      inputLine_.setCellWidth(prompt_, "1");
      inputLine_.add(input_.asWidget(), DockPanel.CENTER);
      inputLine_.setCellWidth(input_.asWidget(), "100%");
      inputLine_.setWidth("100%");

      verticalPanel_ = new VerticalPanel() ;
      verticalPanel_.setStylePrimaryName(styles_.console());
      verticalPanel_.addStyleName("ace_text-layer");
      verticalPanel_.addStyleName("ace_line");
      FontSizer.applyNormalFontSize(verticalPanel_);
      verticalPanel_.add(output_) ;
      verticalPanel_.add(pendingInput_) ;
      verticalPanel_.add(inputLine_) ;
      verticalPanel_.setWidth("100%") ;

      scrollPanel_ = new ClickableScrollPanel() ;
      scrollPanel_.setWidget(verticalPanel_) ;
      scrollPanel_.addStyleName("ace_editor");
      scrollPanel_.addStyleName("ace_scroller");
      scrollPanel_.addClickHandler(secondaryInputHandler);
      scrollPanel_.addKeyDownHandler(secondaryInputHandler);

      secondaryInputHandler.setInput(editor);

      resizeCommand_ = new TimeBufferedCommand(5)
      {
         @Override
         protected void performAction(boolean shouldSchedulePassive)
         {
            scrollPanel_.onContentSizeChanged();
            if (!DomUtils.selectionExists() && !scrollPanel_.isScrolledToBottom())
               scrollPanel_.scrollToBottom();
         }
      };

      initWidget(scrollPanel_) ;

      addCopyHook(getElement());
   }

   private native void addCopyHook(Element element) /*-{
      if ($wnd.desktop) {
         var clean = function() {
            setTimeout(function() {
               $wnd.desktop.cleanClipboard(true);
            }, 100)
         };
         element.addEventListener("copy", clean, true);
         element.addEventListener("cut", clean, true);
      }
   }-*/;

 
   public void scrollToBottom()
   {
      scrollPanel_.scrollToBottom();
   }

   private boolean initialized_ = false;
   @Override
   protected void onLoad()
   {
      super.onLoad();
      if (!initialized_)
      {
         initialized_ = true;
         Scheduler.get().scheduleDeferred(new ScheduledCommand()
         {
            public void execute()
            {
               doOnLoad();
               scrollPanel_.scrollToBottom();
            }
         });
      }

      ElementIds.assignElementId(this.getElement(), ElementIds.SHELL_WIDGET);
   }

   protected void doOnLoad()
   {
      input_.autoHeight();
      // Console scroll pos jumps on first typing without this, because the
      // textarea is in the upper left corner of the screen and when focus
      // moves to it scrolling ensues.
      input_.forceCursorChange();
   }

   public void setSuppressPendingInput(boolean suppressPendingInput)
   {
      suppressPendingInput_ = suppressPendingInput;
   }
   
   public void consoleWriteError(final String error)
   {
      clearPendingInput();
      output(error, getErrorClass(), false);

      // Pick up the last element emitted to the console. If we get extended
      // information for this error, we'll need to swap out the simple error
      // element for the extended error element. 
      Element outputElement = output_.getElement();
      if (outputElement.hasChildNodes())
      {
         // the last output group includes a <span> for each output type; pick
         // up the one referring to the error we just emitted
         Node lastNode = outputElement.getChild(
               outputElement.getChildCount() - 1);
         if (lastNode.hasChildNodes())
         {
            Node errorNode = lastNode.getChild(lastNode.getChildCount() - 1);
            if (clearErrors_)
            {
               errorNodes_.clear();
               clearErrors_ = false;
            }
            errorNodes_.put(error, errorNode);
         }
      }
   }
   
   public void consoleWriteExtendedError(
         final String error, UnhandledError traceInfo, 
         boolean expand, String command)
   {
      if (errorNodes_.containsKey(error))
      {
         Node errorNode = errorNodes_.get(error);
         clearPendingInput();
         ConsoleError errorWidget = new ConsoleError(
               traceInfo, getErrorClass(), this, command);
   
         if (expand)
            errorWidget.setTracebackVisible(true);
         
         errorNode.getParentNode().replaceChild(errorWidget.getElement(),
                                           errorNode);
         
         scrollPanel_.onContentSizeChanged();
         errorNodes_.remove(error);
      }
   }
   
   @Override
   public void runCommandWithDebug(String command)
   {
      events_.fireEvent(new RunCommandWithDebugEvent(command));
   }

   public void consoleWriteOutput(final String output)
   {
      clearPendingInput();
      output(output, styles_.output(), false);
   }

   public void consoleWriteInput(final String input, String console)
   {
      // if coming from another console id (i.e. notebook chunk), clear the
      // prompt since this input hasn't been processed yet (we'll redraw when
      // the prompt reappears)
      if (!StringUtil.isNullOrEmpty(console))
         prompt_.setHTML("");

      clearPendingInput();
      output(input, styles_.command() + KEYWORD_CLASS_NAME, false);
   }
   
   private void clearPendingInput()
   {
      pendingInput_.setText("");
      pendingInput_.setVisible(false);
   }

   public void consoleWritePrompt(final String prompt)
   {
      output(prompt, styles_.prompt() + KEYWORD_CLASS_NAME, false);
      clearErrors_ = true;
   }

   public void consolePrompt(String prompt, boolean showInput)
   {
      if (prompt != null)
         prompt = VirtualConsole.consolify(prompt);

      prompt_.getElement().setInnerText(prompt);
      //input_.clear() ;
      ensureInputVisible();

      // Deal gracefully with multi-line prompts
      int promptLines = StringUtil.notNull(prompt).split("\\n").length;
      input_.asWidget().getElement().getStyle().setPaddingTop((promptLines - 1) * 15,
                                                   Unit.PX);
      
      input_.setPasswordMode(!showInput);
      clearErrors_ = true;
      
      // make sure we're starting on a new line
      if (virtualConsole_ != null)
      {
         Node child = virtualConsole_.getParent().getLastChild();
         if (child != null &&
             child.getNodeType() == Node.ELEMENT_NODE &&
             !Element.as(child).getInnerText().endsWith("\n"))
         {
            virtualConsole_.submit("\n");
         }
         // clear the virtual console so we start with a fresh slate
         virtualConsole_ = null;
      }
   }

   public void ensureInputVisible()
   {
      scrollPanel_.scrollToBottom();
   }
   
   private String getErrorClass()
   {
      return styles_.error() + " " + 
             RStudioGinjector.INSTANCE.getUIPrefs().getThemeErrorClass();
   }

   private boolean output(String text,
                          String className,
                          boolean addToTop)
   {
      if (text.indexOf('\f') >= 0)
         clearOutput();

      Element outEl = output_.getElement();
      
      if (addToTop)
      {
         SpanElement leading = Document.get().createSpanElement();
         outEl.insertFirst(leading);
         VirtualConsole console = new VirtualConsole(leading);
         console.submit(text, className);
         lines_ += DomUtils.countLines(leading, true);
      }
      else
      {
         // create trailing output console if it doesn't already exist 
         if (virtualConsole_ == null)
         {
            SpanElement trailing = Document.get().createSpanElement();
            outEl.appendChild(trailing);
            virtualConsole_ = new VirtualConsole(trailing);
         }

         int oldLineCount = DomUtils.countLines(
               virtualConsole_.getParent(), true);
         virtualConsole_.submit(text, className);
         int newLineCount = DomUtils.countLines(
               virtualConsole_.getParent(), true);
         lines_ += newLineCount - oldLineCount;
      }

      boolean result = !trimExcess();

      resizeCommand_.nudge();
      
      return result;
   }

   private String ensureNewLine(String s)
   {
      if (s.length() == 0 || s.charAt(s.length() - 1) == '\n')
         return s;
      else
         return s + '\n';
   }

   private boolean trimExcess()
   {
      if (maxLines_ <= 0)
         return false;  // No limit in effect

      int linesToTrim = lines_ - maxLines_;
      if (linesToTrim > 0)
      {
         lines_ -= DomUtils.trimLines(output_.getElement(), linesToTrim);
         return true;
      }

      return false;
   }

   public void playbackActions(final RpcObjectList<ConsoleAction> actions)
   {
      Scheduler.get().scheduleIncremental(new RepeatingCommand()
      {
         private int i = actions.length() - 1;
         private int chunksize = 1000;

         public boolean execute()
         {
            int end = i - chunksize;
            chunksize = 10;
            for (; i > end && i >= 0; i--)
            {
               // User hit Ctrl+L at some point--we're done.
               if (cleared_)
                  return false;

               boolean canContinue = false;

               ConsoleAction action = actions.get(i);
               switch (action.getType())
               {
                  case ConsoleAction.INPUT:
                     canContinue = output(action.getData() + "\n",
                                          styles_.command() + " " + KEYWORD_CLASS_NAME,
                                          true);
                     break;
                  case ConsoleAction.OUTPUT:
                     canContinue = output(action.getData(),
                                          styles_.output(),
                                          true);
                     break;
                  case ConsoleAction.ERROR:
                     canContinue = output(action.getData(),
                                          styles_.error(),
                                          true);
                     break;
                  case ConsoleAction.PROMPT:
                     canContinue = output(action.getData(),
                                          styles_.prompt() + " " + KEYWORD_CLASS_NAME,
                                          true);
                     break;
               }
               if (!canContinue)
                  return false;
            }
            if (!DomUtils.selectionExists())
               scrollPanel_.scrollToBottom();

            return i >= 0;
         }
      });
   }

   public void focus()
   {
      input_.setFocus(true) ;
   }
   
   /**
    * Directs focus/selection to the input box when a (different) widget
    * is clicked.
    */
   private class SelectInputClickHandler implements ClickHandler,
                                                    KeyDownHandler,
                                                    PasteEvent.Handler
   {
      public void onClick(ClickEvent event)
      {
         // If clicking on the input panel already, stop propagation.
         if (event.getSource() == input_)
         {
            event.stopPropagation();
            return;
         }

         // Don't drive focus to the input unless there is no selection.
         // Otherwise it would interfere with the ability to select stuff
         // from the output buffer for copying to the clipboard.
         if (!DomUtils.selectionExists() && isInputOnscreen())
            input_.setFocus(true) ;
      }

      public void onKeyDown(KeyDownEvent event)
      {
         if (event.getSource() == input_)
            return;

         // Filter out some keystrokes you might reasonably expect to keep
         // focus inside the output pane
         switch (event.getNativeKeyCode())
         {
            case KeyCodes.KEY_PAGEDOWN:
            case KeyCodes.KEY_PAGEUP:
            case KeyCodes.KEY_HOME:
            case KeyCodes.KEY_END:
            case KeyCodes.KEY_CTRL:
            case KeyCodes.KEY_ALT:
            case KeyCodes.KEY_SHIFT:
            case 224: // META (Command) on Firefox/Mac
               return;
            case 91: case 93: // Left/Right META (Command), but also [ and ], on Safari
               if (event.isMetaKeyDown())
                  return;
               break;
            case 'C':
               if (event.isControlKeyDown() || event.isMetaKeyDown())
                  return;
               break;
         }
         input_.setFocus(true);
         delegateEvent(input_.asWidget(), event);
      }
      
      public void onPaste(PasteEvent event)
      {
         // When pasting, focus the input so it'll receive the pasted text
         input_.setFocus(true);
      }
      
      public void setInput(AceEditor input)
      {
         input_ = input;
      }

      private AceEditor input_;
   }

   private boolean isInputOnscreen()
   {
      return DomUtils.isVisibleVert(scrollPanel_.getElement(),
                                    inputLine_.getElement());
   }

   protected class ClickableScrollPanel extends BottomScrollPanel
   {
      private ClickableScrollPanel()
      {
         super();
         getElement().setTabIndex(-1);
      }

      public HandlerRegistration addClickHandler(ClickHandler handler)
      {
         return addDomHandler(handler, ClickEvent.getType());
      }

      public HandlerRegistration addKeyDownHandler(KeyDownHandler handler)
      {
         return addDomHandler(handler, KeyDownEvent.getType());
      }

      public void focus()
      {
         getElement().focus();
      }
   }

   public void clearOutput()
   {
      output_.setText("") ;
      lines_ = 0;
      cleared_ = true;
      trailingOutput_ = null;
      virtualConsole_ = null;
   }
   
   public InputEditorDisplay getInputEditorDisplay()
   {
      return input_ ;
   }

   public String processCommandEntry()
   {
      // parse out the command text
      String promptText = prompt_.getElement().getInnerText();
      String commandText = input_.getCode();
      input_.setText("");
      // Force render to avoid subtle command movement in the console, caused
      // by the prompt disappearing before the input line does
      input_.forceImmediateRender();
      prompt_.setHTML("");

      SpanElement pendingPrompt = Document.get().createSpanElement();
      pendingPrompt.setInnerText(promptText);
      pendingPrompt.setClassName(styles_.prompt() + " " + KEYWORD_CLASS_NAME);

      if (!suppressPendingInput_ && !input_.isPasswordMode())
      {
         SpanElement pendingInput = Document.get().createSpanElement();
         String[] lines = StringUtil.notNull(commandText).split("\n");
         String firstLine = lines.length > 0 ? lines[0] : "";
         pendingInput.setInnerText(firstLine + "\n");
         pendingInput.setClassName(styles_.command() + " " + KEYWORD_CLASS_NAME);
         pendingInput_.getElement().appendChild(pendingPrompt);
         pendingInput_.getElement().appendChild(pendingInput);
         pendingInput_.setVisible(true);
      }

      ensureInputVisible();

      return commandText ;
   }

   public HandlerRegistration addCapturingKeyDownHandler(KeyDownHandler handler)
   {
      return input_.addCapturingKeyDownHandler(handler) ;
   }

   public HandlerRegistration addKeyPressHandler(KeyPressHandler handler)
   {
      return input_.addKeyPressHandler(handler) ;
   }
   
   @Override
   public HandlerRegistration addCapturingKeyUpHandler(KeyUpHandler handler)
   {
      return input_.addCapturingKeyUpHandler(handler);
   }

   @Override
   public HandlerRegistration addKeyUpHandler(KeyUpHandler handler)
   {
      return input_.addKeyUpHandler(handler);
   }

   public int getCharacterWidth()
   {
      return DomUtils.getCharacterWidth(getElement(), styles_.console());
   }
   
   public boolean isPromptEmpty()
   {
      return StringUtil.isNullOrEmpty(prompt_.getText());
   }
   
   public String getPromptText()
   {
      return StringUtil.notNull(prompt_.getText());
   }
   
   public void setReadOnly(boolean readOnly)
   {
      input_.setReadOnly(readOnly);
   }

   public int getMaxOutputLines()
   {
      return maxLines_;
   }
   
   public void setMaxOutputLines(int maxLines)
   {
      maxLines_ = maxLines;
      trimExcess();
   }
   
   @Override
   public Widget getShellWidget()
   {
      return this;
   }

   public void onResize()
   {
      if (getWidget() instanceof RequiresResize)
         ((RequiresResize)getWidget()).onResize();
   }

   @Override
   public void onErrorBoxResize()
   {
      scrollPanel_.onContentSizeChanged();
   }
   
   private int lines_ = 0;
   private int maxLines_ = -1;
   private boolean cleared_ = false;
   private final PreWidget output_ ;
   private PreWidget pendingInput_ ;
   // Save a reference to the most recent output text node in case the
   // next bit of output contains \b or \r control characters
   private PreWidget trailingOutput_ ;
   private VirtualConsole virtualConsole_ ;
   private final HTML prompt_ ;
   protected final AceEditor input_ ;
   private final DockPanel inputLine_ ;
   private final VerticalPanel verticalPanel_ ;
   protected final ClickableScrollPanel scrollPanel_ ;
   private ConsoleResources.ConsoleStyles styles_;
   private final TimeBufferedCommand resizeCommand_;
   private boolean suppressPendingInput_;
   private final EventBus events_;
   
   // A list of errors that have occurred between console prompts. 
   private Map<String, Node> errorNodes_ = new TreeMap<String, Node>();
   private boolean clearErrors_ = false;

   private static final String KEYWORD_CLASS_NAME = ConsoleResources.KEYWORD_CLASS_NAME;
}
