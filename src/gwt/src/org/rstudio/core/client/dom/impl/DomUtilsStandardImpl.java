/*
 * DomUtilsStandardImpl.java
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
package org.rstudio.core.client.dom.impl;

import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.dom.client.Text;
import org.rstudio.core.client.Rectangle;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.dom.ElementEx;
import org.rstudio.core.client.dom.NativeWindow;

public class DomUtilsStandardImpl implements DomUtilsImpl
{
   public void focus(Element element, boolean alwaysDriveSelection)
   {
      ElementEx el = (ElementEx)element;

      el.focus();
      if (alwaysDriveSelection
            || (el.getContentEditable() &&
                (el.getInnerText() == null || el.getInnerText() == "")))
      {
         Document doc = el.getOwnerDocument();
         Range range = Range.create(doc);
         range.selectNodeContents(el);
         Selection sel = Selection.get(NativeWindow.get(doc));
         sel.setRange(range);
      }

      NativeWindow.get().focus();
   }

   public void collapseSelection(boolean toStart)
   {
      Selection sel = Selection.get();
      if (sel == null || sel.getRangeCount() <= 0)
         return;
      Range range = sel.getRangeAt(0);
      range.collapse(toStart);
      sel.removeAllRanges();
      sel.addRange(range);
   }

   public boolean isSelectionCollapsed()
   {
      Selection sel = Selection.get();
      return sel != null
             && sel.getRangeCount() == 1
             && sel.getRangeAt(0).isCollapsed();
   }

   public boolean isSelectionInElement(Element element)
   {
      Range rng = getSelectionRange(
            NativeWindow.get(element.getOwnerDocument()), false);
      if (rng == null)
         return false;
      return DomUtils.contains(element, rng.getCommonAncestorContainer());
   }

   public boolean selectionExists()
   {
      Selection sel = Selection.get();
      if (sel == null || sel.getRangeCount() == 0)
         return false;
      if (sel.getRangeCount() > 1)
         return true;
      return !sel.getRangeAt(0).isCollapsed();
   }

   public Range getSelectionRange(NativeWindow window, boolean clone)
   {
      Selection sel = Selection.get(window);
      if (sel.getRangeCount() != 1)
         return null;

      Range result = sel.getRangeAt(0);
      if (clone)
         result = result.cloneRange();
      return result;
   }

   public Rectangle getCursorBounds(Document doc)
   {

      Selection sel = Selection.get(NativeWindow.get(doc));
      Range selRng = sel.getRangeAt(0);
      if (selRng == null)
         return null;
      sel.removeAllRanges();
      SpanElement span = doc.createSpanElement();

      Range rng = selRng.cloneRange();
      rng.collapse(true);
      rng.insertNode(span);

      int x = span.getAbsoluteLeft();
      int y = span.getAbsoluteTop();
      int w = 0;
      int h = span.getOffsetHeight();
      Rectangle result = new Rectangle(x, y, w, h);

      ElementEx parent = (ElementEx)span.getParentElement();
      parent.removeChild(span);
      parent.normalize();
      sel.setRange(selRng);
      return result;
   }

   public String replaceSelection(Document document, String text)
   {
      if (!isSelectionInElement(document.getBody()))
         throw new IllegalStateException("Selection is not active");

      Range rng = getSelectionRange(NativeWindow.get(document), true);
      String orig = rng.toStringJs();
      rng.deleteContents();

      Text textNode = document.createTextNode(text);
      rng.insertNode(textNode);
      rng.selectNode(textNode);

      Selection.get(NativeWindow.get(document)).setRange(rng);

      return orig;
   }

   public String getSelectionText(Document document)
   {
      Range range = getSelectionRange(NativeWindow.get(document), false);
      if (range == null || range.isCollapsed())
         return null;
      else
         return range.toStringJs();
   }

   public int[] getSelectionOffsets(Element container)
   {
      Range rng = getSelectionRange(
            NativeWindow.get(container.getOwnerDocument()),
            false);

      if (rng == null)
         return null;

      int start = NodeRelativePosition.toOffset(container,
                           new NodeRelativePosition(rng.getStartContainer(),
                                                    rng.getStartOffset()));
      int end = NodeRelativePosition.toOffset(container,
                         new NodeRelativePosition(rng.getEndContainer(),
                                                  rng.getEndOffset()));
      if (start >= 0 && end >= 0)
         return new int[] {start, end};
      else
         return null;
   }

   public void setSelectionOffsets(Element container, int start, int end)
   {
      NodeRelativePosition startp = NodeRelativePosition.toPosition(container, start);
      NodeRelativePosition endp = NodeRelativePosition.toPosition(container, end);

      Document doc = container.getOwnerDocument();
      Range rng = Range.create(doc);
      rng.setStart(startp.node, startp.offset);
      rng.setEnd(endp.node, endp.offset);
      Selection.get(NativeWindow.get(doc)).setRange(rng);

   }

   public boolean isSelectionAsynchronous()
   {
      return false;
   }

   @Override
   public void selectElement(Element el)
   {
      Document doc = el.getOwnerDocument();
      Range rng = Range.create(doc);
      rng.selectNode(el);
      Selection.get(NativeWindow.get(doc)).setRange(rng);
   }
}
