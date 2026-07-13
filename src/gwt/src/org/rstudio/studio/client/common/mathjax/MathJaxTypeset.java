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

      var onCompleted = $entry(function(error) {
         @org.rstudio.studio.client.common.mathjax.MathJaxTypeset::onMathJaxTypesetCompleted(ZLjava/lang/Object;)(error, callback);
      });

      // bail (keeping the typeset queue alive) if the target has been
      // detached in the interim
      if (el.parentNode == null)
      {
         onCompleted(true);
         return;
      }

      // typeset into a hidden scratch sibling, so that in-progress (or
      // failed) renders are never visible -- while typing, incomplete
      // expressions would otherwise flash a rendered TeX error before the
      // previous render could be restored. the scratch element shares the
      // target's parent and class so it typesets with the same font metrics
      var scratch = el.ownerDocument.createElement(el.tagName);
      scratch.className = el.className;
      scratch.style.position = "absolute";
      scratch.style.visibility = "hidden";
      if (el.offsetWidth > 0)
         scratch.style.width = el.offsetWidth + "px";
      scratch.innerText = currentText;
      el.parentNode.appendChild(scratch);

      var cleanup = function() {
         MathJax.typesetClear([scratch]);
         scratch.parentNode.removeChild(scratch);
      };

      MathJax.typesetPromise([scratch]).then(function() {

         // failed typesets surface in two ways, neither of which rejects
         // the typeset promise: recoverable TeX errors render as 'merror'
         // nodes, while malformed inputs (e.g. unbalanced braces) are
         // skipped by the math finder entirely, leaving raw text behind
         var container = scratch.querySelector("mjx-container");
         var merror = scratch.querySelector("mjx-merror, [data-mjx-error]");
         var error = container == null || merror != null;

         // swap the new output into place on success; on failure, keep any
         // previous (successful) render, but surface the error output when
         // there is no previous render to preserve
         if (!error || el.querySelector("mjx-container") == null)
         {
            el.innerHTML = "";
            while (scratch.firstChild)
               el.appendChild(scratch.firstChild);
         }

         cleanup();
         onCompleted(error);

      }, function(err) {
         cleanup();
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
