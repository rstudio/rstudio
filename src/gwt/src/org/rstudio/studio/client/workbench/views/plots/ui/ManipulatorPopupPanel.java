package org.rstudio.studio.client.workbench.views.plots.ui;

import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.core.client.widget.ThemedPopupPanel;
import org.rstudio.studio.client.workbench.views.plots.model.Manipulator;
import org.rstudio.studio.client.workbench.views.plots.ui.ManipulatorManager.ManipulatorChangedHandler;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;

public class ManipulatorPopupPanel extends ThemedPopupPanel 
                             
{
   public ManipulatorPopupPanel(final ManipulatorChangedHandler changedHandler)
   {
      super(true, false);
      
      changedHandler_ = changedHandler;
      
      
      mainPanel_ = new VerticalPanel();
      mainPanel_.setHeight("200px;");
      setWidget(mainPanel_);
      
     
      
      
      setWidget(mainPanel_);
   }
   
   public void update(Manipulator manipulator)
   {
      mainPanel_.clear();
      
      if (manipulator != null)
      {
         final TextBox inputBox = new TextBox();
         mainPanel_.add(inputBox);
         
         mainPanel_.add(new ThemedButton("Change", new ClickHandler() {
   
            public void onClick(ClickEvent event)
            {
               int value = Integer.parseInt(inputBox.getText());
              
               JSONObject jsObject = new JSONObject();
               jsObject.put("x", new JSONNumber(value));
    
               changedHandler_.onManipulatorChanged(jsObject);       
            }
            
         }));
      }
   }
   
  
   private final VerticalPanel mainPanel_;
   private final ManipulatorChangedHandler changedHandler_;

}
