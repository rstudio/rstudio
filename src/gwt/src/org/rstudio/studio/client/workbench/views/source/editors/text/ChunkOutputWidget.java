/*
 * ChunkOutputWidget.java
 *
 * Copyright (C) 2009-16 by RStudio, Inc.
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
package org.rstudio.studio.client.workbench.views.source.editors.text;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.VirtualConsole;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.js.JsArrayEx;
import org.rstudio.core.client.resources.CoreResources;
import org.rstudio.core.client.widget.PreWidget;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.ApplicationInterrupt.InterruptHandler;
import org.rstudio.studio.client.application.events.EventBus;
import org.rstudio.studio.client.common.shell.ShellWidget;
import org.rstudio.studio.client.rmarkdown.model.RmdChunkOutput;
import org.rstudio.studio.client.rmarkdown.model.RmdChunkOutputUnit;
import org.rstudio.studio.client.server.ServerError;
import org.rstudio.studio.client.workbench.views.console.events.ConsolePromptEvent;
import org.rstudio.studio.client.workbench.views.console.events.ConsolePromptHandler;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleWriteErrorEvent;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleWriteErrorHandler;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleWriteOutputEvent;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleWriteOutputHandler;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleWriteInputEvent;
import org.rstudio.studio.client.workbench.views.console.events.ConsoleWriteInputHandler;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.BodyElement;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Overflow;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Event;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HTMLPanel;
import com.google.gwt.user.client.ui.Image;
import com.google.gwt.user.client.ui.Widget;

public class ChunkOutputWidget extends Composite
                               implements ConsoleWriteOutputHandler,
                                          ConsoleWriteErrorHandler,
                                          ConsolePromptHandler,
                                          ConsoleWriteInputHandler
{

   private static ChunkOutputWidgetUiBinder uiBinder = GWT
         .create(ChunkOutputWidgetUiBinder.class);

   interface ChunkOutputWidgetUiBinder
         extends UiBinder<Widget, ChunkOutputWidget>
   {
      
   }
   
   public interface ChunkStyle extends CssResource
   {
      String overflowY();
   }

   public ChunkOutputWidget(String chunkId,
         CommandWithArg<Integer> onRenderCompleted,
         final Command onChunkCleared)
   {
      chunkId_ = chunkId;
      initWidget(uiBinder.createAndBindUi(this));
      applyCachedEditorStyle();
      
      onRenderCompleted_ = onRenderCompleted;
      
      DOM.sinkEvents(interrupt_.getElement(), Event.ONCLICK);
      DOM.setEventListener(interrupt_.getElement(), new EventListener()
      {
         @Override
         public void onBrowserEvent(Event evt)
         {
            switch(DOM.eventGetType(evt))
            {
            case Event.ONCLICK:
               RStudioGinjector.INSTANCE.getApplicationInterrupt().interruptR(
                     new InterruptHandler()
                     {
                        @Override
                        public void onInterruptFinished()
                        {
                           completeInterrupt();
                        }
                     });
               break;
            };
         }
      });
      
      DOM.sinkEvents(clear_.getElement(), Event.ONCLICK);
      DOM.setEventListener(clear_.getElement(), new EventListener()
      {
         @Override
         public void onBrowserEvent(Event evt)
         {
            switch(DOM.eventGetType(evt))
            {
            case Event.ONCLICK:
               if (state_ == CONSOLE_READY ||
                   state_ == CONSOLE_EXECUTING)
               {
                  destroyConsole();
               }
               onChunkCleared.execute();
               break;
            };
         }
      });
      
      DOM.sinkEvents(root_.getElement(), Event.ONMOUSEOVER | Event.ONMOUSEOUT);
      DOM.setEventListener(root_.getElement(), new EventListener()
      {
         @Override
         public void onBrowserEvent(Event evt)
         {
            if (state_ == CONSOLE_READY || state_ == CHUNK_RENDERED)
            {
               switch(DOM.eventGetType(evt))
               {
                  case Event.ONMOUSEOVER:
                     if (Element.as(evt.getEventTarget()) == 
                         clear_.getElement())
                        clear_.getElement().getStyle().setOpacity(1);
                     else 
                        clear_.getElement().getStyle().setOpacity(0.5);
                     break;
                  
                  case Event.ONMOUSEOUT:
                     if (Element.as(evt.getEventTarget()) == 
                         clear_.getElement())
                        clear_.getElement().getStyle().setOpacity(0.5);
                     else
                        clear_.getElement().getStyle().clearOpacity();
                     break;
               }
            }
         }
      });

      interrupt_.setResource(RStudioGinjector.INSTANCE.getCommands()
            .interruptR().getImageResource());
      busy_.setResource(CoreResources.INSTANCE.progress_gray());
   }

   public void showChunkOutput(RmdChunkOutput output)
   {
      // loop over the output units and emit the appropriate contents for each
      JsArray<RmdChunkOutputUnit> units = output.getUnits();
      for (int i = 0; i < units.length(); i++)
      {
         RmdChunkOutputUnit unit = units.get(i);
         switch(unit.getType())
         {
         case RmdChunkOutputUnit.TYPE_TEXT:
            showConsoleOutput(unit.getArray());
            break;
         case RmdChunkOutputUnit.TYPE_HTML:
            showHtmlOutput(unit.getString());
            break;
         }
      }
   }
   
   private String classOfOutput(int type)
   {
      if (type == CONSOLE_ERROR)
        return RStudioGinjector.INSTANCE.getUIPrefs().getThemeErrorClass();
      else if (type == CONSOLE_INPUT)
        return "ace_keyword";
      return null;
   }
   
   private void showConsoleOutput(JsArray<JsArrayEx> output)
   {
      initConsole();
      
      for (int i = 0; i < output.length(); i++)
      {
         if (output.get(i).length() < 2)
            continue;
         vconsole_.submit(
               (output.get(i).getInt(0) == CONSOLE_INPUT ? 
                     "> " + output.get(i).getString(1)  + "\n" :
                     output.get(i).getString(1)), 
               classOfOutput(output.get(i).getInt(0)));
      }
      if (state_ == CHUNK_EXECUTING || state_ == CHUNK_EMPTY)
         showReadyState();
      vconsole_.redraw(console_.getElement());
      onRenderCompleted_.execute(console_.getElement().getOffsetHeight());
      // scroll to the bottom
      console_.getElement().setScrollTop(console_.getElement().getScrollHeight());
      state_ = CONSOLE_READY;
      setOverflowStyle();
   }
   
   private void showHtmlOutput(String url)
   {
      final ChunkOutputFrame frame = new ChunkOutputFrame();
      frame.getElement().getStyle().setHeight(100, Unit.PCT);
      frame.getElement().getStyle().setWidth(100, Unit.PCT);
      root_.add(frame);

      frame.loadUrl(url, new Command() 
      {
         @Override
         public void execute()
         {
            if (state_ != CHUNK_RENDERING)
               return;
            state_ = CHUNK_RENDERED;
            applyCachedEditorStyle();
            showReadyState();
            setOverflowStyle();
            injectEmptyText(frame.getDocument().getBody());
            Element doc = frame.getDocument().getDocumentElement();
            int height = doc.getScrollHeight();
            if (doc.getScrollWidth() > doc.getOffsetWidth())
            {
               // if there's a horizontal scrollbar we need to allocate space
               // for it (otherwise the horizontal scrollbar will overflow and
               // cause a vertical scrollbar too)
               height += ShellWidget.ESTIMATED_SCROLLBAR_WIDTH;
            }
            onRenderCompleted_.execute(height);
         };
      });
      state_ = CHUNK_RENDERING;
   }
   
   @Override
   public void onConsoleWriteOutput(ConsoleWriteOutputEvent event)
   {
      if (state_ != CONSOLE_EXECUTING || event.getConsole() != chunkId_)
         return;
      renderConsoleOutput(event.getOutput(), classOfOutput(CONSOLE_OUTPUT));
   }
   
   @Override
   public void onConsoleWriteError(ConsoleWriteErrorEvent event)
   {
      if (state_ != CONSOLE_EXECUTING || event.getConsole() != chunkId_)
         return;
      renderConsoleOutput(event.getError(), classOfOutput(CONSOLE_ERROR));
   }
   
   @Override
   public void onConsoleWriteInput(ConsoleWriteInputEvent event)
   {
      if (state_ != CONSOLE_EXECUTING || event.getConsole() != chunkId_)
         return;
      renderConsoleOutput("> " + event.getInput(), 
            classOfOutput(CONSOLE_INPUT));
   }

   @Override
   public void onConsolePrompt(ConsolePromptEvent event)
   {
      if (state_ != CONSOLE_EXECUTING)
         return;
      state_ = CONSOLE_READY;
      setOverflowStyle();
      unregisterConsoleEvents();
      showReadyState();
   }

   private void renderConsoleOutput(String text, String clazz)
   {
      vconsole_.submitAndRender(text, clazz,
            console_.getElement());
      console_.getElement().setScrollTop(console_.getElement().getScrollHeight());
      onRenderCompleted_.execute(console_.getElement().getOffsetHeight());
   }

   public void setChunkExecuting()
   {
      if (state_ == CHUNK_EXECUTING)
         return;
      if (state_ == CONSOLE_READY)
         destroyConsole();
      state_ = CHUNK_EXECUTING;
      showBusyState();
   }
   
   public void showServerError(ServerError error)
   {
      // consider: less obtrusive error message 
      RStudioGinjector.INSTANCE.getGlobalDisplay().showErrorMessage(
            "Chunk Execution Error", error.getMessage());
      
      // treat as an interrupt (don't clear output)
      completeInterrupt();
   }
   
   public void applyCachedEditorStyle()
   {
      if (!isEditorStyleCached())
         return;
      Style frameStyle = getElement().getStyle();
      frameStyle.setBorderColor(s_outlineColor);

      // apply the style to any frames in the output
      for (Widget w: root_)
      {
         if (w instanceof ChunkOutputFrame)
         {
            ChunkOutputFrame frame = (ChunkOutputFrame)w;
            Style bodyStyle = frame.getDocument().getBody().getStyle();
            bodyStyle.setColor(s_color);
         }
      }
      getElement().getStyle().setBackgroundColor(s_backgroundColor);
   }
   
   public void showConsoleCode(String code)
   {
      if (state_ != CONSOLE_READY &&
          state_ != CONSOLE_EXECUTING)
      {
         initConsole();
         state_ = CONSOLE_READY;
      }
      if (state_ == CONSOLE_READY)
      {
         registerConsoleEvents();
         state_ = CONSOLE_EXECUTING;
         showBusyState();
      }
      onRenderCompleted_.execute(console_.getElement().getOffsetHeight());
   }
   
   public static void cacheEditorStyle(Element editorContainer, 
         Style editorStyle)
   {
      s_backgroundColor = editorStyle.getBackgroundColor();
      s_color = editorStyle.getColor();
      s_outlineColor = DomUtils.extractCssValue("ace_print-margin", 
            "backgroundColor");
      JsArrayString classes = JsArrayString.createArray().cast();
      classes.push("ace_marker-layer");
      classes.push("ace_foreign_line");
      s_busyColor = DomUtils.extractCssValue(classes, "backgroundColor");
   }
   
   private void initConsole()
   {
      if (vconsole_ == null)
         vconsole_ = new VirtualConsole();
      else
         vconsole_.clear();
      if (console_ == null)
      {
         console_ = new PreWidget();
         console_.getElement().removeAttribute("tabIndex");
         console_.getElement().getStyle().setMarginTop(0, Unit.PX);
         console_.getElement().getStyle().setPropertyPx("maxHeight", 500);
         console_.getElement().getStyle().setOverflowY(Overflow.AUTO);
      }
      else
      {
         console_.getElement().setInnerHTML("");
      }

      // attach the console
      root_.add(console_);
   }
   
   private void destroyConsole()
   {
      vconsole_.clear();
      console_.removeFromParent();
      console_ = null;
   }
   
   private void registerConsoleEvents()
   {
      EventBus events = RStudioGinjector.INSTANCE.getEventBus();
      events.addHandler(ConsoleWriteOutputEvent.TYPE, this);
      events.addHandler(ConsoleWriteErrorEvent.TYPE, this);
      events.addHandler(ConsoleWriteInputEvent.TYPE, this);
      events.addHandler(ConsolePromptEvent.TYPE, this);
   }

   private void unregisterConsoleEvents()
   {
      EventBus events = RStudioGinjector.INSTANCE.getEventBus();
      events.removeHandler(ConsoleWriteOutputEvent.TYPE, this);
      events.removeHandler(ConsoleWriteErrorEvent.TYPE, this);
      events.removeHandler(ConsoleWriteInputEvent.TYPE, this);
      events.removeHandler(ConsolePromptEvent.TYPE, this);
   }
   
   public static boolean isEditorStyleCached()
   {
      return s_backgroundColor != null &&
             s_color != null &&
             s_outlineColor != null;
   }
   
   private void showBusyState()
   {
      getElement().getStyle().setBackgroundColor(s_busyColor);
      clear_.setVisible(false);
      interrupt_.setVisible(state_ == CONSOLE_EXECUTING);
      busy_.setVisible(state_ == CHUNK_EXECUTING);
   }

   private void showReadyState()
   {
      getElement().getStyle().setBackgroundColor(s_backgroundColor);
      clear_.setVisible(true);
      interrupt_.setVisible(false);
      busy_.setVisible(false);
   }
   
   private void setOverflowStyle()
   {
      Element ele = root_.getElement();
      boolean hasOverflow = ele.getScrollHeight() > ele.getOffsetHeight();
      if (hasOverflow && !root_.getElement().hasClassName(style.overflowY()))
      {
         root_.getElement().addClassName(style.overflowY());
      }
      else if (!hasOverflow && 
               root_.getElement().hasClassName(style.overflowY()))
      {
         root_.getElement().removeClassName(style.overflowY());
      }
   }
   
   private void completeInterrupt()
   {
      if (state_ == CONSOLE_EXECUTING)
      {
         state_ = CONSOLE_READY;
      }
      else if (state_ == CHUNK_EXECUTING)
      {
         state_ = CHUNK_RENDERED;
      }
      else
      {
         return;
      }
      showReadyState();
   }
   
   private void injectEmptyText(BodyElement body)
   {
      // if the chunk has empty content, show something so the user doesn't
      // just see a blank box
      if (body.getInnerHTML().trim().isEmpty())
      {
         body.setInnerHTML("<div class=\"emptyText\">" +
               "Chunk did not produce output." +
               "</div>");
      }
   }
   
   @UiField Image interrupt_;
   @UiField Image clear_;
   @UiField Image busy_;
   @UiField HTMLPanel root_;
   @UiField ChunkStyle style;
   
   private PreWidget console_;
   private VirtualConsole vconsole_;
   
   private int state_ = CHUNK_EMPTY;
   
   private CommandWithArg<Integer> onRenderCompleted_;
   private final String chunkId_;

   private static String s_outlineColor    = null;
   private static String s_backgroundColor = null;
   private static String s_color           = null;
   private static String s_busyColor       = null;
   
   public final static int CHUNK_EMPTY       = 0;
   public final static int CHUNK_EXECUTING   = 1;
   public final static int CHUNK_RENDERING   = 2;
   public final static int CHUNK_RENDERED    = 3;
   public final static int CONSOLE_READY     = 4;
   public final static int CONSOLE_EXECUTING = 5;
   
   public final static int CONSOLE_INPUT  = 0;
   public final static int CONSOLE_OUTPUT = 1;
   public final static int CONSOLE_ERROR  = 2;
}
