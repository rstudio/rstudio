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
 * \file boost/process/stream_ends.hpp
 *
 * Includes the declaration of the stream_ends class.
 */

#ifndef BOOST_PROCESS_STREAM_ENDS_HPP
#define BOOST_PROCESS_STREAM_ENDS_HPP

#include <boost/process/config.hpp>
#include <boost/process/handle.hpp>

namespace boost {
namespace process {

/**
 * A pair of handles to configure streams.
 *
 * Stream behaviors return a pair of handles to specify how a child's stream
 * should be configured and possibly the opposite end of a child's end. This
 * is the end remaining in the parent process and which can be used for example
 * to communicate with a child process through its standard streams.
 */
struct stream_ends {
    /**
     * The child's end.
     */
    handle child;

    /**
     * The parent's end.
     */
    handle parent;

    /**
     * Standard constructor creating two invalid handles.
     */
    stream_ends()
    {
    }

    /**
     * Helper constructor to easily initialize handles.
     */
    stream_ends(handle c, handle p)
    : child(c),
    parent(p)
    {
    }
};

}
}

#endif
