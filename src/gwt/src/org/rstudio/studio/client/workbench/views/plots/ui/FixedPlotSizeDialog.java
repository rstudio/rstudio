/*
 * FixedPlotSizeDialog.java
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
package org.rstudio.studio.client.workbench.views.plots.ui;

import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.widget.FormLabel;
import org.rstudio.core.client.widget.LayoutGrid;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.NumericTextBox;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.common.GlobalDisplay;
import org.rstudio.studio.client.workbench.prefs.model.UserStateAccessor.FixedPlotSize;
import org.rstudio.studio.client.workbench.views.plots.PlotsConstants;
import org.rstudio.studio.client.workbench.views.plots.model.FixedPlotSizeUtils;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class FixedPlotSizeDialog extends ModalDialog<FixedPlotSize>
{
   public FixedPlotSizeDialog(FixedPlotSize size,
                              OperationWithInput<FixedPlotSize> onSelected,
                              Operation onCancelled)
   {
      super(constants_.fixedPlotSizeCaption(),
            Roles.getDialogRole(),
            onSelected,
            onCancelled);
      setThemeAware(true);

      units_ = size.getUnits();
      width_ = createValueBox(ElementIds.FIXED_PLOT_SIZE_WIDTH);
      width_.setText(FixedPlotSizeUtils.formatValue(size.getWidth(), units_));
      height_ = createValueBox(ElementIds.FIXED_PLOT_SIZE_HEIGHT);
      height_.setText(FixedPlotSizeUtils.formatValue(size.getHeight(), units_));

      unitsList_ = new ListBox();
      ElementIds.assignElementId(unitsList_, ElementIds.FIXED_PLOT_SIZE_UNITS);
      unitsList_.addItem(constants_.inchesUnitsLabel(), FixedPlotSizeUtils.UNITS_INCHES);
      unitsList_.addItem(constants_.centimetersUnitsLabel(), FixedPlotSizeUtils.UNITS_CENTIMETERS);
      unitsList_.addItem(constants_.pixelsUnitsLabel(), FixedPlotSizeUtils.UNITS_PIXELS);
      for (int i = 0; i < unitsList_.getItemCount(); i++)
      {
         if (unitsList_.getValue(i).equals(units_))
            unitsList_.setSelectedIndex(i);
      }
      unitsList_.addChangeHandler(event -> onUnitsChanged());
   }

   @Override
   protected Widget createMainWidget()
   {
      VerticalPanel panel = new VerticalPanel();

      Label description = new Label(constants_.fixedPlotSizeDescription());
      description.getElement().getStyle().setProperty("maxWidth", "300px");
      description.getElement().getStyle().setMarginBottom(10, Unit.PX);
      panel.add(description);

      LayoutGrid grid = new LayoutGrid(3, 2);
      grid.setCellSpacing(4);
      grid.setWidget(0, 0, new FormLabel(constants_.widthLabel(), width_));
      grid.setWidget(0, 1, width_);
      grid.setWidget(1, 0, new FormLabel(constants_.heightLabel(), height_));
      grid.setWidget(1, 1, height_);
      grid.setWidget(2, 0, new FormLabel(constants_.unitsLabel(), unitsList_));
      grid.setWidget(2, 1, unitsList_);
      panel.add(grid);

      return panel;
   }

   @Override
   protected FixedPlotSize collectInput()
   {
      return FixedPlotSizeUtils.create(true, readValue(width_), readValue(height_), units_);
   }

   @Override
   protected boolean validate(FixedPlotSize size)
   {
      if (isValid(size.getWidth()) && isValid(size.getHeight()))
         return true;

      GlobalDisplay globalDisplay = RStudioGinjector.INSTANCE.getGlobalDisplay();
      double pixelsPerUnit = FixedPlotSizeUtils.pixelsPerUnit(units_);
      globalDisplay.showErrorMessage(
            constants_.invalidPlotSizeCaption(),
            constants_.invalidPlotSizeMessage(
                  FixedPlotSizeUtils.formatValue(FixedPlotSizeUtils.MIN_PIXELS / pixelsPerUnit, units_),
                  FixedPlotSizeUtils.formatValue(FixedPlotSizeUtils.MAX_PIXELS / pixelsPerUnit, units_),
                  unitsName(units_)));
      return false;
   }

   // Converts the entered values when the units change, so the size is kept.
   private void onUnitsChanged()
   {
      String units = unitsList_.getSelectedValue();
      double scale = FixedPlotSizeUtils.pixelsPerUnit(units_) / FixedPlotSizeUtils.pixelsPerUnit(units);
      convertValue(width_, scale, units);
      convertValue(height_, scale, units);
      units_ = units;
   }

   private void convertValue(NumericTextBox box, double scale, String units)
   {
      double value = readValue(box);
      if (!Double.isNaN(value))
         box.setText(FixedPlotSizeUtils.formatValue(value * scale, units));
   }

   private boolean isValid(double value)
   {
      if (Double.isNaN(value))
         return false;

      int pixels = FixedPlotSizeUtils.toPixels(value, units_);
      return pixels >= FixedPlotSizeUtils.MIN_PIXELS && pixels <= FixedPlotSizeUtils.MAX_PIXELS;
   }

   private static double readValue(NumericTextBox box)
   {
      return StringUtil.parseDouble(box.getText().trim(), Double.NaN);
   }

   private static NumericTextBox createValueBox(String id)
   {
      NumericTextBox box = new NumericTextBox();
      box.getElement().setAttribute("step", "any");
      box.setWidth("80px");
      ElementIds.assignElementId(box, id);
      return box;
   }

   private static String unitsName(String units)
   {
      if (FixedPlotSizeUtils.UNITS_CENTIMETERS.equals(units))
         return constants_.centimetersLabel();
      else if (FixedPlotSizeUtils.UNITS_PIXELS.equals(units))
         return constants_.pixelsLabel();
      else
         return constants_.inchesLabel();
   }

   private final NumericTextBox width_;
   private final NumericTextBox height_;
   private final ListBox unitsList_;
   private String units_;

   private static final PlotsConstants constants_ = GWT.create(PlotsConstants.class);
}
