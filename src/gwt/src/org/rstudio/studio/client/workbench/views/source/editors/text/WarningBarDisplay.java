/*
 * WarningBarDisplay.java
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
package org.rstudio.studio.client.workbench.views.source.editors.text;

import java.util.List;

import com.google.gwt.user.client.Command;
import com.google.gwt.user.client.ui.IsWidget;

public interface WarningBarDisplay extends IsWidget
{
   void showReadOnlyWarning(List<String> alternatives);
   void showRequiredPackagesMissingWarning(List<String> packages);
   void showTexInstallationMissingWarning(String message);
   void showPanmirrorFormatChanged(Command onReload);
   void showWarningBar(String message);
   void showWarningBar(String message, String actionLabel, Command command);
   
   void hideWarningBar();
}
