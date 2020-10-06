/*
 * MutationObserver.java
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
package org.rstudio.core.client.dom;

import org.rstudio.core.client.js.JsObject;

import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.dom.client.Element;
import com.google.gwt.user.client.Command;

/**
 * A wrapper class around the JavaScript MutationObserver.
 * See: https://developer.mozilla.org/en-US/docs/Web/API/MutationObserver
 */
public class MutationObserver
{
   public static class Builder
   {
      public Builder(Command callback)
      {
         this.callback = callback;
      }

      public Builder attributes(boolean value)
      {
         this.attributes = value;
         return this;
      }

      public Builder characterData(boolean value)
      {
         this.characterData = value;
         return this;
      }

      public Builder childList(boolean value)
      {
         this.childList = value;
         return this;
      }

      public Builder subtree(boolean value)
      {
         this.subtree = value;
         return this;
      }

      public MutationObserver get()
      {
         JsObject config = JsObject.createJsObject();

         config.setBoolean("attributes",    attributes);
         config.setBoolean("characterData", characterData);
         config.setBoolean("childList",     childList);
         config.setBoolean("subtree",       subtree);

         return new MutationObserver(callback, config);
      }

      private final Command callback;

      private boolean attributes    = false;
      private boolean characterData = false;
      private boolean childList     = false;
      private boolean subtree       = false;
   }

   // TODO: should this accept a CommandWithArg<MutationList> or similar?
   private MutationObserver(Command callback,
                            JavaScriptObject config)
   {
      observer_ = observerCreate(callback);
      config_   = config;
   }

   public void observe(Element el)
   {
      observerObserve(observer_, el, config_);
   }

   public void disconnect()
   {
      observerDisconnect(observer_);
   }

   private static final native JavaScriptObject observerCreate(Command command)
   /*-{

      var callback = $entry(function(mutations) {
         command.@com.google.gwt.user.client.Command::execute()();
      });

      return new MutationObserver(callback);

   }-*/;

   private static final native void observerObserve(JavaScriptObject observer,
                                                    Element el,
                                                    JavaScriptObject config)
   /*-{
      return observer.observe(el, config);
   }-*/;

   private static final native void observerDisconnect(JavaScriptObject observer)
   /*-{
      observer.disconnect();
   }-*/;


   private final JavaScriptObject observer_;
   private final JavaScriptObject config_;
}
