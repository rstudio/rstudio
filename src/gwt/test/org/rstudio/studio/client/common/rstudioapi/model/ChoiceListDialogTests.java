/*
 * ChoiceListDialogTests.java
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
package org.rstudio.studio.client.common.rstudioapi.model;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.LabelElement;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.junit.client.GWTTestCase;

public class ChoiceListDialogTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   public void testListIsLabelledByMessage()
   {
      Element content = createContent("Accounts", "Choose an account:");
      Element select = onlySelect(content);

      assertFalse("the list needs an id for <label for>", select.getId().isEmpty());
      assertEquals("Choose an account:", labelTextFor(content, select.getId()));
      assertEquals("the <label> already names the list", "",
            select.getAttribute("aria-label"));
   }

   public void testListIsNamedByTitleWithoutMessage()
   {
      Element select = onlySelect(createContent("Accounts", ""));
      assertEquals("Accounts", select.getAttribute("aria-label"));
   }

   public void testListIsNamedByDefaultTitleWithoutTitleOrMessage()
   {
      Element select = onlySelect(createContent(null, null));
      assertEquals("Select", select.getAttribute("aria-label"));
   }

   private static Element createContent(String title, String message)
   {
      ChoiceListDialog dialog = new ChoiceListDialog(
            title, message, new String[] { "alpha", "beta" }, null, null);
      return dialog.createMainWidget().getElement();
   }

   private static Element onlySelect(Element content)
   {
      NodeList<Element> selects = content.getElementsByTagName("select");
      assertEquals("the dialog should hold one list", 1, selects.getLength());
      return selects.getItem(0);
   }

   private static String labelTextFor(Element container, String id)
   {
      NodeList<Element> labels = container.getElementsByTagName("label");
      for (int i = 0; i < labels.getLength(); i++)
      {
         LabelElement label = labels.getItem(i).cast();
         if (id.equals(label.getHtmlFor()))
            return label.getInnerText();
      }
      return null;
   }
}
