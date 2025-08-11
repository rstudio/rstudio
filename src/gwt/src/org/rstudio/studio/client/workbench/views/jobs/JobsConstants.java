/*
 * JobsConstants.java
 *
 * Copyright (C) 2022 by Posit Software, PBC
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
package org.rstudio.studio.client.workbench.views.jobs;

public interface JobsConstants extends com.google.gwt.i18n.client.Messages {
    String backgroundJobsRunningCaption();
    String backgroundJobsRunningMessage(String localJobMessage);
    String backgroundJobsUnfinished();
    String backgroundJobUnfinished();
    String backgroundJobsTitle();
    String cannotRetrieveJobOutputCaption();
    String runningState();
    String idleState();
    String cancelledState();
    String failedState();
    String succeededState();
    String unknownState(int state);
    String removeCompletedBackgroundJobsCaption();
    String removeCompletedBackgroundJobsMessage();
    String removeJobsLabel();
    String cancelLabel();
    String numJobsLabel(int jobs);
    String runSelectionAsBackgroundJobCaption();
    String runScriptAsBackgroundJobCaption();
    String stopJobTitle();
    String selectJobText();
    String waitingText();
    String rScriptLabel();
    String workingDirectoryCaption();
    String dontCopyText();
    String toGlobalEnvironmentText();
    String toResultObjectText();
    String startButtonCaption();
    String currentSelectionText();
    String selectionText(String path);
    String replayJobText();
    String jobListLabel(int count);
    String terminateRunningJobsCaption();
    String terminateJobsCaption();
    String backgroundJobsTabLabel();
    String viewAllJobsTitle();
    String workbenchJobsTitle();
}
