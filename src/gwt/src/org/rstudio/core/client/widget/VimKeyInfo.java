package org.rstudio.core.client.widget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.Element;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.ui.UIObject;

public class VimKeyInfo extends UIObject
{

   private static VimKeyInfoUiBinder uiBinder = GWT
         .create(VimKeyInfoUiBinder.class);

   interface VimKeyInfoUiBinder extends UiBinder<Element, VimKeyInfo>
   {
   }

   public VimKeyInfo()
   {
      setElement(uiBinder.createAndBindUi(this));
   }

}
