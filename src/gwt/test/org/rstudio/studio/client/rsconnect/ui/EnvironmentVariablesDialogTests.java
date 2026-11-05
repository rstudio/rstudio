/*
 * EnvironmentVariablesDialogTests.java
 *
 * Copyright (C) 2026 by Posit Software, PBC
 *
 * Unless you have received this program directly from Posit Software pursuant
 * to the terms of a commercial license agreement with Posit Software, then
 * this program is licensed to you under the terms of version 3 of the
 * GNU Affero General Public License. This program is distributed WITHOUT
 * ANY EXPRESS OR IMPLIED WARRANTY, INCLUDING THOSE OF NON-INFRINGEMENT,
 * MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE. Please refer to the
 * AGPL (http://www.gnu.org/licenses/agpl-3.0.txt) for more details.
 *
 */
package org.rstudio.studio.client.rsconnect.ui;

import static org.rstudio.core.client.dom.LabelTestHelper.labelTextFor;

import java.util.Arrays;

import org.rstudio.studio.client.rsconnect.ui.RSConnectDeploy.EnvironmentVariablesDialog;

import com.google.gwt.dom.client.Element;
import com.google.gwt.dom.client.NodeList;
import com.google.gwt.junit.client.GWTTestCase;

public class EnvironmentVariablesDialogTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   public void testVariableListIsLabelledByInstructions()
   {
      EnvironmentVariablesDialog dialog = new EnvironmentVariablesDialog(
            "document", Arrays.asList("API_KEY", "DB_URL"), null, null);
      Element content = dialog.createMainWidget().getElement();

      NodeList<Element> selects = content.getElementsByTagName("select");
      assertEquals("the dialog should hold one variable list", 1, selects.getLength());
            assertEquals(
            "Select one or more environment variables to publish with this document.",
            labelTextFor(content, selects.getItem(0)));
   }
}
