/*
 * PanmirrorEditCalloutDialog.java
 *
 * Copyright (C) 2022 by Posit, PBC
 *
 * Unless you have received this program directly from Posit pursuant
 * to the terms of a commercial license agreement with Posit, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */


package org.rstudio.studio.client.panmirror.dialogs;


import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Style.Unit;

import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.dom.DomUtils;
import org.rstudio.core.client.theme.DialogTabLayoutPanel;
import org.rstudio.core.client.theme.VerticalTabPanel;
import org.rstudio.core.client.widget.FormCheckBox;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.SelectWidget;
import org.rstudio.core.client.widget.ThemedButton;
import org.rstudio.studio.client.panmirror.PanmirrorConstants;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorCalloutProps;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorCalloutEditProps;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorCalloutEditResult;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.CheckBox;
import com.google.gwt.user.client.ui.HorizontalPanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;


public class PanmirrorEditCalloutDialog extends ModalDialog<PanmirrorCalloutEditResult>
{
   public PanmirrorEditCalloutDialog(boolean removeEnabled,
                                     PanmirrorCalloutEditProps props, 
                                     OperationWithInput<PanmirrorCalloutEditResult> operation)
   {
      super(constants_.calloutCaption(), Roles.getDialogRole(), operation, () -> {
         // cancel returns null
         operation.execute(null);
      });
      
      if (removeEnabled)
      {
         ThemedButton removeAttributesButton = new ThemedButton(constants_.unwrapDivTitle());
         removeAttributesButton.addClickHandler((event) -> {
            PanmirrorCalloutEditResult result = collectInput();
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
         addLeftButton(removeAttributesButton, ElementIds.VISUAL_MD_CALLOUT_REMOVE_BUTTON);
      }
      
      // create attributes editor and tab
      editAttr_ =  new PanmirrorEditAttrWidget();
      editAttr_.setAttr(props.attr, null);
      VerticalTabPanel attributesTab = new VerticalTabPanel(ElementIds.VISUAL_MD_CALLOUT_TAB_ATTRIBUTES);
      attributesTab.addStyleName(RES.styles().dialog());
      attributesTab.add(editAttr_);
      
      VerticalTabPanel calloutTab = new VerticalTabPanel(ElementIds.VISUAL_MD_CALLOUT_TAB_CALLOUT);
      calloutTab.addStyleName(RES.styles().dialog());
      
      HorizontalPanel calloutPanel = new HorizontalPanel();
      calloutPanel.getElement().getStyle().setMarginTop(10, Unit.PX);
      
      // type
      calloutType_ = new SelectWidget(
         constants_.typeLabel(),
         new String[]{"note", "tip", "important", "caution", "warning"}
      );
      calloutType_.setValue(props.callout.type);
      calloutType_.getLabel().getElement().getStyle().setMarginLeft(0, Unit.PX);
      calloutType_.getElement().getStyle().setMarginRight(10, Unit.PX);

      calloutPanel.add(calloutType_);
      
      
      // appearance
      calloutAppearance_ = new SelectWidget(
        constants_.appearanceLabel(),
        new String[] {"default", "simple", "minimal"}
      );
      calloutAppearance_.setValue(props.callout.appearance);
      calloutAppearance_.getElement().getStyle().setMarginRight(10, Unit.PX);
      calloutPanel.add(calloutAppearance_);
      
      calloutCheckBox_ = new FormCheckBox(constants_.showIconLabel(), ElementIds.VISUAL_MD_CALLOUT_ICON);
      calloutCheckBox_.setValue(props.callout.icon);
      calloutPanel.add(calloutCheckBox_);
      
      calloutTab.add(calloutPanel);
      
      // caption
      calloutCaption_  = PanmirrorDialogsUtil.addTextBox(
         calloutTab, 
         ElementIds.VISUAL_MD_CALLOUT_CAPTION, 
         constants_.captionLabel(),
         props.callout.caption
      );
      DomUtils.setPlaceholder(calloutCaption_, constants_.optionalPlaceholder());
      

      
      DialogTabLayoutPanel tabPanel = new DialogTabLayoutPanel(constants_.divTabList());
      tabPanel.addStyleName(RES.styles().divDialogTabs());
      tabPanel.add(calloutTab, constants_.calloutText(), calloutTab.getBasePanelId());
      tabPanel.add(attributesTab, constants_.attributesText(), attributesTab.getBasePanelId());
      tabPanel.selectTab(0);

      mainWidget_ = tabPanel;
   }

   @Override
   protected Widget createMainWidget()
   {
      return mainWidget_;
   }

   @Override
   public void focusInitialControl()
   {
      calloutType_.getListBox().setFocus(true);  
   }

   @Override
   protected PanmirrorCalloutEditResult collectInput()
   {
      PanmirrorCalloutEditResult result = new PanmirrorCalloutEditResult();
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
   protected boolean validate(PanmirrorCalloutEditResult input)
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
   private static final PanmirrorConstants constants_ = GWT.create(PanmirrorConstants.class);

}
