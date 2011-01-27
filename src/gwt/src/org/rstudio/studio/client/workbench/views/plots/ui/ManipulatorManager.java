package org.rstudio.studio.client.workbench.views.plots.ui;

import org.rstudio.studio.client.workbench.views.plots.model.Manipulator;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.json.client.JSONObject;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Panel;

public class ManipulatorManager
{
   public interface ManipulatorChangedHandler
   {
      void onManipulatorChanged(JSONObject values);
   }
   
   public ManipulatorManager(Panel plotsSurface,
                             ManipulatorChangedHandler changedHandler)
   {
      // references
      plotsSurface_ = plotsSurface;
      
      // no manipulator to start
      manipulator_ = null;
      manipulatorPopup_ = null;
      
      // create manipulator button
      manipulatorButton_ = new Button("M");
      manipulatorButton_.setHeight("25px;");
      manipulatorButton_.addClickHandler(new ClickHandler() {
         @Override
         public void onClick(ClickEvent event)
         {
            showManipulatorPopup();
         }
      });
      plotsSurface_.add(manipulatorButton_);
      manipulatorButton_.setVisible(false);
      
      // create manipulator popup panel
      manipulatorPopup_ = new ManipulatorPopupPanel(changedHandler);
   }
   
   
   public void setManipulator(Manipulator manipulator, boolean show)
   {
      // set active manipulator
      manipulator_ = manipulator;
          
      // set visibility of manipulator button
      manipulatorButton_.setVisible(manipulator_ != null);
      
      // update manipulator popup panel
      manipulatorPopup_.update(manipulator_); 
      
      // if we have a manipulator then show if requested, otherwise hide
      if (manipulator_ != null)
      {
         // show if requested
         if (show)
            showManipulatorPopup();  
      }
      else
      {
         manipulatorPopup_.hide();
      }
   }
   
   private void showManipulatorPopup()
   {
      // show it if necessary
      if (!manipulatorPopup_.isShowing())
         manipulatorPopup_.showRelativeTo(plotsSurface_);   
   }
   
   
   private final Panel plotsSurface_;
   private Manipulator manipulator_;
   private Button manipulatorButton_;
   private ManipulatorPopupPanel manipulatorPopup_;
  
}
