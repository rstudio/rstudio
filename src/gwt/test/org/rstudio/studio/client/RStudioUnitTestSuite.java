/*
 * RStudioUnitTestSuite.java
 *
 * Copyright (C) 2009-17 by RStudio, Inc.
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
package org.rstudio.studio.client;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import org.rstudio.studio.client.common.r.RTokenizerTests;
import org.rstudio.studio.client.workbench.views.vcs.common.diff.UnifiedParserTest;

@RunWith(Suite.class)
@SuiteClasses({ RTokenizerTests.class,
                UnifiedParserTest.class,
                })
public class RStudioUnitTestSuite 
{

}
