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
 * \file boost/process/postream.hpp
 *
 * Includes the declaration of the postream class.
 */

#ifndef BOOST_PROCESS_POSTREAM_HPP
#define BOOST_PROCESS_POSTREAM_HPP

#include <boost/process/handle.hpp>
#include <boost/process/detail/systembuf.hpp>
#include <boost/noncopyable.hpp>
#include <ostream>

namespace boost {
namespace process {

/**
 * Child process' input stream.
 *
 * The postream class represents an input communication channel with the
 * child process. The child process reads data from this stream and the
 * parent process can write to it through the postream object. In other
 * words, from the child's point of view, the communication channel is an
 * input one, but from the parent's point of view it is an output one;
 * hence the confusing postream name.
 *
 * postream objects cannot be copied because they buffer data that flows
 * through the communication channel.
 *
 * A postream object behaves as a std::ostream stream in all senses.
 * The class is only provided because it must provide a method to let
 * the caller explicitly close the communication channel.
 *
 * \remark Blocking remarks: Functions that write data to this
 *         stream can block if the associated handle blocks during
 *         the write. As this class is used to communicate with child
 *         processes through anonymous pipes, the most typical blocking
 *         condition happens when the child is not processing the data
 *         in the pipe's system buffer. When this happens, the buffer
 *         eventually fills up and the system blocks until the reader
 *         consumes some data, leaving some new room.
 */
class postream : public std::ostream, public boost::noncopyable
{
public:
    /**
     * Creates a new process' input stream.
     */
    explicit postream(boost::process::handle h)
        : std::ostream(0),
          handle_(h),
          systembuf_(handle_.native())
    {
        rdbuf(&systembuf_);
    }

    /**
     * Returns the handle managed by this stream.
     */
    const boost::process::handle &handle() const
    {
        return handle_;
    }

    /**
     * Returns the handle managed by this stream.
     */
    boost::process::handle &handle()
    {
        return handle_;
    }

    /**
     * Closes the handle managed by this stream.
     *
     * Explicitly closes the handle managed by this stream. This
     * function can be used by the user to tell the child process there
     * is no more data to send.
     */
    void close()
    {
        systembuf_.sync();
        handle_.close();
    }

private:
    /**
     * The handle managed by this stream.
     */
    boost::process::handle handle_;

    /**
     * The systembuf object used to manage this stream's data.
     */
    detail::systembuf systembuf_;
};

}
}

#endif
