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
 * \file boost/process/handle.hpp
 *
 * Includes the declaration of the handle class.
 */

#ifndef BOOST_PROCESS_HANDLE_HPP
#define BOOST_PROCESS_HANDLE_HPP

#include <boost/process/config.hpp>

#if defined(BOOST_POSIX_API)
#   include <unistd.h>
#elif defined(BOOST_WINDOWS_API)
#   include <windows.h>
#else
#   error "Unsupported platform."
#endif

#include <boost/shared_ptr.hpp>
#include <boost/make_shared.hpp>

namespace boost {
namespace process {

/**
 * RAII model for handles.
 *
 * The \a handle class is a RAII model for native handles. This class wraps
 * one of such handles grabbing its ownership, and automaticaly closes it
 * upon destruction. It is used to avoid leaking open handles, shall an
 * unexpected execution trace occur.
 */
class handle
{
public:
#if defined(BOOST_PROCESS_DOXYGEN)
    /**
     * Opaque name for the native handle type.
     *
     * On POSIX systems \a NativeSystemHandle is an integer type while it is
     * a \a HANDLE on Windows systems.
     */
    typedef NativeSystemHandle native_type;
#elif defined(BOOST_POSIX_API)
    typedef int native_type;
#elif defined(BOOST_WINDOWS_API)
    typedef HANDLE native_type;
#endif

    /**
     * Constructs an invalid handle.
     *
     * \see valid()
     */
    handle()
    {
    }

    /**
     * RAII settings to specify if handle should be automatically closed.
     */
    enum close_type { do_close, dont_close };

    /**
     * Constructs a handle from a native handle.
     *
     * This constructor creates a new \a handle object that takes
     * ownership of the given \a native handle. If \a close is set to
     * handle::dont_close the \a native handle is not closed upon destruction.
     * The user must not close \a native if it is owned by a \a handle object.
     * Ownership can be reclaimed using release().
     *
     * \see release()
     */
    handle(native_type native, close_type close = handle::do_close)
    : impl_(boost::make_shared<impl>(native, close))
    {
    }

    /**
     * Checks whether the handle is valid or not.
     *
     * \return true if the handle is valid; false otherwise.
     */
    bool valid() const
    {
        return impl_ && impl_->valid();
    }

    /**
     * Closes the handle.
     *
     * \post The handle is invalid.
     * \post The native handle is closed.
     */
    void close()
    {
        if (impl_)
            impl_->close();
    }

    /**
     * Gets the native handle.
     *
     * The caller can issue any operation on it except closing it.
     * If closing is required, release() shall be used.
     *
     * \return The native handle.
     */
    native_type native() const
    {
        return impl_ ? impl_->native() : invalid_handle();
    }

    /**
     * Reclaims ownership of the native handle.
     *
     * The caller is responsible of closing the native handle.
     *
     * \post The handle is invalid.
     * \return The native handle.
     */
    native_type release()
    {
        return impl_ ? impl_->release() : invalid_handle();
    }

private:
    class impl
    {
    public:
        typedef handle::native_type native_type;

        impl(native_type native, close_type close)
        : native_(native),
        close_(close)
        {
        }

        ~impl()
        {
            if (valid() && close_ == handle::do_close)
            {
#if defined(BOOST_POSIX_API)
                ::close(native_);
#elif defined(BOOST_WINDOWS_API)
                CloseHandle(native_);
#endif
            }
        }

        bool valid() const
        {
            return native_ != handle::invalid_handle();
        }

        void close()
        {
            if (valid())
            {
#if defined(BOOST_POSIX_API)
                ::close(native_);
#elif defined(BOOST_WINDOWS_API)
                CloseHandle(native_);
#endif
                native_ = handle::invalid_handle();
            }
        }

        native_type native() const
        {
            return native_;
        }

        native_type release()
        {
            native_type native = native_;
            native_ = handle::invalid_handle();
            return native;
        }

    private:
        native_type native_;
        close_type close_;
    };

    /**
     * Implementation of handle to store native handle value.
     *
     * A shared pointer is used as handles represent system resources. If a
     * handle is closed and becomes invalid the state of copies of the handle
     * object will be updated as they all share the handle implementation.
     */
    boost::shared_ptr<impl> impl_;

    /**
     * Constant function representing an invalid handle value.
     *
     * Returns the platform-specific handle value that represents an
     * invalid handle. This is a constant function rather than a regular
     * constant because, in the latter case, we cannot define it under
     * Windows due to the value being of a complex type.
     */
    static const native_type invalid_handle()
    {
#if defined(BOOST_POSIX_API)
        return -1;
#elif defined(BOOST_WINDOWS_API)
        return INVALID_HANDLE_VALUE;
#endif
    }
};

}
}

#endif
