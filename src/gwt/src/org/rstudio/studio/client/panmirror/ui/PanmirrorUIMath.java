/*
 * PanmirrorUIMath.java
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


package org.rstudio.studio.client.panmirror.ui;

import org.rstudio.studio.client.common.mathjax.MathJaxLoader;
import org.rstudio.studio.client.common.mathjax.MathJaxTypeset;

import com.google.gwt.dom.client.Element;

import elemental2.promise.Promise;
import elemental2.promise.Promise.PromiseExecutorCallbackFn.RejectCallbackFn;
import elemental2.promise.Promise.PromiseExecutorCallbackFn.ResolveCallbackFn;
import jsinterop.annotations.JsType;

@JsType
public class PanmirrorUIMath {
   
   public Promise<Boolean> typeset(Element el, String text, boolean priority)
   {
      return new Promise<Boolean>((ResolveCallbackFn<Boolean> resolve, RejectCallbackFn reject) -> {
         MathJaxLoader.withMathJaxLoaded((alreadyLoaded) -> {
            MathJaxTypeset.typeset(el, text, priority, (error) -> {
               resolve.onInvoke(error);
            });
         });
      });
   }
}
