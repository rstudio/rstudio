//
// Boost.Process
// ~~~~~~~~~~~~~
//
// Copyright (c) 2006, 2007 Julio M. Merino Vidal
// Copyright (c) 2008 Ilya Sokolov, Boris Schaeling
// Copyright (c) 2009 Boris Schaeling
// Copyright (c) 2010 Felipe Tanus, Boris Schaeling
//
// Distributed under the Boost Software License, Version 1.0. (See accompanying
// file LICENSE_1_0.txt or copy at http://www.boost.org/LICENSE_1_0.txt)
//

/**
 * \file boost/process/process.hpp
 *
 * Includes the declaration of the process class.
 */

#ifndef BOOST_PROCESS_PROCESS_HPP
#define BOOST_PROCESS_PROCESS_HPP

#include <boost/process/config.hpp>

#if defined(BOOST_POSIX_API)
#   include <unistd.h>
#   include <sys/types.h>
#   include <signal.h>
#   include <sys/wait.h>
#   include <errno.h>
#elif defined(BOOST_WINDOWS_API)
#   include <boost/process/handle.hpp>
#   include <cstdlib>
#   include <windows.h>
#else
#   error "Unsupported platform."
#endif

#include <boost/process/pid_type.hpp>

namespace boost {
namespace process {

/**
 * Process class to represent any running process.
 */
class process
{
public:
    /**
     * Constructs a new process object.
     *
     * Creates a new process object that represents a running process
     * within the system.
     *
     * On Windows the process is opened and a handle saved. This is required
     * to avoid the operating system removing process resources when the
     * process exits. The handle is closed when the process instance (and all
     * of its copies) is destroyed.
     */
    process(pid_type id)
        : id_(id)
#if defined(BOOST_WINDOWS_API)
        , handle_(open_process(id))
#endif
    {
    }

#if defined(BOOST_WINDOWS_API) || defined(BOOST_PROCESS_DOXYGEN)
    /**
     * Constructs a new process object.
     *
     * Creates a new process object that represents a running process
     * within the system.
     *
     * This operation is only available on Windows systems. The handle is
     * closed when the process instance (and all of its copies) is destroyed.
     */
    process(handle h)
        : id_(GetProcessId(h.native())),
        handle_(h)
    {
    }
#endif

    /**
     * Returns the process identifier.
     */
    pid_type get_id() const
    {
        return id_;
    }

    /**
     * Terminates the process execution.
     *
     * Forces the termination of the process execution. Some platforms
     * allow processes to ignore some external termination notifications
     * or to capture them for a proper exit cleanup. You can set the
     * \a force flag to true to force their termination regardless
     * of any exit handler.
     *
     * After this call, accessing this object can be dangerous because the
     * process identifier may have been reused by a different process. It
     * might still be valid, though, if the process has refused to die.
     *
     * \throw boost::system::system_error If system calls used to terminate the
     *        process fail.
     */
    void terminate(bool force = false) const
    {
#if defined(BOOST_POSIX_API)
        if (kill(id_, force ? SIGKILL : SIGTERM) == -1)
            BOOST_PROCESS_THROW_LAST_SYSTEM_ERROR("kill(2) failed");
#elif defined(BOOST_WINDOWS_API)
#if defined(BOOST_MSVC)
        force;
#endif
        HANDLE h = OpenProcess(PROCESS_TERMINATE, FALSE, id_);
        if (h == NULL)
            BOOST_PROCESS_THROW_LAST_SYSTEM_ERROR("OpenProcess() failed");
        if (!TerminateProcess(h, EXIT_FAILURE))
        {
            CloseHandle(h);
            BOOST_PROCESS_THROW_LAST_SYSTEM_ERROR("TerminateProcess() failed");
        }
        if (!CloseHandle(h))
            BOOST_PROCESS_THROW_LAST_SYSTEM_ERROR("CloseHandle() failed");
#endif
    }

    /**
     * Blocks and waits for the process to terminate.
     *
     * Returns an exit code. The process object ceases to be valid after this
     * call.
     *
     * \remark Blocking remarks: This call blocks if the process has not
     *         finalized execution and waits until it terminates.
     *
     * \throw boost::system::system_error If system calls used to wait for the
     *        process fail.
     */
    int wait() const
    {
#if defined(BOOST_POSIX_API)
        pid_t p;
        int status;
        do
        {
            p = waitpid(id_, &status, 0);
        } while (p == -1 && errno == EINTR);
        if (p == -1)
            BOOST_PROCESS_THROW_LAST_SYSTEM_ERROR("waitpid(2) failed");
        return status;
#elif defined(BOOST_WINDOWS_API)
        HANDLE h = OpenProcess(PROCESS_QUERY_INFORMATION | SYNCHRONIZE, FALSE,
            id_);
        if (h == NULL) 
            BOOST_PROCESS_THROW_LAST_SYSTEM_ERROR("OpenProcess() failed");
        if (WaitForSingleObject(h, INFINITE) == WAIT_FAILED)
        {
            CloseHandle(h);
            BOOST_PROCESS_THROW_LAST_SYSTEM_ERROR(
                "WaitForSingleObject() failed");
        }
        DWORD exit_code;
        if (!GetExitCodeProcess(h, &exit_code))
        {
            CloseHandle(h);
            BOOST_PROCESS_THROW_LAST_SYSTEM_ERROR(
                "GetExitCodeProcess() failed");
        }
        if (!CloseHandle(h))
            BOOST_PROCESS_THROW_LAST_SYSTEM_ERROR("CloseHandle() failed");
        return exit_code;
#endif
    }

private:
    /**
     * The process identifier.
     */
    pid_type id_;

#if defined(BOOST_WINDOWS_API)
    /**
     * Opens a process and returns a handle.
     *
     * OpenProcess() returns NULL and not INVALID_HANDLE_VALUE on failure.
     * That's why the return value is manually checked in this helper function
     * instead of simply passing it to the constructor of the handle class.
     */
    HANDLE open_process(pid_type id)
    {
        HANDLE h = OpenProcess(PROCESS_QUERY_INFORMATION, FALSE, id);
        if (h == NULL)
            BOOST_PROCESS_THROW_LAST_SYSTEM_ERROR("OpenProcess() failed");
        return h;
    }

    /**
     * The process handle.
     */
    handle handle_;
#endif
};

}
}

#endif
