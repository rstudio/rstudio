/*
 * ShellPane.java
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
package org.rstudio.studio.client.workbench.views.console.shell;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.*;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DeferredCommand;
import com.google.gwt.user.client.IncrementalCommand;
import com.google.gwt.user.client.ui.*;
import com.google.inject.Inject;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.jsonrpc.RpcObjectList;
import org.rstudio.core.client.widget.BottomScrollPanel;
import org.rstudio.core.client.widget.FontSizer;
import org.rstudio.studio.client.workbench.model.ConsoleAction;
import org.rstudio.studio.client.workbench.views.console.ConsoleResources;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorDisplay;
import org.rstudio.studio.client.workbench.views.console.shell.editor.PlainTextEditor;

public class ShellPane extends Composite implements Shell.Display,
                                                    RequiresResize
{
   @Inject
   public ShellPane(PlainTextEditor editor)
   {
      styles_ = ConsoleResources.INSTANCE.consoleStyles();

      SelectInputClickHandler secondaryInputHandler = new SelectInputClickHandler();

      output_ = new PreWidget() ;
      output_.setStylePrimaryName(styles_.output()) ;
      output_.addClickHandler(secondaryInputHandler);

      prompt_ = new HTML() ;
      prompt_.setStylePrimaryName(styles_.prompt()) ;
      prompt_.addStyleName(KEYWORD_CLASS_NAME);

      input_ = editor ;
      input_.addClickHandler(secondaryInputHandler) ;
      input_.setStylePrimaryName(styles_.input());
      input_.addStyleName(KEYWORD_CLASS_NAME);

      inputLine_ = new DockPanel();
      inputLine_.setHorizontalAlignment(DockPanel.ALIGN_LEFT);
      inputLine_.setVerticalAlignment(DockPanel.ALIGN_TOP);
      inputLine_.add(prompt_, DockPanel.WEST);
      inputLine_.setCellWidth(prompt_, "1");
      inputLine_.add(input_, DockPanel.CENTER);
      inputLine_.setCellWidth(input_, "100%");
      inputLine_.setWidth("100%");

      verticalPanel_ = new VerticalPanel() ;
      verticalPanel_.setStylePrimaryName(styles_.console());
      verticalPanel_.addStyleName("ace_text-layer");
      verticalPanel_.addStyleName("ace_line");
      FontSizer.applyNormalFontSize(verticalPanel_);
      verticalPanel_.add(output_) ;
      verticalPanel_.add(inputLine_) ;
      verticalPanel_.setWidth("100%") ;

      scrollPanel_ = new ClickableScrollPanel() ;
      scrollPanel_.setWidget(verticalPanel_) ;
      scrollPanel_.addStyleName("ace_editor");
      scrollPanel_.addStyleName("ace_scroller");
      scrollPanel_.addClickHandler(secondaryInputHandler);
      scrollPanel_.addKeyDownHandler(secondaryInputHandler);

      secondaryInputHandler.setInput(editor);

      initWidget(scrollPanel_) ;
   }

   @Override
   protected void onLoad()
   {
      super.onLoad();
      DeferredCommand.addCommand(new Command()
      {
         public void execute()
         {
            scrollPanel_.scrollToBottom();
         }
      });
   }

   public void consoleError(String error)
   {
      output(error, styles_.error(), false);
      if (!DomUtils.selectionExists())
         scrollPanel_.scrollToBottom();
   }

   public void consoleOutput(String output)
   {
      output(output, styles_.output(), false);
      if (!DomUtils.selectionExists())
         scrollPanel_.scrollToBottom();
   }

   public void consolePrompt(String prompt)
   {
      prompt_.getElement().setInnerText(prompt);
      //input_.clear() ;
      ensureInputVisible();

      // Deal gracefully with multi-line prompts
      int promptLines = StringUtil.notNull(prompt).split("\\n").length;
      input_.getElement().getStyle().setPaddingTop((promptLines - 1) * 15,
                                                   Unit.PX);
   }

   public void ensureInputVisible()
   {
      scrollPanel_.scrollToBottom();
   }

   private boolean output(String text,
                          String className,
                          boolean addToTop)
   {
      Node node;
      if (StringUtil.isNullOrEmpty(className)
          || className.equals(styles_.output()))
      {
         node = Document.get().createTextNode(text);
      }
      else
      {
         SpanElement span = Document.get().createSpanElement();
         span.setClassName(className);
         span.setInnerText(text);
         node = span;
      }

      if (addToTop)
         output_.getElement().insertFirst(node);
      else
         output_.getElement().appendChild(node);
      
      lines_ += DomUtils.countLines(node, true);
      return !trimExcess();
   }

   private boolean trimExcess()
   {
      if (maxLines_ <= 0)
         return false;  // No limit in effect

      int linesToTrim = lines_ - maxLines_;
      if (linesToTrim > 0)
      {
         lines_ -= DomUtils.trimLines(output_.getElement(),
                                      lines_ - maxLines_);
         return true;
      }

      return false;
   }

   public void playbackActions(final RpcObjectList<ConsoleAction> actions)
   {
      DeferredCommand.addCommand(new IncrementalCommand()
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

   public void setFocus(boolean focused)
   {
      input_.setFocus(focused) ;
   }
   
   /**
    * Directs focus/selection to the input box when a (different) widget
    * is clicked.
    */
   private class SelectInputClickHandler implements ClickHandler,
                                                    KeyDownHandler
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
         // (BUG: DomUtils.selectionExists() doesn't work in a timely
         // fashion on IE8.)
         if (!DomUtils.selectionExists() && isInputOnscreen())
         {
            input_.setFocus(true) ;
            DomUtils.collapseSelection(false);
         }
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
         delegateEvent(input_, event);
      }

      public void setInput(PlainTextEditor input)
      {
         input_ = input;
      }

      private PlainTextEditor input_;
   }

   private boolean isInputOnscreen()
   {
      return DomUtils.isVisibleVert(scrollPanel_.getElement(),
                                    inputLine_.getElement());
   }

   private class ClickableScrollPanel extends BottomScrollPanel
   {
      private ClickableScrollPanel()
      {
         super();
      }

      public HandlerRegistration addClickHandler(ClickHandler handler)
      {
         return addDomHandler(handler, ClickEvent.getType());
      }

      public HandlerRegistration addKeyDownHandler(KeyDownHandler handler)
      {
         return addDomHandler(handler, KeyDownEvent.getType());
      }
   }

   private class PreWidget extends Widget
         implements HasKeyDownHandlers, HasClickHandlers
   {
      public PreWidget()
      {
         setElement(Document.get().createPreElement());
         getElement().setTabIndex(0);
      }

      public HandlerRegistration addClickHandler(ClickHandler handler)
      {
         return addDomHandler(handler, ClickEvent.getType());
      }

      public HandlerRegistration addKeyDownHandler(KeyDownHandler handler)
      {
         return addDomHandler(handler, KeyDownEvent.getType());
      }

      public void setText(String text)
      {
         getElement().setInnerText(text);
      }
   }

   public void clearOutput()
   {
      output_.setText("") ;
      lines_ = 0;
      cleared_ = true;
   }

   public InputEditorDisplay getInputEditorDisplay()
   {
      return input_ ;
   }

   public String processCommandEntry()
   {
      // parse out the command text
      String promptText = prompt_.getElement().getInnerText();
      String commandText = input_.getText();
      input_.setText("");
      prompt_.setHTML("");
      output(promptText, styles_.prompt() + " " + KEYWORD_CLASS_NAME, false);
      output(commandText + "\n",
             styles_.command() + " " + KEYWORD_CLASS_NAME, 
             false);
      ensureInputVisible();

      return commandText ;
   }

   public HandlerRegistration addKeyDownHandler(KeyDownHandler handler)
   {
      return input_.addKeyDownHandler(handler) ;
   }

   public HandlerRegistration addKeyPressHandler(KeyPressHandler handler)
   {
      return input_.addKeyPressHandler(handler) ;
   }
   
   public int getCharacterWidth()
   {
      // create width checker label and add it to the root panel
      Label widthChecker = new Label();
      widthChecker.setStylePrimaryName(styles_.console());
      FontSizer.applyNormalFontSize(widthChecker);
      RootPanel.get().add(widthChecker, -1000, -1000);
      
      // put the text into the label, measure it, and remove it
      String text = new String("abcdefghijklmnopqrstuvwzyz0123456789");
      widthChecker.setText(text);
      int labelWidth = widthChecker.getOffsetWidth();
      RootPanel.get().remove(widthChecker);
      
      // compute the points per character 
      int pointsPerCharacter = labelWidth / text.length();
      
      // compute client width
      int clientWidth = getElement().getClientWidth();
      int offsetWidth = getOffsetWidth();
      if (clientWidth == offsetWidth)
      {
         // if the two widths are the same then there are no scrollbars.
         // however, we know there will eventually be a scrollbar so we 
         // should offset by an estimated amount
         // (is there a more accurate way to estimate this?)
         final int ESTIMATED_SCROLLBAR_WIDTH = 19;
         clientWidth -= ESTIMATED_SCROLLBAR_WIDTH;
      }
      
      // compute character width (add pad so characters aren't flush to right)
      final int RIGHT_CHARACTER_PAD = 2;
      int width = (clientWidth / pointsPerCharacter) - RIGHT_CHARACTER_PAD ;
      
      // enforce a minimum width
      final int MINIMUM_WIDTH = 30;
      return Math.max(width, MINIMUM_WIDTH);
   }

   public boolean isPromptEmpty()
   {
      return StringUtil.isNullOrEmpty(prompt_.getText());
   }

   public void setMaxOutputLines(int maxLines)
   {
      maxLines_ = maxLines;
      trimExcess();
   }

   public void onResize()
   {
      if (getWidget() instanceof RequiresResize)
         ((RequiresResize)getWidget()).onResize();
   }

   private int lines_ = 0;
   private int maxLines_ = -1;
   private boolean cleared_ = false;
   private final PreWidget output_ ;
   private final HTML prompt_ ;
   private final PlainTextEditor input_ ; 
   private final DockPanel inputLine_ ;
   private final VerticalPanel verticalPanel_ ;
   private final ClickableScrollPanel scrollPanel_ ;
   private ConsoleResources.ConsoleStyles styles_;

   private static final String KEYWORD_CLASS_NAME = " ace_keyword";
}
