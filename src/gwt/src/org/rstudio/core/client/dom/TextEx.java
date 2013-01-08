/*
 * TextEx.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
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
package org.rstudio.core.client.dom;

import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.Text;

import java.util.ArrayList;

public class TextEx extends Text
{
   protected TextEx()
   {
   }
   
   public static ArrayList<TextEx> allTextNodes(Node node)
   {
      ArrayList<TextEx> results = new ArrayList<TextEx>() ;
      allTextNodesHelper(results, node) ;
      return results ;
   }
   
   private static void allTextNodesHelper(ArrayList<TextEx> list, Node node)
   {
      if (node.getNodeType() == Node.ELEMENT_NODE)
      {
         for (Node child = node.getFirstChild();
              child != null;
              child = child.getNextSibling())
         {
            allTextNodesHelper(list, child);
         }
      }
      else if (node.getNodeType() == Node.TEXT_NODE)
      {
         list.add((TextEx) node) ;
      }
   }
}
