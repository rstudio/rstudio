/*
 * ConsoleOutputWriter.java
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
package org.rstudio.core.client;

import java.util.List;

import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.widget.PreWidget;
import org.rstudio.studio.client.workbench.views.console.ConsoleResources;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.SpanElement;

import elemental2.dom.DomGlobal;
import elemental2.dom.EventListener;
import elemental2.dom.FocusEvent;
import elemental2.dom.HTMLElement;
import elemental2.dom.KeyboardEvent;
import jsinterop.base.Js;

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
      output_.getElement().addClassName(OUTPUT_CLASS);
      if (!StringUtil.isNullOrEmpty(a11yLabel))
      {
         output_.getElement().setAttribute("aria-label", a11yLabel);
         Roles.getDocumentRole().set(output_.getElement());
      }
      trackPointerFocus();
   }

   /**
    * Mark the output while the mouse put focus in it, so the outline on the
    * output itself stays hidden after a key press such as Shift or Cmd+C.
    * The mark outlasts the window losing focus, but not focus landing outside
    * the output, even while the window is inactive.
    */
   private void trackPointerFocus()
   {
      HTMLElement el = Js.cast(output_.getElement());
      EventListener clearOnFocusElsewhere = event ->
      {
         elemental2.dom.Node target = Js.uncheckedCast(event.target);
         if (target == null || !el.contains(target))
            el.classList.remove(POINTER_FOCUS_CLASS);
      };
      output_.addAttachHandler(event ->
      {
         if (event.isAttached())
            DomGlobal.document.addEventListener("focusin", clearOnFocusElsewhere, true);
         else
            DomGlobal.document.removeEventListener("focusin", clearOnFocusElsewhere, true);
      });
      el.addEventListener("mousedown", event ->
      {
         el.classList.add(POINTER_FOCUS_CLASS);
         el.classList.remove(KEYBOARD_FOCUS_CLASS);
      });
      el.addEventListener("keydown", event ->
      {
         if (StringUtil.equals(((KeyboardEvent) event).key, "Tab"))
            el.classList.remove(POINTER_FOCUS_CLASS);
      });
      el.addEventListener("focusout", event ->
      {
         if (!DomGlobal.document.hasFocus())
            return;
         elemental2.dom.Node next = Js.uncheckedCast(((FocusEvent) event).relatedTarget);
         if (next == null || !el.contains(next))
            el.classList.remove(POINTER_FOCUS_CLASS);
      });
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

      output_.setText("");
      virtualConsole_ = null;
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

      // The virtual console tracks the newlines it writes, so the lines this
      // submit adds can be read off it; counting them in the DOM meant walking
      // the whole scrollback twice for every chunk of output.
      int oldLineCount = virtualConsole_.getNewlinesWritten();
      if (StringUtil.equals(className, OUTPUT_ERROR_CLASS))
         virtualConsole_.submit(text, VirtualConsole.Type.STDERR, isError, ariaLiveAnnounce);
      else
         virtualConsole_.submit(text, className, isError, ariaLiveAnnounce);
      int newLineCount = virtualConsole_.getNewlinesWritten();

      lines_ += newLineCount - oldLineCount;

      return ignoreLineCount || !trimExcess();
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

   public void normalizePreviousOutput()
   {
      if (virtualConsole_ != null)
      {
         // trimming a group's surrounding newlines changes the line count
         int before = virtualConsole_.getNewlinesWritten();
         virtualConsole_.normalizePreviousOutput();
         lines_ += virtualConsole_.getNewlinesWritten() - before;
      }
   }
   
   public void ensureStartingOnNewLine()
   {
      if (virtualConsole_ != null)
      {
         // no more output will reach this console, so show anything it was
         // still waiting on
         int before = virtualConsole_.getNewlinesWritten();
         virtualConsole_.flushPartialAnsiCode();
         virtualConsole_.ensureStartingOnNewLine();
         lines_ += virtualConsole_.getNewlinesWritten() - before;

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

   /**
    * Focus the most recent output.
    *
    * @return false if there's no output to focus, e.g. just after a clear
    */
   public boolean focusEnd()
   {
      // Skip empty output: a form feed (e.g. cat("\f")) clears the output but
      // leaves its now-childless output chunk behind.
      Node last = output_.getElement().getLastChild();
      while (last != null && !last.hasChildNodes())
         last = last.getPreviousSibling();
      if (last == null)
         return false;

      // Chunks aren't in the tab order, so this is the only keyboard route onto
      // one. The mark stays when focus leaves, so a dialog or menu that hands
      // focus back to the chunk brings the outline back with it.
      output_.getElement().removeClassName(POINTER_FOCUS_CLASS);
      output_.getElement().addClassName(KEYBOARD_FOCUS_CLASS);
      Element.as(last).focus();
      return true;
   }
   
   public static final String OUTPUT_ERROR_CLASS = "__error";

   // Not obfuscated: focus-visible.css, loaded as plain CSS, refers to them.
   private static final String OUTPUT_CLASS = "rstudio-console-output";
   private static final String POINTER_FOCUS_CLASS = "rstudio-pointer-focus";
   // package-private for tests
   static final String KEYBOARD_FOCUS_CLASS = "rstudio-keyboard-focus";

   private int maxLines_ = -1;
   private int lines_ = 0;
   private final PreWidget output_;
   private VirtualConsole virtualConsole_;
   private final VirtualConsoleFactory vcFactory_;
}
