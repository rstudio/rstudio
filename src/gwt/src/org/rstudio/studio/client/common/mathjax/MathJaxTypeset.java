/*
 * MathJaxTypeset.java
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
package org.rstudio.studio.client.common.mathjax;

import org.rstudio.core.client.SerializedCommand;
import org.rstudio.core.client.SerializedCommandQueue;
import org.rstudio.core.client.dom.DomUtils;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Visibility;
import com.google.gwt.user.client.Timer;

public class MathJaxTypeset
{ 
   public static interface Callback
   {
      void onMathJaxTypesetComplete(boolean error);
   }
   
   public static void typeset(Element el, String currentText, Callback callback)
   {
      typeset(el, currentText, false, callback);
   }
   
   public static void typeset(Element el, String currentText, boolean priority, Callback callback)
   {
      SerializedCommand cmd = (cont) -> {
         typesetNative(el, currentText, new Callback() {
            @Override
            public void onMathJaxTypesetComplete(boolean error)
            {
               callback.onMathJaxTypesetComplete(error);
               cont.execute();
            }
         }, 0);
      };
         
      if (priority)
         TYPESET_QUEUE.addPriorityCommand(cmd);
      else
         TYPESET_QUEUE.addCommand(cmd);
   }

   public static final native void typesetNative(Element el, 
                                                 String currentText,
                                                 Object callback,
                                                 int attempt)
   /*-{
      var MathJax = $wnd.MathJax;
      
      // save last rendered text
      var jax = MathJax.Hub.getAllJax(el)[0];
      var lastRenderedText = jax && jax.originalText || "";
      
      // update text in element
      el.innerText = currentText;
      
      // typeset element
      var self = this;
      MathJax.Hub.Queue($entry(function() {
         MathJax.Hub.Typeset(el, $entry(function() {
            // restore original typesetting on failure
            jax = MathJax.Hub.getAllJax(el)[0];
            var error = !!(jax && jax.texError);
            if (error && lastRenderedText.length)
               jax.Text(lastRenderedText);

            // callback to GWT
            @org.rstudio.studio.client.common.mathjax.MathJaxTypeset::onMathJaxTypesetCompleted(Ljava/lang/Object;Ljava/lang/String;ZLjava/lang/Object;I)(el, currentText, error, callback, attempt);
         }));
      }));
   }-*/;
   
   
   private static void onMathJaxTypesetCompleted(final Object mathjaxElObject,
                                                 final String text,
                                                 final boolean error,
                                                 final Object commandObject,
                                                 final int attempt)
   {
      final Element mathjaxEl = (Element) mathjaxElObject;

      if (attempt < MAX_RENDER_ATTEMPTS)
      {
         // if mathjax displayed an error, try re-rendering once more
         Element[] errorEls = DomUtils.getElementsByClassName(mathjaxEl, 
               "MathJax_Error");
         if (errorEls != null && errorEls.length > 0 &&
             attempt < MAX_RENDER_ATTEMPTS)
         {
            // hide the error and retry in 25ms (experimentally this seems to
            // produce a better shot at rendering successfully than an immediate
            // or deferred retry)
            mathjaxEl.getStyle().setVisibility(Visibility.HIDDEN);
            new Timer()
            {
               @Override
               public void run()
               {
                  typesetNative(mathjaxEl, text, commandObject, attempt + 1);
               }
            }.schedule(25);
            return;
         }
      }
      
      // show whatever we've got (could be an error if we ran out of retries)
      mathjaxEl.getStyle().setVisibility(Visibility.VISIBLE);
      
      // execute callback
      if (commandObject != null && commandObject instanceof MathJaxTypeset.Callback)
      {
         MathJaxTypeset.Callback callback = (MathJaxTypeset.Callback) commandObject;
         callback.onMathJaxTypesetComplete(error);
      }
   }
   
   
   // sometimes MathJax fails to render initially but succeeds if asked to do so
   // again; this is the maximum number of times we'll try to re-render the same
   // text automatically before giving up
   private static final int MAX_RENDER_ATTEMPTS = 2;
   
   // can't call mathjax for typesetting concurrently so we serialize the calls with this queue
   private static final SerializedCommandQueue TYPESET_QUEUE = new SerializedCommandQueue();
   
}
