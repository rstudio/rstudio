/*
 * DomUtils.java
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
package org.rstudio.core.client.dom;

import java.util.ArrayList;
import java.util.List;

import org.rstudio.core.client.BrowseCap;
import org.rstudio.core.client.Debug;
import org.rstudio.core.client.Point;
import org.rstudio.core.client.Rectangle;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.command.KeyboardShortcut;
import org.rstudio.core.client.dom.impl.DomUtilsImpl;
import org.rstudio.core.client.dom.impl.NodeRelativePosition;
import org.rstudio.core.client.regex.Match;
import org.rstudio.core.client.regex.Pattern;
import org.rstudio.core.client.widget.FontSizer;
import org.rstudio.studio.client.application.Desktop;

import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.core.client.JsArrayMixed;
import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.dom.client.BRElement;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.EventTarget;
import com.google.gwt.dom.client.HeadElement;
import com.google.gwt.dom.client.IFrameElement;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.PreElement;
import com.google.gwt.dom.client.ScriptElement;
import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Position;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.dom.client.Text;
import com.google.gwt.event.dom.client.HasAllKeyHandlers;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.event.dom.client.KeyDownEvent;
import com.google.gwt.event.dom.client.KeyDownHandler;
import com.google.gwt.event.dom.client.KeyPressEvent;
import com.google.gwt.event.dom.client.KeyPressHandler;
import com.google.gwt.event.dom.client.KeyUpEvent;
import com.google.gwt.event.dom.client.KeyUpHandler;
import com.google.gwt.resources.client.TextResource;
import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.UIObject;
import com.google.gwt.user.client.ui.Widget;

/**
 * Helper methods that are mostly useful for interacting with
 * contentEditable regions.
 */
public class DomUtils
{
   public interface NodePredicate
   {
      boolean test(Node n);
   }

   public static native Element getActiveElement() /*-{
      return $doc.activeElement;
   }-*/;
   
   public static native void setFocus(Element el, boolean preventScroll)
   /*-{
      el.focus({
         preventScroll: preventScroll
      });
   }-*/;

   /**
    * In IE8, focusing the history table (which is larger than the scroll
    * panel it's contained in) causes the scroll panel to jump to the top.
    * Using setActive() solves this problem. Other browsers don't support
    * setActive but also don't have the scrolling problem.
    * @param element
    */
   public static native void setActive(Element element) /*-{
      if (element.setActive)
         element.setActive();
      else
         element.focus();
   }-*/;

   /**
    * Trim excess lines from the beginning of the text of an element.
    *
    * @param element The element to trim lines from.
    * @param linesToTrim The number of lines to trim.
    * @return Number of lines trimmed
    */
   public static int trimLines(Element element, int linesToTrim)
   {
      return trimLines(element.getChildNodes(), linesToTrim);
   }

   public static native void scrollToBottom(Element element) /*-{
      element.scrollTop = element.scrollHeight;
   }-*/;

   public static JavaScriptObject splice(JavaScriptObject array,
                                         int index,
                                         int howMany,
                                         String... elements)
   {
      JsArrayMixed args = JavaScriptObject.createArray().cast();
      args.push(index);
      args.push(howMany);
      for (String el : elements)
         args.push(el);
      return spliceInternal(array, args);
   }

   private static native JsArrayString spliceInternal(JavaScriptObject array,
                                                      JsArrayMixed args) /*-{
      return Array.prototype.splice.apply(array, args);
   }-*/;

   public static Node findNodeUpwards(Node node,
                                      Element scope,
                                      NodePredicate predicate)
   {
      if (scope != null && !scope.isOrHasChild(node))
         throw new IllegalArgumentException("Incorrect scope passed to findParentNode");

      for (; node != null; node = node.getParentNode())
      {
         if (predicate.test(node))
            return node;
         if (scope == node)
            return null;
      }
      return null;
   }

   public static boolean isEffectivelyVisible(Element element)
   {
      while (element != null)
      {
         if (!UIObject.isVisible(element))
            return false;

         // If element never equals body, then the element is not attached
         if (element == Document.get().getBody())
            return true;

         element = element.getParentElement();
      }

      // Element is not attached
      return false;
   }

   public static void selectElement(Element el)
   {
      impl.selectElement(el);
   }

   private static final Pattern NEWLINE = Pattern.create("\\n");
   private static int trimLines(NodeList<Node> nodes, final int linesToTrim)
   {
      if (nodes == null || nodes.getLength() == 0 || linesToTrim == 0)
         return 0;

      int linesLeft = linesToTrim;

      Node node = nodes.getItem(0);

      while (node != null && linesLeft > 0)
      {
         switch (node.getNodeType())
         {
            case Node.ELEMENT_NODE:
               if (((Element)node).getTagName().equalsIgnoreCase("br"))
               {
                  linesLeft--;
                  node = removeAndGetNext(node);
                  continue;
               }
               else
               {
                  int trimmed = trimLines(node.getChildNodes(), linesLeft);
                  linesLeft -= trimmed;
                  if (!node.hasChildNodes())
                     node = removeAndGetNext(node);
                  continue;
               }
            case Node.TEXT_NODE:
               String text = ((Text)node).getData();

               Match lastMatch = null;
               Match match = NEWLINE.match(text, 0);
               while (match != null && linesLeft > 0)
               {
                  lastMatch = match;
                  linesLeft--;
                  match = match.nextMatch();
               }

               if (linesLeft > 0 || lastMatch == null)
               {
                  node = removeAndGetNext(node);
                  continue;
               }
               else
               {
                  int index = lastMatch.getIndex() + 1;
                  if (text.length() == index)
                     node.removeFromParent();
                  else
                     ((Text) node).deleteData(0, index);
                  break;
               }
         }
      }

      return linesToTrim - linesLeft;
   }

   private static Node removeAndGetNext(Node node)
   {
      Node next = node.getNextSibling();
      node.removeFromParent();
      return next;
   }

   /**
    *
    * @param node
    * @param pre Count hard returns in text nodes as newlines
    *   (only true if white-space mode is pre*)
    * @return
    */
   public static int countLines(Node node, boolean pre)
   {
      if (pre)
      {
         switch (node.getNodeType())
         {
         case Node.TEXT_NODE:
            return countLinesInternal((Text)node);
         case Node.ELEMENT_NODE:
            return countLinesInternal((Element)node);
         default:
            return 0;
         }
      }
      else
      {
         // Just count the total number of <br> elements
         switch (node.getNodeType())
         {
         
         case Node.ELEMENT_NODE:
            
            if (Element.as(node).getTagName() == BRElement.TAG)
            {
               return 1;
            }
            else
            {
               return DomUtils.querySelectorAll(Element.as(node), "br").getLength();
            }
            
         default:
            return 0;
         }
      }
   }

   private static int countLinesInternal(Node node)
   {
      switch (node.getNodeType())
      {
      
      case Node.TEXT_NODE:
      {
         Text textNode = Text.as(node);
         String value = textNode.getData();
         return StringUtil.newlineCount(value);
      }
      
      case Node.ELEMENT_NODE:
      {
         Element elementNode = Element.as(node);
         if (elementNode.getTagName().equalsIgnoreCase("br"))
            return 1;

         int result = 0;
         NodeList<Node> children = elementNode.getChildNodes();
         for (int i = 0; i < children.getLength(); i++)
            result += countLinesInternal(children.getItem(i));
         return result;
      }
      
      default:
      {
         return 0;
      }
      
      }
   }

   private final static DomUtilsImpl impl = GWT.create(DomUtilsImpl.class);

   /**
    * Drives focus to the element, and if (the element is contentEditable and
    * contains no text) or alwaysDriveSelection is true, also drives window
    * selection to the contents of the element. This is sometimes necessary
    * to get focus to move at all.
    */
   public static void focus(Element element, boolean alwaysDriveSelection)
   {
      impl.focus(element, alwaysDriveSelection);
   }

   public static boolean isFocusable(Element element)
   {
      // If it has a non-negative tab index, it can be focused
      if (element.getTabIndex() >= 0)
         return true;

      // Otherwise, determine whether an item can be focused based
      // on its tag name.
      String tagName = element.getTagName().toLowerCase();
      if (tagName == "a")
      {
         return element.hasAttribute("href");
      }
      else if (tagName == "input"    || tagName == "select" ||
               tagName == "textarea" || tagName == "button")
      {
         return !element.hasAttribute("disabled");
      }
      else
      {
         return false;
      }
   }

   public static native boolean hasFocus(Element element) /*-{
      return element === $doc.activeElement;
   }-*/;

   /**
    * Gets an ordered list of keyboard-focusable elements in the passed element.
    */
   public static ArrayList<Element> getFocusableElements(Element element)
   {
      String focusableElements =
         "button:not([hidden]):not([disabled]), [href]:not([hidden]), " +
            "input:not([hidden]):not([type=\"hidden\"]):not([disabled]), " +
            "select:not([hidden]):not([disabled]), textarea:not([hidden]):not([disabled]), " +
            "[tabindex=\"0\"]:not([hidden]):not([disabled]), summary:not([hidden]), " +
            "[contenteditable]:not([hidden]), audio[controls]:not([hidden]), " +
            "video[controls]:not([hidden])";
      NodeList<Element> potentiallyFocusable = DomUtils.querySelectorAll(element,
         focusableElements);

      ArrayList<Element> focusable = new ArrayList<>();
      for (int i = 0; i < potentiallyFocusable.getLength(); i++)
      {
         // only include items taking up space
         if (potentiallyFocusable.getItem(i).getOffsetWidth() > 0 &&
            potentiallyFocusable.getItem(i).getOffsetHeight() > 0)
            focusable.add(potentiallyFocusable.getItem(i));
      }
      return focusable;
   }

   public static void collapseSelection(boolean toStart)
   {
      impl.collapseSelection(toStart);
   }

   public static boolean isSelectionCollapsed()
   {
      return impl.isSelectionCollapsed();
   }

   public static boolean isSelectionInElement(Element element)
   {
      return impl.isSelectionInElement(element);
   }

   /**
    * Returns true if the window contains an active selection.
    */
   public static boolean selectionExists()
   {
      return impl.selectionExists();
   }

   public static final native boolean contains(Element container, Node descendant) /*-{
      return container.contains(descendant);
   }-*/;

   /**
    * CharacterData.deleteData(node, index, offset)
    */
   public static final native void deleteTextData(Text node,
                                                  int offset,
                                                  int length) /*-{
      node.deleteData(offset, length);
   }-*/;

   public static native void insertTextData(Text node,
                                            int offset,
                                            String data) /*-{
      node.insertData(offset, data);
   }-*/;

   public static Rectangle getCursorBounds()
   {
      return getCursorBounds(Document.get());
   }

   public static Rectangle getCursorBounds(Document doc)
   {
      return impl.getCursorBounds(doc);
   }

   public static String replaceSelection(Document document, String text)
   {
      return impl.replaceSelection(document, text);
   }

   public static String getSelectionText(Document document)
   {
      return impl.getSelectionText(document);
   }

   public static int[] getSelectionOffsets(Element container)
   {
      return impl.getSelectionOffsets(container);
   }

   public static void setSelectionOffsets(Element container,
                                          int start,
                                          int end)
   {
      impl.setSelectionOffsets(container, start, end);
   }

   public static Text splitTextNodeAt(Element container, int offset)
   {
      NodeRelativePosition pos = NodeRelativePosition.toPosition(container, offset);

      if (pos != null)
      {
         return ((Text)pos.node).splitText(pos.offset);
      }
      else
      {
         Text newNode = container.getOwnerDocument().createTextNode("");
         container.appendChild(newNode);
         return newNode;
      }
   }

   public static native Element getTableCell(Element table, int row, int col) /*-{
      return table.rows[row].cells[col];
   }-*/;

   public static void dump(Node node, String label)
   {
      StringBuffer buffer = new StringBuffer();
      dump(node, "", buffer, false);
      Debug.log("Dumping " + label + ":\n\n" + buffer.toString());
   }

   private static void dump(Node node,
                            String indent,
                            StringBuffer out,
                            boolean doSiblings)
   {
      if (node == null)
         return;

      out.append(indent)
         .append(node.getNodeName());
      if (node.getNodeType() != 1)
      {
         out.append(": \"")
            .append(node.getNodeValue())
            .append("\"");
      }
      out.append("\n");

      dump(node.getFirstChild(), indent + "\u00A0\u00A0", out, true);
      if (doSiblings)
         dump(node.getNextSibling(), indent, out, true);
   }

   public static native void ensureVisibleVert(
                                           Element container,
                                           Element child,
                                           int padding) /*-{
      if (!child)
         return;

      var height = child.offsetHeight;
      var top = 0;
      while (child && child != container)
      {
         top += child.offsetTop;
         child = child.offsetParent;
      }

      if (!child)
         return;

      // padding
      top -= padding;
      height += padding*2;

      if (top < container.scrollTop)
      {
         container.scrollTop = top;
      }
      else if (container.scrollTop + container.offsetHeight < top + height)
      {
         container.scrollTop = top + height - container.offsetHeight;
      }
   }-*/;

   // Forked from com.google.gwt.dom.client.Element.scrollIntoView()
   public static native void scrollIntoViewVert(Element elem) /*-{
     var top = elem.offsetTop;
     var height = elem.offsetHeight;

     if (elem.parentNode != elem.offsetParent) {
       top -= elem.parentNode.offsetTop;
     }

     var cur = elem.parentNode;
     while (cur && (cur.nodeType == 1)) {
       if (top < cur.scrollTop) {
         cur.scrollTop = top;
       }
       if (top + height > cur.scrollTop + cur.clientHeight) {
         cur.scrollTop = (top + height) - cur.clientHeight;
       }

       var offsetTop = cur.offsetTop;
       if (cur.parentNode != cur.offsetParent) {
         offsetTop -= cur.parentNode.offsetTop;
       }

       top += offsetTop - cur.scrollTop;
       cur = cur.parentNode;
     }
   }-*/;

   public static Point getRelativePosition(Element container,
                                                  Element child)
   {
      int left = 0, top = 0;
      while (child != null && child != container)
      {
         left += child.getOffsetLeft();
         top += child.getOffsetTop();
         child = child.getOffsetParent();
      }

      return Point.create(left, top);
   }

   public static int ensureVisibleHoriz(Element container,
                                         Element child,
                                         int paddingLeft,
                                         int paddingRight,
                                         boolean calculateOnly)
   {
      final int scrollLeft = container.getScrollLeft();

      if (child == null)
         return scrollLeft;

      int width = child.getOffsetWidth();
      int left = getRelativePosition(container, child).x;
      left -= paddingLeft;
      width += paddingLeft + paddingRight;

      int result;
      if (left < scrollLeft)
         result = left;
      else if (scrollLeft + container.getOffsetWidth() < left + width)
         result = left + width - container.getOffsetWidth();
      else
         result = scrollLeft;

      if (!calculateOnly && result != scrollLeft)
         container.setScrollLeft(result);

      return result;
   }

   public static native boolean isVisibleVert(Element container,
                                              Element child) /*-{
      if (!container || !child)
         return false;

      var height = child.offsetHeight;
      var top = 0;
      while (child && child != container)
      {
         top += child.offsetTop;
         child = child.offsetParent;
      }
      if (!child)
         throw new Error("Child was not in container or " +
                         "container wasn't offset parent");

      var bottom = top + height;
      var scrollTop = container.scrollTop;
      var scrollBottom = container.scrollTop + container.clientHeight;

      return (top > scrollTop && top < scrollBottom)
            || (bottom > scrollTop && bottom < scrollBottom);

   }-*/;

   public static String getHtml(Node node)
   {
      switch (node.getNodeType())
      {
      case Node.DOCUMENT_NODE:
         return ((ElementEx)node).getOuterHtml();
      case Node.ELEMENT_NODE:
         return ((ElementEx)node).getOuterHtml();
      case Node.TEXT_NODE:
         return node.getNodeValue();
      default:
         assert false :
                  "Add case statement for node type " + node.getNodeType();
         return node.getNodeValue();
      }
   }

   public static boolean isDescendant(Node el, Node ancestor)
   {
      for (Node parent = el.getParentNode();
           parent != null;
           parent = parent.getParentNode())
      {
         if (parent.equals(ancestor))
            return true;
      }
      return false;
   }

   public static boolean isDescendantOfElementWithTag(Element el, String[] tags)
   {
      for (Element parent = el.getParentElement();
           parent != null;
           parent = parent.getParentElement())
      {
         for (String tag : tags)
            if (tag.toLowerCase().equals(parent.getTagName().toLowerCase()))
               return true;
      }
      return false;
   }

   /**
    * Finds a node that matches the predicate.
    *
    * @param start The node from which to start.
    * @param recursive If true, recurses into child nodes.
    * @param siblings If true, looks at the next sibling from "start".
    * @param filter The predicate that determines a match.
    * @return The first matching node encountered in documented order, or null.
    */
   public static Node findNode(Node start,
                               boolean recursive,
                               boolean siblings,
                               NodePredicate filter)
   {
      List<Node> results = findNodes(start, 1, recursive ? 99 : 0, siblings, filter);
      if (results.isEmpty())
         return null;
      return results.get(0);
   }

   /**
    * Finds a node that matches the predicate.
    *
    * @param start The node from which to start.
    * @param depth The maximum recursive depth
    * @param siblings If true, looks at the next sibling from "start".
    * @param filter The predicate that determines a match.
    * @return The first matching node encountered in documented order, or null.
    */
   public static Node findNode(Node start,
                               int depth,
                               boolean siblings,
                               NodePredicate filter)
   {
      List<Node> results = findNodes(start, 1, depth, siblings, filter);
      if (results.isEmpty())
         return null;
      return results.get(0);
   }

   /**
    * Finds all the nodes that match the predicate.
    *
    * @param start The node from which to start.
    * @param max The maximum number of nodes to find.
    * @param depth The maximum recursive depth.
    * @param siblings If true, looks at the next sibling from "start".
    * @param filter The predicate that determines a match.
    * @return The first matching node encountered in documented order, or null.
    */
   public static List<Node> findNodes(Node start,
                                      int max,
                                      int depth,
                                      boolean siblings,
                                      NodePredicate filter)
   {
      List<Node> results = new ArrayList<>();
      int remaining = 0;

      if (start == null)
         return results;

      if (filter.test(start))
      {
         results.add(start);
         if (results.size() >= max)
            return results;
      }

      if (depth > 0)
      {
         remaining = max - results.size();
         List<Node> matched = findNodes(start.getFirstChild(), remaining,
               depth - 1, true, filter);
         results.addAll(matched);
      }

      if (siblings && results.size() < max)
      {
         remaining = max - results.size();
         List<Node> matched = findNodes(start.getNextSibling(), remaining,
               depth, true, filter);
         results.addAll(matched);
      }

      return results;
   }

   /**
    * Converts plaintext to HTML, preserving whitespace semantics
    * as much as possible.
    */
   public static String textToHtml(String text)
   {
      // Order of these replacement operations is important.
      return
         text.replaceAll("&", "&amp;")
             .replaceAll("<", "&lt;")
             .replaceAll(">", "&gt;")
             .replaceAll("\\n", "<br />")
             .replaceAll("\\t", "    ")
             .replaceAll(" ", "&nbsp;")
             .replaceAll("&nbsp;(?!&nbsp;)", " ")
             .replaceAll(" $", "&nbsp;")
             .replaceAll("^ ", "&nbsp;");
   }

   public static String textToPreHtml(String text)
   {
      // Order of these replacement operations is important.
      return
         text.replaceAll("&", "&amp;")
             .replaceAll("<", "&lt;")
             .replaceAll(">", "&gt;")
             .replaceAll("\\t", "  ");
   }

   public static String htmlToText(String html)
   {
      Element el = DOM.createSpan();
      el.setInnerHTML(html);
      return el.getInnerText();
   }

   /**
    * Similar to Element.getInnerText() but converts br tags to newlines.
    */
   public static String getInnerText(Element el)
   {
      return getInnerText(el, false);
   }

   public static String getInnerText(Element el, boolean pasteMode)
   {
      StringBuilder out = new StringBuilder();
      getInnerText(el, out, pasteMode);
      return out.toString();
   }

   private static void getInnerText(Node node,
                                    StringBuilder out,
                                    boolean pasteMode)
   {
      if (node == null)
         return;

      for (Node child = node.getFirstChild();
           child != null;
           child = child.getNextSibling())
      {
         switch (child.getNodeType())
         {
            case Node.TEXT_NODE:
               out.append(child.getNodeValue());
               break;
            case Node.ELEMENT_NODE:
               Element childEl = (Element) child;
               String tag = childEl.getTagName().toLowerCase();
               // Sometimes when pasting text (e.g. from IntelliJ) into console
               // the line breaks turn into <br _moz_dirty="true"/> or whatever.
               // We want to keep them in those cases. But in other cases
               // the _moz_dirty breaks are just spurious.
               if (tag == "br" && (pasteMode || !childEl.hasAttribute("_moz_dirty")))
                  out.append("\n");
               else if (tag == "script" || tag == "style")
                  continue;
               getInnerText(child, out, pasteMode);
               break;
         }
      }
   }

   public static void setInnerText(Element el, String plainText)
   {
      el.setInnerText("");
      if (plainText == null || plainText.length() == 0)
         return;

      Document doc = el.getOwnerDocument();

      Pattern pattern = Pattern.create("\\n");
      int tail = 0;
      Match match = pattern.match(plainText, 0);
      while (match != null)
      {
         if (tail != match.getIndex())
         {
            String line = StringUtil.substring(plainText, tail, match.getIndex());
            el.appendChild(doc.createTextNode(line));
         }
         el.appendChild(doc.createBRElement());
         tail = match.getIndex() + 1;
         match = match.nextMatch();
      }

      if (tail < plainText.length())
         el.appendChild(doc.createTextNode(StringUtil.substring(plainText, tail)));
   }

   public static boolean isSelectionAsynchronous()
   {
      return impl.isSelectionAsynchronous();
   }

   public static boolean isCommandClick(NativeEvent nativeEvt)
   {
      int modifierKeys = KeyboardShortcut.getModifierValue(nativeEvt);

      boolean isCommandPressed = BrowseCap.isMacintosh() ?
            modifierKeys == KeyboardShortcut.META :
               modifierKeys == KeyboardShortcut.CTRL;

      return (nativeEvt.getButton() == NativeEvent.BUTTON_LEFT) && isCommandPressed;
   }

   // Returns the relative vertical position of a child to its parent.
   // Presumes that the parent is one of the elements from which the child's
   // position is computed; if this is not the case, the child's position
   // relative to the body is returned.
   public static int topRelativeTo(Element parent, Element child)
   {
      int top = 0;
      Element el = child;
      while (el != null && el != parent)
      {
         top += el.getOffsetTop();
         el = el.getOffsetParent();
      }
      return top;
   }

   public static int bottomRelativeTo(Element parent, Element child)
   {
      return topRelativeTo(parent, child) + child.getOffsetHeight();
   }

   public static int leftRelativeTo(Element parent, Element child)
   {
      int left = 0;
      Element el = child;
      while (el != null && el != parent)
      {
         left += el.getOffsetLeft();
         el = el.getOffsetParent();
      }
      return left;
   }

   public static final native void setStyle(Element element,
                                            String name,
                                            String value) /*-{
      element.style[name] = value;
   }-*/;

   public static final native void fillIFrame(IFrameElement iframe, String content) /*-{
      var doc = iframe.contentWindow.document;
      doc.open();
      doc.writeln(content);
      doc.close();
   }-*/;

   /**
    * Forwards wheel events between a document and element. Originally written to pass wheel
    * events up from an iframe.
    * @param fromDoc The document that first receives the wheel event.
    * @param toElement The element the event is forwarded to.
    */
   public static final native void forwardWheelEvent(Document fromDoc, Element toElement) /*-{

       function forward(event) {
           toElement.dispatchEvent(new event.constructor(event.type, event));
       }
       // While "wheel" is the current standard, Ace will not handle the event if "mousewheel" is
       // supported by the browser. Older browsers require "DomMouseScroll".
       var wheelEvent = $wnd.document.onmousewheel !== undefined ? "mousewheel" :
           "onwheel" in toElement ? "wheel" : "DomMouseScroll";
       fromDoc.addEventListener(wheelEvent, forward);
   }-*/;

   public static native final Element getElementById(String id) /*-{
      return $doc.getElementById(id);
   }-*/;

   public static Element[] getElementsByClassName(String classes)
   {
      Element documentEl = Document.get().cast();
      return getElementsByClassName(documentEl, classes);
   }

   public static final native Element[] getElementsByClassName(Element parent, String classes) /*-{
      var result = [];
      var elements = parent.getElementsByClassName(classes);
      for (var i = 0; i < elements.length; i++) {
         result.push(elements[i]);
      }
      return result;
   }-*/;

   public static final Element getFirstElementWithClassName(Element parent, String classes)
   {
      Element[] elements = getElementsByClassName(parent, classes);
      if (elements.length == 0)
         return null;
      return elements[0];
   }

   public static final Element getParent(Element element, int times)
   {
      Element parent = element;
      for (int i = 0; i < times; i++)
      {
         if (parent == null) return null;
         parent = parent.getParentElement();
      }
      return parent;
   }

   public static final native Style getComputedStyles(Element el)
   /*-{
      return $wnd.getComputedStyle(el);
   }-*/;

   public static void toggleClass(Element element,
                                  String cssClass,
                                  boolean value)
   {
      if (value && !element.hasClassName(cssClass))
         element.addClassName(cssClass);

      if (!value && element.hasClassName(cssClass))
         element.removeClassName(cssClass);
   }

   public interface NativeEventHandler
   {
      public void onNativeEvent(NativeEvent event);
   }

   public static void addKeyHandlers(HasAllKeyHandlers widget,
                                     final NativeEventHandler handler)
   {
      widget.addKeyDownHandler(new KeyDownHandler()
      {
         @Override
         public void onKeyDown(final KeyDownEvent event)
         {
            handler.onNativeEvent(event.getNativeEvent());
         }
      });

      widget.addKeyPressHandler(new KeyPressHandler()
      {
         @Override
         public void onKeyPress(final KeyPressEvent event)
         {
            handler.onNativeEvent(event.getNativeEvent());
         }
      });

      widget.addKeyUpHandler(new KeyUpHandler()
      {
         @Override
         public void onKeyUp(final KeyUpEvent event)
         {
            handler.onNativeEvent(event.getNativeEvent());
         }
      });
   }

   public interface ElementPredicate
   {
      public boolean test(Element el);
   }

   public static Element findParentElement(Element el,
                                           ElementPredicate predicate)
   {
      return findParentElement(el, false, predicate);
   }

   public static Element findParentElement(Element el,
                                           boolean includeSelf,
                                           ElementPredicate predicate)
   {
      Element parent = includeSelf ? el : el.getParentElement();
      while (parent != null)
      {
         if (predicate.test(parent))
            return parent;

         parent = parent.getParentElement();
      }
      return null;
   }

   public final static native Element elementFromPoint(int x, int y) /*-{
      return $doc.elementFromPoint(x, y);
   }-*/;

   public static final native void setSelectionRange(Element el, int start, int end)
   /*-{
      if (el.setSelectionRange)
         el.setSelectionRange(start, end);
   }-*/;

   public static final String extractCssValue(String className, String propertyName)
   {
      JsArrayString classes = JsArrayString.createArray().cast();
      classes.push(className);
      return extractCssValue(classes, propertyName);
   }

   public static final boolean preventBackspaceCausingBrowserBack(NativeEvent event)
   {
      if (Desktop.hasDesktopFrame())
         return false;

      if (event.getKeyCode() != KeyCodes.KEY_BACKSPACE)
         return false;

      EventTarget target = event.getEventTarget();
      if (target == null)
         return false;

      Element elementTarget = Element.as(target);
      if (elementTarget.getNodeName() != "BODY")
         return false;

      event.preventDefault();
      return true;
   }

   public static final native String extractCssValue(JsArrayString className,
         String propertyName) /*-{
      // A more elegant way of performing this would be to iterate through the
      // document's styleSheet collection, but unfortunately browsers don't
      // expose the cssRules in all cases
      var ele = null, parent = null, root = null;
      for (var i = 0; i < className.length; i++)
      {
         ele = $doc.createElement("div");
         ele.style.display = "none";
         ele.className = className[i];
         if (parent != null)
            parent.appendChild(ele);
         parent = ele;
         if (root == null)
            root = ele;
      }
      $doc.body.appendChild(root);
      var computed = $wnd.getComputedStyle(ele);
      var result = computed[propertyName] || "";
      $doc.body.removeChild(root);
      return result;
   }-*/;

   public static int getCharacterWidth(int clientWidth,
                                       int offsetWidth,
                                       String style)
   {
      // create width checker element and add it to the document
      PreElement widthChecker = Document.get().createPreElement();
      widthChecker.getStyle().setPosition(Position.ABSOLUTE);
      widthChecker.getStyle().setLeft(-1000, Unit.PX);
      widthChecker.getStyle().setTop(-1000, Unit.PX);
      widthChecker.addClassName(style);
      FontSizer.applyNormalFontSize(widthChecker);
      Document.get().getBody().appendChild(widthChecker);

      // put the text into the label, measure it, and remove it
      String text = new String("abcdefghijklmnopqrstuvwzyz0123456789");
      widthChecker.setInnerText(text);
      int labelWidth = widthChecker.getOffsetWidth();
      widthChecker.removeFromParent();

      // compute the points per character
      double pointsPerCharacter = (double) labelWidth / (double) text.length();

      // compute client width
      if (clientWidth == offsetWidth)
      {
         // if the two widths are the same then there are no scrollbars.
         // however, we know there will eventually be a scrollbar so we
         // should offset by an estimated amount
         // (is there a more accurate way to estimate this?)
         clientWidth -= ESTIMATED_SCROLLBAR_WIDTH;
      }

      // compute character width (add pad so characters aren't flush to right)
      final int RIGHT_CHARACTER_PAD = 2;
      int computedWidth = (int) Math.floor(clientWidth / pointsPerCharacter);
      int adjustedWidth = computedWidth - RIGHT_CHARACTER_PAD;
      
      // enforce a minimum width
      final int MINIMUM_WIDTH = 30;
      return Math.max(adjustedWidth, MINIMUM_WIDTH);
   }

   public static int getCharacterWidth(Element ele, String style)
   {
      return getCharacterWidth(ele.getClientWidth(), ele.getOffsetWidth(),
            style);
   }

   public static void disableAutoBehavior(Element ele)
   {
      ele.setAttribute("autocomplete", "off");
      ele.setAttribute("autocorrect", "off");
      ele.setAttribute("autocapitalize", "off");
   }

   public static void disableAutoBehavior(Widget w)
   {
      disableAutoBehavior(w.getElement());
   }

   public static void disableSpellcheck(Element ele)
   {
      ele.setAttribute("spellcheck", "false");
   }

   public static void disableSpellcheck(Widget w)
   {
      disableSpellcheck(w.getElement());
   }

   /**
    * Set placeholder attribute on an element (assumed to be a textbox).
    * This is considered a somewhat dubious technique from an accessibility
    * standpoint, especially if the placeholder is serving as the de facto label
    * for the textbox. Avoid introducing new uses of placeholder text.
    * @param ele
    * @param placeholder
    */
   public static void setPlaceholder(Element ele, String placeholder)
   {
      ele.setAttribute("placeholder", placeholder);
   }

   /**
    * Set placeholder attribute on a TextBox widget.
    * This is considered a somewhat dubious technique from an accessibility
    * standpoint, especially if the placeholder is serving as the de facto label
    * for the textbox. Avoid introducing new uses of placeholder text.
    * @param w
    * @param placeholder
    */
   public static void setPlaceholder(TextBox w, String placeholder)
   {
      setPlaceholder(w.getElement(), placeholder);
   }

   /**
    * Set disabled attribute on an element's child
    * @param element The parent element
    * @param ordinal The index representing the child to disable
    * @param disable Whether we are adding or removing the disable attribute
    */
   public static void setOptionDisabled(Element element, int ordinal, boolean disable)
   {
      if (disable)
         ((Element) element.getChild(ordinal))
            .setAttribute("disabled", "disabled");
      else
         ((Element) element.getChild(ordinal)).removeAttribute("disabled");
   }

   /**
    * If an element doesn't already have an id, assign one
    * @param ele element to operate on
    * @return id of the element
    */
   public static String ensureHasId(Element ele)
   {
      String controlId = ele.getId();
      if (StringUtil.isNullOrEmpty(controlId))
      {
         controlId = DOM.createUniqueId();
         ele.setId(controlId);
      }
      return controlId;
   }

   /**
    * Given any URL, resolves it to an absolute URL (using the current window as
    * the base URL), and returns the result.
    *
    * @param url A relative or absolute URL.
    * @return The same URL, in absolute form.
    */
   public final static native String makeAbsoluteUrl(String url) /*-{
     var ele = document.createElement("a")
     ele.href = url;
     return ele.href;
   }-*/;

   public static final int getScrollbarWidth()
   {
      if (SCROLLBAR_WIDTH == -1)
         SCROLLBAR_WIDTH = getScrollbarWidthImpl();

      return SCROLLBAR_WIDTH;
   }

   private static final native int getScrollbarWidthImpl()
   /*-{

      // create our scroller
      var div = $doc.createElement("div");

      // style to place offscreen
      div.style.width    = "100px";
      div.style.height   = "100px";
      div.style.overflow = "scroll";
      div.style.position = "absolute";
      div.style.top      = "-10000px";

      // compute scrollbar width after attaching to DOM
      $doc.body.appendChild(div);
      var width = div.offsetWidth - div.clientWidth;
      $doc.body.removeChild(div);

      return width;

   }-*/;

   /**
    * Returns the path of a URL
    *
    * @param url A relative or absolute URL.
    * @return The same URL, in absolute form.
    */
   public final static native String getUrlPath(String url) /*-{
     var ele = document.createElement("a")
     ele.href = url;
     return ele.pathname;
   }-*/;

   public static final native NodeList<Element> querySelectorAll(Element element, String query)
   /*-{
      return element.querySelectorAll(query);
   }-*/;

   public static final void loadScript(TextResource resource)
   {
      ScriptElement scriptEl = Document.get().createScriptElement();
      scriptEl.setAttribute("type", "text/javascript");
      scriptEl.setText(resource.getText());

      HeadElement headEl = Document.get().getHead();
      headEl.appendChild(scriptEl);
   }

   /**
    * Gets the href target of a link element from the document's head.
    *
    * @param rel The link's relationship to this document.
    * @return The href attribute of the first link with the given relationship, or
    *    null if no links with the given relationship were found.
    */
   public static final String getLinkHref(String rel)
   {
      HeadElement headEl = Document.get().getHead();
      NodeList<Element> links = headEl.getElementsByTagName("link");
      for (int i = 0; i < links.getLength(); i++)
      {
         Element link = links.getItem(i);
         // Look for a matching rel; if we find it, return the href if any
         if (StringUtil.equals(link.getAttribute("rel"), rel))
         {
            String href = link.getAttribute("href");
            if (!StringUtil.isNullOrEmpty(href))
            {
              return href;
            }
         }
      }

      // Did not find a matching <link>
      return null;
   }

   public static final native DOMRect getBoundingClientRect(Element el)
   /*-{
      return el.getBoundingClientRect();
   }-*/;
   
   public static final int ESTIMATED_SCROLLBAR_WIDTH = 19;
   private static int SCROLLBAR_WIDTH = -1;
}
