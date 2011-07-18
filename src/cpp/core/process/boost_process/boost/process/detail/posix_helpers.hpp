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
 * \file boost/process/detail/posix_helpers.hpp
 *
 * Includes the declaration of helper functions for POSIX systems.
 */

#ifndef BOOST_PROCESS_POSIX_HELPERS_HPP
#define BOOST_PROCESS_POSIX_HELPERS_HPP

#include <boost/process/config.hpp>
#include <boost/process/environment.hpp>
#include <string>
#include <utility>
#include <cstring>
#include <cstddef>

namespace boost {
namespace process {
namespace detail {

/**
 * Converts an environment to a char** table as used by execve().
 *
 * Converts the environment's contents to the format used by the
 * execve() system call. The returned char** array is allocated
 * in dynamic memory; the caller must free it when not used any
 * more. Each entry is also allocated in dynamic memory and is a
 * NULL-terminated string of the form var=value; these must also be
 * released by the caller.
 *
 * This operation is only available on POSIX systems.
 *
 * \return The first argument of the pair is an integer that indicates
 *         how many strings are stored in the second argument. The
 *         second argument is a NULL-terminated, dynamically allocated
 *         array of dynamically allocated strings representing the
 *         enviroment's content. Each array entry is a NULL-terminated
 *         string of the form var=value. The caller is responsible for
 *         freeing them.
 */
inline std::pair<std::size_t, char**> environment_to_envp(const environment
    &env)
{
    std::size_t nargs = env.size();
    char **envp = new char*[nargs + 1];
    environment::size_type i = 0;
    for (environment::const_iterator it = env.begin(); it != env.end(); ++it)
    {
        std::string s = it->first + "=" + it->second;
        envp[i] = new char[s.size() + 1];
        std::strncpy(envp[i], s.c_str(), s.size() + 1);
        ++i;
    }
    envp[i] = 0;
    return std::pair<std::size_t, char**>(nargs, envp);
}

/**
 * Converts the command line to an array of C strings.
 *
 * Converts the command line's list of arguments to the format expected
 * by the \a argv parameter in the POSIX execve() system call.
 *
 * This operation is only available on POSIX systems.
 *
 * \return The first argument of the pair is an integer that indicates
 *         how many strings are stored in the second argument. The
 *         second argument is a NULL-terminated, dynamically allocated
 *         array of dynamically allocated strings holding the arguments
 *         to the executable. The caller is responsible for freeing them.
 */
template <class Arguments>
inline std::pair<std::size_t, char**> collection_to_argv(const Arguments &args)
{
    std::size_t nargs = args.size();
    char **argv = new char*[nargs + 1];
    typename Arguments::size_type i = 0;
    for (typename Arguments::const_iterator it = args.begin(); it != args.end();
        ++it)
    {
        argv[i] = new char[it->size() + 1];
        std::strncpy(argv[i], it->c_str(), it->size() + 1);
        ++i;
    }
    argv[nargs] = 0;
    return std::pair<std::size_t, char**>(nargs, argv);
}

}
}
}

#endif
