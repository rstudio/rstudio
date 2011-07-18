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
 * \file boost/process/pid_type.hpp
 *
 * Includes the declaration of the pid type.
 */

#ifndef BOOST_PROCESS_PID_TYPE_HPP
#define BOOST_PROCESS_PID_TYPE_HPP

#include <boost/process/config.hpp>

#if defined(BOOST_POSIX_API)
#   include <sys/types.h>
#elif defined(BOOST_WINDOWS_API)
#   include <windows.h>
#endif

namespace boost {
namespace process {

#if defined(BOOST_PROCESS_DOXYGEN)
/**
 * Opaque name for the process identifier type.
 *
 * Each operating system identifies processes using a specific type.
 * The \a pid_type type is used to transparently refer to a process
 * regardless of the operating system.
 *
 * This type is guaranteed to be an integral type on all supported
 * platforms. On POSIX systems it is defined as pid_t, on Windows systems as
 * DWORD.
 */
typedef NativeProcessId pid_type;
#elif defined(BOOST_POSIX_API)
typedef pid_t pid_type;
#elif defined(BOOST_WINDOWS_API)
typedef DWORD pid_type;
#endif

}
}

#endif
