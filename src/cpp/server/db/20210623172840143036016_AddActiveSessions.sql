/* Stores active session metadata */
CREATE TABLE active_session_metadata (
    /* The ID of the session */
    session_id TEXT primary key,

    /* The ID of the user who owns this session (FK to the auto-incrementing PK of the users table) */
    user_id INT,

    /* The type of workbench that was launched (rstudio|vscode|jupyternote|jupyterlab) */
    workbench TEXT,

    /* The date and time at which the session was created */
    created TEXT,

    /* The date and time at which the session was last used */
    last_used TEXT,

    /* The version of R in use, if the workbench is rstudio */
    r_version TEXT,

    /* The label of the R version, for display in the UI, if the workbench is rstudio */
    r_version_label TEXT,

    /* The open project of the session, if the workbench is rstudio */
    project TEXT,

    /* The working directory of the session */
    working_dir TEXT,

    /* The state of the session (initializing|running|executing|suspended) */
    activity_state TEXT,

    /* The label of the session, for display in the UI */
    label TEXT,

    /* The parameters with which the session was launched, in JSON format */
    launch_parameters TEXT,

    /* The R Home of the R version in use by the session, if the workbench is rstudio */
    r_version_home TEXT,

    /* Whether a save prompt is required on exit of the session, if the workbench is rstudio */
    save_prompt_required TEXT,

    /* The size of the suspended session in kilobytes */
    suspended_size_kb INT
);