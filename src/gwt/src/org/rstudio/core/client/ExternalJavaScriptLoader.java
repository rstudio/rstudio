package org.rstudio.core.client;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.ScriptElement;

import java.util.LinkedList;

public class ExternalJavaScriptLoader
{
   public static interface Callback
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
      url_ = url;
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
      ScriptElement script = Document.get().createScriptElement();
      script.setType("text/javascript");
      script.setSrc(url_);
      registerCallback(script);
      Element head = Document.get().getElementsByTagName("head").getItem(0);
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

   @SuppressWarnings("unused")
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
}
