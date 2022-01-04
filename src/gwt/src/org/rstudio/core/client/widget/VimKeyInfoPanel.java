/*
 * VimKeyInfoPanel.java
 *
 * Copyright (C) 2022 by RStudio, PBC
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
package org.rstudio.core.client.widget;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.ui.Widget;
import org.rstudio.core.client.CoreClientConstants;

public class VimKeyInfoPanel extends ShortcutInfoPanel
{
   public VimKeyInfoPanel()
   {
      super(null);
   }
   
   @Override
   protected String getHeaderText()
   {
      return constants_.vimKeyboardShortcutsText();
   }
   
   @Override
   protected Widget getShortcutContent()
   {
     return new VimKeyInfo();
   }
   private static final CoreClientConstants constants_ = GWT.create(CoreClientConstants.class);
}
