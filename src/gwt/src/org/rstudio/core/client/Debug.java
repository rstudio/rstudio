/*
 * Debug.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
package org.rstudio.core.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.AttachDetachException;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HTML;
import com.google.gwt.user.client.ui.VerticalPanel;

import org.rstudio.core.client.regex.Pattern;
import org.rstudio.core.client.widget.MiniPopupPanel;
import org.rstudio.studio.client.server.ServerError;

public class Debug
{
   public static native void injectDebug() /*-{
      $Debug = {
         log: $entry(function(message) {
            @org.rstudio.core.client.Debug::log(Ljava/lang/String;)(message);
         })
      };
      $wnd['$Debug'] = $Debug;
   }-*/;

   public static void log(String message)
   {
      GWT.log(message, null) ;
      logToConsole(message) ;
   }
   
   public static void logWarning(String warning)
   {
      printStackTrace("WARNING: " + warning);
   }
   
   public static native void logObject(Object object) /*-{
      if (typeof(console) != "undefined")
      {
         console.log(object);
      }
   }-*/;
   
   public static native void logToConsole(String message) /*-{
    if (typeof(console) != "undefined")
    {
         console.log(message) ;
    }
   }-*/ ;

   public static <T> T printValue(String label, T value)
   {
      Debug.log(label + '=' + value);
      return value;
   }
   
   public static void printStackTrace(String label)
   {
      StringBuffer buf = new StringBuffer(label + "\n");
      for (StackTraceElement ste : new Throwable().getStackTrace())
      {
         buf.append("\tat " + ste + "\n");
      }
      log(buf.toString());
   }

   public static void logError(ServerError error)
   {
      Debug.log(error.toString());
   }
   
   public static void logException(Exception e)
   {
      Debug.log(e.toString());
   }

   /**
    * Same as log() but for short-term messages that should not be checked
    * in. Making this a different method from log() allows devs to use Find
    * Usages to get rid of all calls before checking in changes. 
    */
   public static void devlog(String label)
   {
      log(label);
   }

   public static <T> T devlog(String label, T passthrough)
   {
      Debug.devlog(label);
      return passthrough;
   }

   public static void dump(JavaScriptObject object)
   {
      Debug.log(new JSONObject(object).toString());
   }
   
   public static native void prettyPrint(JavaScriptObject obj) /*-{
      var str = JSON.stringify(obj, undefined, 2);
      console.log(str);
   }-*/ ;

   public static void logAttachDetachException(AttachDetachException ade)
   {
      if (ade == null)
         return;

      for (Throwable t : ade.getCauses())
      {
         if (t instanceof AttachDetachException)
            logAttachDetachException((AttachDetachException) t);
         else
         {
            Debug.devlog(t.toString());
            for (StackTraceElement ste : t.getStackTrace())
               Debug.devlog(ste.toString());
         }
      }
   }

   public static void devlogf(String format,
                              Object... args)
   {
      int i = 0;
      for (Object arg : args)
      {
         format = format.replaceFirst(Pattern.escape("{" + (i++) + "}"),
                                      arg == null ? "NULL" : arg.toString());
      }
      devlog(format);
   }
   
   public static void logToRConsole(String message)
   {
      Element consoleEl = Document.get().getElementById("rstudio_console_output");
      if (consoleEl == null)
         return;
      
      Element textEl = Document.get().createSpanElement();
      String safe = SafeHtmlUtils.fromString(message).asString();
      textEl.setInnerHTML("* " + safe);
      consoleEl.appendChild(textEl);
      consoleEl.appendChild(Document.get().createBRElement());
      consoleEl.setScrollTop(Integer.MAX_VALUE);
   }
   
   public static void logStatus(String html)
   {
      Element el = debugPopupElement();
      html = html.replaceAll("\n", "<br />");
      el.setInnerHTML(html);
   }
   
   private static final Element debugPopupElement()
   {
      Element el = DOM.getElementById("rstudio_debug_output");
      if (el != null)
         return el;
               
      final MiniPopupPanel popupPanel = new MiniPopupPanel(false, false);
      VerticalPanel verticalPanel = new VerticalPanel();
      FlowPanel contentPanel = new FlowPanel();
      contentPanel.getElement().setId("rstudio_debug_output");
      verticalPanel.add(new HTML("<h3 style='margin: 0;'>RStudio Debug Output</h4><hr />"));
      verticalPanel.add(contentPanel);
      popupPanel.setWidget(verticalPanel);
      popupPanel.setAutoHideEnabled(false);
      popupPanel.setAutoHideOnHistoryEventsEnabled(false);
      popupPanel.show();
      return contentPanel.getElement();
   }
   
   public static native void breakpoint() /*-{
      debugger;
   }-*/;
      
}
