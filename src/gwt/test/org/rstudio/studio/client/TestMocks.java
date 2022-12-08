package org.rstudio.studio.client;

import org.rstudio.studio.client.application.model.DummyApplicationServerOperations;
import org.rstudio.studio.client.common.DummyGlobalDisplay;
import org.rstudio.studio.client.widget.MockDynamicPopupMenuCallback;
import org.rstudio.studio.client.workbench.commands.DummyCommands;

import com.google.gwt.junit.client.GWTTestCase;

public class TestMocks extends GWTTestCase
{
    @Override
    public String getModuleName()
    {
        return "org.rstudio.studio.RStudioTests";
    }

    /*
     * Test Mocks that depend on open source classes that are created to be 
     * used across open source and pro unit tests should be added here.
     * 
     * This test case runs in both open source and pro, and will prevent pro-only tests
     * that depend on these mocks from being broken if any of the mocks' dependencies 
     * change in open source.
     */
    public void testMocks()
    {
        new DummyApplicationServerOperations();
        new DummyGlobalDisplay();
        new DummyCommands();
        new MockDynamicPopupMenuCallback();
    }
}
