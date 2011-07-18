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
 * \file boost/process/detail/basic_status_service.hpp
 *
 * Includes the declaration of the basic status service class.
 */

#ifndef BOOST_PROCESS_DETAIL_BASIC_STATUS_SERVICE_HPP
#define BOOST_PROCESS_DETAIL_BASIC_STATUS_SERVICE_HPP

#include <boost/process/config.hpp>

#if defined(BOOST_POSIX_API)
#   include <boost/process/operations.hpp>
#   include <string>
#   include <sys/types.h>
#   include <sys/wait.h>
#elif defined(BOOST_WINDOWS_API)
#   include <windows.h>
#else
#   error "Unsupported platform."
#endif

#include <boost/process/pid_type.hpp>
#include <boost/process/detail/status_impl.hpp>
#include <boost/asio.hpp>
#include <boost/thread.hpp>
#include <boost/shared_ptr.hpp>
#include <boost/make_shared.hpp>
#include <boost/scoped_ptr.hpp>
#include <boost/system/error_code.hpp>
#include <boost/unordered_map.hpp>
#include <vector>
#include <algorithm>

namespace boost {
namespace process {
namespace detail {

/**
 * The basic_status_service class provides the service to wait for processes
 * synchronously and asynchronously.
 */
template <typename StatusImplementation = status_impl>
class basic_status_service
    : public boost::asio::detail::service_base<StatusImplementation>
{
public:
    explicit basic_status_service(boost::asio::io_service &io_service)
        : boost::asio::detail::service_base<StatusImplementation>(io_service),
#if defined(BOOST_POSIX_API)
        interrupt_pid_(-1),
        pids_(0)
#elif defined(BOOST_WINDOWS_API)
        run_(true)
#endif
    {
#if defined(BOOST_WINDOWS_API)
        handles_.push_back(CreateEvent(NULL, FALSE, FALSE, NULL));
        if (handles_[0] == NULL)
            BOOST_PROCESS_THROW_LAST_SYSTEM_ERROR("CreateEvent() failed");
        work_thread_ = boost::thread(
            &basic_status_service<StatusImplementation>::work_thread, this);
#endif
    }

    ~basic_status_service()
    {
#if defined(BOOST_POSIX_API)
        boost::unique_lock<boost::mutex> lock(work_thread_mutex_);
        bool worker_thread_active = (pids_ != 0);
        lock.unlock();
        if (worker_thread_active)
        {
            stop_work_thread();
            work_thread_.join();
        }
#elif defined(BOOST_WINDOWS_API)
        stop_work_thread();
        work_thread_.join();
        CloseHandle(handles_[0]);
#endif
    }

    typedef boost::shared_ptr<StatusImplementation> implementation_type;

    void construct(implementation_type &impl)
    {
        impl = boost::make_shared<StatusImplementation>();
        boost::unique_lock<boost::mutex> lock(work_thread_mutex_);
        impls_.push_back(impl);
    }

    void destroy(implementation_type &impl)
    {
        boost::unique_lock<boost::mutex> lock(work_thread_mutex_);
        typename std::vector<implementation_type>::iterator it =
            std::find(impls_.begin(), impls_.end(), impl);
        if (it != impls_.end())
            impls_.erase(it);
#if defined(BOOST_WINDOWS_API)
        interrupt_work_thread();
        work_thread_cond_.wait(work_thread_mutex_);
        impl->clear(handles_);
        work_thread_cond_.notify_all();
#endif
        impl.reset();
    }

    int wait(implementation_type &impl, pid_type pid)
    {
        boost::system::error_code ec;
        int status = impl->wait(pid, ec);
#if defined(BOOST_POSIX_API)
        if (ec.value() == ECHILD)
        {
            boost::unique_lock<boost::mutex> lock(work_thread_mutex_);
            boost::unordered_map<pid_t, int>::iterator it = statuses_.find(pid);
            if (it == statuses_.end())
            {
                work_thread_cond_.wait(work_thread_mutex_);
                it = statuses_.find(pid);
            }
            if (it != statuses_.end())
            {
                status = it->second;
                statuses_.erase(it);
                ec.clear();
            }
        }
#endif
        boost::asio::detail::throw_error(ec);
        return status;
    }

    template <typename Handler>
    void async_wait(implementation_type &impl, pid_type pid, Handler handler)
    {
#if defined(BOOST_POSIX_API)
        boost::unique_lock<boost::mutex> lock(work_thread_mutex_);
        if (++pids_ == 1)
        {
            work_.reset(new boost::asio::io_service::work(
                this->get_io_service()));
            work_thread_ = boost::thread(
                &basic_status_service<StatusImplementation>::work_thread,
                this);
        }
        impl->async_wait(pid, this->get_io_service().wrap(handler));
#elif defined(BOOST_WINDOWS_API)
        HANDLE handle = OpenProcess(SYNCHRONIZE | PROCESS_QUERY_INFORMATION,
            FALSE, pid);
        if (handle == NULL)
            BOOST_PROCESS_THROW_LAST_SYSTEM_ERROR("OpenProcess() failed");
        boost::unique_lock<boost::mutex> lock(work_thread_mutex_);
        if (!work_)
            work_.reset(new boost::asio::io_service::work(
                this->get_io_service()));
        interrupt_work_thread();
        work_thread_cond_.wait(work_thread_mutex_);
        handles_.push_back(handle);
        impl->async_wait(handle, this->get_io_service().wrap(handler));
        work_thread_cond_.notify_all();
#endif
    }

private:
    void shutdown_service()
    {
#if defined(BOOST_WINDOWS_API)
        boost::unique_lock<boost::mutex> lock(work_thread_mutex_);
        work_.reset();
#endif
    }

    void work_thread()
    {
#if defined(BOOST_POSIX_API)
        for (;;)
        {
            int status;
            pid_t pid = ::wait(&status);
            if (pid == -1)
            {
                if (errno != EINTR)
                    BOOST_PROCESS_THROW_LAST_SYSTEM_ERROR("wait(2) failed");
            }
            else if (interrupted(pid))
            {
                // On POSIX the only reason to interrupt is to break out.
                break;
            }
            else
            {
                boost::unique_lock<boost::mutex> lock(work_thread_mutex_);
                bool regchild = false;
                for (typename std::vector<implementation_type>::iterator it =
                    impls_.begin(); it != impls_.end(); ++it)
                    regchild |= (*it)->complete(pid, status);
                if (regchild && --pids_ == 0)
                {
                    work_.reset();
                    break;
                }
                else if (!regchild)
                {
                    statuses_.insert(boost::unordered_map<pid_t, int>::
                        value_type(pid, status));
                    work_thread_cond_.notify_all();
                }
            }
        }
#elif defined(BOOST_WINDOWS_API)
        for (;;)
        {
            DWORD res = WaitForMultipleObjects(handles_.size(), &handles_[0],
                FALSE, INFINITE);
            if (res == WAIT_FAILED)
                BOOST_PROCESS_THROW_LAST_SYSTEM_ERROR(
                    "WaitForMultipleObjects() failed");
            else if (res - WAIT_OBJECT_0 == 0)
            {
                boost::unique_lock<boost::mutex> lock(work_thread_mutex_);
                if (!run_)
                    break;
                work_thread_cond_.notify_all();
                work_thread_cond_.wait(work_thread_mutex_);
            }
            else if (res - WAIT_OBJECT_0 > 0)
            {
                HANDLE handle = handles_[res - WAIT_OBJECT_0];
                DWORD exit_code;
                if (!GetExitCodeProcess(handle, &exit_code))
                    BOOST_PROCESS_THROW_LAST_SYSTEM_ERROR(
                        "GetExitCodeProcess() failed");
                boost::unique_lock<boost::mutex> lock(work_thread_mutex_);
                for (typename std::vector<implementation_type>::iterator it =
                    impls_.begin(); it != impls_.end(); ++it)
                    (*it)->complete(handle, exit_code);
                std::vector<HANDLE>::iterator it = handles_.begin();
                std::advance(it, res - WAIT_OBJECT_0);
                handles_.erase(it);
                if (handles_.size() == 1)
                    work_.reset();
            }
        }
#endif
    }

    void interrupt_work_thread()
    {
#if defined(BOOST_POSIX_API)
        // By creating a child process which immediately exits
        // we interrupt wait().
        std::vector<std::string> args;
        args.push_back("-c");
        args.push_back("'exit'");
        interrupt_pid_ = create_child("/bin/sh", args).get_id();
#elif defined(BOOST_WINDOWS_API)
        // By signaling the event in the first slot WaitForMultipleObjects()
        // will return. The work thread won't do anything except checking if
        // it should continue to run.
        if (!SetEvent(handles_[0]))
            BOOST_PROCESS_THROW_LAST_SYSTEM_ERROR("SetEvent() failed");
#endif
    }

#if defined(BOOST_POSIX_API)
    bool interrupted(pid_t pid)
    {
        boost::mutex::scoped_lock lock(work_thread_mutex_);
        return interrupt_pid_ == pid;
    }
#endif

    void stop_work_thread()
    {
        boost::mutex::scoped_lock lock(work_thread_mutex_);
#if defined(BOOST_WINDOWS_API)
        // Access to run_ must be sychronized with running().
        run_ = false;
#endif
        // Access to interrupt_pid_ must be sychronized with interrupted().
        interrupt_work_thread();
    }

    boost::scoped_ptr<boost::asio::io_service::work> work_;
    std::vector<implementation_type> impls_;
    boost::mutex work_thread_mutex_;
    boost::thread work_thread_;
    boost::condition_variable_any work_thread_cond_;
#if defined(BOOST_POSIX_API)
    pid_t interrupt_pid_;
    int pids_;
    boost::unordered_map<pid_t, int> statuses_;
#elif defined(BOOST_WINDOWS_API)
    bool run_;
    std::vector<HANDLE> handles_;
#endif
};

}
}
}

#endif
