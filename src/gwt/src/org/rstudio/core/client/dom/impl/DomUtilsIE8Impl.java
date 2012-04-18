/*
 * DomUtilsIE8Impl.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client.dom.impl;

import com.google.gwt.core.client.JsArrayInteger;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import org.rstudio.core.client.Rectangle;
import org.rstudio.core.client.dom.ElementEx;

public class DomUtilsIE8Impl implements DomUtilsImpl
{
   public void focus(Element element, boolean alwaysDriveSelection)
   {
      ElementEx el = (ElementEx) element;
      el.focus();
   }

   public native final void collapseSelection(boolean toStart) /*-{
      var rng = document.selection.createRange();
      rng.collapse(toStart);
   }-*/;

   public native final boolean isSelectionCollapsed() /*-{
      var rng = document.selection.createRange();
      var testRng = rng.duplicate();
      testRng.collapse(true);
      return rng.isEqual(testRng);
   }-*/;

   public native final boolean isSelectionInElement(Element element) /*-{
      var rng = element.ownerDocument.selection.createRange();
      var el = rng.parentElement();

      while (el != null)
      {
         if (el === element)
            return true ;

         el = el.parentNode ;
      }
      return false ;
   }-*/;

   private native String getSelectionType() /*-{
      return document.selection.type;
   }-*/;

   public boolean selectionExists()
   {
      return !("None".equals(getSelectionType())) && !isSelectionCollapsed();
   }

   public Rectangle getCursorBounds(Document doc)
   {
      JsArrayInteger result = getSelectionBoundsInternal(doc);
      return new Rectangle(
            result.get(0),
            result.get(1),
            result.get(2),
            result.get(3)
      );
   }

   private native JsArrayInteger getSelectionBoundsInternal(Document doc) /*-{
      var rng = doc.selection.createRange();
      return [
         rng.boundingLeft,
         rng.boundingTop,
         rng.boundingWidth,
         rng.boundingHeight];
   }-*/;

   private native final Element getSelectionParentElement(Document doc) /*-{
      return doc.selection.createRange().parentElement();
   }-*/;

   public native final String replaceSelection(Document doc,
                                               String text) /*-{
      var rng = doc.selection.createRange();
      var orig = rng.text;
      var html = @org.rstudio.core.client.dom.DomUtils::textToHtml(Ljava/lang/String;)(text);
      rng.pasteHTML(html);
      rng.select();
      return orig;
   }-*/;

   public native final String getSelectionText(Document document) /*-{
      var rng = doc.selection.createRange();
      return rng.text;
   }-*/;

   public int[] getSelectionOffsets(Element container)
   {
      if (!isSelectionInElement(container))
         return null;

      JsArrayInteger results = getSelectionOffsetsJs(container);
      if (results == null)
         return null;
      else
         return new int[] {results.get(0), results.get(1)};
   }

   private native final JsArrayInteger getSelectionOffsetsJs(
         Element container) /*-{
      var rng = container.ownerDocument.body.createTextRange();
      rng.moveToElementText(container);
      rng.collapse(true);
      var sel = container.ownerDocument.selection.createRange();

      var startOffset = 0;
      var endOffset = 0;
      // Ideally this would be a binary search instead of linear probing
      while (rng.compareEndPoints("EndToEnd", sel) < 0) {
         rng.moveEnd("character");
         startOffset++;
      }
      while (rng.compareEndPoints("StartToStart", sel) < 0) {
         rng.moveStart("character");
         endOffset++;
      }

      if (!rng.isEqual(sel))
         return null;
      else
         return [startOffset, endOffset];
   }-*/;

   public native final void setSelectionOffsets(Element container,
                                                int start,
                                                int end) /*-{
      var rng = container.ownerDocument.body.createTextRange();
      rng.moveToElementText(container);
      for (var i = 0; i < end; i++)
         rng.moveEnd('character');
      for (var j = 0; j < start; j++)
         rng.moveStart('character');

      var containerRng = container.ownerDocument.body.createTextRange();
      containerRng.moveToElementText(container);
      if (rng.compareEndPoints('EndToEnd', containerRng) > 0) {
         rng.setEndPoint('EndToEnd', containerRng);
      }

      rng.select();
      container.focus();
   }-*/;

   public boolean isSelectionAsynchronous()
   {
      return true;
   }
}
