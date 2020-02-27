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

import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.files.FileSystemItem;
import org.rstudio.core.client.theme.DialogTabLayoutPanel;
import org.rstudio.core.client.theme.VerticalTabPanel;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorAttrProps;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorImageProps;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;


public class PanmirrorEditImageDialog extends ModalDialog<PanmirrorImageProps>
{
   public PanmirrorEditImageDialog(PanmirrorImageProps props,
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
      title_ = PanmirrorDialogsUtil.addTextBox(imageTab, ElementIds.VISUAL_MD_IMAGE_TITLE, "Title/Tooltip", props.title);
      alt_ = PanmirrorDialogsUtil.addTextBox(imageTab, ElementIds.VISUAL_MD_IMAGE_ALT, "Caption/Alt", props.alt); 
         
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
      result.src = url_.getText().trim();
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
   
   private static PanmirrorDialogsResources RES = PanmirrorDialogsResources.INSTANCE;
   
   private final Widget mainWidget_;

   private final PanmirrorImageChooser url_;
   private final TextBox title_;
   private final TextBox alt_;
   private final PanmirrorEditAttrWidget editAttr_;
}
