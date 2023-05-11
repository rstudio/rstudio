package org.rstudio.studio.client.common;

import org.rstudio.core.client.dom.WindowEx;
import org.rstudio.core.client.widget.DialogBuilder;
import org.rstudio.core.client.widget.Operation;
import org.rstudio.core.client.widget.OperationWithInput;
import org.rstudio.core.client.widget.ProgressIndicator;
import org.rstudio.core.client.widget.ProgressOperationWithInput;

import com.google.gwt.user.client.Command;

public class DummyGlobalDisplay extends GlobalDisplay
{

    @Override
    public void openWindow(String url)
    {

    }

    @Override
    public void openWindow(String url, NewWindowOptions options)
    {

    }

    @Override
    public void openProgressWindow(String name,
                                   String message,
                                   OperationWithInput<WindowEx> openOperation)
    {

    }

    @Override
    public void openMinimalWindow(String url, int width, int height)
    {

    }

    @Override
    public void openMinimalWindow(String url,
                                  boolean showLocation,
                                  int width,
                                  int height)
    {

    }

    @Override
    public void openMinimalWindow(String url,
                                  boolean showLocation,
                                  int width,
                                  int height,
                                  NewWindowOptions options)
    {

    }

    @Override
    public void openWebMinimalWindow(String url,
                                     boolean showLocation,
                                     int width,
                                     int height,
                                     NewWindowOptions options)
    {

    }

    @Override
    public void openSatelliteWindow(String name, int width, int height)
    {

    }

    @Override
    public void openSatelliteWindow(String name,
                                    int width,
                                    int height,
                                    NewWindowOptions options)
    {

    }

    @Override
    public void bringWindowToFront(String name)
    {

    }

    @Override
    public void showHtmlFile(String path)
    {

    }

    @Override
    public void showWordDoc(String path)
    {

    }

    @Override
    public void showPptPresentation(String path)
    {

    }

    @Override
    public void openRStudioLink(String linkName, boolean includeVersionInfo)
    {

    }

    @Override
    public Command showProgress(String message)
    {

        return null;
    }

    @Override
    public void showLicenseWarningBar(boolean severe, String message)
    {

    }

    @Override
    public void showWarningBar(boolean severe, String message)
    {

    }

    @Override
    public void hideWarningBar()
    {

    }

    @Override
    public ProgressIndicator getProgressIndicator(String errorCaption)
    {

        return null;
    }

    @Override
    public void promptForText(String title,
                              String label,
                              String initialValue,
                              OperationWithInput<String> operation)
    {

    }

    @Override
    public void promptForText(String title,
                              String label,
                              String initialValue,
                              boolean optional,
                              OperationWithInput<String> operation)
    {

    }

    @Override
    public void promptForText(String title,
                              String label,
                              int type,
                              OperationWithInput<String> operation)
    {

    }

    @Override
    public void promptForText(String title,
                              String label,
                              String initialValue,
                              ProgressOperationWithInput<String> operation)
    {

    }

    @Override
    public void promptForText(String title,
                              String label,
                              String initialValue,
                              int selectionStart,
                              int selectionLength,
                              String okButtonCaption,
                              ProgressOperationWithInput<String> operation)
    {

    }

    @Override
    public void promptForText(String title,
                              String label,
                              String initialValue,
                              int selectionStart,
                              int selectionLength,
                              String okButtonCaption,
                              ProgressOperationWithInput<String> operation,
                              Operation cancelOperation)
    {

    }

    @Override
    public void promptForTextWithOption(String title,
                                        String label,
                                        String initialValue,
                                        int type,
                                        String extraOption,
                                        boolean extraOptionDefault,
                                        ProgressOperationWithInput<PromptWithOptionResult> okOperation,
                                        Operation cancelOperation)
    {

    }

    @Override
    public void promptForInteger(String title,
                                 String label,
                                 Integer initialValue,
                                 ProgressOperationWithInput<Integer> okOperation,
                                 Operation cancelOperation)
    {

    }

    @Override
    protected DialogBuilder createDialog(int type,
                                         String caption,
                                         String message)
    {

        return null;
    }

}
