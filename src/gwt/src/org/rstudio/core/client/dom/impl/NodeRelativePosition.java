/*
 * NodeRelativePosition.java
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

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.dom.client.Text;
import org.rstudio.core.client.Debug;

public class NodeRelativePosition
{
   /**
    * Converts a node-relative position to an absolute character offset,
    * or -1 if the node-based position is not present in the editor.
    */
   public static int toOffset(Element container, NodeRelativePosition position)
   {
      int[] counter = new int[] {0};
      if (toOffsetHelper(container, position, counter))
         return counter[0];
      return -1;
   }

   private static boolean toOffsetHelper(Node here,
                                         NodeRelativePosition target,
                                         int[] counter)
   {
      if (target.node.equals(here))
      {
         switch (here.getNodeType())
         {
            case Node.TEXT_NODE:
               counter[0] += target.offset;
               return true;
            case Node.ELEMENT_NODE:
               NodeList<Node> children = here.getChildNodes();
               for (int i = 0; i < target.offset; i++)
                  toOffsetHelper(children.getItem(i), target, counter);
               return true;
            default:
               Debug.log("Unexpected node type for selection offset: "
                         + here.getNodeType());
               return false;
         }
      }
      else
      {
         if (here.getNodeType() == Node.TEXT_NODE)
         {
            counter[0] += ((Text)here).getLength();
            return false;
         }
         else if (here.getNodeType() == Node.ELEMENT_NODE)
         {
            Element el = (Element) here;
            String tag = el.getTagName().toLowerCase();
            if (tag == "br")
            {
               counter[0] += 1;
               return false;
            }
            if (tag == "script" || tag == "style")
               return false;

            // Otherwise continue to iteration code below
         }

         NodeList<Node> children = here.getChildNodes();
         for (int i = 0; i < children.getLength(); i++)
            if (toOffsetHelper(children.getItem(i), target, counter))
               return true;

         return false;
      }
   }

   /**
    * Convert an absolute character-based offset to a DOM node and offset.
    * Returns null if the offset points off the end of the DOM.
    */
   public static NodeRelativePosition toPosition(Element container, int offset)
   {
      if (offset < 0)
         throw new IllegalArgumentException("Offset must not be negative");

      int[] counter = new int[] {offset};
      NodeRelativePosition result = toPositionHelper(container, counter);
      if (result == null && counter[0] == 0)
         return new NodeRelativePosition(container, container.getChildCount());
      return result;
   }

   private static NodeRelativePosition toPositionHelper(Node here,
                                                        int[] counter)
   {
      switch (here.getNodeType())
      {
         case Node.TEXT_NODE:
            Text text = (Text) here;
            if (counter[0] <= text.getLength())
               return new NodeRelativePosition(here, counter[0]);
            counter[0] -= text.getLength();
            return null;
         case Node.ELEMENT_NODE:
            Element el = (Element) here;
            String tagName = el.getTagName().toLowerCase();
            if (tagName == "br")
            {
               if (counter[0] <= 0)
                  return new NodeRelativePosition(here, 0);
               counter[0] -= 1;
               return null;
            }
            else if (tagName == "script" || tagName == "style")
               return null;
            break;
      }

      NodeList<Node> children = here.getChildNodes();
      for (int i = 0; i < children.getLength(); i++)
      {
         NodeRelativePosition result = toPositionHelper(
               children.getItem(i), counter);
         if (result != null)
            return result;
      }

      return null;
   }

   public final Node node;

   public final int offset;

   public NodeRelativePosition(Node node, int offset)
   {
      super();
      this.node = node;
      this.offset = offset;
   }

}
