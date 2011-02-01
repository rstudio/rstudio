package org.rstudio.studio.client.workbench.views.plots.ui.manipulator;

import org.rstudio.core.client.widget.MiniDialogPopupPanel;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.workbench.views.plots.model.Manipulator;
import org.rstudio.studio.client.workbench.views.plots.ui.manipulator.ManipulatorManager.ManipulatorChangedHandler;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;

import com.google.gwt.json.client.JSONNumber;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.VerticalPanel;
import com.google.gwt.user.client.ui.Widget;

public class ManipulatorPopupPanel extends MiniDialogPopupPanel 
                             
{
   public ManipulatorPopupPanel(final ManipulatorChangedHandler changedHandler)
   {
      super(true, false);
      
      changedHandler_ = changedHandler;
      
      
      setCaption("Manipulate");
   
        
      
      
   }
   
   @Override
   protected Widget createMainWidget()
   {
      mainPanel_ = new VerticalPanel();
      mainPanel_.setWidth("250px");
      return mainPanel_;
   }
   
   public void update(Manipulator manipulator)
   {  
      mainPanel_.clear();
      
      if (manipulator != null)
      {         
         ManipulatorControlSlider slider = new ManipulatorControlSlider();
         slider.setCaption("SliderMe");
         slider.setValueText("67");
         
         mainPanel_.add(slider);
         
         final TextBox varInputBox = new TextBox();
         varInputBox.setText(manipulator.getVariables().toString());
         mainPanel_.add(varInputBox);
         
         final TextBox valueInputBox = new TextBox();
         mainPanel_.add(valueInputBox);
         
         mainPanel_.add(new ThemedButton("Change", new ClickHandler() {
   
            public void onClick(ClickEvent event)
            {
               String var = varInputBox.getText().trim();
               int value = Integer.parseInt(valueInputBox.getText().trim());
              
               JSONObject jsObject = new JSONObject();
               jsObject.put(var, new JSONNumber(value));
    
               changedHandler_.onManipulatorChanged(jsObject);       
            }
            
         }));
      }
   }
   
  
   private VerticalPanel mainPanel_;
   private final ManipulatorChangedHandler changedHandler_;

}
