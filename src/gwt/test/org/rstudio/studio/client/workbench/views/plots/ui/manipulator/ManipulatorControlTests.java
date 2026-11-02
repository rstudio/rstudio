/*
 * ManipulatorControlTests.java
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
package org.rstudio.studio.client.workbench.views.plots.ui.manipulator;

import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.studio.client.workbench.views.plots.model.Manipulator;

import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.dom.client.Element;
import com.google.gwt.junit.client.GWTTestCase;
import com.google.gwt.user.client.ui.RootPanel;
import com.google.gwt.user.client.ui.Widget;

public class ManipulatorControlTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   @Override
   protected void gwtTearDown()
   {
      RootPanel.get().clear();
   }

   public void testPickerListIsLabelledByCaption()
   {
      Manipulator.Picker picker = JsonUtils.<Manipulator.Picker>safeEval(
            "{\"type\":[1],\"label\":[\"Plot type\"],"
            + "\"choices\":[\"Points\",\"Line\"],\"initialValue\":[\"Points\"]}");
      Widget control = attach(new ManipulatorControlPicker(
            "type", "Points", picker, values -> {}));

      Element select = DomUtils.querySelector(control.getElement(), "select");
      assertEquals("Plot type:", labelTextFor(control, select));
   }

   public void testCheckBoxInputIsLabelled()
   {
      Manipulator.CheckBox checkBox = JsonUtils.<Manipulator.CheckBox>safeEval(
            "{\"type\":[2],\"label\":[\"Show axes\"],\"initialValue\":[true]}");
      Widget control = attach(new ManipulatorControlCheckBox(
            "axes", true, checkBox, values -> {}));

      Element input = DomUtils.querySelector(control.getElement(), "input");
      assertEquals("Show axes", labelTextFor(control, input));
   }

   private static Widget attach(Widget widget)
   {
      RootPanel.get().add(widget);
      return widget;
   }

   private static String labelTextFor(Widget control, Element target)
   {
      assertFalse("control has no id", target.getId().isEmpty());
      Element label = DomUtils.querySelector(
            control.getElement(), "label[for=\"" + target.getId() + "\"]");
      assertNotNull("no <label> for the control", label);
      return label.getInnerText();
   }
}
