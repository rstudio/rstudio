/*
 * DiagnosticsHelpLink.java
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

package org.rstudio.studio.client.common;

import com.google.gwt.core.client.GWT;
import org.rstudio.studio.client.workbench.prefs.model.UserPrefsAccessorConstants;

public class DiagnosticsHelpLink extends HelpLink
{
   public DiagnosticsHelpLink()
   {
      super(constants_.codeDiagnosticsLabel(), "code_diagnostics");
   }
   private static final UserPrefsAccessorConstants constants_ = GWT.create(UserPrefsAccessorConstants.class);
}
