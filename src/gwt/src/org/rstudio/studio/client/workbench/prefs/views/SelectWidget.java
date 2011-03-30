package org.rstudio.studio.client.workbench.prefs.views;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Panel;

public class SelectWidget extends Composite
{
   public SelectWidget(String label, String[] options)
   {
      this(label, options, null, false, true);
   }

   public SelectWidget(String label,
                       String[] options,
                       String[] values,
                       boolean isMultipleSelect)
   {
      this(label, options, values, isMultipleSelect, false);
   }
   
   public SelectWidget(String label,
                       String[] options,
                       String[] values,
                       boolean isMultipleSelect,
                       boolean horizontalLayout)
   {
      if (values == null)
         values = options;

      Panel panel = null;
      if (horizontalLayout)
      {
         HorizontalPanel horizontalPanel = new HorizontalPanel();
         Label labelWidget = new Label(label);
         horizontalPanel.add(labelWidget);
         horizontalPanel.setCellVerticalAlignment(
                                          labelWidget, 
                                          HasVerticalAlignment.ALIGN_MIDDLE);
         panel = horizontalPanel;
      }
      else
      {
         FlowPanel flowPanel = new FlowPanel();
         flowPanel.add(new Label(label, true));
         panel = flowPanel;
      }

      listBox_ = new ListBox(isMultipleSelect);
      for (int i = 0; i < options.length; i++)
         listBox_.addItem(options[i], values[i]);
      panel.add(listBox_);

      initWidget(panel);
      PreferencesDialogResources res = GWT.create(PreferencesDialogResources.class);
      addStyleName(res.styles().selectWidget());
   }

   public ListBox getListBox()
   {
      return listBox_;
   }

   public void setEnabled(boolean enabled)
   {
      listBox_.setEnabled(enabled);
   }

   public boolean isEnabled()
   {
      return listBox_.isEnabled();
   }

   public boolean setValue(String value)
   {
      for (int i = 0; i < listBox_.getItemCount(); i++)
         if (value.equals(listBox_.getValue(i)))
         {
            listBox_.setSelectedIndex(i);
            return true;
         }
      return false;
   }

   public String getValue()
   {
      return listBox_.getValue(listBox_.getSelectedIndex());
   }

   private final ListBox listBox_;
}
