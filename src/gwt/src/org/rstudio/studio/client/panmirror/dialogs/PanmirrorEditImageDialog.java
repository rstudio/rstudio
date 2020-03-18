/*
 * PanmirrorEditImageDialog.java
 *
 * Copyright (C) 2009-20 by RStudio, PBC
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


package org.rstudio.studio.client.panmirror.dialogs;

import org.rstudio.core.client.Debug;
import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.theme.DialogTabLayoutPanel;
import org.rstudio.core.client.theme.VerticalTabPanel;
import org.rstudio.core.client.widget.FormLabel;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.NumericTextBox;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorAttrProps;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorImageDimensions;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorImageProps;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HasVerticalAlignment;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.ListBox;
import com.google.gwt.user.client.ui.Panel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;


public class PanmirrorEditImageDialog extends ModalDialog<PanmirrorImageProps>
{
   public PanmirrorEditImageDialog(PanmirrorImageProps props,
                                   PanmirrorImageDimensions dims,
                                   String resourceDir,
                                   boolean editAttributes,
                                   OperationWithInput<PanmirrorImageProps> operation)
   {
      super("Image", Roles.getDialogRole(), operation, () -> {
         // cancel returns null
         operation.execute(null);
      }); 
      
      VerticalTabPanel imageTab = new VerticalTabPanel(ElementIds.VISUAL_MD_IMAGE_TAB_IMAGE);
      imageTab.addStyleName(RES.styles().dialog());
      
      
      imageTab.add(url_ = new PanmirrorImageChooser(FileSystemItem.createDir(resourceDir)));
      url_.addStyleName(RES.styles().spaced());
      if (!StringUtil.isNullOrEmpty(props.src))
         url_.setText(props.src);
      
      
      // size 
      HorizontalPanel sizePanel = new HorizontalPanel();
      sizePanel.addStyleName(RES.styles().spaced());
      sizePanel.setVerticalAlignment(HasVerticalAlignment.ALIGN_MIDDLE);
      width_ = addSizeInput(props.width, sizePanel, ElementIds.VISUAL_MD_IMAGE_WIDTH, "Width:");
      if (props.width != null)
         width_.setText(props.width.toString());
      height_ = addSizeInput(props.height, sizePanel, ElementIds.VISUAL_MD_IMAGE_HEIGHT, "Height:");
      units_ = addUnitsSelect(sizePanel);
      lockRatio_ = new CheckBox("Lock ratio");
      lockRatio_.addStyleName(RES.styles().lockRatioCheckbox());
      lockRatio_.getElement().setId(ElementIds.VISUAL_MD_IMAGE_LOCK_RATIO);
      sizePanel.add(lockRatio_);
      if (editAttributes)
      {
         imageTab.add(sizePanel);
      }
      
      title_ = PanmirrorDialogsUtil.addTextBox(imageTab, ElementIds.VISUAL_MD_IMAGE_TITLE, "Title/Tooltip:", props.title);
      alt_ = PanmirrorDialogsUtil.addTextBox(imageTab, ElementIds.VISUAL_MD_IMAGE_ALT, "Caption/Alt:", props.alt); 
         
      editAttr_ =  new PanmirrorEditAttrWidget();
      editAttr_.setAttr(props);
      
      if (editAttributes)
      {
         VerticalTabPanel attributesTab = new VerticalTabPanel(ElementIds.VISUAL_MD_IMAGE_TAB_ATTRIBUTES);
         attributesTab.addStyleName(RES.styles().dialog());
         attributesTab.add(editAttr_);
         
         DialogTabLayoutPanel tabPanel = new DialogTabLayoutPanel("Image");
         tabPanel.addStyleName(RES.styles().imageDialogTabs());
         tabPanel.add(imageTab, "Image", imageTab.getBasePanelId());
         tabPanel.add(attributesTab, "Attributes", attributesTab.getBasePanelId());
         tabPanel.selectTab(0);
         
         mainWidget_ = tabPanel;
      }
      else
      {
         mainWidget_ = imageTab;
      }
   }
   
   @Override
   protected Widget createMainWidget()
   {
      return mainWidget_;
   }
   
   @Override
   public void focusFirstControl()
   {
      url_.getTextBox().setFocus(true);
      url_.getTextBox().selectAll();
   }
   
   @Override
   protected PanmirrorImageProps collectInput()
   {
      PanmirrorImageProps result = new PanmirrorImageProps();
      result.src = url_.getTextBox().getValue().trim();
      result.title = title_.getValue().trim();
      result.alt = alt_.getValue().trim();
      PanmirrorAttrProps attr = editAttr_.getAttr();
      result.id = attr.id;
      result.classes = attr.classes;
      result.keyvalue = attr.keyvalue;
      return result;
   }
   
   @Override
   protected boolean validate(PanmirrorImageProps result)
   {
      return true;
   }
   
   private static NumericTextBox addSizeInput(Double value, Panel panel, String id, String labelText)
   {
      FormLabel label = createHorizontalLabel(labelText);
      NumericTextBox input = new NumericTextBox();
      input.setMin(1);
      input.setMax(10000);
      if (value != null)
         input.setText(value.toString());
      input.addStyleName(RES.styles().horizontalInput());
      input.getElement().setId(id);
      label.setFor(input);
      panel.add(label);
      panel.add(input);
      return input;
   }
   
   private static ListBox addUnitsSelect(Panel panel)
   {
      String[] options = {"px", "in", "cm", "mm", "%"};
      ListBox units = new ListBox();
      units.addStyleName(RES.styles().horizontalInput());
      for (int i = 0; i < options.length; i++)
         units.addItem(options[i], options[i]);
      units.getElement().setId(ElementIds.VISUAL_MD_IMAGE_UNITS);
      panel.add(units);
      return units;
   }
   
   private static FormLabel createHorizontalLabel(String text)
   {
      FormLabel label = new FormLabel(text);
      label.addStyleName(RES.styles().horizontalLabel());
      return label;
   }
   
   private static PanmirrorDialogsResources RES = PanmirrorDialogsResources.INSTANCE;
   
   private final Widget mainWidget_;

   private final PanmirrorImageChooser url_;
   private final NumericTextBox width_;
   private final NumericTextBox height_;
   private final ListBox units_;
   private final CheckBox lockRatio_;
   private final TextBox title_;
   private final TextBox alt_;
   private final PanmirrorEditAttrWidget editAttr_;
}
