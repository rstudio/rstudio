/*
 * ApplicationUtilsTests.java
 *
 * Copyright (C) 2023 by Posit Software, PBC
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

package org.rstudio.studio.client.application;

import com.google.gwt.junit.client.GWTTestCase;

public class ApplicationUtilsTests extends GWTTestCase {

    @Override
    public String getModuleName() {
        return "org.rstudio.studio.RStudioTests";
    }

    public void testCompareVersions() {
        // RStudio IDE Versions
        assertTrue(ApplicationUtils.compareVersions("2022.12.0+353", "2023.06.1+524") < 0);
        assertTrue(ApplicationUtils.compareVersions("2021.09.3+396", "2021.09.3+396") == 0);
        assertTrue(ApplicationUtils.compareVersions("2022.03.2+454", "2022.03.2+455") < 0);
        assertTrue(ApplicationUtils.compareVersions("2022.07.2+576", "2022.07.1+554") > 0);
        assertTrue(ApplicationUtils.compareVersions("1.4.1743-4", "2023.06.1+524") < 0);

        // R Versions
        assertTrue(ApplicationUtils.compareVersions("3.6.0", "3.5.3") > 0);
        assertTrue(ApplicationUtils.compareVersions("4.3.2", "4.3.2") == 0);
        assertTrue(ApplicationUtils.compareVersions("3.5.3", "4.2.1") < 0);

        // Other Version Formats
        assertTrue(ApplicationUtils.compareVersions("1.2.3", "0.0") > 0);
        assertTrue(ApplicationUtils.compareVersions("0.0", "0.0") == 0);
        assertTrue(ApplicationUtils.compareVersions("0", "0.0") == 0);
        assertTrue(ApplicationUtils.compareVersions("0.0", "1.2.3") < 0);
    }
}
