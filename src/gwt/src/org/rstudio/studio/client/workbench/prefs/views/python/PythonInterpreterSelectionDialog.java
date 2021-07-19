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

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.GWT;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.js.JsUtil;
import org.rstudio.core.client.theme.DialogTabLayoutPanel;
import org.rstudio.core.client.theme.VerticalTabPanel;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.core.client.widget.WidgetListBox;
import org.rstudio.studio.client.workbench.prefs.views.PythonInterpreter;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.DoubleClickEvent;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.studio.client.workbench.prefs.views.PythonPreferencesPaneConstants;

public class PythonInterpreterSelectionDialog extends ModalDialog<PythonInterpreter>
{
   private final String LABEL_SYSTEM  = constants_.systemTab();
   private final String LABEL_VIRTUAL = constants_.virtualEnvTab();
   private final String LABEL_CONDA   = constants_.condaEnvTab();
   
   public PythonInterpreterSelectionDialog(final JsArray<PythonInterpreter> interpreters,
                                           final OperationWithInput<PythonInterpreter> operation)
   {
      super(constants_.interpretersCaption(), Roles.getDialogRole(), operation);
      setOkButtonCaption(constants_.okButtonCaption());
      
      // initialize widget list boxes
      widgets_ = new HashMap<>();
      for (String label : new String[] { LABEL_SYSTEM, LABEL_VIRTUAL, LABEL_CONDA })
      {
         WidgetListBox<PythonInterpreterListEntryUi> listBox = new WidgetListBox<>();
         listBox.setSize("598px", "468px");
         listBox.setAriaLabel(label);
         
         listBox.setEmptyText(constants_.noneAvailableListBox());
         
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
      tabPanel_ = new DialogTabLayoutPanel(constants_.tabPanelCaption());
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
   private static final PythonPreferencesPaneConstants constants_ = GWT.create(PythonPreferencesPaneConstants.class);
}
