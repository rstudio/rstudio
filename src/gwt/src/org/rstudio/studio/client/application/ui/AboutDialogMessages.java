package org.rstudio.studio.client.application.ui;

import com.google.gwt.i18n.client.Messages;

public interface AboutDialogMessages extends Messages {
    @DefaultMessage("|About {0}")
    String title(String version);
}
