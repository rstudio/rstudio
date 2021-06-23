/*
    This create doesn't include a user ID, reason is that
    open source doesn't have a user table to reference, and
    since open source server only allows for one session, it's
    not a problem.
*/
CREATE TABLE active_session_metadata (
    session_id TEXT primary key,
    user_id INT,
    workbench TEXT,
    created TEXT,
    last_used TEXT,
    r_version TEXT,
    r_version_label TEXT,
    project TEXT,
    working_dir TEXT,
    activity_state TEXT,
    label TEXT,
    launch_parameters TEXT,
    r_version_home TEXT,
    save_prompt_required TEXT,
    suspended_size_kb INT
);