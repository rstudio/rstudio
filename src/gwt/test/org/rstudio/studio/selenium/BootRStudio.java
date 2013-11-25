package org.rstudio.studio.selenium;

import java.net.MalformedURLException;
import java.net.URL;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import static org.junit.Assert.*;

import org.junit.Test; 

public class BootRStudio  {

   @Test
   public void testRStudioBoot() throws MalformedURLException {
       WebDriver driver = new RemoteWebDriver(
             new URL("http://localhost:9515/"), DesiredCapabilities.chrome());
       
       driver.get("http://localhost:8787/");

       // Check the title of the page
       assertEquals(driver.getTitle(), "RStudio");

       // Close the browser
       driver.quit();
   }
}
