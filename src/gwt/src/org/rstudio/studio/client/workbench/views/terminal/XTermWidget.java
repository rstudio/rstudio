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
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.jsonrpc.RpcObjectList;
import org.rstudio.core.client.resources.StaticDataResource;
import org.rstudio.studio.client.common.SuperDevMode;
import org.rstudio.studio.client.common.debugging.model.UnhandledError;
import org.rstudio.studio.client.common.shell.ShellDisplay;
import org.rstudio.studio.client.workbench.model.ConsoleAction;
import org.rstudio.studio.client.workbench.views.console.ConsoleResources;
import org.rstudio.studio.client.workbench.views.console.shell.editor.InputEditorDisplay;
import org.rstudio.studio.client.workbench.views.source.editors.text.ace.AceDocumentChangeEventNative;
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
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.shared.GwtEvent;
import com.google.gwt.event.shared.HandlerManager;
import com.google.gwt.event.shared.HandlerRegistration;
import com.google.gwt.event.shared.HasHandlers;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.RequiresResize;
import com.google.gwt.user.client.ui.Widget;

public class XTermWidget extends Widget implements ShellDisplay,
                                                   RequiresResize
                                                   
{
   public XTermWidget()
   {
      input_ = new XTermInputEditor(this);
      styles_ = ConsoleResources.INSTANCE.consoleStyles();
      createContainerElement();
      terminal_ = XTermNative.createTerminal(true);
      terminal_.open(getElement());
   }

   private void showBanner()
   {
      terminal_.writeln("Welcome to RStudio shell.");
      terminal_.writeln("Now brought to you by XTerm.");
      
      XTermDimensions screenSize = terminal_.proposeGeometry();
      String msg = new String("Screen size=");
      msg += screenSize.getCols();
      msg += "x";
      msg += screenSize.getRows();
      terminal_.writeln(msg);
   }
   
   private void createContainerElement()
   {
      attachTheme(XTermThemeResources.INSTANCE.xtermcss());
      setElement(Document.get().createDivElement());
      getElement().setTabIndex(0);
      getElement().getStyle().setMargin(0, Unit.PX);
      getElement().getStyle().setBackgroundColor("#111");
      getElement().getStyle().setColor("#fafafa");
   }
   
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
            }
         });

         ElementIds.assignElementId(this.getElement(), ElementIds.XTERM_WIDGET);
      }
   }
   
   protected void doOnLoad()
   {
      terminal_.fit();
      attachToWidget(getElement(), terminal_);
      terminal_.focus();
      showBanner(); 
   }
   
   public static void load(final Command command)
   {
      xtermLoader_.addCallback(new Callback()
      {
         public void onLoaded()
         {
            xtermFitLoader_.addCallback(new Callback()
            {
               public void onLoaded()
               {
                  if (command != null)
                     command.execute();
               }
            });
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
      terminal_.write(string);
   }

   @Override
   public void consoleWriteExtendedError(String string,
                                         UnhandledError traceInfo,
                                         boolean expand, String command)
   {
      terminal_.write(string);
   }

   @Override
   public void consoleWriteOutput(String output)
   {
      terminal_.write(output);;
   }

   @Override
   public void focus()
   {
      input_.setFocus(true);
   }

   @Override
   public HandlerRegistration addKeyPressHandler(KeyPressHandler handler)
   {
      return addHandler(handler, KeyPressEvent.getType());
   }

   @Override
   public void fireEvent(GwtEvent<?> event)
   {
      // TODO Auto-generated method stub
   }

   @Override
   public void consoleWriteInput(String input, String console)
   {
      terminal_.write(input);
   }

   @Override
   public void consoleWritePrompt(String prompt)
   {
      terminal_.write(prompt);
   }

   @Override
   public void consolePrompt(String prompt, boolean showInput)
   {
      terminal_.write(prompt);
   }

   @Override
   public void ensureInputVisible()
   {
      // TODO Auto-generated method stub
      
   }

   @Override
   public InputEditorDisplay getInputEditorDisplay()
   {
      return input_;
   }

   @Override
   public void clearOutput()
   {
      // TODO Auto-generated method stub
      
   }

   @Override
   public String processCommandEntry()
   {
      // parse out the command text
      // String promptText = prompt_.getElement().getInnerText();
      String commandText = "ls";// input_.getCode();
      input_.setText("");
      /*
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
*/
      return commandText ;
   }

   @Override
   public int getCharacterWidth()
   {
      return DomUtils.getCharacterWidth(getElement(), styles_.console());
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
      return "";
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
      return null;
      //return capturingHandlers_.addHandler(KeyDownEvent.getType(), handler);
   }

   @Override
   public Widget getShellWidget()
   {
      return this;
   }
   @Override
   public void onResize()
   {
      // TODO Auto-generated method stub
      
   }
   
   @Override
   public void addDataEventHandler(CommandWithArg<String> handler)
   {
      terminal_.onData(handler);
   }
   
   @Override
   public void setSuppressPendingInput(boolean suppressPendingInput)
   {
      // TODO Auto-generated method stub
      
   }

   public void setFocus(boolean focused)
   {
      if (focused)
         terminal_.focus();
      else
         terminal_.blur(); 
   }
   
   private static native void addEventListener(Element element,
                                        String event,
                                        HasHandlers handlers) /*-{
      var listener = $entry(function(e) {
         @com.google.gwt.event.dom.client.DomEvent::fireNativeEvent(Lcom/google/gwt/dom/client/NativeEvent;Lcom/google/gwt/event/shared/HasHandlers;Lcom/google/gwt/dom/client/Element;)(e, handlers, element);
      });
      element.addEventListener(event, listener, true);

   }-*/;
   
   private XTermNative terminal_;
   private XTermInputEditor input_;
   private LinkElement currentStyleEl_;
   private ConsoleResources.ConsoleStyles styles_;
   //private final HandlerManager capturingHandlers_;                                                           
   
   private static final ExternalJavaScriptLoader getLoader(StaticDataResource release)
   {
      return getLoader(release, null);
   }
   
   private static final ExternalJavaScriptLoader getLoader(StaticDataResource release,
                                                           StaticDataResource debug)
   {
      if (debug == null || !SuperDevMode.isActive())
         return new ExternalJavaScriptLoader(release.getSafeUri().asString());
      else
         return new ExternalJavaScriptLoader(debug.getSafeUri().asString());
   }
   
   private static final ExternalJavaScriptLoader xtermLoader_ =
         getLoader(XTermResources.INSTANCE.xtermjs(), 
               XTermResources.INSTANCE.xtermjs() /*TODO uncompressed flavor */);

   private static final ExternalJavaScriptLoader xtermFitLoader_ =
         getLoader(XTermResources.INSTANCE.xtermfitjs());

}
