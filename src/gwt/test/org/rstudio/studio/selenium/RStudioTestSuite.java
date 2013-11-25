package org.rstudio.studio.selenium;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ BootRStudio.class, 
                RConsoleInteraction.class })
public class RStudioTestSuite 
{


}
