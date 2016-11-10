/*
 * XTermWidget.java
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

package org.rstudio.studio.client.workbench.views.terminal;

import org.rstudio.core.client.CommandWithArg;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.ExternalJavaScriptLoader;
import org.rstudio.core.client.ExternalJavaScriptLoader.Callback;
import org.rstudio.core.client.resources.StaticDataResource;
import org.rstudio.studio.client.common.SuperDevMode;
import org.rstudio.studio.client.workbench.views.terminal.events.ResizeTerminalEvent;
import org.rstudio.studio.client.workbench.views.terminal.events.TerminalDataInputEvent;
import org.rstudio.studio.client.workbench.views.terminal.xterm.XTermDimensions;
import org.rstudio.studio.client.workbench.views.terminal.xterm.XTermNative;
import org.rstudio.studio.client.workbench.views.terminal.xterm.XTermResources;
import org.rstudio.studio.client.workbench.views.terminal.xterm.XTermThemeResources;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.ScheduledCommand;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.LinkElement;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;

/**
 * Xterm-compatible terminal emulator
 */
public class XTermWidget extends Widget implements RequiresResize,
                                                   ResizeTerminalEvent.HasHandlers,
                                                   TerminalDataInputEvent.HasHandlers
{
   public enum AnsiColor
   {
      DEFAULT     ("0;0"),
      BLACK       ("0;30"),
      BLUE        ("0;34"),
      GREEN       ("0;32"),
      CYAN        ("0;36"),
      RED         ("0;31"),
      PURPLE      ("0;35"),
      BROWN       ("0;33"),
      LIGHTGRAY   ("0;37"),
      DARKGRAY    ("1;30"),
      LIGHTBLUE   ("1;34"),
      LIGHTCYAN   ("1;32"),
      LIGHTRED    ("1;31"),
      LIGHTPURPLE ("1;35"), 
      YELLOW      ("1;33"),
      WHITE       ("1;37");
      
      private final String color;
      AnsiColor(String color)
      {
         this.color = color;
      }
      
      public String toString()
      {
         return "\33[" + color + "m";
      }
   }      
   
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
      
      // Create and attach the native terminal object to this Widget
      attachTheme(XTermThemeResources.INSTANCE.xtermcss());
      terminal_ = XTermNative.createTerminal(getElement(), true);
   }
   
   /**
    * Show a greeting in the terminal
    */
   private void showBanner()
   {
      writeln("Welcome to " + AnsiColor.LIGHTBLUE + "RStudio" +
            AnsiColor.DEFAULT + " shell.");
   }
  
   /**
    * One one line of text to the terminal.
    * @param str Text to write (CRLF will be appended)
    */
   public void writeln(String str)
   {
      terminal_.writeln(str);
   }
   
   /**
    * Write text to the terminal.
    * @param str Text to write
    */
   public void write(String str)
   {
      terminal_.write(str);
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
               showBanner();
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
               terminal_.destroy();
            }
         });
      }
   }
   
   @Override
   public void onResize()
   {
      // Notify the local terminal UI that it has resized so it computes new
      // dimensions
      terminal_.fit();
      XTermDimensions size = getTerminalSize();
     
      fireEvent(new ResizeTerminalEvent(size.getCols(), size.getRows()));
   }

   private void addDataEventHandler(CommandWithArg<String> handler)
   {
      terminal_.onTerminalData(handler);
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
      return handlers_.addHandler(ResizeTerminalEvent.TYPE, handler);
   }
   
   @Override
   public HandlerRegistration addTerminalDataInputHandler(TerminalDataInputEvent.Handler handler)
   {
      assert !hasTerminalDataInputHandler_ : "Cannot install multiple terminalDataInput handlers";
      if (hasTerminalDataInputHandler_)
         return null;

      addDataEventHandler(new CommandWithArg<String>()
      {
         public void execute(String data)
         {
            fireEvent(new TerminalDataInputEvent(data));
         }
      });
            
      hasTerminalDataInputHandler_ = true;
      return handlers_.addHandler(TerminalDataInputEvent.TYPE, handler);
   }

   @Override
   public void fireEvent(GwtEvent<?> event)
   {
      handlers_.fireEvent(event);
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
   private final HandlerManager handlers_ = new HandlerManager(this);
   private boolean hasTerminalDataInputHandler_ = false;
}