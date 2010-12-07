/*
 * PrintDialog.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.workbench.views.plots.ui;


import com.google.gwt.core.client.GWT;
import com.google.gwt.event.dom.client.ChangeEvent;
import com.google.gwt.event.dom.client.ChangeHandler;
import com.google.gwt.i18n.client.NumberFormat;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.gwt.user.client.ui.*;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.workbench.views.plots.model.PrintOptions;

import java.util.ArrayList;
import java.util.List;

public class PrintDialog extends ModalDialog<PrintOptions>
{
   public PrintDialog(OperationWithInput<PrintOptions> operation)
   {
      super("Print to PDF", operation);
      
      paperSizes_.add(new PaperSize("US Letter", 8.5, 11));
      paperSizes_.add(new PaperSize("US Legal", 8.5, 14));
      paperSizes_.add(new PaperSize("A4", 8.27, 11.69));
      paperSizes_.add(new PaperSize("A5", 5.83, 8.27));
      paperSizes_.add(new PaperSize("A6", 4.13, 5.83));
      paperSizes_.add(new PaperSize("4 x 6 in.", 4, 6));
      paperSizes_.add(new PaperSize("5 x 7 in.", 5, 7));
      paperSizes_.add(new PaperSize("6 x 8 in.", 6, 8));
   }
   
   @Override
   protected PrintOptions collectInput()
   {
      // get width and height
      PaperSize size = selectedPaperSize();
      if (isPortraitOrientation())
         return PrintOptions.create(size.getWidth(), size.getHeight());
      else
         return PrintOptions.create(size.getHeight(), size.getWidth());
   }
   
   @Override
   protected boolean validate(PrintOptions input)
   {
      return true;
   }

   @Override
   protected Widget createMainWidget()
   {
      Grid grid = new Grid(2, 2);
      grid.setStylePrimaryName(RESOURCES.styles().mainWidget());
      final int kComponentSpacing = 7;
      
      // paper size
      grid.setWidget(0, 0, new Label("Paper Size:"));
      HorizontalPanel paperSizePanel = new HorizontalPanel();
      paperSizePanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
      paperSizePanel.setSpacing(kComponentSpacing);
       
      // paper size list box
      paperSizeListBox_ = new ListBox();
      paperSizeListBox_.setStylePrimaryName(RESOURCES.styles().sizeListBox());
      for (int i=0; i<paperSizes_.size(); i++)
         paperSizeListBox_.addItem(paperSizes_.get(i).getName());
      paperSizeListBox_.addChangeHandler(new ChangeHandler() {
         public void onChange(ChangeEvent event)
         {
            updateSizeDescription();  
         } 
      });
      paperSizePanel.add(paperSizeListBox_);
      
      // paper size label
      paperSizeLabel_ = new Label();
      paperSizeLabel_.setStylePrimaryName(RESOURCES.styles().sizeLabel());
      paperSizePanel.add(paperSizeLabel_);
      
      // add to grid
      grid.setWidget(0, 1, paperSizePanel);
      
      // orientation
      grid.setWidget(1, 0, new Label("Orientation:"));
      HorizontalPanel orientationPanel = new HorizontalPanel();
      orientationPanel.setSpacing(kComponentSpacing);
      VerticalPanel orientationGroupPanel = new VerticalPanel();
      final String kOrientationGroup = new String("Orientation");
      portraitRadioButton_ = new RadioButton(kOrientationGroup, "Portrait");
      orientationGroupPanel.add(portraitRadioButton_);
      landscapeRadioButton_ = new RadioButton(kOrientationGroup, "Landscape");
      orientationGroupPanel.add(landscapeRadioButton_);
      orientationPanel.add(orientationGroupPanel);
      grid.setWidget(1, 1, orientationPanel);
      
      // set default values
      paperSizeListBox_.setSelectedIndex(0);
      updateSizeDescription();
      portraitRadioButton_.setValue(true);
      
      // return the widget
      return grid;
   }
   
   private PaperSize selectedPaperSize()
   {
      int selectedSize =  paperSizeListBox_.getSelectedIndex();
      return paperSizes_.get(selectedSize);
   }
   
   private boolean isPortraitOrientation()
   {
      return portraitRadioButton_.getValue();
   }
   
   private void updateSizeDescription()
   {
      paperSizeLabel_.setText(selectedPaperSize().getSizeDescription()); 
   }
   
   private Label paperSizeLabel_ ;
   private ListBox paperSizeListBox_;
   private RadioButton portraitRadioButton_ ;
   private RadioButton landscapeRadioButton_;
  
   private class PaperSize
   {
      public PaperSize(String name, double width, double height)
      {
         name_ = name;
         width_ = width;
         height_ = height;
      }
      
      public String getName() { return name_; }
      public double getWidth() { return width_; }
      public double getHeight() { return height_; }
      
      public String getSizeDescription()
      {
         return sizeFormat_.format(getWidth()) + 
                " by " + 
                sizeFormat_.format(getHeight()) +
                " inches";
      }
      
      private final String name_ ;
      private final double width_ ;
      private final double height_ ;
      private final NumberFormat sizeFormat_ = NumberFormat.getFormat("##0.00");
   }
   
   private final List<PaperSize> paperSizes_ = new ArrayList<PaperSize>(); 
   
   static interface Styles extends CssResource
   {
      String mainWidget();
      String sizeListBox();
      String sizeLabel();
   }
  
   static interface Resources extends ClientBundle
   {
      @Source("PrintDialog.css")
      Styles styles();
   }
   
   static Resources RESOURCES = (Resources)GWT.create(Resources.class) ;
   public static void ensureStylesInjected()
   {
      RESOURCES.styles().ensureInjected();
   }
}
