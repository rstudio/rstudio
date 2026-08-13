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
      // detached in the interim -- typesetting a detached element would
      // compute zero font metrics and render at zero size
      if (!el.isConnected)
      {
         onCompleted(true);
         return;
      }

      // typeset into a hidden scratch child, so that in-progress (or
      // failed) renders are never visible -- while typing, incomplete
      // expressions would otherwise flash a rendered TeX error before the
      // previous render could be restored. the scratch element lives inside
      // the target (same inherited font metrics) rather than beside it: in
      // the visual editor the target is a ProseMirror widget, and ProseMirror
      // ignores DOM mutations within widgets but reverts unexpected siblings
      // (removing the scratch mid-typeset, and re-parsing its raw TeX into
      // the document) -- see https://github.com/rstudio/rstudio/issues/18551
      var hadRender = el.querySelector("mjx-container") != null;
      var scratch = el.ownerDocument.createElement(el.tagName);
      scratch.className = el.className;
      scratch.style.position = "absolute";
      scratch.style.visibility = "hidden";
      if (el.offsetWidth > 0)
         scratch.style.width = el.offsetWidth + "px";
      scratch.innerText = currentText;
      el.appendChild(scratch);

      var cleanup = function() {
         try
         {
            MathJax.typesetClear([scratch]);
            if (scratch.parentNode != null)
               scratch.parentNode.removeChild(scratch);
         }
         catch (e)
         {
            $wnd.console.warn(e);
         }
      };

      MathJax.typesetPromise([scratch]).then($entry(function() {

         var error = true;
         try
         {
            // failed typesets surface in two ways, neither of which rejects
            // the typeset promise: recoverable TeX errors render as 'merror'
            // nodes, while malformed inputs (e.g. unbalanced braces) are
            // skipped by the math finder entirely, leaving raw text behind
            var container = scratch.querySelector("mjx-container");
            var merror = scratch.querySelector("mjx-merror, [data-mjx-error]");
            error = container == null || merror != null;

            // swap the new output into place on success; on failure, keep any
            // previous (successful) render, but surface the error output when
            // there is no previous render to preserve
            if (!error || !hadRender)
            {
               while (el.firstChild != null)
               {
                  if (el.firstChild == scratch)
                     break;
                  el.removeChild(el.firstChild);
               }

               while (scratch.firstChild != null)
                  el.insertBefore(scratch.firstChild, scratch);
            }
         }
         finally
         {
            // the queue must always advance, even if the swap fails
            // unexpectedly -- a missed completion wedges all future typesets
            cleanup();
            onCompleted(error);
         }

      }), $entry(function(err) {
         cleanup();
         onCompleted(true);
      }));
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
