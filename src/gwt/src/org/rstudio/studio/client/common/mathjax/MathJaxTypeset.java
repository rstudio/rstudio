/*
 * MathJaxTypeset.java
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

import org.rstudio.core.client.SerializedCommand;
import org.rstudio.core.client.SerializedCommandQueue;

import com.google.gwt.dom.client.Element;

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
         });
      };

      if (priority)
         TYPESET_QUEUE.addPriorityCommand(cmd);
      else
         TYPESET_QUEUE.addCommand(cmd);
   }

   private static final native void typesetNative(Element el,
                                                  String currentText,
                                                  Object callback)
   /*-{
      var MathJax = $wnd.MathJax;

      // save last successfully rendered text, so we can restore the previous
      // rendering if the new text fails to typeset
      var lastRenderedText = el.getAttribute("data-mathjax-last-text") || "";

      var onCompleted = $entry(function(error) {
         @org.rstudio.studio.client.common.mathjax.MathJaxTypeset::onMathJaxTypesetCompleted(ZLjava/lang/Object;)(error, callback);
      });

      var typeset = function(text) {
         MathJax.typesetClear([el]);
         el.innerText = text;
         return MathJax.typesetPromise([el]).then(function() {
            // failed typesets surface in two ways, neither of which rejects
            // the typeset promise: recoverable TeX errors render as 'merror'
            // nodes, while malformed inputs (e.g. unbalanced braces) are
            // skipped by the math finder entirely, leaving raw text behind
            var container = el.querySelector("mjx-container");
            var merror = el.querySelector("mjx-merror, [data-mjx-error]");
            return container == null || merror != null;
         });
      };

      typeset(currentText).then(function(error) {

         if (!error)
         {
            el.setAttribute("data-mathjax-last-text", currentText);
            onCompleted(false);
            return;
         }

         // restore the previous (successful) render on failure
         if (lastRenderedText.length)
         {
            typeset(lastRenderedText).then(function() {
               onCompleted(true);
            }, function() {
               onCompleted(true);
            });
            return;
         }

         onCompleted(true);

      }, function(err) {
         onCompleted(true);
      });
   }-*/;


   private static void onMathJaxTypesetCompleted(final boolean error,
                                                 final Object commandObject)
   {
      if (commandObject != null && commandObject instanceof MathJaxTypeset.Callback)
      {
         MathJaxTypeset.Callback callback = (MathJaxTypeset.Callback) commandObject;
         callback.onMathJaxTypesetComplete(error);
      }
   }


   // can't call mathjax for typesetting concurrently so we serialize the calls with this queue
   private static final SerializedCommandQueue TYPESET_QUEUE = new SerializedCommandQueue();

}
