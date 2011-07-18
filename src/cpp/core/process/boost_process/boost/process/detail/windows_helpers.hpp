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
 * \file boost/process/detail/windows_helpers.hpp
 *
 * Includes the declaration of helper functions for Windows systems.
 */

#ifndef BOOST_PROCESS_WINDOWS_HELPERS_HPP
#define BOOST_PROCESS_WINDOWS_HELPERS_HPP

#include <boost/process/config.hpp>
#include <boost/process/environment.hpp>
#include <boost/shared_array.hpp>
#include <string>
#include <vector>
#include <cstddef>
#include <string.h>
#include <windows.h>

namespace boost {
namespace process {
namespace detail {

/**
 * Converts an environment to a string used by CreateProcess().
 *
 * Converts the environment's contents to the format used by the
 * CreateProcess() system call. The returned char* string is
 * allocated in dynamic memory; the caller must free it when not
 * used any more. This is enforced by the use of a shared pointer.
 *
 * This operation is only available on Windows systems.
 *
 * \return A dynamically allocated char* string that represents
 *         the environment's content. This string is of the form
 *         var1=value1\\0var2=value2\\0\\0.
 */
inline boost::shared_array<char> environment_to_windows_strings(environment
    &env)
{
    boost::shared_array<char> envp;

    if (env.empty())
    {
        envp.reset(new char[2]);
        ZeroMemory(envp.get(), 2);
    }
    else
    {
        std::string s;
        for (environment::const_iterator it = env.begin(); it != env.end();
            ++it)
        {
            s += it->first + "=" + it->second;
            s.push_back(0);
        }
        envp.reset(new char[s.size() + 1]);
#if (BOOST_MSVC >= 1400)
        memcpy_s(envp.get(), s.size() + 1, s.c_str(), s.size() + 1);
#else
        memcpy(envp.get(), s.c_str(), s.size() + 1);
#endif
    }

    return envp;
}

/**
 * Converts the command line to a plain string.
 *
 * Converts the command line's list of arguments to the format expected by the
 * \a lpCommandLine parameter in the CreateProcess() system call.
 *
 * This operation is only available on Windows systems.
 *
 * \return A dynamically allocated string holding the command line
 *         to be passed to the executable. It is returned in a
 *         shared_array object to ensure its release at some point.
 */
template <class Arguments>
inline boost::shared_array<char> collection_to_windows_cmdline(const Arguments
    &args)
{
    typedef std::vector<std::string> arguments_t;
    arguments_t args2;
    typename Arguments::size_type i = 0;
    std::size_t size = 0;
    for (typename Arguments::const_iterator it = args.begin(); it != args.end();
        ++it)
    {
        std::string arg = *it;

        std::string::size_type pos = 0;
        while ( (pos = arg.find('"', pos)) != std::string::npos)
        {
            arg.replace(pos, 1, "\\\"");
            pos += 2;
        }

        if (arg.find(' ') != std::string::npos)
            arg = '\"' + arg + '\"';

        if (i++ != args.size() - 1)
            arg += ' ';

        args2.push_back(arg);
        size += arg.size() + 1;
    }

    boost::shared_array<char> cmdline(new char[size]);
    cmdline.get()[0] = '\0';
    for (arguments_t::size_type i = 0; i < args.size(); ++i)
#if (BOOST_MSVC >= 1400)
        strcat_s(cmdline.get(), size, args2[i].c_str());
#else
        strncat(cmdline.get(), args2[i].c_str(), args2[i].size());
#endif

    return cmdline;
}

}
}
}

#endif
