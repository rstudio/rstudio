/*
 * FormCheckBox.java
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.core.client.widget;


import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Node;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.user.client.ui.CheckBox;

public class FormCheckBox extends CheckBox
                          implements CanSetControlId
{
   public FormCheckBox()
   {
      super();
   }

   /**
    * @param label label text
    * @param id unique element ID
    */
   public FormCheckBox(String label, String id)
   {
      super(label);
      setElementId(id);
   }

   @Override
   public void setElementId(String id)
   {
      Element wrapper = getElement();
      NodeList<Node> children = wrapper.getChildNodes();
      for (int i = 0; i < children.getLength(); i++)
      {
         Node node = children.getItem(i);
         if (node.getNodeType() == Node.ELEMENT_NODE)
         {
            if (node.getNodeName().equalsIgnoreCase("input"))
               ((Element)node).setId(id);
            else if (node.getNodeName().equalsIgnoreCase("label"))
               ((Element)node).setAttribute("for", id);
         }
      }
   }
}
