package org.rstudio.studio.client.workbench.prefs.views;

import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.FlowPanel;
import com.google.gwt.user.client.ui.ListBox;

public class SelectWidget extends Composite
{
   public SelectWidget(String label, String[] options)
   {
      FlowPanel flowPanel = new FlowPanel();
      flowPanel.add(new SpanLabel(label, true));

      listBox_ = new ListBox(false);
      for (String option : options)
         listBox_.addItem(option);
      listBox_.getElement().getStyle().setMarginLeft(0.6, Unit.EM);
      flowPanel.add(listBox_);

      initWidget(flowPanel);
   }

   public ListBox getListBox()
   {
      return listBox_;
   }

   private final ListBox listBox_;
}
