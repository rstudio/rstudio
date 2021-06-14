/*
 * PythonInterpreterSelectionDialog.java
 *
 * Copyright (C) 2021 by RStudio, PBC
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
package org.rstudio.studio.client.workbench.prefs.views.python;

import org.rstudio.core.client.js.JsUtil;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.core.client.widget.WidgetListBox;
import org.rstudio.studio.client.workbench.prefs.views.PythonInterpreter;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.user.client.ui.Widget;

public class PythonInterpreterSelectionDialog extends ModalDialog<PythonInterpreter>
{
   public PythonInterpreterSelectionDialog(final JsArray<PythonInterpreter> interpreters,
                                           final OperationWithInput<PythonInterpreter> operation)
   {
      super("Python Interpreters", Roles.getDialogRole(), operation);
      setOkButtonCaption("Select");
      
      widgets_ = new WidgetListBox<>();
      widgets_.setHeight("420px");
      widgets_.setAriaLabel("Python Interpreters");
      
      for (PythonInterpreter interpreter : JsUtil.asIterable(interpreters))
         if (interpreter.isValid())
            widgets_.addItem(new PythonInterpreterListEntryUi(interpreter));
 
      // allow double-click to select the requested interpreter
      widgets_.addDoubleClickHandler((DoubleClickEvent event) ->
      {
         if (widgets_.getSelectedItem() != null)
         {
            clickOkButton();
         }
      });
      
   }
   
   @Override
   protected PythonInterpreter collectInput()
   {
      PythonInterpreterListEntryUi item = widgets_.getSelectedItem();
      if (item == null)
         return null;
      
      return item.getInterpreter();
   }

   @Override
   protected Widget createMainWidget()
   {
      return widgets_;
   }
   
   WidgetListBox<PythonInterpreterListEntryUi> widgets_;
   ThemedButton useDefaultBtn_;
}
