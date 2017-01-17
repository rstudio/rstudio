/*
 * XTermWidget.java
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

package org.rstudio.studio.client.workbench.views.terminal.xterm;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.ExternalJavaScriptLoader;
import org.rstudio.core.client.ExternalJavaScriptLoader.Callback;
import org.rstudio.core.client.resources.StaticDataResource;
import org.rstudio.core.client.theme.res.ThemeStyles;
import org.rstudio.studio.client.common.SuperDevMode;
import org.rstudio.studio.client.workbench.views.terminal.events.ResizeTerminalEvent;
import org.rstudio.studio.client.workbench.views.terminal.events.TerminalDataInputEvent;
import org.rstudio.studio.client.workbench.views.terminal.events.XTermTitleEvent;
import org.rstudio.studio.client.workbench.views.terminal.events.XTermTitleEvent.Handler;
import org.rstudio.studio.client.workbench.views.terminal.xterm.XTermDimensions;
import org.rstudio.studio.client.workbench.views.terminal.xterm.XTermNative;
import org.rstudio.studio.client.workbench.views.terminal.xterm.XTermResources;
import org.rstudio.studio.client.workbench.views.terminal.xterm.XTermThemeResources;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.LinkElement;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.Timer;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;

/**
 * Xterm-compatible terminal emulator
 */
public class XTermWidget extends Widget implements RequiresResize,
                                                   ResizeTerminalEvent.HasHandlers, TerminalDataInputEvent.HasHandlers,
                                                   XTermTitleEvent.HasHandlers
{
  /**
    *  Creates an XTermWidget.
    */
   public XTermWidget()
   {
      // Create an element to hold the terminal widget
      setElement(Document.get().createDivElement());
      getElement().setTabIndex(0);
      getElement().getStyle().setMargin(0, Unit.PX);
      getElement().getStyle().setBackgroundColor("#111");
      getElement().getStyle().setColor("#fafafa");
      getElement().addClassName(ThemeStyles.INSTANCE.selectableText());
      getElement().addClassName(XTERM_CLASS);

      // Create and attach the native terminal object to this Widget
      attachTheme(XTermThemeResources.INSTANCE.xtermcss());
      terminal_ = XTermNative.createTerminal(getElement(), true);

      // Handle keystrokes from the xterm and dispatch them
      addDataEventHandler(new CommandWithArg<String>()
      {
         public void execute(String data)
         {
            fireEvent(new TerminalDataInputEvent(data));
         }
      });
      
      // Handle title events from the xterm and dispatch them
      addTitleEventHandler(new CommandWithArg<String>()
      {
         public void execute(String title)
         {
            fireEvent(new XTermTitleEvent(title));
         }
      });
   }
   
   /**
    * Perform actions when the terminal is ready.
    */
   protected void terminalReady()
   {
   }

   /**
    * One one line of text to the terminal.
    * @param str Text to write (CRLF will be appended)
    */
   public void writeln(String str)
   {
      terminal_.scrollToBottom();
      terminal_.writeln(str);
   }
   
   /**
    * Write text to the terminal.
    * @param str Text to write
    */
   public void write(String str)
   {
      terminal_.scrollToBottom();
      terminal_.write(str);
   }

   /**
    * Clear terminal buffer.
    */
   public void clear()
   {
      terminal_.clear();
      setFocus(true);
   }

   /**
    * Inject the xterm.js styles into the page.
    * @param cssResource
    */
   private void attachTheme(StaticDataResource cssResource)
   {
      if (currentStyleEl_ != null)
         currentStyleEl_.removeFromParent();

      currentStyleEl_ = Document.get().createLinkElement();
      currentStyleEl_.setType("text/css");
      currentStyleEl_.setRel("stylesheet");
      currentStyleEl_.setHref(cssResource.getSafeUri().asString());
      Document.get().getBody().appendChild(currentStyleEl_);
   }
  
   @Override
   protected void onLoad()
   {
      super.onLoad();
      if (!initialized_)
      {
         initialized_ = true;
         Scheduler.get().scheduleDeferred(new ScheduledCommand()
         {
            @Override
            public void execute()
            {
               terminal_.fit();
               terminal_.focus();
               terminalReady();
            }
         });

         ElementIds.assignElementId(this.getElement(), ElementIds.XTERM_WIDGET);
      }
   }
   
   @Override
   protected void onUnload()
   {
      super.onUnload();
      if (initialized_)
      {
         initialized_ = false;
         Scheduler.get().scheduleDeferred(new ScheduledCommand()
         {
            public void execute()
            {
               terminal_.blur();
            }
         });
      }
   }
   
   @Override
   public void onResize()
   {
      if (!isVisible())
      {
         return;
      }
      
      // Notify the local terminal UI that it has resized so it computes new
      // dimensions; debounce this slightly as it is somewhat expensive
      resizeTerminalLocal_.schedule(50);
      
      // Notify the remote pseudo-terminal that it has resized; this is quite
      // expensive so debounce more heavily; e.g. dragging the pane
      // splitters or resizing the entire window
      resizeTerminalRemote_.schedule(150);
   }
   
   private Timer resizeTerminalLocal_ = new Timer()
   {
      @Override
      public void run()
      {
         terminal_.fit();
      }
   };
   
   private Timer resizeTerminalRemote_ = new Timer()
   {
      @Override
      public void run()
      {
         XTermDimensions size = getTerminalSize();
         
         int cols = size.getCols();
         int rows = size.getRows();
         
         // ignore if a reasonable size couldn't be computed
         if (cols < 1 || rows < 1)
         {
            return;
         }

         // don't send same size multiple times
         if (cols == previousCols_ && rows == previousRows_)
         {
            return;
         }
         
         previousCols_ = cols;
         previousRows_ = rows;
         
         fireEvent(new ResizeTerminalEvent(cols, rows)); 
      }
   };
   
   private void addDataEventHandler(CommandWithArg<String> handler)
   {
      terminal_.onTerminalData(handler);
   }
   
   private void addTitleEventHandler(CommandWithArg<String> handler)
   {
      terminal_.onTitleData(handler);
   }
   
   public XTermDimensions getTerminalSize()
   {
      return terminal_.proposeGeometry(); 
   }
   
   public void setFocus(boolean focused)
   {
      if (focused)
         terminal_.focus();
      else
         terminal_.blur(); 
   }
  
   public static boolean isXTerm(Element el)
   {
      while (el != null)
      {
         if (el.hasClassName(XTERM_CLASS))
            return true;
         el = el.getParentElement();
      }
      return false;
   }
   
   private static final ExternalJavaScriptLoader getLoader(StaticDataResource release,
                                                           StaticDataResource debug)
   {
      if (debug == null || !SuperDevMode.isActive())
         return new ExternalJavaScriptLoader(release.getSafeUri().asString());
      else
         return new ExternalJavaScriptLoader(debug.getSafeUri().asString());
   }
   
   @Override
   public HandlerRegistration addResizeTerminalHandler(ResizeTerminalEvent.Handler handler)
   {
      return addHandler(handler, ResizeTerminalEvent.TYPE);
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

   /**
    * Load resources for XTermWidget.
    * 
    * @param command Command to execute after resources are loaded
    */
   public static void load(final Command command)
   {
      xtermLoader_.addCallback(new Callback()
      {
         @Override
         public void onLoaded()
         {
            xtermFitLoader_.addCallback(new Callback()
            {
               @Override
               public void onLoaded()
               {
                  if (command != null)
                     command.execute();
               }
            });
         }
     });
   }

   private static final ExternalJavaScriptLoader xtermLoader_ =
         getLoader(XTermResources.INSTANCE.xtermjs(), 
                   XTermResources.INSTANCE.xtermjsUncompressed());

   private static final ExternalJavaScriptLoader xtermFitLoader_ =
         getLoader(XTermResources.INSTANCE.xtermfitjs(),
                   XTermResources.INSTANCE.xtermfitjsUncompressed());

   private XTermNative terminal_;
   private LinkElement currentStyleEl_;
   private boolean initialized_ = false;

   private int previousRows_ = -1;
   private int previousCols_ = -1;
   private final static String XTERM_CLASS = "xterm-rstudio";

}