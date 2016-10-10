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

import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.ExternalJavaScriptLoader;
import org.rstudio.core.client.ExternalJavaScriptLoader.Callback;
import org.rstudio.core.client.jsonrpc.RpcObjectList;
import org.rstudio.core.client.resources.StaticDataResource;
import org.rstudio.studio.client.common.SuperDevMode;
import org.rstudio.studio.client.common.debugging.model.UnhandledError;
import org.rstudio.studio.client.common.shell.ShellDisplay;
import org.rstudio.studio.client.workbench.model.ConsoleAction;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorDisplay;
import org.rstudio.studio.client.workbench.views.terminal.xterm.XTermNative;
import org.rstudio.studio.client.workbench.views.terminal.xterm.XTermResources;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.logical.shared.AttachEvent;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Widget;

public class XTermWidget extends Widget implements ShellDisplay
{
   public XTermWidget()
   {
      setElement(Document.get().createDivElement());
      getElement().setTabIndex(0);
      terminal_ = XTermNative.createTerminal();
      terminal_.open(getElement());;
      attachToWidget(getElement(), terminal_);
     /* 
      terminalEventListeners_ = new ArrayList<HandlerRegistration>();
      addAttachHandler(new AttachEvent.Handler()
      {
         @Override
         public void onAttachOrDetach(AttachEvent event)
         {
            if (event.isAttached())
               attachToWidget(getElement(), terminal_);
            else
               detachFromWidget(getElement());
            
            if (!event.isAttached())
            {
               for (HandlerRegistration handler : terminalEventListeners_)
                  handler.removeHandler();
               terminalEventListeners_.clear();
            }
         }
      });
      */
   }
   
   public static void preload()
   {
      load(null);
   }

   public static void load(final Command command)
   {
      xtermLoader_.addCallback(new Callback()
      {
         public void onLoaded()
         {
            if (command != null)
               command.execute();
         }
     });
   }
   
   private static final native void attachToWidget(Element el, XTermNative terminal)
   /*-{
      el.$RStudioXTermTerminal= terminal;
   }-*/;

   private static final native void detachFromWidget(Element el)
   /*-{
      el.$RStudioXTermTerminal = null;
   }-*/; 

   @Override
   public void consoleWriteError(String string)
   {
      // TODO Auto-generated method stub
      
   }

   @Override
   public void consoleWriteExtendedError(String string,
                                         UnhandledError traceInfo,
                                         boolean expand, String command)
   {
      // TODO Auto-generated method stub
      
   }

   @Override
   public void consoleWriteOutput(String output)
   {
      // TODO Auto-generated method stub
      
   }

   @Override
   public void focus()
   {
      // TODO Auto-generated method stub
      
   }

   @Override
   public HandlerRegistration addKeyPressHandler(KeyPressHandler handler)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public void fireEvent(GwtEvent<?> event)
   {
      // TODO Auto-generated method stub
      
   }

   @Override
   public void consoleWriteInput(String input, String console)
   {
      // TODO Auto-generated method stub
      
   }

   @Override
   public void consoleWritePrompt(String prompt)
   {
      // TODO Auto-generated method stub
      
   }

   @Override
   public void consolePrompt(String prompt, boolean showInput)
   {
      // TODO Auto-generated method stub
      
   }

   @Override
   public void ensureInputVisible()
   {
      // TODO Auto-generated method stub
      
   }

   @Override
   public InputEditorDisplay getInputEditorDisplay()
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public void clearOutput()
   {
      // TODO Auto-generated method stub
      
   }

   @Override
   public String processCommandEntry()
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public int getCharacterWidth()
   {
      // TODO Auto-generated method stub
      return 0;
   }

   @Override
   public boolean isPromptEmpty()
   {
      // TODO Auto-generated method stub
      return false;
   }

   @Override
   public String getPromptText()
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public void setReadOnly(boolean readOnly)
   {
      // TODO Auto-generated method stub
      
   }

   @Override
   public void playbackActions(RpcObjectList<ConsoleAction> actions)
   {
      // TODO Auto-generated method stub
      
   }

   @Override
   public int getMaxOutputLines()
   {
      // TODO Auto-generated method stub
      return 0;
   }

   @Override
   public void setMaxOutputLines(int maxLines)
   {
      // TODO Auto-generated method stub
      
   }

   @Override
   public HandlerRegistration addCapturingKeyDownHandler(KeyDownHandler handler)
   {
      // TODO Auto-generated method stub
      return null;
   }

   @Override
   public Widget getShellWidget()
   {
      // TODO Auto-generated method stub
      return null;
   }
   
   private static final ExternalJavaScriptLoader getLoader(StaticDataResource release)
   {
      return getLoader(release, null);
   }
   
   private final XTermNative terminal_;
                                                           
   private static final ExternalJavaScriptLoader getLoader(StaticDataResource release,
                                                           StaticDataResource debug)
   {
      if (debug == null || !SuperDevMode.isActive())
         return new ExternalJavaScriptLoader(release.getSafeUri().asString());
      else
         return new ExternalJavaScriptLoader(debug.getSafeUri().asString());
   }
   
   private static final ExternalJavaScriptLoader xtermLoader_ =
         getLoader(XTermResources.INSTANCE.xtermjs(), XTermResources.INSTANCE.xtermjs() /*TODO uncompressed flavor */);

   //private final List<HandlerRegistration> terminalEventListeners_;
}
