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
 * \file boost/process/pistream.hpp
 *
 * Includes the declaration of the pistream class.
 */

#ifndef BOOST_PROCESS_PISTREAM_HPP
#define BOOST_PROCESS_PISTREAM_HPP

#include <boost/process/handle.hpp>
#include <boost/process/detail/systembuf.hpp>
#include <boost/noncopyable.hpp>
#include <istream>

namespace boost {
namespace process {

/**
 * Child process' output stream.
 *
 * The pistream class represents an output communication channel with the
 * child process. The child process writes data to this stream and the
 * parent process can read it through the pistream object. In other
 * words, from the child's point of view, the communication channel is an
 * output one, but from the parent's point of view it is an input one;
 * hence the confusing pistream name.
 *
 * pistream objects cannot be copied because they buffer data
 * that flows through the communication channel.
 *
 * A pistream object behaves as a std::istream stream in all senses.
 * The class is only provided because it must provide a method to let
 * the caller explicitly close the communication channel.
 *
 * \remark Blocking remarks: Functions that read data from this
 *         stream can block if the associated handle blocks during
 *         the read. As this class is used to communicate with child
 *         processes through anonymous pipes, the most typical blocking
 *         condition happens when the child has no more data to send to
 *         the pipe's system buffer. When this happens, the buffer
 *         eventually empties and the system blocks until the writer
 *         generates some data.
 */
class pistream : public std::istream, public boost::noncopyable
{
public:
    /**
     * Creates a new process' output stream.
     */
    explicit pistream(boost::process::handle h)
        : std::istream(0),
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
     * function can be used by the user to tell the child process it's
     * not willing to receive more data.
     */
    void close()
    {
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
