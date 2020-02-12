/*
 * PanmirrorEditLinkDialog.java
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
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.theme.DialogTabLayoutPanel;
import org.rstudio.core.client.theme.VerticalTabPanel;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorAttrProps;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorLinkCapabilities;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorLinkEditResult;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorLinkProps;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorLinkTargets;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorLinkType;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;


public class PanmirrorEditLinkDialog extends ModalDialog<PanmirrorLinkEditResult>
{
   public PanmirrorEditLinkDialog(PanmirrorLinkProps link, 
                                  PanmirrorLinkTargets targets, 
                                  PanmirrorLinkCapabilities capabilities,
                                  OperationWithInput<PanmirrorLinkEditResult> operation)
   {
      super("Link", Roles.getDialogRole(), operation, () -> {
         // cancel returns null
         operation.execute(null);
      });
   
      VerticalTabPanel linkTab = new VerticalTabPanel(ElementIds.VISUAL_MD_LINK_TAB_LINK);
      linkTab.addStyleName(RES.styles().dialog());
      
      if (!StringUtil.isNullOrEmpty(link.href))
      {
         ThemedButton removeLinkButton = new ThemedButton("Remove Link");
         removeLinkButton.addClickHandler((event) -> {
            PanmirrorLinkEditResult result = collectInput();
            result.action = "remove";
            validateAndGo(result, new Command()
            {
               @Override
               public void execute()
               {
                  closeDialog();
                  if (operation != null)
                     operation.execute(result);
                  onSuccess();
               }
            });
         });
         addLeftButton(removeLinkButton, ElementIds.VISUAL_MD_LINK_REMOVE_LINK_BUTTON); 
      }
      
      href_ = new PanmirrorHRefSelect(targets, capabilities);
      href_.addStyleName(RES.styles().hrefSelect());
      href_.addStyleName(RES.styles().spaced());
      linkTab.add(href_);
      
      text_ = PanmirrorDialogsUtil.addTextBox(
         linkTab, 
         ElementIds.VISUAL_MD_LINK_TEXT, 
         textLabel_ = new Label("Text"), 
         link.text
      );
      title_ = PanmirrorDialogsUtil.addTextBox(
         linkTab, 
         ElementIds.VISUAL_MD_LINK_TITLE, 
         titleLabel_ = new Label("Title/Tooltip"), 
         link.title
      );
        
      editAttr_ =  new PanmirrorEditAttrWidget();
      editAttr_.setAttr(link);
      
      
      href_.addTypeChangedHandler((event) -> {
        manageVisibility();
      });
      href_.setHRef(link.type, link.href);
      manageVisibility();
    
      
      if (capabilities.attributes)
      {
         VerticalTabPanel attributesTab = new VerticalTabPanel(ElementIds.VISUAL_MD_IMAGE_TAB_ATTRIBUTES);
         attributesTab.addStyleName(RES.styles().dialog());
         attributesTab.add(editAttr_);
         
         DialogTabLayoutPanel tabPanel = new DialogTabLayoutPanel("Image");
         tabPanel.addStyleName(RES.styles().linkDialogTabs());
         tabPanel.add(linkTab, "Link", linkTab.getBasePanelId());
         tabPanel.add(attributesTab, "Attributes", attributesTab.getBasePanelId());
         tabPanel.selectTab(0);
         
         mainWidget_ = tabPanel;
      }
      else
      {
         mainWidget_ = linkTab;
      }
   }
   
   @Override
   protected Widget createMainWidget()
   {
      return mainWidget_;
   }
   
   @Override
   protected PanmirrorLinkEditResult collectInput()
   {
      PanmirrorLinkEditResult result = new PanmirrorLinkEditResult();
      result.action = "edit";
      result.link = new PanmirrorLinkProps();
      result.link.type = href_.getType();
      result.link.href = href_.getHRef();
      if (result.link.type != PanmirrorLinkType.Heading)
      {
         result.link.text = text_.getValue().trim();
         result.link.title = title_.getValue().trim();
         PanmirrorAttrProps attr = editAttr_.getAttr();
         result.link.id = attr.id;
         result.link.classes = attr.classes;
         result.link.keyvalue = attr.keyvalue;
      }
      else
      {
         result.link.text = result.link.href;
      }
      return result;
   }
   
   @Override
   protected boolean validate(PanmirrorLinkEditResult result)
   {
      return true;
   }
   
   private void manageVisibility()
   {
      text_.setVisible(href_.getType() != PanmirrorLinkType.Heading);
      textLabel_.setVisible(text_.isVisible());
      title_.setVisible(text_.isVisible());
      titleLabel_.setVisible(text_.isVisible());
      editAttr_.setVisible(text_.isVisible());
   }
   
   private static PanmirrorDialogsResources RES = PanmirrorDialogsResources.INSTANCE;
   
   private final Widget mainWidget_;
   
   private final PanmirrorHRefSelect href_;
   
   private final Label textLabel_;
   private final TextBox text_;
   private final Label titleLabel_;
   private final TextBox title_;

   private final PanmirrorEditAttrWidget editAttr_;
}
