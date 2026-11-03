/*
 * ProjectTemplateWidgetTests.java
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
package org.rstudio.studio.client.projects.model;

import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.LabelElement;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.junit.client.GWTTestCase;

public class ProjectTemplateWidgetTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   public void testTextInputIsLabelled()
   {
      ProjectTemplateWidget widget = newWidget(
            "[{\"parameter\": \"name\", \"type\": \"textinput\", \"label\": \"Author name\"}]");
      assertEquals("Author name:", labelTextFor(widget, textInput(widget, 0)));
   }

   // the two-column layout places the inputs in separate panels; each must
   // still be named by its own label
   public void testTextInputsInBothColumnsAreLabelled()
   {
      ProjectTemplateWidget widget = newWidget(
            "[{\"parameter\": \"name\", \"type\": \"textinput\", \"label\": \"Author name\"}," +
            " {\"parameter\": \"tag\", \"type\": \"textinput\", \"label\": \"Tag:\"," +
            "  \"position\": \"right\"}]");
      assertEquals("Author name:", labelTextFor(widget, textInput(widget, 0)));
      assertEquals("Tag:", labelTextFor(widget, textInput(widget, 1)));
   }

   // Label is optional in a template's DCF; adding a colon to an empty label
   // would name the input ":"
   public void testTextInputWithEmptyLabelIsNotNamedColon()
   {
      ProjectTemplateWidget widget = newWidget(
            "[{\"parameter\": \"name\", \"type\": \"textinput\", \"label\": \"\"}]");
      assertEquals("", labelTextFor(widget, textInput(widget, 0)));
   }

   private static ProjectTemplateWidget newWidget(String widgetsJson)
   {
      ProjectTemplateDescription description = JsonUtils.safeEval(
            "{\"package\": \"pkg\", \"binding\": \"fn\", \"title\": \"Test\"," +
            " \"widgets\": " + widgetsJson + "}");
      return new ProjectTemplateWidget(description);
   }

   private static Element textInput(ProjectTemplateWidget widget, int index)
   {
      NodeList<Element> inputs = widget.getElement().getElementsByTagName("input");
      int seen = 0;
      for (int i = 0; i < inputs.getLength(); i++)
      {
         InputElement input = inputs.getItem(i).cast();
         if (input.getType().equals("text") && seen++ == index)
            return input;
      }
      fail("expected text input " + index);
      return null;
   }

   // a <label for> only names the control if the id matches, so look the
   // label up by the control's id rather than trusting widget wiring
   private static String labelTextFor(ProjectTemplateWidget container, Element control)
   {
      String id = control.getId();
      assertFalse("the control needs an id for <label for>", id.isEmpty());

      NodeList<Element> labels = container.getElement().getElementsByTagName("label");
      for (int i = 0; i < labels.getLength(); i++)
      {
         LabelElement label = labels.getItem(i).cast();
         if (id.equals(label.getHtmlFor()))
            return label.getInnerText();
      }
      return null;
   }
}
