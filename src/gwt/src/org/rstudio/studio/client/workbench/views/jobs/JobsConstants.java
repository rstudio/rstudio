/*
 * JobsConstants.java
 *
 * Copyright (C) 2022 by RStudio, PBC
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
package org.rstudio.studio.client.workbench.views.jobs;

public interface JobsConstants extends com.google.gwt.i18n.client.Messages {

    /**
     * Translated "Connect to Existing Data Sources".
     *
     * @return translated "Local Jobs Still Running"
     */
    @DefaultMessage("Local Jobs Still Running")
    @Key("localJobsRunningCaption")
    String localJobsRunningCaption();

    /**
     * Translated "The Jobs tab cannot be closed while there {0}\n\nWait until all local jobs have completed.".
     *
     * @return translated "The Jobs tab cannot be closed while there {0}\n\nWait until all local jobs have completed."
     */
    @DefaultMessage("The Jobs tab cannot be closed while there {0}\\n\\nWait until all local jobs have completed.")
    @Key("localJobsRunningMessage")
    String localJobsRunningMessage(String localJobMessage);

    /**
     * Translated "are unfinished local jobs.".
     *
     * @return translated "are unfinished local jobs."
     */
    @DefaultMessage("are unfinished local jobs.")
    @Key("localJobsUnfinished")
    String localJobsUnfinished();

    /**
     * Translated "is an unfinished local job.".
     *
     * @return translated "is an unfinished local job."
     */
    @DefaultMessage("is an unfinished local job.")
    @Key("localJobUnfinished")
    String localJobUnfinished();

    /**
     * Translated "Cannot retrieve job output".
     *
     * @return translated "Cannot retrieve job output"
     */
    @DefaultMessage("Cannot retrieve job output")
    @Key("cannotRetrieveJobOutputCaption")
    String cannotRetrieveJobOutputCaption();

    /**
     * Translated "Running".
     *
     * @return translated "Running"
     */
    @DefaultMessage("Running")
    @Key("runningState")
    String runningState();

    /**
     * Translated "Idle".
     *
     * @return translated "Idle"
     */
    @DefaultMessage("Idle")
    @Key("idleState")
    String idleState();

    /**
     * Translated "Cancelled".
     *
     * @return translated "Cancelled"
     */
    @DefaultMessage("Cancelled")
    @Key("cancelledState")
    String cancelledState();

    /**
     * Translated "Failed".
     *
     * @return translated "Failed"
     */
    @DefaultMessage("Failed")
    @Key("failedState")
    String failedState();

    /**
     * Translated "Succeeded".
     *
     * @return translated "Succeeded"
     */
    @DefaultMessage("Succeeded")
    @Key("succeededState")
    String succeededState();

    /**
     * Translated "Unknown {0}".
     *
     * @return translated "Unknown {0}"
     */
    @DefaultMessage("Unknown {0}")
    @Key("unknownState")
    String unknownState(int state);

    /**
     * Translated "Remove Completed Local Jobs".
     *
     * @return translated "Remove Completed Local Jobs"
     */
    @DefaultMessage("Remove Completed Local Jobs")
    @Key("removeCompletedLocalJobsCaption")
    String removeCompletedLocalJobsCaption();

    /**
     * Translated "Are you sure you want to remove completed local jobs from the list of jobs?\n\nOnce removed, local jobs cannot be recovered.".
     *
     * @return translated "Are you sure you want to remove completed local jobs from the list of jobs?\n\nOnce removed, local jobs cannot be recovered."
     */
    @DefaultMessage("Are you sure you want to remove completed local jobs from the list of jobs?\\n\\nOnce removed, local jobs cannot be recovered.")
    @Key("removeCompletedLocalJobsMessage")
    String removeCompletedLocalJobsMessage();

    /**
     * Translated "Remove jobs".
     *
     * @return translated "Remove jobs"
     */
    @DefaultMessage("Remove jobs")
    @Key("removeJobsLabel")
    String removeJobsLabel();

    /**
     * Translated "Cancel".
     *
     * @return translated "Cancel"
     */
    @DefaultMessage("Cancel")
    @Key("cancelLabel")
    String cancelLabel();

    /**
     * Translated "{0} jobs".
     *
     * @return translated "{0} jobs"
     */
    @DefaultMessage("{0} jobs")
    @Key("numJobsLabel")
    String numJobsLabel(int jobs);

    /**
     * Translated "Run Selection as Job".
     *
     * @return translated "Run Selection as Job"
     */
    @DefaultMessage("Run Selection as Job")
    @Key("runSelectionAsJobCaption")
    String runSelectionAsJobCaption();

    /**
     * Translated "Run Script as Local Job".
     *
     * @return translated "Run Script as Local Job"
     */
    @DefaultMessage("Run Script as Local Job")
    @Key("runScriptAsLocalJobCaption")
    String runScriptAsLocalJobCaption();

    /**
     * Translated "Stop job".
     *
     * @return translated "Stop job"
     */
    @DefaultMessage("Stop job")
    @Key("stopJobTitle")
    String stopJobTitle();

    /**
     * Translated "Select Job".
     *
     * @return translated "Select Job"
     */
    @DefaultMessage("Select Job")
    @Key("selectJobText")
    String selectJobText();

    /**
     * Translated "Waiting".
     *
     * @return translated "Waiting"
     */
    @DefaultMessage("Waiting")
    @Key("waitingText")
    String waitingText();

    /**
     * Translated "R Script".
     *
     * @return translated "R Script"
     */
    @DefaultMessage("R Script")
    @Key("rScriptLabel")
    String rScriptLabel();

    /**
     * Translated "Working Directory".
     *
     * @return translated "Working Directory"
     */
    @DefaultMessage("Working Directory")
    @Key("workingDirectoryCaption")
    String workingDirectoryCaption();

    /**
     * Translated "(Don''t copy)".
     *
     * @return translated "(Don''t copy)"
     */
    @DefaultMessage("(Don''t copy)")
    @Key("dontCopyText")
    String dontCopyText();

    /**
     * Translated "To global environment".
     *
     * @return translated "To global environment"
     */
    @DefaultMessage("To global environment")
    @Key("toGlobalEnvironmentText")
    String toGlobalEnvironmentText();

    /**
     * Translated "To results object in global environment".
     *
     * @return translated "To results object in global environment"
     */
    @DefaultMessage("To results object in global environment")
    @Key("toResultObjectText")
    String toResultObjectText();

    /**
     * Translated "Start".
     *
     * @return translated "Start"
     */
    @DefaultMessage("Start")
    @Key("startButtonCaption")
    String startButtonCaption();

    /**
     * Translated "Current selection".
     *
     * @return translated "Current selection"
     */
    @DefaultMessage("Current selection")
    @Key("currentSelectionText")
    String currentSelectionText();

    /**
     * Translated "{0} selection".
     *
     * @return translated "{0} selection"
     */
    @DefaultMessage("{0} selection")
    @Key("selectionText")
    String selectionText(String path);

    /**
     * Translated "Replay job".
     *
     * @return translated "Replay job"
     */
    @DefaultMessage("Replay job")
    @Key("replayJobText")
    String replayJobText();

    /**
     * Translated "The following {0} jobs are still running.".
     *
     * @return translated "The following {0} jobs are still running."
     */
    @DefaultMessage("The following {0} jobs are still running.")
    @Key("jobListLabel")
    String jobListLabel(int count);

    /**
     * Translated "Terminate Running Jobs".
     *
     * @return translated "Terminate Running Jobs"
     */
    @DefaultMessage("Terminate Running Jobs")
    @Key("terminateRunningJobsCaption")
    String terminateRunningJobsCaption();

    /**
     * Translated "Terminate Jobs".
     *
     * @return translated "Terminate Jobs"
     */
    @DefaultMessage("Terminate Jobs")
    @Key("terminateJobsCaption")
    String terminateJobsCaption();

    /**
     * Translated "Jobs Tab".
     *
     * @return translated "Jobs Tab"
     */
    @DefaultMessage("Jobs Tab")
    @Key("jobsTabLabel")
    String jobsTabLabel();

    /**
     * Translated "View all jobs".
     *
     * @return translated "View all jobs"
     */
    @DefaultMessage("View all jobs")
    @Key("viewAllJobsTitle")
    String viewAllJobsTitle();


}
