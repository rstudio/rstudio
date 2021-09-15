/*
 * PanmirrorEditDivDialog.java
 *
 * Copyright (C) 2021 by RStudio, PBC
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


import com.google.gwt.aria.client.Roles;

import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.theme.DialogTabLayoutPanel;
import org.rstudio.core.client.theme.VerticalTabPanel;
import org.rstudio.core.client.widget.FormCheckBox;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.SelectWidget;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorCalloutProps;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorDivEditProps;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorDivEditResult;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;


public class PanmirrorEditDivDialog extends ModalDialog<PanmirrorDivEditResult>
{
   public PanmirrorEditDivDialog(String caption,
                                 boolean removeEnabled,
                                 PanmirrorDivEditProps props, 
                                 OperationWithInput<PanmirrorDivEditResult> operation)
   {
      super(caption, Roles.getDialogRole(), operation, () -> {
         // cancel returns null
         operation.execute(null);
      });
      
      if (removeEnabled)
      {
         ThemedButton removeAttributesButton = new ThemedButton("Unwrap Div");
         removeAttributesButton.addClickHandler((event) -> {
            PanmirrorDivEditResult result = collectInput();
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
         addLeftButton(removeAttributesButton, ElementIds.VISUAL_MD_DIV_REMOVE_BUTTON);
      }
      
      // create attributes editor and tab
      editAttr_ =  new PanmirrorEditAttrWidget();
      editAttr_.setAttr(props.attr, null);
      VerticalTabPanel attributesTab = new VerticalTabPanel(ElementIds.VISUAL_MD_DIV_TAB_ATTRIBUTES);
      attributesTab.addStyleName(RES.styles().dialog());
      attributesTab.add(editAttr_);
      
      if (props.callout != null)
      {
         VerticalTabPanel calloutTab = new VerticalTabPanel(ElementIds.VISUAL_MD_DIV_TAB_CALLOUT);
         
         // type
         calloutType_ = new SelectWidget(
            "Callout type: ", 
            new String[]{"note", "tip", "important", "caution", "warning"}
         );
         calloutType_.setValue(props.callout.type);
         calloutTab.add(calloutType_);
         
         // appearance
         calloutAppearance_ = new SelectWidget(
           "Appearance: ",
           new String[] {"default", "simple", "minimal"}
         );
         calloutAppearance_.setValue(props.callout.appearance);
         calloutTab.add(calloutAppearance_);
         
         // caption
         calloutCaption_  = PanmirrorDialogsUtil.addTextBox(
            calloutTab, 
            ElementIds.VISUAL_MD_DIV_CALLOUT_CAPTION, 
            "Caption",
            props.callout.caption
         );
         DomUtils.setPlaceholder(calloutCaption_, "(Optional)");
         
         // icon
         calloutCheckBox_ = new FormCheckBox("Display icon", ElementIds.VISUAL_MD_DIV_CALLOUT_ICON);
         calloutCheckBox_.setValue(props.callout.icon);
         calloutTab.add(calloutCheckBox_);
         
         DialogTabLayoutPanel tabPanel = new DialogTabLayoutPanel("Div");
         tabPanel.addStyleName(RES.styles().divDialogTabs());
         tabPanel.add(calloutTab, "Callout", calloutTab.getBasePanelId());
         tabPanel.add(attributesTab, "Attributes", attributesTab.getBasePanelId());
         tabPanel.selectTab(0);

         mainWidget_ = tabPanel;
      
      }
      else 
      {
         mainWidget_ = attributesTab;
      } 
   }

   @Override
   protected Widget createMainWidget()
   {
      return mainWidget_;
   }

   @Override
   public void focusInitialControl()
   {
     editAttr_.setFocus();
   }

   @Override
   protected PanmirrorDivEditResult collectInput()
   {
      PanmirrorDivEditResult result = new PanmirrorDivEditResult();
      result.attr = editAttr_.getAttr();
      result.action = "edit";
      result.callout = new PanmirrorCalloutProps();
      result.callout.type = calloutType_.getValue();
      result.callout.appearance = calloutAppearance_.getValue();
      result.callout.icon = calloutCheckBox_.getValue();
      result.callout.caption = calloutCaption_.getText().trim();
      return result;
   }


   @Override
   protected boolean validate(PanmirrorDivEditResult input)
   {
      return true;
   }


   private static PanmirrorDialogsResources RES = PanmirrorDialogsResources.INSTANCE;

   private Widget mainWidget_;

   private PanmirrorEditAttrWidget editAttr_;
   private SelectWidget calloutType_;
   private SelectWidget calloutAppearance_;
   private TextBox calloutCaption_;
   private CheckBox calloutCheckBox_;

}
