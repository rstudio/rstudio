/*
 * ChoiceListDialog.java
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

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.widget.FormListBox;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class ChoiceListDialog extends ModalDialog<Integer>
{
   public ChoiceListDialog(String title,
                           String message,
                           String[] choices,
                           OperationWithInput<Integer> operation,
                           Operation cancelOperation)
   {
      super(StringUtil.isNullOrEmpty(title) ? "Select" : title,
            Roles.getDialogRole(),
            operation,
            cancelOperation);
      message_ = message;
      choices_ = choices == null ? new String[0] : choices;
      setThemeAware(true);
   }

   @Override
   protected Widget createMainWidget()
   {
      VerticalPanel panel = new VerticalPanel();
      panel.setWidth("400px");

      if (!StringUtil.isNullOrEmpty(message_))
      {
         Label msg = new Label(message_);
         msg.getElement().getStyle().setPaddingBottom(8, Unit.PX);
         panel.add(msg);
      }

      listBox_ = new FormListBox();
      listBox_.setWidth("100%");
      int visible = Math.min(Math.max(choices_.length, 3), 10);
      listBox_.setVisibleItemCount(visible);
      for (String choice : choices_)
         listBox_.addItem(choice);
      if (choices_.length > 0)
         listBox_.setSelectedIndex(0);

      // double-click to accept the selection
      listBox_.addDoubleClickHandler((DoubleClickEvent event) -> clickOkButton());

      panel.add(listBox_);
      return panel;
   }

   @Override
   protected Integer collectInput()
   {
      int idx = listBox_ == null ? -1 : listBox_.getSelectedIndex();
      if (idx < 0)
         return null;
      return idx + 1;
   }

   @Override
   protected boolean validate(Integer input)
   {
      return input != null && input > 0;
   }

   private final String message_;
   private final String[] choices_;
   private FormListBox listBox_;
}
