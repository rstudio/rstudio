/*
 * PanmirrorEditAttrWidget.java
 *
 * Copyright (C) 2020 by RStudio, PBC
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
import org.rstudio.core.client.widget.FormTextArea;
import org.rstudio.core.client.widget.SmallButton;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorAttrEditInput;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorAttrProps;
import org.rstudio.studio.client.panmirror.uitools.PanmirrorUITools;
import org.rstudio.studio.client.panmirror.uitools.PanmirrorUIToolsAttr;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.Style.Unit;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;


public class PanmirrorEditAttrWidget extends SimplePanel
{ 
   public PanmirrorEditAttrWidget()
   {
      uiTools_ = new PanmirrorUITools().attr;
      mainWidget_ = GWT.<Binder>create(Binder.class).createAndBindUi(this);
      setWidget(mainWidget_);
      id_.getElement().setId(ElementIds.VISUAL_MD_ATTR_ID);

      // form controls each have two labels; markup for screen reader
      Roles.getTextboxRole().setAriaLabelledbyProperty(id_.getElement(),
         ElementIds.getAriaElementId(ElementIds.VISUAL_MD_ATTR_ID_LABEL1),
         ElementIds.getAriaElementId(ElementIds.VISUAL_MD_ATTR_ID_LABEL2));
      Roles.getTextboxRole().setAriaLabelledbyProperty(classes_.getElement(),
         ElementIds.getAriaElementId(ElementIds.VISUAL_MD_ATTR_CLASSES_LABEL1),
         ElementIds.getAriaElementId(ElementIds.VISUAL_MD_ATTR_CLASSES_LABEL2));
      Roles.getTextboxRole().setAriaLabelledbyProperty(style_.getElement(),
         ElementIds.getAriaElementId(ElementIds.VISUAL_MD_ATTR_STYLE_LABEL1),
         ElementIds.getAriaElementId(ElementIds.VISUAL_MD_ATTR_STYLE_LABEL2));
      Roles.getTextboxRole().setAriaLabelledbyProperty(attributes_.getElement(),
         ElementIds.getAriaElementId(ElementIds.VISUAL_MD_ATTR_KEYVALUE_LABEL1),
         ElementIds.getAriaElementId(ElementIds.VISUAL_MD_ATTR_KEYVALUE_LABEL2));
      
      // autogen btn 
      Element autogenEl = autogenBtn_.getElement();
      Roles.getButtonRole().set(autogenEl);
      autogenEl.setId(ElementIds.VISUAL_MD_ATTR_ID_GENERATE);
      autogenEl.getStyle().setMarginTop(-4, Unit.PX);
      autogenEl.getStyle().setMarginBottom(1, Unit.PX);
      autogenBtn_.addClickHandler(event -> {
         if (idHint_ != null)
         {
            id_.setText("#" + idHint_);
            id_.setFocus(true);
         }
      });
   }
   
   public void setFocus()
   {
      id_.setFocus(true);
   }
   
   public void setAttr(PanmirrorAttrProps attr, String idHint)
   {  
      PanmirrorAttrEditInput input = uiTools_.propsToInput(attr);
      id_.setText(input.id);
      classes_.setText(input.classes);
      style_.setText(input.style);
      attributes_.setText(input.keyvalue);
      idHint_ = idHint;
      autogenBtn_.setVisible(idHint_ != null);
   }
   
   public PanmirrorAttrProps getAttr()
   {
      PanmirrorAttrEditInput input = new PanmirrorAttrEditInput();
      input.id = id_.getValue().trim();
      input.classes = classes_.getValue().trim();
      input.style = style_.getValue().trim();
      input.keyvalue = attributes_.getValue().trim();
      return uiTools_.inputToProps(input);
   }
 
   private final PanmirrorUIToolsAttr uiTools_;
   
   interface Binder extends UiBinder<Widget, PanmirrorEditAttrWidget> {}
   
   private Widget mainWidget_; 
   
   private String idHint_ = null;
   
   @UiField SmallButton autogenBtn_;
   @UiField TextBox id_;
   @UiField TextBox classes_;
   @UiField TextBox style_;
   @UiField FormTextArea attributes_;
  
}
