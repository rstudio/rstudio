/*
 * RenvActionDialog.java
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
package org.rstudio.studio.client.renv.ui;

import com.google.gwt.core.client.GWT;
import org.rstudio.core.client.StringUtil;
import org.rstudio.core.client.widget.ModalDialog;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.studio.client.renv.RenvConstants;
import org.rstudio.studio.client.workbench.projects.RenvAction;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.JsArray;
import com.google.gwt.user.client.ui.Widget;

public class RenvActionDialog extends ModalDialog<Void>
{
   public RenvActionDialog(String action,
                           JsArray<RenvAction> actions,
                           final OperationWithInput<Void> operation)
   {
      super(
            action + constant_.libraryCaption(),
            Roles.getDialogRole(),
            operation);
      
      widget_ = new RenvActionDialogContents(action, actions);
      
      setOkButtonCaption(StringUtil.capitalize(action));
      setWidth("600px");
   }
   
   @Override
   protected Widget createMainWidget()
   {
      return widget_;
   }
   
   @Override
   protected Void collectInput()
   {
      return null;
   }
   
   
   private final RenvActionDialogContents widget_;

   private static final RenvConstants constant_ = GWT.create(RenvConstants.class);

}
