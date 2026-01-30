/*
 * ShellWidget.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.common.shell;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.rstudio.core.client.AnsiCode;
import org.rstudio.core.client.ConsoleOutputWriter;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.JsVector;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.TimeBufferedCommand;
import org.rstudio.core.client.VirtualConsole;
import org.rstudio.core.client.dom.DOMRect;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.jsonrpc.RpcObjectList;
import org.rstudio.core.client.widget.BottomScrollPanel;
import org.rstudio.core.client.widget.FontSizer;
import org.rstudio.core.client.widget.PreWidget;
import org.rstudio.core.client.widget.find.BrowserSelectionFindBar;
import org.rstudio.core.client.widget.find.FindBar;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.ApplicationAutomation;
import org.rstudio.studio.client.application.AriaLiveService;
import org.rstudio.studio.client.application.Desktop;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.debugging.model.UnhandledError;
import org.rstudio.studio.client.common.debugging.ui.ConsoleError;
import org.rstudio.studio.client.workbench.model.ConsoleAction;
import org.rstudio.studio.client.workbench.model.Session;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefs;
import org.rstudio.studio.client.workbench.prefs.model.UserState;
import org.rstudio.studio.client.workbench.views.console.ConsoleResources;
import org.rstudio.studio.client.workbench.views.console.events.RunCommandWithDebugEvent;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor.NewLineMode;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Renderer;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.CursorChangedEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.PasteEvent;
import org.rstudio.studio.client.workbench.views.source.editors.text.events.RenderFinishedEvent;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.BlurEvent;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.dom.client.FocusEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.DockLayoutPanel;
import com.google.gwt.user.client.ui.DockPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;
import com.google.inject.Inject;

import jsinterop.base.Js;

public class ShellWidget extends Composite implements ShellDisplay,
                                                      RequiresResize,
                                                      ConsoleError.Observer
{
   public static interface ErrorClass
   {
      public String get();
   }
   
   public ShellWidget(AceEditor editor,
                      UserPrefs prefs,
                      EventBus events,
                      AriaLiveService ariaLive,
                      String outputLabel)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      
      styles_ = ConsoleResources.INSTANCE.consoleStyles();
      events_ = events;
      prefs_ = prefs;
      ariaLive_ = ariaLive;

      SelectInputClickHandler secondaryInputHandler = new SelectInputClickHandler(true);

      container_ = new DockLayoutPanel(Unit.PX);

      output_ = new ConsoleOutputWriter(RStudioGinjector.INSTANCE.getVirtualConsoleFactory(), outputLabel);
      output_.getWidget().setStylePrimaryName(styles_.output());
      output_.getWidget().addClickHandler(secondaryInputHandler);
      ElementIds.assignElementId(output_.getElement(), ElementIds.CONSOLE_OUTPUT);
      output_.getWidget().addPasteHandler(secondaryInputHandler);

      if (prefs_ != null)
      {
         syncOutputStyles();
      }

      pendingInput_ = new PreWidget();
      pendingInput_.setStyleName(styles_.output());
      pendingInput_.addClickHandler(secondaryInputHandler);

      prompt_ = new HTML();
      prompt_.setStylePrimaryName(styles_.prompt());
      prompt_.addStyleName(KEYWORD_CLASS_NAME);

      input_ = editor;
      renderer_ = input_.getWidget().getEditor().getRenderer();

      input_.setShowLineNumbers(false);
      input_.setShowPrintMargin(false);
      input_.setUseWrapMode(true);
      input_.setPadding(0);
      input_.autoHeight();

      if (!Desktop.isDesktop())
         input_.setNewLineMode(NewLineMode.Unix);

      input_.addClickHandler(secondaryInputHandler);
      
      windowBlurHandler_ = WindowEx.addBlurHandler((BlurEvent event) ->
      {
         // https://github.com/rstudio/rstudio/issues/1638
         ignoreNextFocus_ = true;
      });
      
      input_.addFocusHandler((FocusEvent event) ->
      {
         if (ignoreNextFocus_)
         {
            ignoreNextFocus_ = false;
            return;
         }

         scrollIntoView();  
      });

      // NOTE: we cannot scroll into view immediately after the cursor
      // has changed, as Ace may not have rendered the updated cursor
      // position yet. For that reason, we set the pending scroll flag
      // and allow it to happen at the completion of next Ace render.
      input_.addCursorChangedHandler((CursorChangedEvent event) ->
      {
         scrollIntoViewPending_ = true;
      });

      // This one is kind of awkward. If a user pastes multi-line content
      // into the Ace instance, it might force the scroll panel to render
      // the associated scrollbar. However, doing so will also force Ace
      // to re-wrap code, since the editor width has effectively been
      // decreased in response to the new scroller being rendered. This
      // unfortunately leads to an awkward case where pasting multi-line
      // code can actually cause Ace to stop rendering the last few lines
      // of pasted content.
      //
      // The solution here is to force Ace to check whether a resize
      // is necessary after a render has finished.
      //
      // We also check whether any other code has requested a scroll into
      // view at this point as well, since the rendered cursor implies we
      // can correctly compute the scroll position.
      input_.addRenderFinishedHandler(new RenderFinishedEvent.Handler()
      {
         @Override
         public void onRenderFinished(RenderFinishedEvent event)
         {
            checkForResize();
            checkForPendingScroll();
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

            // If the user hits PageUp or PageDown from inside the console
            // input, we need to simulate its action because focus is not contained
            // in the scroll panel (it's in the hidden textarea that Ace uses
            // under the covers).

            int keyCode = event.getNativeKeyCode();
            switch (keyCode)
            {
               case KeyCodes.KEY_PAGEUP:
               {
                  event.stopPropagation();
                  event.preventDefault();

                  // Can't scroll any further up. Return before we change focus.
                  if (scrollPanel_.getVerticalScrollPosition() == 0)
                     return;

                  int newScrollTop =
                        scrollPanel_.getVerticalScrollPosition() -
                        scrollPanel_.getOffsetHeight() +
                        40;

                  scrollPanel_.focus();
                  scrollPanel_.setVerticalScrollPosition(Math.max(0, newScrollTop));
                  break;
               }

               case KeyCodes.KEY_PAGEDOWN:
               {
                  event.stopPropagation();
                  event.preventDefault();

                  if (scrollPanel_.isScrolledToBottom())
                     return;

                  int newScrollTop =
                        scrollPanel_.getVerticalScrollPosition() +
                        scrollPanel_.getOffsetHeight() -
                        40;

                  scrollPanel_.focus();
                  scrollPanel_.setVerticalScrollPosition(newScrollTop);
                  break;
               }
            }
         }
      });

      final Widget inputWidget = input_.asWidget();
      ElementIds.assignElementId(inputWidget.getElement(), ElementIds.CONSOLE_INPUT);
      inputWidget.addStyleName(styles_.input());

      inputLine_ = new DockPanel();
      inputLine_.setHorizontalAlignment(DockPanel.ALIGN_LEFT);
      inputLine_.setVerticalAlignment(DockPanel.ALIGN_TOP);
      inputLine_.add(prompt_, DockPanel.WEST);
      inputLine_.setCellWidth(prompt_, "1");
      inputLine_.add(input_.asWidget(), DockPanel.CENTER);
      inputLine_.setCellWidth(input_.asWidget(), "100%");
      inputLine_.setWidth("100%");

      verticalPanel_ = new VerticalPanel();
      verticalPanel_.setStylePrimaryName(styles_.console());
      FontSizer.applyNormalFontSize(verticalPanel_);
      verticalPanel_.add(output_.getWidget());
      verticalPanel_.add(pendingInput_);
      verticalPanel_.add(inputLine_);
      verticalPanel_.setWidth("100%");

      scrollPanel_ = new ClickableScrollPanel(true);
      scrollPanel_.setWidget(verticalPanel_);
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
      
      errorClass_ = new ErrorClass()
      {
         @Override
         public String get()
         {
            return styles_.error();
         }
      };
      
      findBar_ = new BrowserSelectionFindBar(container_, scrollPanel_, Js.cast(output_.getElement()));
      container_.addNorth(findBar_, findBar_.getHeightPx());
      container_.setWidgetHidden(findBar_, true);
      container_.add(scrollPanel_);
      initWidget(container_);

      addDomHandler(secondaryInputHandler, ClickEvent.getType());
      
      addCopyHook(getElement());
      addPasteHook(getElement());
   }
   
   @Inject
   private void initialize(ApplicationAutomation automation,
                           Session session,
                           UserState userState)
   {
      automation_ = automation;
      session_ = session;
      userState_ = userState;
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
   
   private native final void addPasteHook(Element el) /*-{
   
      var self = this;
      var eventTarget = null;
      
      $doc.body.addEventListener("paste", function(event) {
         
         // check whether we lost focus, and the paste event is now just targeting the body element
         // this seems to happen on Windows for whatever reason
         // https://github.com/rstudio/rstudio/issues/14538#issuecomment-2452526631
         if (eventTarget != null && event.target === $doc.body) {
            var text = event.clipboardData.getData("text/plain");
            self.@org.rstudio.studio.client.common.shell.ShellWidget::onPaste(*)(event, text);
         }
         
         // reset event target, so that this handler only ever tries to fire once
         eventTarget = null;
         
      }, true);
      
      $doc.body.addEventListener("contextmenu", function(event) {
         
         if (el.contains(event.target)) {
            
            // save the 'contextmenu' event target, in case 'paste' is fired with
            // focus lost and sent to the 'body' element
            eventTarget = event.target;
         
            // make this element briefly editable, so the generated
            // context menu will include 'Paste' as an option
            el.setAttribute("contenteditable", "true");
            setTimeout(function() { el.setAttribute("contenteditable", "false"); }, 0);
            
         }
         
      }, true);
   
   }-*/;
   
   private void onPaste(NativeEvent event, String text)
   {
      event.stopPropagation();
      event.preventDefault();
      
      input_.focus();
      input_.insertCode(text);
   }


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
         Scheduler.get().scheduleDeferred(() -> {
            doOnLoad();
            scrollPanel_.scrollToBottom();
         });
      }

      ElementIds.assignElementId(this.getElement(), ElementIds.SHELL_WIDGET);
      getElement().addClassName("rstudio_shell_widget");

      if (prefs_ != null && consoleSoftWrapHandler_ == null)
      {
         consoleSoftWrapHandler_ = prefs_.consoleSoftWrap().addValueChangeHandler(
            event -> syncOutputStyles());
      }
   }

   protected void doOnLoad()
   {
      input_.autoHeight();
      // Console scroll pos jumps on first typing without this, because the
      // textarea is in the upper left corner of the screen and when focus
      // moves to it scrolling ensues.
      input_.forceCursorChange();
   }
   
   @Override
   public void setBusy(boolean busy)
   {
      Element el = input_.getWidget().getElement();
      if (busy)
      {
         el.addClassName(RSTUDIO_CONSOLE_BUSY);
      }
      else
      {
         el.removeClassName(RSTUDIO_CONSOLE_BUSY);
      }
   }

   @Override
   public void setSuppressPendingInput(boolean suppressPendingInput)
   {
      suppressPendingInput_ = suppressPendingInput;
   }
   
   private String asErrorKey(String error)
   {
      String stripped = AnsiCode.strip(error);
      return stripped.replaceFirst("[^:]*:\\s*", "").trim();
   }

   public void consoleWriteError(final String error)
   {
      clearPendingInput();
      output(error, getErrorClass(), true /*isError*/, false /*ignoreLineCount*/,
            isAnnouncementEnabled(AriaLiveService.CONSOLE_LOG));

      // Pick up the elements emitted to the console by this call. If we get
      // extended information for this error, we'll need to swap out the simple
      // error elements for the extended error element.
      List<Element> newElements = output_.getNewElements();
      if (!newElements.isEmpty())
      {
         if (clearErrors_)
         {
            errorNodes_.clear();
            errorKeys_.clear();
            clearErrors_ = false;
         }
         
         String key = asErrorKey(error);
         errorNodes_.put(key, newElements);
         errorKeys_.put(key, error);
      }
   }

   public void consoleWriteExtendedError(String error,
                                         UnhandledError traceInfo,
                                         boolean expand,
                                         String command)
   {
      String key = asErrorKey(error);
      if (!errorNodes_.containsKey(key))
         return;
      
      List<Element> errorEls = errorNodes_.get(key);
      if (errorEls == null || errorEls.isEmpty())
         return;

      if (errorKeys_.containsKey(key))
      {
         String originalError = errorKeys_.get(key);
         traceInfo.setErrorMessage(originalError);
      }
      
      clearPendingInput();
      ConsoleError errorWidget = new ConsoleError(traceInfo, getErrorClass(), command, this, null);

      if (expand)
         errorWidget.setTracebackVisible(true);

      insertErrorWidgetElement(
            errorEls,
            errorWidget.getElement());
      
      scrollPanel_.onContentSizeChanged();
      errorNodes_.remove(key);
      errorKeys_.remove(key);
   }
   
   private void insertErrorWidgetElement(List<Element> errorEls,
                                         Element widgetEl)
   {
      // If console groups are enabled, the error output might have been collected
      // into a single 'groupError' span element. Search the parent element, plus
      // all of that element's siblings.
      for (int i = 0, n = errorEls.size(); i < n; i++)
      {
         Element errorEl = errorEls.get(n - i - 1);
         Element parentEl = errorEl.getParentElement();
         for (Node node = parentEl.getParentElement().getLastChild();
              node != null;
              node = node.getPreviousSibling())
         {
            if (Element.is(node))
            {
               Element el = Element.as(node);
               if (el.hasClassName(VirtualConsole.RES.styles().groupError()))
               {
                  el.getParentElement().replaceChild(widgetEl, el);
                  return;
               }
            }
         }
      }
      
      // Otherwise, error output should be a sequence of DOM elements within
      // some collection. Replace the last one with our error widget, and then
      // remove all the other error elements.
      Element lastErrorEl = errorEls.get(errorEls.size() - 1);
      lastErrorEl.getParentElement().replaceChild(widgetEl, lastErrorEl);
      for (int i = 0, n = errorEls.size() - 1; i < n; i++)
         errorEls.get(i).removeFromParent();
   }

   @Override
   public void runCommandWithDebug(String command)
   {
      events_.fireEvent(new RunCommandWithDebugEvent(command));
   }

   @Override
   public void consoleWriteOutput(final String output)
   {
      clearPendingInput();
      output(output, styles_.output(), false /*isError*/, false /*ignoreLineCount*/,
            isAnnouncementEnabled(AriaLiveService.CONSOLE_LOG));
   }

   @Override
   public void consoleWriteInput(final String input, String console)
   {
      // if coming from another console id (i.e. notebook chunk), clear the
      // prompt since this input hasn't been processed yet (we'll redraw when
      // the prompt reappears)
      if (!StringUtil.isNullOrEmpty(console))
         prompt_.setHTML("");

      clearPendingInput();
      output(input, styles_.command() + KEYWORD_CLASS_NAME, false /*isError*/,
            false /*ignoreLineCount*/, isAnnouncementEnabled(AriaLiveService.CONSOLE_COMMAND));
   }

   private void clearPendingInput()
   {
      pendingInput_.setText("");
      pendingInput_.setVisible(false);
   }

   @Override
   public void consoleWritePrompt(final String prompt)
   {
      output(prompt, styles_.prompt() + KEYWORD_CLASS_NAME, false /*isError*/,
            false /*ignoreLineCount*/, isAnnouncementEnabled(AriaLiveService.CONSOLE_COMMAND));
      clearErrors_ = true;
   }

   public static String consolify(String text)
   {
      VirtualConsole console = RStudioGinjector.INSTANCE.getVirtualConsoleFactory().create(null);
      console.submit(text);
      return console.toString();
   }

   @Override
   public void consolePrompt(String prompt, boolean showInput)
   {
      if (prompt != null)
         prompt = consolify(prompt);

      prompt_.getElement().setInnerText(prompt);
      ensureInputVisible();

      // Deal gracefully with multi-line prompts
      int promptLines = StringUtil.notNull(prompt).split("\\n").length;
      input_.asWidget().getElement().getStyle().setPaddingTop((promptLines - 1) * 15,
                                                   Unit.PX);

      input_.setPasswordMode(!showInput);
      clearErrors_ = true;
      
      output_.normalizePreviousOutput();
      output_.ensureStartingOnNewLine();
   }

   @Override
   public void ensureInputVisible()
   {
      // NOTE: we don't scroll immediately as this is normally called
      // in response to mutations of the console input buffer, and so
      // we need to wait until Ace has finished rendering in response
      // to that change.
      //
      // In case there wasn't an Ace render in-flight, we also force a check
      // for pending scroll (which will then force the cursor into view)
      if (!scrollIntoViewPending_)
      {
         scrollIntoViewPending_ = true;
         Scheduler.get().scheduleDeferred(() -> checkForPendingScroll());
      }
   }
   
   /**
    * Send text to the console
    * @param text Text to output
    * @param className Text style
    * @param isError Is this an error message?
    * @param ignoreLineCount Output without checking buffer length?
    * @param ariaLiveAnnounce Include in arialive output announcement
    * @return was this output below the maximum buffer line count?
    */
   private boolean output(String text,
                          String className,
                          boolean isError,
                          boolean ignoreLineCount,
                          boolean ariaLiveAnnounce)
   {
      boolean canContinue = output_.outputToConsole(
            text, className,
            isError, ignoreLineCount,
            ariaLiveAnnounce);

      // if we're currently scrolled to the bottom, nudge the timer so that we
      // will keep up with output
      if (scrollPanel_.isScrolledToBottom())
         resizeCommand_.nudge();

      if (liveRegion_ != null)
         liveRegion_.announce(output_.getNewText());

      // if we're a desktop automation host, write output to console
      if (Desktop.isDesktop() && automation_.isAutomationHost())
      {
         if (isError)
         {
            Desktop.getFrame().writeStderr(text);
         }
         else
         {
            Desktop.getFrame().writeStdout(text);
         }
      }
      
      return canContinue;
   }

   private String ensureNewLine(String s)
   {
      if (s.length() == 0 || s.charAt(s.length() - 1) == '\n')
         return s;
      else
         return s + '\n';
   }

   public void playbackActions(final RpcObjectList<ConsoleAction> consoleActions)
   {
      int n = consoleActions.length();
      if (n == 0)
         return;

      // Console actions are stored on the session side in chunks. However, when
      // we render these in the console, we may need to truncate long strings.
      // To support this, we flatten the action list so that multiple chunks
      // of the same type are joined into a single console action. This occurs
      // most typically for very long strings.
      JsVector<ConsoleAction> actions = JsVector.createVector();
      StringBuffer buffer = new StringBuffer();

      ConsoleAction lastAction = consoleActions.get(0);
      buffer.append(lastAction.getData());
      for (int i = 1; i < n; i++)
      {
         ConsoleAction currentAction = consoleActions.get(i);
         if (currentAction.getType() != lastAction.getType())
         {
            actions.push(ConsoleAction.create(lastAction.getType(), buffer.toString()));
            buffer.setLength(0);
         }

         buffer.append(currentAction.getData());
         lastAction = currentAction;
      }
      actions.push(ConsoleAction.create(lastAction.getType(), buffer.toString()));

      // Server persists 1000 most recent ConsoleActions in a circular buffer.
      //
      // One ConsoleAction can generate multiple lines of output, and we want
      // to limit number of lines added to the console's DOM; see trimExcess().
      //
      // First walk through the actions in reverse, and determine how many
      // lines they will generate (without actually writing anything),
      // then play-back in normal order. Finally, trim to the max-lines we support
      // to catch any rounding from final chunk.
      int lines = 0;
      int revIndex = actions.length() - 1;
      for (; revIndex >= 0; revIndex--)
      {
         ConsoleAction action = actions.get(revIndex);

         if (action.getType() == ConsoleAction.INPUT)
            lines++;

         lines = lines + StringUtil.newlineCount(action.getData());

         if (lines > output_.getMaxOutputLines())
            break;
      }
      if (revIndex < 0)
         revIndex = 0;

      final int startIndex = revIndex;
      final int endIndex = actions.length() - 1;

      Scheduler.get().scheduleIncremental(new RepeatingCommand()
      {
         private int i = startIndex;
         private int chunksize = 1000;

         @Override
         public boolean execute()
         {
            boolean canContinue = false;
            int end = i + chunksize;
            chunksize = 10;
            for (; i <= end && i <= endIndex; i++)
            {
               // User hit Ctrl+L at some point--we're done.
               if (cleared_)
               {
                  canContinue = false;
                  break;
               }

               ConsoleAction action = actions.get(i);

               switch (action.getType())
               {
                  case ConsoleAction.INPUT:
                     canContinue = output(action.getData() + "\n",
                                          styles_.command() + " " + KEYWORD_CLASS_NAME,
                                          false /*isError*/,
                                          true /*ignoreLineCount*/,
                                          false /*announce*/);
                     break;
                  case ConsoleAction.OUTPUT:
                     canContinue = output(action.getData(),
                                          styles_.output(),
                                          false /*isError*/,
                                          true /*ignoreLineCount*/,
                                          false /*announce*/);
                     break;
                  case ConsoleAction.ERROR:
                     canContinue = output(action.getData(),
                                          getErrorClass(),
                                          true /*isError*/,
                                          true /*ignoreLineCount*/,
                                          false /*announce*/);
                     break;
                  case ConsoleAction.PROMPT:
                     canContinue = output(action.getData(),
                                          styles_.prompt() + " " + KEYWORD_CLASS_NAME,
                                          false /*isError*/,
                                          true /*ignoreLineCount*/,
                                          false /*announce*/);
                     break;
               }
               if (!canContinue)
               {
                  break;
               }
            }

            if (canContinue)
            {
               canContinue = (i <= endIndex);
            }

            if (!canContinue)
            {
               output_.trimExcess();
            }
            return canContinue;
         }
      });
   }

   @Override
   public void focus()
   {
      input_.setFocus(true);
   }

   /**
    * Directs focus/selection to the input box when a (different) widget
    * is clicked.)
    */
   private class SelectInputClickHandler implements ClickHandler,
                                                    KeyDownHandler,
                                                    PasteEvent.Handler
   {
      public SelectInputClickHandler(boolean scrollOnClick)
      {
         super();
         scrollOnClick_ = scrollOnClick;
      }

      @Override
      public void onClick(ClickEvent event)
      {
         // If clicking on the input panel already, stop propagation.
         if (event.getSource() == input_)
         {
            event.stopPropagation();
            return;
         }

         // Allow clicks into the search bar.
         EventTarget target = event.getNativeEvent().getEventTarget();
         if (Node.is(target))
         {
            Node targetNode = Node.as(target);
            if (DomUtils.contains(findBar_.getElement(), targetNode))
               return;
         }

         if (prefs_ != null && prefs_.consoleDoubleClickSelect().getValue())
         {
            // Some clicks can result in selection (e.g. double clicks). We don't
            // want to grab focus for those clicks, but we don't know yet if this
            // click can generate a selection. Wait 400ms (unfortunately it's not
            // possible to get the OS double-click timeout) for a selection to
            // appear; if it doesn't then drive focus to the input box.
            if (inputFocus_.isRunning())
               inputFocus_.cancel();
            inputFocus_.schedule(400);
         }
         else
         {
            // No selection check needed
            inputFocus_.run();
         }
      }

      @Override
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
            case KeyCodes.KEY_TAB:
               if (prefs_ == null || prefs_.tabKeyMoveFocus().getValue())
                  return;
         }
         input_.setFocus(true);
         delegateEvent(input_.asWidget(), event);
      }

      @Override
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
      private final Timer inputFocus_ = new Timer()
      {
         @Override
         public void run()
         {
            // Don't drive focus to the input unless there is no selection.
            // Otherwise it would interfere with the ability to select stuff
            // from the output buffer for copying to the clipboard.
            if (DomUtils.selectionExists() || !isInputOnscreen())
               return;

            // When focusing Ace, if the user hasn't yet typed anything into
            // the input line, then Ace will erroneously adjust the scroll
            // position upwards upon focus. Rather than patching Ace, we instead
            // just re-scroll to the bottom if we were already scrolled to the
            // bottom after giving focus to the Ace editor instance.
            //
            // https://github.com/rstudio/rstudio/issues/6231
            boolean wasScrolledToBottom = scrollPanel_.isScrolledToBottom();
            input_.setFocus(true);
            if (scrollOnClick_ && wasScrolledToBottom)
               scrollPanel_.scrollToBottom();
         }
      };

      private boolean scrollOnClick_;
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
      }

      private ClickableScrollPanel(boolean useTimer)
      {
         super(useTimer);
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

   @Override
   public void clearOutput()
   {
      output_.clearConsoleOutput();
      clearLiveRegion();
      cleared_ = true;
   }

   @Override
   public InputEditorDisplay getInputEditorDisplay()
   {
      return input_;
   }

   @Override
   public ConsoleOutputWriter getConsoleOutputWriter()
   {
      return output_;
   }

   @Override
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

      return commandText;
   }

   @Override
   public HandlerRegistration addCapturingKeyDownHandler(KeyDownHandler handler)
   {
      return input_.addCapturingKeyDownHandler(handler);
   }

   @Override
   public HandlerRegistration addKeyPressHandler(KeyPressHandler handler)
   {
      return input_.addKeyPressHandler(handler);
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

   @Override
   public int getCharacterWidth()
   {
      return DomUtils.getCharacterWidth(getElement(), styles_.console());
   }

   @Override
   public boolean isPromptEmpty()
   {
      return StringUtil.isNullOrEmpty(prompt_.getText());
   }

   @Override
   public String getPromptText()
   {
      return StringUtil.notNull(prompt_.getText());
   }

   @Override
   public void setReadOnly(boolean readOnly)
   {
      input_.setReadOnly(readOnly);
   }

   @Override
   public int getMaxOutputLines()
   {
      return output_.getMaxOutputLines();
   }

   @Override
   public void setMaxOutputLines(int maxLines)
   {
      output_.setMaxOutputLines(maxLines);
   }

   @Override
   public void setTextInputAriaLabel(String label)
   {
      input_.setTextInputAriaLabel(label);
   }

   @Override
   public Widget getShellWidget()
   {
      return this;
   }

   @Override
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

   public void onConsoleFind()
   {
      String selection = DomUtils.getSelectedText();
      findBar_.show(true);
      if (!StringUtil.isNullOrEmpty(selection))
         findBar_.setValue(selection);
   }

   public Widget getOutputWidget()
   {
      return output_.getWidget();
   }

   @Override
   public void enableLiveReporting()
   {
      liveRegion_ = new AriaLiveShellWidget(prefs_);
      verticalPanel_.add(liveRegion_);
   }

   @Override
   public void clearLiveRegion()
   {
      if (liveRegion_ != null)
         liveRegion_.clearLiveRegion();
   }

   private boolean isAnnouncementEnabled(String announcement)
   {
      return ariaLive_ != null && !ariaLive_.isDisabled(announcement);
   }

   private void scrollIntoView()
   {
      int padding = 8;

      // Get the bounding rectangles for the scroll panel + cursor element.
      // Note that we rely on getBoundingClientRect() here as the Ace cursor
      // element is rendered using CSS transforms, and those transforms are
      // not represented in offsetTop.
      //
      // Note that we cannot reliably synchronously force Ace (or the browser)
      // to render the cursor, so we instead check for a "bogus" rectangle and
      // conclude this implies there is a pending render in-flight that we can
      // later respond to.
      renderer_.renderCursor();
      DOMRect child = DomUtils.getBoundingClientRect(renderer_.getCursorElement());

      boolean isRendering = child.getWidth() == 0 && child.getHeight() == 0;
      if (isRendering)
      {
         scrollIntoViewPending_ = true;
         Scheduler.get().scheduleDeferred(() -> checkForPendingScroll());
         return;
      }

      DOMRect parent = DomUtils.getBoundingClientRect(scrollPanel_.getElement());

      // Scroll the cursor into view as required.
      int oldScrollPos = scrollPanel_.getVerticalScrollPosition();
      int newScrollPos = oldScrollPos;

      if (child.getTop() - padding < parent.getTop())
      {
         newScrollPos =
               scrollPanel_.getVerticalScrollPosition() -
               parent.getTop() +
               child.getTop() -
               padding;
      }
      else if (child.getBottom() + padding > parent.getBottom())
      {
         newScrollPos =
               scrollPanel_.getVerticalScrollPosition() -
               parent.getBottom() +
               child.getBottom() +
               padding;
      }
      else
      {
         // No scroll update required.
         return;
      }

      // Don't scroll if the difference is less than a pixel.
      // This is necessary for cases where the IDE is zoomed,
      // as we will end up comparing fractional pixels which
      // may lead to small but non-zero differences in position.
      int diff = Math.abs(newScrollPos - oldScrollPos);
      if (diff < 1)
         return;

      scrollPanel_.setVerticalScrollPosition(newScrollPos);
   }

   private void checkForResize()
   {
      int width = input_.getWidget().getOffsetWidth();
      if (width == editorWidth_)
         return;

      editorWidth_ = width;
      scrollIntoViewPending_ = true;
      input_.onResize();
      input_.forceImmediateRender();
   }

   private void checkForPendingScroll()
   {
      if (scrollIntoViewPending_)
      {
         scrollIntoViewPending_ = false;
         scrollIntoView();
      }
   }
   
   private String getErrorClass()
   {
      return ConsoleOutputWriter.OUTPUT_ERROR_CLASS;
   }

   private void syncOutputStyles()
   {
      if (prefs_ != null)
      {
         boolean softWrap = prefs_.consoleSoftWrap().getValue();
         if (softWrap)
            output_.getElement().addClassName(styles_.outputSoftWrap());
         else
            output_.getElement().removeClassName(styles_.outputSoftWrap());
      }
   }

   @Override
   protected void onUnload()
   {
      super.onUnload();

      if (windowBlurHandler_ != null)
      {
         windowBlurHandler_.removeHandler();
         windowBlurHandler_ = null;
      }

      if (consoleSoftWrapHandler_ != null)
      {
         consoleSoftWrapHandler_.removeHandler();
         consoleSoftWrapHandler_ = null;
      }
   }

   private boolean cleared_ = false;
   private boolean ignoreNextFocus_ = false;
   private HandlerRegistration windowBlurHandler_;
   private HandlerRegistration consoleSoftWrapHandler_;
   private final ConsoleOutputWriter output_;
   private final FindBar findBar_;
   private final PreWidget pendingInput_;
   private final HTML prompt_;
   private AriaLiveShellWidget liveRegion_ = null;
   protected final AceEditor input_;
   protected final Renderer renderer_;
   private final DockPanel inputLine_;
   protected final ClickableScrollPanel scrollPanel_;
   private final DockLayoutPanel container_;
   private final ConsoleResources.ConsoleStyles styles_;
   private final TimeBufferedCommand resizeCommand_;
   private boolean suppressPendingInput_;
   private final EventBus events_;
   private final UserPrefs prefs_;
   private final AriaLiveService ariaLive_;
   private VerticalPanel verticalPanel_;
   private ErrorClass errorClass_;
   private String aceThemeErrorClass_;

   private int editorWidth_ = -1;
   private boolean scrollIntoViewPending_ = false;

   // A list of errors that have occurred between console prompts.
   private final Map<String, List<Element>> errorNodes_ = new TreeMap<>();
   private final Map<String, String> errorKeys_ = new TreeMap<>();
   private boolean clearErrors_ = false;

   // Injected
   private ApplicationAutomation automation_;
   private Session session_;
   private UserState userState_;
   
   private static final String KEYWORD_CLASS_NAME = ConsoleResources.KEYWORD_CLASS_NAME;
   private static final String RSTUDIO_CONSOLE_BUSY = "rstudio-console-busy";
}
