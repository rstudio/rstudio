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
import org.rstudio.studio.client.panmirror.PanmirrorUITools;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorAttrEditInput;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorAttrKeyvaluePartitioned;
import org.rstudio.studio.client.panmirror.dialogs.model.PanmirrorAttrProps;

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
      style_.getElement().setId(ElementIds.VISUAL_MD_ATTR_STYLE);
      attributes_.getElement().setId(ElementIds.VISUAL_MD_ATTR_KEYVALUE);
   }
   
   
   public void setAttr(PanmirrorAttrProps attr)
   {
      // partition style out of keyvalue
      PanmirrorAttrProps editProps = new PanmirrorAttrProps();
      editProps.id = attr.id;
      editProps.classes = attr.classes;
      String[] styleKey = {"style"};
      PanmirrorAttrKeyvaluePartitioned keyvalue = uiTools_.attrPartitionKeyvalue(styleKey, attr.keyvalue);
      editProps.keyvalue = keyvalue.base;
      
      PanmirrorAttrEditInput input = uiTools_.attrPropsToInput(editProps);
      id_.setText(input.id);
      classes_.setText(input.classes);
      if (keyvalue.partitioned.length > 0)
         style_.setText(keyvalue.partitioned[0][1]);
      attributes_.setText(input.keyvalue);
   }
   
   public PanmirrorAttrProps getAttr()
   {
      PanmirrorAttrEditInput input = new PanmirrorAttrEditInput();
      input.id = id_.getValue().trim();
      input.classes = classes_.getValue().trim();
      String keyvalue = attributes_.getValue().trim();
      String style = style_.getText().trim();
      if (style.length() > 0) {
         keyvalue = keyvalue + "\nstyle=" + style + "\n"; 
      }
      input.keyvalue = keyvalue;
      return uiTools_.attrInputToProps(input);
   }
 
   private final PanmirrorUITools uiTools_;
   
   interface Binder extends UiBinder<Widget, PanmirrorEditAttrWidget> {}
   
   private Widget mainWidget_; 
   
   @UiField TextBox id_;
   @UiField TextBox classes_;
   @UiField TextBox style_;
   @UiField FormTextArea attributes_;
  
}
