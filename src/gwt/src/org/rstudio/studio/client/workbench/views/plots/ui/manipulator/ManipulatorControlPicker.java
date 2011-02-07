package org.rstudio.studio.client.workbench.views.plots.ui.manipulator;

import org.rstudio.studio.client.workbench.views.plots.model.Manipulator;

import com.google.gwt.core.client.JsArrayString;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.json.client.JSONString;
import com.google.gwt.user.client.ui.HasHorizontalAlignment;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.ListBox;

public class ManipulatorControlPicker extends ManipulatorControl
{

   public ManipulatorControlPicker(
                               String variable,
                               String value,
                               Manipulator.Picker picker,
                               final ManipulatorChangedHandler changedHandler)
   {
      super(variable, picker, changedHandler);
      
      // get manipulator styles
      ManipulatorStyles styles = ManipulatorResources.INSTANCE.manipulatorStyles();
   
      // main control
      HorizontalPanel panel = new HorizontalPanel();
      panel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
      panel.setHorizontalAlignment(HasHorizontalAlignment.ALIGN_LEFT);
  
      // caption
      Label captionLabel = new Label();
      captionLabel.setStyleName(styles.captionLabel());
      captionLabel.setText(getLabel() + ":");
      panel.add(captionLabel);
      
      // picker
      listBox_ = new ListBox();
      listBox_.setVisibleItemCount(1);
      JsArrayString choices = picker.getChoices();
      int selectedIndex = 0;
      for (int i=0; i<choices.length(); i++)
      {
         String choice = choices.get(i);
         listBox_.addItem(choice);
         if (choice.equals(value))
            selectedIndex = i;
      }
      listBox_.setSelectedIndex(selectedIndex);
      listBox_.addChangeHandler(new ChangeHandler(){
         @Override
         public void onChange(ChangeEvent event)
         {
            ManipulatorControlPicker.this.onValueChanged(
             new JSONString(listBox_.getItemText(listBox_.getSelectedIndex())));
         }   
      });
      panel.add(listBox_);
      
      initWidget(panel);
      addControlStyle(styles.picker());
   }

   @Override
   public void focus()
   {
      listBox_.setFocus(true);
   }
   
   private ListBox listBox_;

}
