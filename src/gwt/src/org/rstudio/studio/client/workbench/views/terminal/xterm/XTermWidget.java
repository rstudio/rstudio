/*
 * XTermWidget.java
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

package org.rstudio.studio.client.workbench.views.terminal.xterm;

import jsinterop.base.Js;
import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.ExternalJavaScriptLoader;
import org.rstudio.core.client.ExternalStyleSheetLoader;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.core.client.widget.FontSizer;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.studio.client.workbench.views.console.ConsoleResources;
import org.rstudio.studio.client.workbench.views.terminal.events.TerminalDataInputEvent;
import org.rstudio.studio.client.workbench.views.terminal.events.XTermTitleEvent;
import org.rstudio.studio.client.workbench.views.terminal.events.XTermTitleEvent.Handler;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.EventListener;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;

import java.util.ArrayList;
import java.util.function.Consumer;

/**
 * Xterm-compatible terminal emulator widget. This widget does no network
 * communication.
 *
 * To receive input (user typing), subscribe to TerminalDataInputEvent.
 *
 * To send output to the terminal, use write() or writeln().
 *
 * To receive notice of terminal resizes, override resizePTY().
 *
 * For title changes (via escape sequences sent to terminal), subscribe to
 * XTermTitleEvent.
 */
public class XTermWidget extends Widget
                         implements RequiresResize,
                                    TerminalDataInputEvent.HasHandlers,
                                    XTermTitleEvent.HasHandlers,
                                    Consumer<String>
{
   /**
    * Creates an XTermWidget.
    */
   public XTermWidget(XTermOptions options, boolean tabMovesFocus, boolean showWebLinks)
   {
      options_ = options;
      tabMovesFocus_ = tabMovesFocus;
      showWebLinks_ = showWebLinks;

      // Create an element to hold the terminal widget
      setElement(Document.get().createDivElement());
      setStyleName(ConsoleResources.INSTANCE.consoleStyles().console());
      getElement().setTabIndex(0);
      getElement().getStyle().setMargin(0, Unit.PX);
      getElement().addClassName(ThemeStyles.INSTANCE.selectableText());
      getElement().addClassName(XTERM_CLASS);
      getElement().addClassName("ace_editor");
   }

   /**
    * Initialize the xterm control. This requires that the XTermWidget's underlying
    * element is visible (has dimensions) as xterm performs DOM-based measurements
    * when it starts up.
    */
   public void open(Operation callback)
   {
      load(() -> {
         Scheduler.get().scheduleDeferred(() -> {
            // Create and attach the native terminal object to this Widget
            terminal_ = new XTermTerminal(options_);
            fit_ = new XTermFitAddon();
            terminal_.loadAddon(fit_);
            if (showWebLinks_)
            {
               webLinks_ = new XTermWebLinksAddon();
               terminal_.loadAddon(webLinks_);
            }
            terminal_.open(getElement());
            terminal_.focus();
            terminal_.addClass("ace_editor");
            terminal_.addClass(FontSizer.getNormalFontSizeClass());

            // Previous versions of xterm.js used 'terminal' css class for styling, and we
            // have styles based on that. Xterm3 switched to 'xterm' instead of 'terminal',
            // and an API-based styling technique, but they left in 'terminal' class on root
            // element. This causes our styles (which are still in the css so we can get them
            // at runtime and pass to the API) to bleed through when using DOM-based renderer.
            // Fix by removing the unnecessary 'terminal' class.
            terminal_.removeClass("terminal");

            // Handle keystrokes from the xterm and dispatch them
            addDataEventHandler(data -> fireEvent(new TerminalDataInputEvent(data)));

            // Handle title events from the xterm and dispatch them
            addTitleEventHandler(title -> fireEvent(new XTermTitleEvent(title)));

            // Allow tab key to move focus out of terminal?
            terminal_.attachCustomKeyEventHandler(keyboardEvent ->
            {
               return !(tabMovesFocus_ && StringUtil.equals(keyboardEvent.key, "Tab"));
            });

            initialized_ = true;
            fit_.fit();
            terminal_.focus();
            callback.execute();
         });
      });
   }

   /**
    * Has the underlying terminal emulator UI (xterm) been loaded?
    */
   public boolean terminalEmulatorLoaded()
   {
      return terminal_ != null;
   }

   /**
    * One one line of text to the terminal.
    * @param str Text to write (CRLF will be appended)
    */
   public void writeln(String str)
   {
      terminal_.scrollToBottom();
      terminal_.writeln(str, null);
   }

   /**
    * Write text to the terminal.
    * @param str Text to write
    */
   @Override
   public void accept(String str)
   {
      terminal_.scrollToBottom();
      terminal_.write(str, null);
   }

   /**
    * Clear terminal buffer.
    */
   public void clear()
   {
      terminal_.clear();
   }

   @Override
   protected void onUnload()
   {
      super.onUnload();
      if (initialized_)
      {
         initialized_ = false;
         Scheduler.get().scheduleDeferred(() -> terminal_.blur());
      }
   }

   @Override
   public void onResize()
   {
      // Notify the local terminal UI that it has resized so it computes new
      // dimensions; debounce this slightly as it is somewhat expensive
      if (resizeTerminalLocal_.isRunning())
         resizeTerminalLocal_.cancel();
      if (!isResizable())
         return;
      resizeTerminalLocal_.schedule(RESIZE_DELAY);
   }

   /**
    * Is the terminal in a state where we can safely compute dimensions?
    */
   public boolean isResizable()
   {
      if (!isVisible())
         return false;

      if (!terminalEmulatorLoaded())
         return false;

      XTermDimensions size = getTerminalSize();
      return size.getCols() >= 1 && size.getRows() >= 1;
   }

   public void resizePTY(int cols, int rows)
   {
   }

   private final Timer resizeTerminalLocal_ = new Timer()
   {
      @Override
      public void run()
      {
         // if resize was invoked before terminal emulator loaded, delay again
         if (!terminalEmulatorLoaded())
         {
            resizeTerminalLocal_.schedule(RESIZE_DELAY);
            return;
         }

         // if emulator became invisible since resize was issued, resizing may cause exceptions
         if (!isResizable())
         {
            if (resizeTerminalRemote_.isRunning())
               resizeTerminalRemote_.cancel();
            return;
         }

         fit_.fit();

         // Notify the remote pseudo-terminal that it has resized; this is quite
         // expensive so debounce again; e.g. dragging the pane splitters or
         // resizing the entire window
         if (resizeTerminalRemote_.isRunning())
            resizeTerminalRemote_.cancel();
         resizeTerminalRemote_.schedule(RESIZE_DELAY);
      }
   };

   private final Timer resizeTerminalRemote_ = new Timer()
   {
      @Override
      public void run()
      {
         XTermDimensions size = getTerminalSize();

         // ignore if a reasonable size couldn't be computed
         if (size.getCols() < 1 || size.getRows() < 1)
         {
            return;
         }

         resizePTY(size.getCols(), size.getRows());
      }
   };

   private void addDataEventHandler(CommandWithArg<String> handler)
   {
      xtermEventUnsubscribe_.add(terminal_.onData(data -> handler.execute(data)));
   }

   private void addTitleEventHandler(CommandWithArg<String> handler)
   {
      xtermEventUnsubscribe_.add(terminal_.onTitleChange(data -> handler.execute(data)));
   }

   private XTermDimensions getTerminalSize()
   {
      return fit_.proposeDimensions();
   }

   public void setFocus(boolean focused)
   {
      if (focused)
         terminal_.focus();
      else
         terminal_.blur();
   }

   /**
    * @return Current cursor column
    */
   public int getCursorX()
   {
      return terminal_.getBuffer().getActive().getCursorX();
   }

   /**
    * @return Current cursor row
    */
   public int getCursorY()
   {
      return terminal_.getBuffer().getActive().getCursorY();
   }

   /**
    * Whether hitting Tab key moves focus out of terminal, or sends tab key to emulator
    *
    * @param movesFocus
    */
   public void setTabMovesFocus(boolean movesFocus)
   {
      tabMovesFocus_ = movesFocus;
   }

   /**
    * @return true if cursor at end of current line, false if not at EOL or
    * terminal is showing alternate buffer
    */
   public boolean cursorAtEOL()
   {
      if (xtermAltBufferActive())
      {
         return false;
      }

      String line = currentLine();
      if (line == null)
      {
         return false;
      }

      for (int i = getCursorX(); i < line.length(); i++)
      {
         if (line.charAt(i) != ' ')
         {
            return false;
         }
      }
      return true;
   }

   /**
    * @return Text of current line buffer
    */
   public String currentLine()
   {
      XTermBuffer buffer = terminal_.getBuffer().getActive();
      XTermBufferLine line = buffer.getLine(buffer.getCursorY() + buffer.getBaseY());
      if (line == null) // resize may be in progress
         return null;
      return line.translateToString();
   }

   /**
    * Is the terminal showing the alternate full-screen buffer?
    * @return true if full-screen buffer is active
    */
   public boolean xtermAltBufferActive()
   {
      return terminalEmulatorLoaded() &&
         StringUtil.equals(terminal_.getBuffer().getActive().getType(), ALT_BUFFER);
   }

   /**
    * Switch terminal to primary buffer.
    */
   public void showPrimaryBuffer()
   {
      terminal_.write("\u001b[?1047l", null); // show primary buffer
      terminal_.write("\u001b[m", null); // reset all visual attributes
      terminal_.write("\u001b[?9l", null); // reset mouse mode
   }

   /**
    * Switch terminal to alt-buffer
    */
   public void showAltBuffer()
   {
      terminal_.write("\u001b[?1047h", null); // show alt buffer
   }

   /**
    * @param el Element to test, may be null
    * @return If element is part of an XTermWidget, return that widget, otherwise null.
    */
   public static XTermWidget tryGetXTerm(Element el)
   {
      while (el != null)
      {
         if (el.hasClassName(XTERM_CLASS))
         {
            EventListener listener = DOM.getEventListener(el);
            if (listener == null)
            {
               Debug.log("Unexpected failure to get XTERM_CLASS listener");
            }
            else if (listener instanceof XTermWidget)
            {
               return (XTermWidget) listener;
            }
            else
            {
               Debug.log("Unexpected: XTERM_CLASS listener was not an XTermWidget");
            }
            return null;
         }
         el = el.getParentElement();
      }
      return null;
   }

   @Override
   public HandlerRegistration addTerminalDataInputHandler(TerminalDataInputEvent.Handler handler)
   {
      return addHandler(handler, TerminalDataInputEvent.TYPE);
   }

   @Override
   public HandlerRegistration addXTermTitleHandler(Handler handler)
   {
      return addHandler(handler, XTermTitleEvent.TYPE);
   }

   public void updateTheme(XTermTheme theme)
   {
      if (terminalEmulatorLoaded())
         terminal_.setOption("theme", Js.cast(theme));
   }

   public void updateOption(String option, boolean value)
   {
      if (terminalEmulatorLoaded())
         terminal_.setOption(option, Js.cast(value));
   }

   public void updateOption(String option, double value)
   {
      if (terminalEmulatorLoaded())
         terminal_.setOption(option, Js.cast(value));
   }

   public void updateOption(String option, String value)
   {
      if (terminalEmulatorLoaded())
         terminal_.setOption(option, Js.cast(value));
   }

   /**
    * Load resources for XTermWidget.
    *
    * @param command Command to execute after resources are loaded
    */
   public static void load(final Command command)
   {
      xtermCssLoader_.addCallback(() ->
      {
         xtermLoader_.addCallback(() ->
         {
            xtermFitLoader_.addCallback(() ->
            {
               xtermWebLinksLoader_.addCallback(() ->
               {
                  if (command != null)
                     command.execute();
               });
            });
         });
      });
   }

   public void refresh()
   {
      if (terminalEmulatorLoaded())
         terminal_.refresh(0, terminal_.getRows() - 1);
   }

   // XTerm.js buffer modes
   public static final String NORMAL_BUFFER = "normal";
   public static final String ALT_BUFFER = "alternate";

   private static final int RESIZE_DELAY = 50;

   private static final ExternalStyleSheetLoader xtermCssLoader_ =
         new ExternalStyleSheetLoader(XTermThemeResources.INSTANCE.xtermcss().getSafeUri().asString());

   private static final ExternalJavaScriptLoader xtermLoader_ =
         new ExternalJavaScriptLoader(XTermResources.INSTANCE.xtermjs().getSafeUri().asString());

   private static final ExternalJavaScriptLoader xtermFitLoader_ =
         new ExternalJavaScriptLoader(XTermResources.INSTANCE.xtermfitjs().getSafeUri().asString());

   private static final ExternalJavaScriptLoader xtermWebLinksLoader_ =
      new ExternalJavaScriptLoader(XTermResources.INSTANCE.xtermweblinksjs().getSafeUri().asString());

   private XTermTerminal terminal_;
   private XTermFitAddon fit_;
   private XTermWebLinksAddon webLinks_;
   private boolean initialized_ = false;
   private final XTermOptions options_;
   private boolean tabMovesFocus_;
   private boolean showWebLinks_;
   private final ArrayList<XTermDisposable> xtermEventUnsubscribe_ = new ArrayList<>();

   private final static String XTERM_CLASS = "xterm-rstudio";
}
