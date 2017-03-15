/*
 * ClearAllDialog.java
 *
 * Copyright (C) 2009-12 by RStudio, Inc.
 *
 * Unless you have received this program directly from RStudio pursuant
 * to the terms of a commercial license agreement with RStudio, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.environment;

import com.google.gwt.dom.client.Style;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.event.dom.client.ClickHandler;
import com.google.gwt.event.logical.shared.ValueChangeEvent;
import com.google.gwt.event.logical.shared.ValueChangeHandler;
import com.google.gwt.user.client.ui.*;
import com.google.inject.Inject;

import org.rstudio.core.client.resources.ImageResource2x;
import org.rstudio.core.client.theme.res.ThemeResources;
import org.rstudio.core.client.widget.ModalDialogBase;
import org.rstudio.core.client.widget.MultiLineLabel;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.core.client.widget.images.MessageDialogImages;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.workbench.prefs.model.UIPrefs;

public class ClearAllDialog extends ModalDialogBase
{  
   public ClearAllDialog(int numObjects, 
                         final ProgressOperationWithInput<Boolean> okOperation)
   {
      RStudioGinjector.INSTANCE.injectMembers(this);
      numObjects_ = numObjects;
      
      setText("Confirm Remove Objects");
      setButtonAlignment(HasHorizontalAlignment.ALIGN_CENTER);
      
      ThemedButton yesButton = new ThemedButton("Yes", new ClickHandler()
      {
         public void onClick(ClickEvent event)
         {
            if (okOperation != null)
               okOperation.execute(chkIncludeHidden_.getValue(), progress_);
            closeDialog();
         }
      });
      addOkButton(yesButton);
      
      addCancelButton().setText("No");
   }
   
   @Inject 
   void initialize(UIPrefs prefs)
   {
      prefs_ = prefs;
   }

   @Override
   protected Widget createMainWidget()
   {
      progress_ = addProgressIndicator();
      
      VerticalPanel panel = new VerticalPanel();
      
      HorizontalPanel horizontalPanel = new HorizontalPanel();
      horizontalPanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_TOP);

      // add image
      MessageDialogImages images = MessageDialogImages.INSTANCE;
      Image image = new Image(new ImageResource2x(images.dialog_warning2x()));
      horizontalPanel.add(image);

      // add message widget
      String objects;
      if (numObjects_ == 0)
         objects = "all objects";
      else if (numObjects_ == 1)
         objects = "1 object";
      else
         objects = numObjects_ + " objects";
      Label label = new MultiLineLabel(
            "Are you sure you want to remove "  + objects + " from the " +
            "environment? This operation cannot be undone.");
      label.setStylePrimaryName(
            ThemeResources.INSTANCE.themeStyles().dialogMessage());
      horizontalPanel.add(label);
      panel.add(horizontalPanel);
      
      // add include hidden option
      HorizontalPanel optionPanel = new HorizontalPanel();
      Style optionStyle = optionPanel.getElement().getStyle();
      optionStyle.setMarginLeft(image.getWidth(), Unit.PX);
      optionStyle.setMarginBottom(10, Unit.PX);
      
      chkIncludeHidden_ = new CheckBox("Include hidden objects");
      chkIncludeHidden_.setValue(prefs_.clearHidden().getValue());

      if (numObjects_ == 0)
      {
         chkIncludeHidden_.addValueChangeHandler(new ValueChangeHandler<Boolean>(){
            @Override
            public void onValueChange(ValueChangeEvent<Boolean> event)
            {
               prefs_.clearHidden().setGlobalValue(event.getValue());
               prefs_.writeUIPrefs();
            }
         });
         optionPanel.add(chkIncludeHidden_);
      }
      panel.add(optionPanel);
      
      return panel;
   }
   
   private ProgressIndicator progress_ ;
   private CheckBox chkIncludeHidden_;
   private UIPrefs prefs_;
   private int numObjects_;
}
