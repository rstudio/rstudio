/*
 * ConsoleOutputWriter.java
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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

import java.util.List;

import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.widget.PreWidget;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.SpanElement;

/**
 * Displays R Console output to user, with special behaviors for regular output
 * vs. error output.
 */
public class ConsoleOutputWriter
{
   public ConsoleOutputWriter()
   {
      output_ = new PreWidget();
   }
   
   public PreWidget getWidget()
   {
      return output_;
   }
   
   public Element getElement()
   {
      return output_.getElement();
   }

   public void clearConsoleOutput()
   {
      output_.setText("");
      virtualConsole_ = null;
      lines_ = 0;
   }

   public int getMaxOutputLines()
   {
      return maxLines_;
   }
   
   public void setMaxOutputLines(int maxLines)
   {
      maxLines_ = maxLines;
      trimExcess();
   }
   
   /**
    * Send text to the console
    * @param text Text to output
    * @param className Text style
    * @param isError Is this an error message?
    * @param ignoreLineCount Output without checking buffer length?
    * @return was this output below the maximum buffer line count?
    */
   public boolean outputToConsole(String text,
                                  String className,
                                  boolean isError,
                                  boolean ignoreLineCount)
   {
      if (text.indexOf('\f') >= 0)
         clearConsoleOutput();

      Element outEl = output_.getElement();
      
      // create trailing output console if it doesn't already exist 
      if (virtualConsole_ == null)
      {
         SpanElement trailing = Document.get().createSpanElement();
         outEl.appendChild(trailing);
         virtualConsole_ = new VirtualConsole(trailing);
      }

      int oldLineCount = DomUtils.countLines(virtualConsole_.getParent(), true);
      virtualConsole_.submit(text, className, isError);
      int newLineCount = DomUtils.countLines(virtualConsole_.getParent(), true);
      lines_ += newLineCount - oldLineCount;

      return ignoreLineCount ? true : !trimExcess();
   }

   public boolean trimExcess()
   {
      if (maxLines_ <= 0)
         return false;  // No limit in effect

      int linesToTrim = lines_ - maxLines_;
      if (linesToTrim > 0)
      {
         lines_ -= DomUtils.trimLines(getElement(), linesToTrim);
         return true;
      }

      return false;
   }

   // Elements added by last submit call; only captured if 
   // outputToConsole/isError was true for performance reasons
   public List<Element> getNewElements()
   {
      if (virtualConsole_ == null)
         return null;
      else
         return virtualConsole_.getNewElements();
   }

   public void ensureStartingOnNewLine()
   {
      if (virtualConsole_ != null)
      {
         Node child = virtualConsole_.getParent().getLastChild();
         if (child != null &&
             child.getNodeType() == Node.ELEMENT_NODE &&
             !Element.as(child).getInnerText().endsWith("\n"))
         {
            virtualConsole_.submit("\n");
         }
         // clear the virtual console so we start with a fresh slate
         virtualConsole_ = null;
      } 
   }
   
   public int getCurrentLines()
   {
      return lines_;
   }
   
   private int maxLines_ = -1;
   private int lines_ = 0;
   private final PreWidget output_;
   private VirtualConsole virtualConsole_;
}