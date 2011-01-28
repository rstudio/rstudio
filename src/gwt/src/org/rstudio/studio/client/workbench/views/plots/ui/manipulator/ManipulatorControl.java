package org.rstudio.studio.client.workbench.views.plots.ui.manipulator;

import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class ManipulatorControl extends Composite
{
   public ManipulatorControl()
   {
    
      
      panel_ = new VerticalPanel();
      
      
      // setup caption panel and add it
      HorizontalPanel captionPanel = new HorizontalPanel();
  
      label_ = new Label();
      captionPanel.add(label_);
      valueLabel_ = new Label();
      captionPanel.add(valueLabel_);
      panel_.add(captionPanel);
      
      initWidget(panel_);
      setStyleName(ManipulatorResources.INSTANCE.manipulatorStyles().control());
   }
   
   
   public void setLabel(String label)
   {
      label_.setText(label);
   }
   
   public void setValueText(String valueText)
   {
      valueLabel_.setText(valueText);
   }
   
   protected void addWidget(Widget w)
   {
      panel_.add(w);
   }
   
   private Label label_;
   private Label valueLabel_;
   
   private VerticalPanel panel_;
}
