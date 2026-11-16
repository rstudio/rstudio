/*
 * LabelTestHelper.java
 *
 * Copyright (C) 2026 by Posit Software, PBC
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

import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.fail;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.LabelElement;
import com.google.gwt.dom.client.NodeList;

public class LabelTestHelper
{
   /**
    * Returns the text of the label in container whose for attribute matches
    * control's id, failing if control has no id or no such label exists. A
    * label only names a control when the ids match, so tests look it up this
    * way rather than trusting the widget wiring.
    */
   public static String labelTextFor(Element container, Element control)
   {
      String id = control.getId();
      assertFalse("the control needs an id for <label for>", id.isEmpty());

      NodeList<Element> labels = container.getElementsByTagName("label");
      for (int i = 0; i < labels.getLength(); i++)
      {
         LabelElement label = labels.getItem(i).cast();
         if (id.equals(label.getHtmlFor()))
            return label.getInnerText();
      }
      fail("no <label for=\"" + id + "\"> in the container");
      return null;
   }
}
