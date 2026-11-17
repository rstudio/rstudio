/*
 * ChooseRVersionDialogTests.java
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
package org.rstudio.studio.client.renv.ui;

import java.util.ArrayList;
import java.util.List;

import org.rstudio.studio.client.renv.RVersionChooser;
import org.rstudio.studio.client.renv.model.RVersionInstall;

import com.google.gwt.junit.client.GWTTestCase;

// The dialog starts from the R chosen before, so that accepting it as it
// opens never replaces that choice with another R.
public class ChooseRVersionDialogTests extends GWTTestCase
{
   @Override
   public String getModuleName()
   {
      return "org.rstudio.studio.RStudioTests";
   }

   public void testStartsFromTheChosenR()
   {
      RVersionChooser.Choice chosen = new RVersionChooser.Choice(installation("4.4.1", false), false);
      RVersionChooser.Choice start = startingChoice(installation("4.5.0", true), chosen);

      assertFalse(start.useDefault);
      assertEquals(binary("4.4.1"), start.installation.binary);
   }

   public void testStartsFromTheDefaultEntryForAChosenRThatIsNowTheDefault()
   {
      // the chosen R's own entry is left out, as it is the default R
      RVersionChooser.Choice chosen = new RVersionChooser.Choice(installation("4.4.1", false), false);
      RVersionChooser.Choice start = startingChoice(installation("4.4.1", false), chosen);

      assertTrue(start.useDefault);
      assertEquals(binary("4.4.1"), start.installation.binary);
   }

   public void testOffersAChosenRFoundNowhereElse()
   {
      RVersionInstall browsed = new RVersionInstall();
      browsed.version = "4.3.3";
      browsed.binary = "/home/user/R-4.3.3/bin/R";
      RVersionChooser.Choice chosen = new RVersionChooser.Choice(browsed, false);

      RVersionChooser.Choice start = startingChoice(installation("4.5.0", true), chosen);

      assertFalse(start.useDefault);
      assertEquals("/home/user/R-4.3.3/bin/R", start.installation.binary);
   }

   public void testStartsFromTheRInUseWithNothingChosen()
   {
      RVersionChooser.Choice start = startingChoice(null, null);

      assertFalse(start.useDefault);
      assertEquals(binary("4.5.0"), start.installation.binary);
   }

   // The choice the dialog starts from, offering the given default R and the
   // installations 4.5.0 (the R in use) and 4.4.1.
   private static RVersionChooser.Choice startingChoice(RVersionInstall defaultR,
                                                        RVersionChooser.Choice selected)
   {
      List<RVersionInstall> installed = new ArrayList<>();
      installed.add(installation("4.5.0", true));
      installed.add(installation("4.4.1", false));

      ChooseRVersionDialog dialog = new ChooseRVersionDialog(
            "Choose R Version",
            "OK",
            defaultR,
            installed,
            selected,
            onChosen -> {},
            input -> {});
      return dialog.collectInput();
   }

   private static RVersionInstall installation(String version, boolean current)
   {
      RVersionInstall installation = new RVersionInstall();
      installation.version = version;
      installation.path = "/opt/R/" + version;
      installation.home = "/opt/R/" + version + "/lib/R";
      installation.binary = binary(version);
      installation.orthogonal = true;
      installation.current = current;
      return installation;
   }

   private static String binary(String version)
   {
      return "/opt/R/" + version + "/bin/R";
   }
}
