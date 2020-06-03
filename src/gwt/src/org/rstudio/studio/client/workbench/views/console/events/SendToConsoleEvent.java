/*
 * SendToConsoleEvent.java
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
package org.rstudio.studio.client.workbench.views.console.events;

import com.google.gwt.event.shared.EventHandler;
import org.rstudio.core.client.js.JavaScriptSerializable;
import org.rstudio.studio.client.application.events.CrossWindowEvent;

import com.google.gwt.core.client.JavaScriptObject;

@JavaScriptSerializable
public class SendToConsoleEvent extends CrossWindowEvent<SendToConsoleEvent.Handler>
{
   public static final Type<Handler> TYPE = new Type<>();

   public interface Handler extends EventHandler
   {
      void onSendToConsole(SendToConsoleEvent event);
   }
   
   public static class Data extends JavaScriptObject
   {
      protected Data() {}
      
      public final native String getCode()        /*-{ return this["code"];      }-*/;
      public final native String getLanguage()    /*-{ return this["language"];  }-*/;
      
      public final native boolean shouldExecute() /*-{ return !!this["execute"]; }-*/;
      public final native boolean shouldRaise()   /*-{ return !!this["raise"];   }-*/;
      public final native boolean shouldFocus()   /*-{ return !!this["focus"];   }-*/;
      public final native boolean shouldAnimate() /*-{ return !!this["animate"]; }-*/;
   }
  
   public SendToConsoleEvent()
   {
   }

   public SendToConsoleEvent(String code, boolean execute)
   {
      this(code, execute, false);
   }
   
   public SendToConsoleEvent(String code, String language, boolean execute)
   {
      this(code, language, execute, 
            true,   /* raise */
            false,  /* focus */
            false); /* animate */
   }
   
   public SendToConsoleEvent(String code, boolean execute, boolean focus)
   {
      this(code, execute, true, focus, false);
   }
   
   public SendToConsoleEvent(Data data)
   {
      this(
            data.getCode(),
            data.getLanguage(),
            data.shouldExecute(),
            data.shouldRaise(),
            data.shouldFocus(),
            data.shouldAnimate());
   }
   
   public SendToConsoleEvent(String code, 
                             boolean execute, 
                             boolean raise,
                             boolean focus)
   {
      this(code, execute, raise, focus, false);
   }
   
   public SendToConsoleEvent(String code, 
                             boolean execute, 
                             boolean raise,
                             boolean focus,
                             boolean animate)
   {
      this(code, "R", execute, raise, focus, animate);
   }
   
   public SendToConsoleEvent(String code,
                             String language,
                             boolean execute, 
                             boolean raise,
                             boolean focus,
                             boolean animate)
   {
      code_ = code;
      language_ = language;
      execute_ = execute;
      raise_ = raise;
      focus_ = focus;
      animate_ = animate;
   }
   

   public String getCode()
   {
      return code_;
   }
   
   public String getLanguage()
   {
      return language_;
   }

   public boolean shouldExecute()
   {
      return execute_;
   }
   
   public boolean shouldRaise()
   {
      return raise_;
   }
   
   public boolean shouldFocus()
   {
      return focus_;
   }
   
   public boolean shouldAnimate()
   {
      return animate_;
   }
   
   @Override
   public int focusMode()
   {
      return shouldFocus() ? CrossWindowEvent.MODE_FOCUS : 
                             CrossWindowEvent.MODE_AUXILIARY;
   }
   
   @Override
   public Type<Handler> getAssociatedType()
   {
      return TYPE;
   }

   @Override
   protected void dispatch(Handler sendToConsoleHandler)
   {
      sendToConsoleHandler.onSendToConsole(this);
   }

   private String code_;
   private String language_;
   private boolean execute_;
   private boolean focus_;
   private boolean raise_;
   private boolean animate_;
}
