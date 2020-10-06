/*
 * MathJaxRenderQueue.java
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

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import org.rstudio.studio.client.workbench.views.source.editors.text.ace.Range;

public class MathJaxRenderQueue
{
   public MathJaxRenderQueue(MathJax mathjax)
   {
      mathjax_ = mathjax;
      
      ranges_ = new LinkedList<Range>();
      callback_ = new MathJaxTypeset.Callback()
      {
         @Override
         public void onMathJaxTypesetComplete(boolean error)
         {
            renderNext();
         }
      };
      
   }
   
   public void enqueueAndRender(final List<Range> ranges)
   {
      MathJaxLoader.withMathJaxLoaded(new MathJaxLoader.Callback()
      {
         @Override
         public void onLoaded(boolean alreadyLoaded)
         {
            ranges_.addAll(ranges);
            if (isRunning_)
               return;

            renderNext();
         }
      });
   }
   
   // Private Methods ----
   
   private boolean renderNext()
   {
      Range range = ranges_.poll();
      if (range == null)
      {
         isRunning_ = false;
         return false;
      }
      
      isRunning_ = true;
      mathjax_.renderLatex(range, false, callback_);
      return true;
   }
   
   private final MathJax mathjax_;
   
   private final Queue<Range> ranges_;
   private final MathJaxTypeset.Callback callback_;
   private boolean isRunning_;

}
