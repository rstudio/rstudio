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
 * \file boost/process/self.hpp
 *
 * Includes the declaration of the self class.
 */

#ifndef BOOST_PROCESS_SELF_HPP
#define BOOST_PROCESS_SELF_HPP

#include <boost/process/config.hpp>

#if defined(BOOST_POSIX_API)
#   include <boost/scoped_array.hpp>
#   include <errno.h>
#   include <unistd.h>
#   include <limits.h>
#   if defined(__APPLE__)
#       include <crt_externs.h>
#   endif
#elif defined(BOOST_WINDOWS_API)
#   include <windows.h>
#else
#   error "Unsupported platform." 
#endif

#include <boost/process/process.hpp>
#include <boost/process/environment.hpp>
#include <boost/noncopyable.hpp>
#include <boost/assert.hpp>
#include <string>

#if defined(BOOST_POSIX_API)
extern "C"
{
    extern char **environ;
}
#endif

namespace boost {
namespace process {

/**
 * The self class provides access to the process itself.
 */
class self : public process, public boost::noncopyable
{
public:
    /**
     * Returns the self instance representing the caller's process.
     */
    static self &get_instance()
    {
        static self *instance = 0;
        if (!instance)
            instance = new self;
        return *instance;
    }

    /**
     * Returns the current environment.
     *
     * Returns the current process environment variables. Modifying the
     * returned object has no effect on the current environment.
     */
    static environment get_environment()
    {
        environment e;

#if defined(BOOST_POSIX_API)
#   if defined(__APPLE__)
        char **env = *_NSGetEnviron();
#   else
        char **env = environ;
#   endif

        while (*env)
        {
            std::string s = *env;
            std::string::size_type pos = s.find('=');
            e.insert(environment::value_type(s.substr(0, pos),
                s.substr(pos + 1)));
            ++env;
        }
#elif defined(BOOST_WINDOWS_API)
#   ifdef GetEnvironmentStrings
#   undef GetEnvironmentStrings
#   endif

        char *ms_environ = GetEnvironmentStrings();
        if (!ms_environ)
            BOOST_PROCESS_THROW_LAST_SYSTEM_ERROR(
                "GetEnvironmentStrings() failed");
        try
        {
            char *env = ms_environ;
            while (*env)
            {
                std::string s = env;
                std::string::size_type pos = s.find('=');
                e.insert(environment::value_type(s.substr(0, pos),
                    s.substr(pos + 1)));
                env += s.size() + 1;
            }
        }
        catch (...)
        {
            FreeEnvironmentStringsA(ms_environ);
            throw;
        }
        FreeEnvironmentStringsA(ms_environ);
#endif

        return e;
    }

    /**
     * Returns the current work directory.
     */
    static std::string get_work_dir()
    {
#if defined(BOOST_POSIX_API)
#if defined(PATH_MAX)
        char buffer[PATH_MAX];
        char *cwd = buffer;
        long size = PATH_MAX;
#elif defined(_PC_PATH_MAX)
        errno = 0;
        long size = pathconf("/", _PC_PATH_MAX);
        if (size == -1 && errno)
            BOOST_PROCESS_THROW_LAST_SYSTEM_ERROR("pathconf(2) failed");
        else if (size == -1)
            size = BOOST_PROCESS_POSIX_PATH_MAX;
        BOOST_ASSERT(size > 0);
        boost::scoped_array<char> buffer(new char[size]);
        char *cwd = buffer.get();
#else
        char buffer[BOOST_PROCESS_POSIX_PATH_MAX];
        char *cwd = buffer;
        long size = BOOST_PROCESS_POSIX_PATH_MAX;
#endif
        if (!getcwd(cwd, size))
            BOOST_PROCESS_THROW_LAST_SYSTEM_ERROR("getcwd(2) failed");
        BOOST_ASSERT(cwd[0] != '\0');
        return cwd;
#elif defined(BOOST_WINDOWS_API)
        BOOST_ASSERT(MAX_PATH > 0);
        char cwd[MAX_PATH];
        if (!GetCurrentDirectoryA(sizeof(cwd), cwd))
            BOOST_PROCESS_THROW_LAST_SYSTEM_ERROR(
                "GetCurrentDirectory() failed");
        BOOST_ASSERT(cwd[0] != '\0');
        return cwd;
#endif
    }

private:
    /**
     * Constructs a new self object.
     *
     * Creates a new self object that represents the current process.
     */
    self() :
#if defined(BOOST_POSIX_API)
        process(getpid())
#elif defined(BOOST_WINDOWS_API)
        process(GetCurrentProcessId())
#endif
    {
    }
};

}
}

#endif
