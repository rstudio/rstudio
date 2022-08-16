/*
 * ConsoleProgressWidget.java
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
package org.rstudio.studio.client.workbench.views.vcs.common;

import com.google.gwt.aria.client.Roles;
import com.google.gwt.core.client.GWT;
import org.rstudio.studio.client.RStudioGinjector;
import org.rstudio.studio.client.application.AriaLiveService;
import org.rstudio.studio.client.common.shell.ShellDisplay;
import org.rstudio.studio.client.common.shell.ShellWidget;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;
import org.rstudio.studio.client.workbench.views.vcs.ViewVcsConstants;

public class ConsoleProgressWidget extends ShellWidget implements ShellDisplay
{
   public ConsoleProgressWidget()
   {
      super(new AceEditor(), null, null, null, null);
      getEditor().setInsertMatching(false);
      getEditor().setTextInputAriaLabel(constants_.progressDetails());
      if (!RStudioGinjector.INSTANCE.getAriaLiveService().isDisabled(AriaLiveService.PROGRESS_LOG))
         Roles.getLogRole().set(getOutputWidget().getElement());
   }
   
   private AceEditor getEditor()
   {
      return input_;
   }
   private static final ViewVcsConstants constants_ = GWT.create(ViewVcsConstants.class);
}
