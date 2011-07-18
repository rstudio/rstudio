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
 * \file boost/process/context.hpp
 *
 * Includes the declaration of the context class.
 */

#ifndef BOOST_PROCESS_CONTEXT_HPP
#define BOOST_PROCESS_CONTEXT_HPP

#include <boost/process/config.hpp>

#if defined(BOOST_POSIX_API)
#   include <unistd.h>
#elif defined(BOOST_WINDOWS_API)
#   include <windows.h>
#endif

#include <boost/process/stream_id.hpp>
#include <boost/process/stream_ends.hpp>
#include <boost/process/stream_type.hpp>
#include <boost/process/environment.hpp>
#include <boost/process/self.hpp>
#include <boost/process/stream_behavior.hpp>
#include <boost/function.hpp>
#include <string>
#include <map>

namespace boost {
namespace process {

/**
 * Context class to define how a child process is created.
 *
 * The context class is used to configure streams, to set the work directory
 * and define environment variables. It is also used to change a process
 * name (the variable commonly known as argv[0]).
 */
struct context
{
    typedef std::map<stream_id, boost::function<stream_ends (stream_type)> >
        streams_t;

    /**
     * Streams.
     *
     * Streams of a child process can be configured through factory functions
     * which return a pair of handles - one handle to use as a stream end
     * in the child process and possibly another handle to use as a stream end
     * in the parent process (if a pipe is setup both processes can communicate
     * with each other).
     */
    streams_t streams;

    /**
     * Process name.
     *
     * The child process can access the process name via a variable
     * commonly known as argv[0].
     */
    std::string process_name;

    /**
     * Work directory.
     */
    std::string work_dir;

    /**
     * Environment variables.
     */
    environment env;

    /**
     * Constructs a process context.
     *
     * The default behavior of standard streams is to inherit them. The current
     * work directory is also the work directory of the child process. The child
     * process also inherits all environment variables.
     */
    context()
        : work_dir(self::get_work_dir()),
        env(self::get_environment())
    {
#if defined(BOOST_POSIX_API)
        streams[stdin_id] = behavior::inherit(STDIN_FILENO);
        streams[stdout_id] = behavior::inherit(STDOUT_FILENO);
        streams[stderr_id] = behavior::inherit(STDERR_FILENO);
#elif defined(BOOST_WINDOWS_API)
        streams[stdin_id] = behavior::inherit(GetStdHandle(STD_INPUT_HANDLE));
        streams[stdout_id] = behavior::inherit(GetStdHandle(STD_OUTPUT_HANDLE));
        streams[stderr_id] = behavior::inherit(GetStdHandle(STD_ERROR_HANDLE));
#endif
    }

#if defined(BOOST_POSIX_API) || defined(BOOST_PROCESS_DOXYGEN)
    /**
     * Setups a child process.
     *
     * This is an extension point to support more configuration options for
     * child processes. You can initialize \a setup with a user-defined function
     * which is called when a child process is created.
     *
     * On POSIX platforms setup() is called in the child process. That's why in
     * a multithreaded application only async-signal-safe functions must be
     * called in the function \a setup is bound to.
     *
     * On Windows platforms setup() is called in the parent process. A
     * reference to a STARTUPINFOA structure is passed as parameter.
     */
    boost::function<void ()> setup;
#elif defined(BOOST_WINDOWS_API)
    boost::function<void (STARTUPINFOA&)> setup;
#endif
};

}
}

#endif
