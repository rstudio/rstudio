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



import org.rstudio.core.client.ElementIds;
import org.rstudio.core.client.widget.FormTextArea;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorAttrEditInput;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorAttrProps;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorUITools;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.user.client.ui.SimplePanel;
import com.google.gwt.user.client.ui.TextBox;
import com.google.gwt.user.client.ui.Widget;


public class PanmirrorEditAttrWidget extends SimplePanel
{ 
   public PanmirrorEditAttrWidget()
   {
      uiTools_ = new PanmirrorUITools();
      mainWidget_ = GWT.<Binder>create(Binder.class).createAndBindUi(this);
      setWidget(mainWidget_);
      id_.getElement().setId(ElementIds.VISUAL_MD_ATTR_ID);
      classes_.getElement().setId(ElementIds.VISUAL_MD_ATTR_CLASSES);
      attributes_.getElement().setId(ElementIds.VISUAL_MD_ATTR_KEYVALUE);
   }
   
   
   public void setAttr(PanmirrorAttrProps attr)
   {
      PanmirrorAttrEditInput input = uiTools_.attrPropsToInput(attr);
      id_.setText(input.id);
      classes_.setText(input.classes);
      attributes_.setText(input.keyvalue);
   }
   
   public PanmirrorAttrProps getAttr()
   {
      PanmirrorAttrEditInput input = new PanmirrorAttrEditInput();
      input.id = id_.getValue().trim();
      input.classes = classes_.getValue().trim();
      input.keyvalue = attributes_.getValue().trim();
      return uiTools_.attrInputToProps(input);
   }
 
   private final PanmirrorUITools uiTools_;
   
   interface Binder extends UiBinder<Widget, PanmirrorEditAttrWidget> {}
   
   private Widget mainWidget_; 
   
   @UiField TextBox id_;
   @UiField TextBox classes_;
   @UiField FormTextArea attributes_;
  
}
