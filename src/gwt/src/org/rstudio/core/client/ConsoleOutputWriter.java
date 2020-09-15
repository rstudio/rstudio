/*
 * ConsoleOutputWriter.java
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

import java.util.List;

import com.google.gwt.aria.client.Roles;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.virtualscroller.VirtualScrollerManager;
import org.rstudio.core.client.widget.PreWidget;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.SpanElement;
import org.rstudio.studio.client.workbench.views.console.ConsoleResources;

/**
 * Displays R Console output to user, with special behaviors for regular output
 * vs. error output.
 */
public class ConsoleOutputWriter
{
   public ConsoleOutputWriter(VirtualConsoleFactory vcFactory, String a11yLabel)
   {
      vcFactory_ = vcFactory;
      output_ = new PreWidget();
      if (!StringUtil.isNullOrEmpty(a11yLabel))
      {
         output_.getElement().setAttribute("aria-label", a11yLabel);
         Roles.getDocumentRole().set(output_.getElement());
      }
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
      lines_ = 0;

      if (VirtualScrollerManager.scrollerForElement(output_.getElement()) != null)
         VirtualScrollerManager.clear(output_.getElement());
      else
      {
         output_.setText("");
         virtualConsole_ = null;
      }
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
    * @param ariaLiveAnnounce Include in arialive output announcement
    * @return was this output below the maximum buffer line count?
    */
   public boolean outputToConsole(String text,
                                  String className,
                                  boolean isError,
                                  boolean ignoreLineCount,
                                  boolean ariaLiveAnnounce)
   {
      if (text.indexOf('\f') >= 0)
         clearConsoleOutput();

      Element outEl = output_.getElement();

      // create trailing output console if it doesn't already exist
      if (virtualConsole_ == null)
      {
         SpanElement trailing = Document.get().createSpanElement();
         trailing.setTabIndex(-1);
         trailing.setClassName(ConsoleResources.INSTANCE.consoleStyles().outputChunk());
         Roles.getDocumentRole().set(trailing); // https://github.com/rstudio/rstudio/issues/6884
         outEl.appendChild(trailing);
         virtualConsole_ = vcFactory_.create(trailing);
      }

      // set the appendTarget to the VirtualConsole bucket if possible
      Element appendTarget = virtualConsole_.getParent();

      // we never want to count lines based on the trailing element so grab its parent, if possible
      // otherwise just grab the outElement
      if (appendTarget.getAttribute("tabindex").equals("-1") && appendTarget.getTagName().toLowerCase().equals("span"))
      {
         if (appendTarget.getParentElement() != null)
            appendTarget = appendTarget.getParentElement();
         else {
            appendTarget = outEl;
         }
      }

      int oldLineCount = DomUtils.countLines(appendTarget, true);
      virtualConsole_.submit(text, className, isError, ariaLiveAnnounce);
      int newLineCount = DomUtils.countLines(appendTarget, true);

      if (!virtualConsole_.isLimitConsoleVisible())
         lines_ += newLineCount - oldLineCount;

      return ignoreLineCount || !trimExcess();
   }

   public boolean trimExcess()
   {
      if (maxLines_ <= 0 || virtualConsole_ != null && virtualConsole_.isLimitConsoleVisible())
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

   public String getNewText()
   {
      if (virtualConsole_ == null)
         return "";
      else
         return virtualConsole_.getNewText();
   }

   public void focusEnd()
   {
      Node lastChild = output_.getElement().getLastChild();
      if (lastChild == null)
         return;
      Element last = lastChild.cast();
      last.focus();
   }

   private int maxLines_ = -1;
   private int lines_ = 0;
   private final PreWidget output_;
   private VirtualConsole virtualConsole_;
   private final VirtualConsoleFactory vcFactory_;
}
