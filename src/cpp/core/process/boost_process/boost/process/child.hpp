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
 * \file boost/process/child.hpp
 *
 * Includes the declaration of the child class.
 */

#ifndef BOOST_PROCESS_CHILD_HPP
#define BOOST_PROCESS_CHILD_HPP

#include <boost/process/config.hpp>

#if defined(BOOST_POSIX_API)
#elif defined(BOOST_WINDOWS_API)
#   include <windows.h>
#else
#   error "Unsupported platform."
#endif

#include <boost/process/process.hpp>
#include <boost/process/pid_type.hpp>
#include <boost/process/stream_id.hpp>
#include <boost/process/handle.hpp>
#include <map>

namespace boost {
namespace process {

/**
 * The child class provides access to a child process.
 */
class child : public process
{
public:
    /**
     * Creates a new child object that represents the just spawned child
     * process \a id.
     */
    child(pid_type id, std::map<stream_id, handle> handles)
        : process(id),
        handles_(handles)
    {
    }

#if defined(BOOST_WINDOWS_API)
    /**
     * Creates a new child object that represents the just spawned child
     * process \a id.
     *
     * This operation is only available on Windows systems.
     */
    child(handle hprocess, std::map<stream_id, handle> handles)
        : process(hprocess),
        handles_(handles)
    {
    }
#endif

    /**
     * Gets a handle to a stream attached to the child.
     *
     * If the handle doesn't exist an invalid handle is returned.
     */
    handle get_handle(stream_id id) const
    {
        std::map<stream_id, handle>::const_iterator it = handles_.find(id);
        return (it != handles_.end()) ? it->second : handle();
    }

private:
    /**
     * Handles providing access to streams attached to the child process.
     */
    std::map<stream_id, handle> handles_;
};

}
}

#endif
