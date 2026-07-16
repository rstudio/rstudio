/*
 * MathJaxLoader.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.common.mathjax;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.core.client.Scheduler;
import com.google.gwt.core.client.Scheduler.RepeatingCommand;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.HeadElement;
import com.google.gwt.dom.client.ScriptElement;
import com.google.inject.Singleton;

@Singleton
public class MathJaxLoader
{
   public interface Callback
   {
      public void onLoaded(boolean alreadyLoaded);
   }

   public MathJaxLoader()
   {
   }

   public static boolean isMathJaxLoaded()
   {
      return MATHJAX_LOADED;
   }

   public static void ensureMathJaxLoaded()
   {
      if (MATHJAX_LOADED)
         return;

      initializeMathJaxConfig();
      ScriptElement mathJaxEl = createMathJaxScriptElement();
      HeadElement headEl = Document.get().getHead();
      headEl.appendChild(mathJaxEl);
      Scheduler.get().scheduleFixedDelay(new RepeatingCommand()
      {
         @Override
         public boolean execute()
         {
            if (isMathJaxReady())
            {
               MATHJAX_LOADED = true;
               for (Callback callback : MATHJAX_CALLBACKS)
                  callback.onLoaded(false);
               return false;
            }

            RETRY_COUNT++;
            return RETRY_COUNT < 50;
         }
      }, 500);
   }

   public static final void withMathJaxLoaded(Callback callback)
   {
      if (callback == null)
         return;

      if (MATHJAX_LOADED)
      {
         callback.onLoaded(true);
         return;
      }

      MATHJAX_CALLBACKS.add(callback);
   }

   // Private Methods ----

   private static final native void initializeMathJaxConfig() /*-{

      if (typeof $wnd.MathJax !== "undefined")
         return;

      var config = {
         startup: {
            typeset: false
         },
         loader: {
            load: ["ui/safe"]
         },
         tex: {
            inlineMath:  [["$", "$"], ["\\(", "\\)"]],
            displayMath: [["$$", "$$"], ["\\[", "\\]"]],
            processEscapes: true
         },
         options: {
            enableMenu: false,
            enableSpeech: false,
            enableBraille: false,
            enableEnrichment: false
         },
         output: {
            // resolve dynamically-loaded font resources (woff2 web fonts and
            // rare glyph range data) against our locally served copy, rather
            // than the CDN default
            fontPath: "mathjax4/fonts/%%FONT%%-font"
         }
      };

      // MathJax's option merging only accepts plain objects whose constructor
      // is the host window's Object; this literal is created in the GWT
      // frame's realm, so round-trip it through the host window's JSON to
      // re-create it there (otherwise the config is silently ignored)
      $wnd.MathJax = $wnd.JSON.parse(JSON.stringify(config));

   }-*/;

   private static final native boolean isMathJaxReady() /*-{
      // MathJax.typesetPromise is created once startup has finished
      // (including dynamic loading of any requested components)
      var mathjax = $wnd.MathJax || {};
      return typeof mathjax.typesetPromise === "function";
   }-*/;

   private static ScriptElement createMathJaxScriptElement()
   {
      ScriptElement el = Document.get().createScriptElement();
      el.setAttribute("type", "text/javascript");
      el.setSrc("mathjax4/tex-mml-chtml.js");
      el.setAttribute("async", "true");
      return el;
   }

   private static List<Callback> MATHJAX_CALLBACKS = new ArrayList<>();
   private static boolean MATHJAX_LOADED = false;
   private static int RETRY_COUNT = 0;

}
