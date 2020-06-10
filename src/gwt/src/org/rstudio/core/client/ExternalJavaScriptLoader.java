/*
 * ExternalJavaScriptLoader.java
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
package org.rstudio.core.client;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.ScriptElement;

import java.util.LinkedList;

public class ExternalJavaScriptLoader
{
   public interface Callback
   {
      void onLoaded();
   }

   private enum State
   {
      Start,
      Loading,
      Loaded,
      Error
   }

   public static void loadSequentially(String[] urls, final Callback callback)
   {
      final LinkedList<ExternalJavaScriptLoader> loaders =
            new LinkedList<ExternalJavaScriptLoader>();

      for (String url : urls)
         loaders.add(new ExternalJavaScriptLoader(url));

      Callback innerCallback = new Callback()
      {
         public void onLoaded()
         {
            if (!loaders.isEmpty())
               loaders.remove().addCallback(this);
            else
               callback.onLoaded();
         }
      };
      innerCallback.onLoaded();
   }

   public ExternalJavaScriptLoader(String url)
   {
      this(Document.get(), url);
   }
   
   public ExternalJavaScriptLoader(Document document, String url)
   {
      document_ = document;
      url_ = url;
   }
   
   public boolean isLoaded() 
   {
      return state_ == State.Loaded;
   }

   public void addCallback(Callback callback)
   {
      switch (state_)
      {
         case Start:
            callbacks_.add(callback);
            startLoading();
            break;
         case Loading:
            callbacks_.add(callback);
            break;
         case Loaded:
            callback.onLoaded();
            break;
         case Error:
            break;
      }
   }

   private void startLoading()
   {
      assert state_ == State.Start;
      state_ = State.Loading;
      ScriptElement script = document_.createScriptElement();
      script.setType("text/javascript");
      script.setSrc(url_);
      registerCallback(script);
      Element head = document_.getElementsByTagName("head").getItem(0);
      head.appendChild(script);
   }

   private native void registerCallback(ScriptElement script) /*-{
      var self = this;
      script.onreadystatechange = $entry(function() {
         if (this.readyState == 'complete')
            self.@org.rstudio.core.client.ExternalJavaScriptLoader::onLoaded()();
      });
      script.onload = $entry(function() {
         self.@org.rstudio.core.client.ExternalJavaScriptLoader::onLoaded()();
      });
   }-*/;

   private void onLoaded()
   {
      state_ = State.Loaded;
      Scheduler.get().scheduleIncremental(new RepeatingCommand()
      {
         public boolean execute()
         {
            if (!callbacks_.isEmpty())
               callbacks_.remove().onLoaded();

            return !callbacks_.isEmpty();
         }
      });
   }

   private LinkedList<Callback> callbacks_ = new LinkedList<Callback>();
   private State state_ = State.Start;
   private final String url_;
   private final Document document_;
}
