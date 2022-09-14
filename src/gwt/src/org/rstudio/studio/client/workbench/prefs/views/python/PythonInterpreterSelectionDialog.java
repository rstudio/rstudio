/*
 * PythonInterpreterSelectionDialog.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
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
package org.rstudio.studio.client.workbench.prefs.views.python;

import java.util.HashMap;
import java.util.Map;

import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.core.client.theme.DialogTabLayoutPanel;
import org.rstudio.core.client.theme.VerticalTabPanel;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.core.client.widget.WidgetListBox;
import org.rstudio.studio.client.workbench.prefs.views.PythonInterpreter;
import org.rstudio.studio.client.workbench.prefs.PrefsConstants;

import com.google.gwt.core.client.GWT;
import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.user.client.ui.Widget;

public class PythonInterpreterSelectionDialog extends ModalDialog<PythonInterpreter>
{
   private static final PrefsConstants constants_ = GWT.create(PrefsConstants.class);
   private static final String LABEL_SYSTEM  = constants_.system();
   private static final String LABEL_VIRTUAL = constants_.virtualEnvironmentPlural();
   private static final String LABEL_CONDA   = constants_.condaEnvironmentPlural();
   
   public PythonInterpreterSelectionDialog(final JsArray<PythonInterpreter> interpreters,
                                           final OperationWithInput<PythonInterpreter> operation)
   {
      super(constants_.pythonInterpreterPlural(), Roles.getDialogRole(), operation);
      setOkButtonCaption(constants_.select());
      
      // initialize widget list boxes
      widgets_ = new HashMap<>();
      for (String label : new String[] { LABEL_SYSTEM, LABEL_VIRTUAL, LABEL_CONDA })
      {
         WidgetListBox<PythonInterpreterListEntryUi> listBox = new WidgetListBox<>();
         listBox.setSize("598px", "468px");
         listBox.setAriaLabel(label);
         
         listBox.setEmptyText(constants_.noneAvailableParentheses());
         
         // allow double-click to select the requested interpreter
         listBox.addDoubleClickHandler((DoubleClickEvent event) ->
         {
            if (listBox.getSelectedItem() != null)
            {
               selectedItem_ = listBox.getSelectedItem();
               clickOkButton();
            }
         });
         
         listBox.addChangeHandler((ChangeEvent event) ->
         {
            selectedItem_ = listBox.getSelectedItem();
         });
         
         widgets_.put(label, listBox);
      }
      
      // add interpreters to their appropriate buckets
      for (PythonInterpreter interpreter : JsUtil.asIterable(interpreters))
      {
         if (interpreter == null || !interpreter.isValid())
            continue;
         
         String type = interpreter.getType();
         if (type == null)
            continue;
         
         String version = interpreter.getVersion();
         if (version == null || version.startsWith("2"))
            continue;
         
         if (StringUtil.equals(type, "system"))
         {
            widgets_.get(LABEL_SYSTEM).addItem(new PythonInterpreterListEntryUi(interpreter));
         }
         else if (StringUtil.equals(type, "virtualenv"))
         {
            widgets_.get(LABEL_VIRTUAL).addItem(new PythonInterpreterListEntryUi(interpreter));
         }
         else if (StringUtil.equals(type, "conda"))
         {
            widgets_.get(LABEL_CONDA).addItem(new PythonInterpreterListEntryUi(interpreter));
         }
      }
 
      // initialize tab panel
      tabPanel_ = new DialogTabLayoutPanel(constants_.general());
      tabPanel_.setSize("620px", "520px");
      for (Map.Entry<String, WidgetListBox<PythonInterpreterListEntryUi>> entry : widgets_.entrySet())
      {
         VerticalTabPanel panel = new VerticalTabPanel(entry.getKey());
         panel.add(entry.getValue());
         tabPanel_.add(panel, entry.getKey(), panel.getBasePanelId());
      }
      
      tabPanel_.selectTab(0);
   }
   
   @Override
   protected PythonInterpreter collectInput()
   {
      if (selectedItem_ == null)
         return null;
      
      return selectedItem_.getInterpreter();
   }

   @Override
   protected Widget createMainWidget()
   {
      return tabPanel_;
   }
   
   final DialogTabLayoutPanel tabPanel_;
   final Map<String, WidgetListBox<PythonInterpreterListEntryUi>> widgets_;
   
   PythonInterpreterListEntryUi selectedItem_;
   ThemedButton useDefaultBtn_;

}
