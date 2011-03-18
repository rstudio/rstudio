package org.rstudio.studio.client.workbench.prefs.views;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.ListBox;

public class SelectWidget extends Composite
{
   public SelectWidget(String label, String[] options)
   {
      this(label, options, null);
   }

   public SelectWidget(String label, String[] options, String[] values)
   {
      if (values == null)
         values = options;

      FlowPanel flowPanel = new FlowPanel();
      flowPanel.add(new SpanLabel(label, true));

      listBox_ = new ListBox(false);
      for (int i = 0; i < options.length; i++)
         listBox_.addItem(options[i], values[i]);
      listBox_.getElement().getStyle().setMarginLeft(0.6, Unit.EM);
      flowPanel.add(listBox_);

      initWidget(flowPanel);
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
