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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.studio.client.workbench.views.plots.model.Manipulator;

import com.google.gwt.core.client.JsonUtils;
import com.google.gwt.dom.client.Document;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.event.dom.client.KeyCodes;
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

   public void testSliderExposesRoleNameAndRange()
   {
      Element slider = sliderElement(attach(newSlider(25)));

      Element caption = Document.get().getElementById(
            slider.getAttribute("aria-labelledby"));
      assertNotNull("aria-labelledby must point at an element", caption);
      assertEquals("X max:", caption.getInnerText());

      assertEquals("5", slider.getAttribute("aria-valuemin"));
      assertEquals("35", slider.getAttribute("aria-valuemax"));
      assertEquals("25", slider.getAttribute("aria-valuenow"));
      assertEquals("25", slider.getAttribute("aria-valuetext"));
   }

   public void testSliderArrowKeysUpdateValue()
   {
      Element slider = sliderElement(attach(newSlider(25)));

      pressKey(slider, KeyCodes.KEY_UP);
      assertEquals("26", slider.getAttribute("aria-valuenow"));
      assertEquals("26", slider.getAttribute("aria-valuetext"));

      pressKey(slider, KeyCodes.KEY_DOWN);
      pressKey(slider, KeyCodes.KEY_LEFT);
      assertEquals("24", slider.getAttribute("aria-valuenow"));

      pressKey(slider, KeyCodes.KEY_RIGHT);
      assertEquals("25", slider.getAttribute("aria-valuenow"));
   }

   public void testSliderHomeAndEndUpdateManipulatorValue()
   {
      // the plot is redrawn from the values the changed handler reports, so
      // keys that jump the value must report it like arrow keys do
      List<String> reported = new ArrayList<>();
      Element slider = sliderElement(attach(newSlider(
            25, values -> reported.add(values.get("x.max").toString()))));

      pressKey(slider, KeyCodes.KEY_END);
      pressKey(slider, KeyCodes.KEY_HOME);
      assertEquals(Arrays.asList("35", "5"), reported);
   }

   public void testSliderHeldEndKeyReportsOnce()
   {
      // each report re-runs the user's plot code, so auto-repeated keydowns
      // must not report until the key is released
      List<String> reported = new ArrayList<>();
      Element slider = sliderElement(attach(newSlider(
            25, values -> reported.add(values.get("x.max").toString()))));

      Document doc = Document.get();
      for (int i = 0; i < 3; i++)
         slider.dispatchEvent(doc.createKeyDownEvent(
               false, false, false, false, KeyCodes.KEY_END));
      assertTrue(reported.isEmpty());

      slider.dispatchEvent(doc.createKeyUpEvent(
            false, false, false, false, KeyCodes.KEY_END));
      assertEquals(Arrays.asList("35"), reported);
   }

   public void testSliderValueTextMatchesDisplayedValue()
   {
      // A short range gets a fractional step, so aria-valuenow carries raw
      // precision while the visible value is rounded; valuetext must match
      // what sighted users see
      Manipulator.Slider slider = JsonUtils.<Manipulator.Slider>safeEval(
            "{\"type\":[0],\"label\":[\"Alpha\"],\"min\":[0],\"max\":[1],"
            + "\"step\":[-1],\"ticks\":[false],\"initialValue\":[0.333]}");
      Widget control = attach(new ManipulatorControlSlider(
            "alpha", 0.333, slider, values -> {}));
      Element sliderEl = sliderElement(control);

      Element valueLabel = DomUtils.querySelector(
            control.getElement(),
            "." + ManipulatorResources.INSTANCE.manipulatorStyles().sliderValueLabel());
      assertFalse(valueLabel.getInnerText().isEmpty());
      assertEquals(valueLabel.getInnerText(),
                   sliderEl.getAttribute("aria-valuetext"));
   }

   public void testSliderKnobImageIsDecorative()
   {
      Element slider = sliderElement(attach(newSlider(25)));
      assertKnobIsDecorative(slider);

      // releasing a key restores the knob's resting image; the alt must survive
      pressKey(slider, KeyCodes.KEY_RIGHT);
      assertKnobIsDecorative(slider);
   }

   private static void assertKnobIsDecorative(Element slider)
   {
      NodeList<Element> images = DomUtils.querySelectorAll(slider, "img");
      assertTrue("slider has no knob image", images.getLength() > 0);
      for (int i = 0; i < images.getLength(); i++)
      {
         Element img = images.getItem(i);
         assertTrue("knob image needs an alt attribute", img.hasAttribute("alt"));
         assertEquals("", img.getAttribute("alt"));
      }
   }

   private static ManipulatorControlSlider newSlider(double value)
   {
      return newSlider(value, values -> {});
   }

   private static ManipulatorControlSlider newSlider(
         double value, ManipulatorChangedHandler handler)
   {
      Manipulator.Slider slider = JsonUtils.<Manipulator.Slider>safeEval(
            "{\"type\":[0],\"label\":[\"X max\"],\"min\":[5],\"max\":[35],"
            + "\"step\":[-1],\"ticks\":[false],\"initialValue\":[25]}");
      return new ManipulatorControlSlider("x.max", value, slider, handler);
   }

   private static Widget attach(Widget widget)
   {
      RootPanel.get().add(widget);
      return widget;
   }

   private static Element sliderElement(Widget control)
   {
      Element slider = DomUtils.querySelector(control.getElement(), "[role=slider]");
      assertNotNull("no element with role=slider", slider);
      return slider;
   }

   private static String labelTextFor(Widget control, Element target)
   {
      assertFalse("control has no id", target.getId().isEmpty());
      Element label = DomUtils.querySelector(
            control.getElement(), "label[for=\"" + target.getId() + "\"]");
      assertNotNull("no <label> for the control", label);
      return label.getInnerText();
   }

   private static void pressKey(Element target, int keyCode)
   {
      Document doc = Document.get();
      target.dispatchEvent(doc.createKeyDownEvent(false, false, false, false, keyCode));
      target.dispatchEvent(doc.createKeyUpEvent(false, false, false, false, keyCode));
   }
}
