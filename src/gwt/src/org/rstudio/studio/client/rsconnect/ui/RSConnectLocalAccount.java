package org.rstudio.studio.client.rsconnect.ui;

import com.google.gwt.core.client.GWT;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Widget;

public class RSConnectLocalAccount extends Composite
{

   private static RSConnectLocalAccountUiBinder uiBinder = GWT
         .create(RSConnectLocalAccountUiBinder.class);

   interface RSConnectLocalAccountUiBinder extends
         UiBinder<Widget, RSConnectLocalAccount>
   {
   }

   public RSConnectLocalAccount()
   {
      initWidget(uiBinder.createAndBindUi(this));
   }

}
