/*
 * AriaLabeledCheckboxCell.java
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
package org.rstudio.core.client.cellview;

import com.google.gwt.cell.client.AbstractEditableCell;
import com.google.gwt.cell.client.ValueUpdater;
import com.google.gwt.dom.client.BrowserEvents;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.InputElement;
import com.google.gwt.dom.client.NativeEvent;
import com.google.gwt.event.dom.client.KeyCodes;
import com.google.gwt.safehtml.shared.SafeHtmlBuilder;
import com.google.gwt.safehtml.shared.SafeHtmlUtils;
import org.rstudio.core.client.StringUtil;

/**
 * A Cell used to render a checkbox. The value of the checkbox may be
 * toggled using the ENTER key as well as via mouse click. The checkbox is
 * identified to screen readers using the supplied text string.
 */
public class AriaLabeledCheckboxCell extends AbstractEditableCell<LabeledBoolean, LabeledBoolean>
{
   /**
    * Construct a new {@link AriaLabeledCheckboxCell} that optionally controls selection.
    *
    * @param dependsOnSelection true if the cell depends on the selection state
    * @param handlesSelection true if the cell modifies the selection state
    */
   public AriaLabeledCheckboxCell(boolean dependsOnSelection, boolean handlesSelection)
   {
      super(BrowserEvents.CHANGE, BrowserEvents.KEYDOWN);
      this.dependsOnSelection_ = dependsOnSelection;
      this.handlesSelection_ = handlesSelection;
   }

   @Override
   public boolean dependsOnSelection()
   {
      return dependsOnSelection_;
   }

   @Override
   public boolean handlesSelection()
   {
      return handlesSelection_;
   }

   @Override
   public boolean isEditing(Context context, Element parent, LabeledBoolean value)
   {
      // A checkbox is never in "edit mode". There is no intermediate state
      // between checked and unchecked.
      return false;
   }

   @Override
   public void onBrowserEvent(Context context, Element parent, LabeledBoolean value,
                              NativeEvent event, ValueUpdater<LabeledBoolean> valueUpdater)
   {
      String type = event.getType();

      boolean enterPressed = BrowserEvents.KEYDOWN.equals(type)
            && event.getKeyCode() == KeyCodes.KEY_ENTER;
      if (BrowserEvents.CHANGE.equals(type) || enterPressed)
      {
         InputElement input = parent.getFirstChild().cast();
         Boolean isChecked = input.isChecked();

         /*
          * Toggle the value if the enter key was pressed and the cell handles
          * selection or doesn't depend on selection. If the cell depends on
          * selection but doesn't handle selection, then ignore the enter key and
          * let the SelectionEventManager determine which keys will trigger a
          * change.
          */
         if (enterPressed && (handlesSelection() || !dependsOnSelection()))
         {
            isChecked = !isChecked;
            input.setChecked(isChecked);
         }

         /*
          * Save the new value. However, if the cell depends on the selection, then
          * do not save the value because we can get into an inconsistent state.
          */
         if (value.getBool() != isChecked && !dependsOnSelection())
         {
            setViewData(context.getKey(), new LabeledBoolean(value.getLabel(), isChecked));
         }
         else
         {
            clearViewData(context.getKey());
         }

         if (valueUpdater != null)
         {
            valueUpdater.update(new LabeledBoolean(value.getLabel(), isChecked));
         }
      }
   }

   @Override
   public void render(Context context, LabeledBoolean value, SafeHtmlBuilder sb)
   {
      // Get the view data.
      Object key = context.getKey();
      LabeledBoolean viewData = getViewData(key);
      if (viewData != null && viewData.equals(value))
      {
         clearViewData(key);
         viewData = null;
      }

      String label = value != null ? value.getLabel() : null;
      if (StringUtil.isNullOrEmpty(label) && viewData != null)
      {
         label = viewData.getLabel();
      }

      if (StringUtil.isNullOrEmpty(label))
         return;

      boolean isChecked = false;
      if (value != null && value.getBool() && ((viewData != null) ? viewData.getBool() : value.getBool()))
      {
         isChecked = true;
      }

      sb.append(SafeHtmlUtils.fromTrustedString(
            "<input type=\"checkbox\" tabindex=\"-1\" aria-label=\"" +
            SafeHtmlUtils.htmlEscape(label) +
            "\""));

      if (isChecked)
      {
         sb.append(SafeHtmlUtils.fromTrustedString(" checked"));
      }
      sb.append(SafeHtmlUtils.fromTrustedString("/>"));
   }

   private final boolean dependsOnSelection_;
   private final boolean handlesSelection_;
}
