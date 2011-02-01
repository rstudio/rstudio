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
      // get manipulator styles
      ManipulatorStyles styles = ManipulatorResources.INSTANCE.manipulatorStyles();
      
      panel_ = new VerticalPanel();
      
      
      // setup caption panel and add it
      HorizontalPanel captionPanel = new HorizontalPanel();
  
      captionLabel_ = new Label();
      captionLabel_.setStyleName(styles.captionLabel());
      captionPanel.add(captionLabel_);
      valueLabel_ = new Label();
      valueLabel_.setStyleName(styles.valueLabel());
      captionPanel.add(valueLabel_);
      panel_.add(captionPanel);
      
      initWidget(panel_);
      setStyleName(styles.control());
   }
   
   
   public void setCaption(String captionText)
   {
      captionLabel_.setText(captionText + ":");
   }
   
   public void setValueText(String valueText)
   {
      valueLabel_.setText(valueText);
   }
   
   protected void addWidget(Widget w)
   {
      panel_.add(w);
   }
   
   private Label captionLabel_;
   private Label valueLabel_;
   
   private VerticalPanel panel_;
}
