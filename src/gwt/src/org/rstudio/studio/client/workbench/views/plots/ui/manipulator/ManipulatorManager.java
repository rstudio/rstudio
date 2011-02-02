package org.rstudio.studio.client.workbench.views.plots.ui.manipulator;

import org.rstudio.core.client.widget.ToolbarButton;
import org.rstudio.studio.client.workbench.views.plots.model.Manipulator;

import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.PopupPanel.PositionCallback;

public class ManipulatorManager
{
   public ManipulatorManager(Panel plotsSurface,
                             ManipulatorChangedHandler changedHandler)
   {
      // references
      plotsSurface_ = plotsSurface;
      
      // no manipulator to start
      manipulator_ = null;
      manipulatorPopup_ = null;
      
      // create manipulator button
      manipulatorButton_ = new ToolbarButton(
            ManipulatorResources.INSTANCE.manipulateButton(),
            new ClickHandler() { 
               public void onClick(ClickEvent event)
               {
                  showManipulatorPopup();
               }
            });
      manipulatorButton_.addStyleName(ManipulatorStyles.INSTANCE.manipulateButton());
      manipulatorButton_.setTitle("Show plot manipulator");
      plotsSurface_.add(manipulatorButton_);
      manipulatorButton_.setVisible(false);
      
      // create manipulator popup panel
      manipulatorPopup_ = new ManipulatorPopupPanel(changedHandler);
   }
   
   
   public void setManipulator(Manipulator manipulator, boolean show)
   {
      if (isNewManipulatorState(manipulator))
      {
         // set active manipulator
         manipulator_ = manipulator;
             
         // set visibility of manipulator button
         manipulatorButton_.setVisible(manipulator_ != null);
         
         // update UI
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
   }
   
   private boolean isNewManipulatorState(Manipulator manipulator)
   {
      if (manipulator_ == null && manipulator != null)
         return true;
      if (manipulator == null && manipulator_ != null)
         return true;
      if (!manipulator_.getID().equals(manipulator.getID()))
         return true;
      else
         return false;
   }
   
   private void showManipulatorPopup()
   {
      // show it if necessary
      if (!manipulatorPopup_.isShowing())
      {
         manipulatorPopup_.setPopupPositionAndShow(new PositionCallback(){
            @Override
            public void setPosition(int offsetWidth, int offsetHeight)
            {
               manipulatorPopup_.setPopupPosition(
                     plotsSurface_.getAbsoluteLeft() - offsetWidth + 22,
                     plotsSurface_.getAbsoluteTop() - 6);
            }
            
         }) ;
         
         
      }
   }
   
   
   private final Panel plotsSurface_;
   private Manipulator manipulator_;
   private ToolbarButton manipulatorButton_;
   private ManipulatorPopupPanel manipulatorPopup_;
  
}
