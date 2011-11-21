/*
 * PosixShellWidget.java
 *
 * Copyright (C) 2009-11 by RStudio, Inc.
 *
 * This program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.common.posixshell;

import org.rstudio.studio.client.common.shell.ShellWidget;
import org.rstudio.studio.client.workbench.views.source.editors.text.AceEditor;

import com.google.gwt.core.client.GWT;
import com.google.gwt.resources.client.ClientBundle;
import com.google.gwt.resources.client.CssResource;
import com.google.inject.Inject;

public class PosixShellWidget extends ShellWidget implements PosixShell.Display
{
   @Inject
   public PosixShellWidget(AceEditor editor)
   {
      super(editor);
      
      addStyleName(RES.styles().shellWidget());
     
   }
   
   
   
   public static void ensureStylesInjected()
   {
      RES.styles().ensureInjected();
   }

   interface Resources extends ClientBundle
   {
      @Source("PosixShellWidget.css")
      Styles styles();

   }

   interface Styles extends CssResource
   {
      String shellWidget();
   }
   
   static Resources RES = GWT.create(Resources.class);

}
