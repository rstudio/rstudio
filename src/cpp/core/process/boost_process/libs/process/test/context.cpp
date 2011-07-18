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

#include <boost/process/config.hpp> 

#if defined(BOOST_POSIX_API) 
#   include <unistd.h> 
#elif defined(BOOST_WINDOWS_API) 
#   include <windows.h> 
#else 
#   error "Unsupported platform." 
#endif 

#define BOOST_TEST_MAIN 
#include "util/boost.hpp" 

BOOST_AUTO_TEST_CASE(test_context_with_default_streams) 
{ 
    bp::context ctx; 

    bp::stream_ends stdin_ends = ctx.streams[bp::stdin_id](
        bp::input_stream); 
    bp::stream_ends stdout_ends = ctx.streams[bp::stdout_id](
        bp::output_stream); 
    bp::stream_ends stderr_ends = ctx.streams[bp::stderr_id](
        bp::output_stream); 

#if defined(BOOST_POSIX_API) 
    BOOST_CHECK_EQUAL(stdin_ends.child.native(), STDIN_FILENO); 
    BOOST_CHECK_EQUAL(stdout_ends.child.native(), STDOUT_FILENO); 
    BOOST_CHECK_EQUAL(stderr_ends.child.native(), STDERR_FILENO); 
#elif defined(BOOST_WINDOWS_API) && (_WIN32_WINNT < 0x0601) 
    BOOST_CHECK_EQUAL(stdin_ends.child.native(), 
        GetStdHandle(STD_INPUT_HANDLE)); 
    BOOST_CHECK_EQUAL(stdout_ends.child.native(), 
        GetStdHandle(STD_OUTPUT_HANDLE)); 
    BOOST_CHECK_EQUAL(stderr_ends.child.native(), 
        GetStdHandle(STD_ERROR_HANDLE)); 
#endif 
} 

class close_std_handles 
{ 
public: 
    close_std_handles() 
        : reverted_(false) 
    { 
#if defined(BOOST_POSIX_API) 
        BOOST_REQUIRE(dup2(STDIN_FILENO, 100) != -1); 
        close(STDIN_FILENO); 
        BOOST_REQUIRE(dup2(STDOUT_FILENO, 101) != -1); 
        close(STDOUT_FILENO); 
        BOOST_REQUIRE(dup2(STDERR_FILENO, 102) != -1); 
        close(STDERR_FILENO); 
#elif defined(BOOST_WINDOWS_API) 
        handles_[0] = GetStdHandle(STD_INPUT_HANDLE); 
        BOOST_REQUIRE(SetStdHandle(STD_INPUT_HANDLE, INVALID_HANDLE_VALUE)); 
        handles_[1] = GetStdHandle(STD_OUTPUT_HANDLE); 
        BOOST_REQUIRE(SetStdHandle(STD_OUTPUT_HANDLE, INVALID_HANDLE_VALUE)); 
        handles_[2] = GetStdHandle(STD_ERROR_HANDLE); 
        BOOST_REQUIRE(SetStdHandle(STD_ERROR_HANDLE, INVALID_HANDLE_VALUE)); 
#endif 
    } 

    ~close_std_handles() 
    { 
        if (!reverted_) 
            revert(); 
    } 

    void revert() 
    { 
        reverted_ = true; 
#if defined(BOOST_POSIX_API) 
        BOOST_REQUIRE(dup2(100, STDIN_FILENO) != -1); 
        close(100); 
        BOOST_REQUIRE(dup2(101, STDOUT_FILENO) != -1); 
        close(101); 
        BOOST_REQUIRE(dup2(102, STDERR_FILENO) != -1); 
        close(102); 
#elif defined(BOOST_WINDOWS_API) 
        BOOST_REQUIRE(SetStdHandle(STD_INPUT_HANDLE, handles_[0])); 
        BOOST_REQUIRE(SetStdHandle(STD_OUTPUT_HANDLE, handles_[1])); 
        BOOST_REQUIRE(SetStdHandle(STD_ERROR_HANDLE, handles_[2])); 
#endif 
    } 

private: 
    bool reverted_; 
#if defined(BOOST_WINDOWS_API) 
    HANDLE handles_[3]; 
#endif 
}; 

BOOST_AUTO_TEST_CASE(test_context_with_closed_streams) 
{ 
    close_std_handles csh; 

    bp::context ctx; 

    bp::stream_ends stdin_ends = ctx.streams[bp::stdin_id](
        bp::input_stream); 
    bp::stream_ends stdout_ends = ctx.streams[bp::stdout_id](
        bp::output_stream); 
    bp::stream_ends stderr_ends = ctx.streams[bp::stderr_id](
        bp::output_stream); 

    csh.revert(); 

#if defined(BOOST_POSIX_API) 
    BOOST_CHECK_EQUAL(stdin_ends.child.native(), 0); 
    BOOST_CHECK_EQUAL(stdout_ends.child.native(), 1); 
    BOOST_CHECK_EQUAL(stderr_ends.child.native(), 2); 
#elif defined(BOOST_WINDOWS_API) 
    BOOST_CHECK_EQUAL(stdin_ends.child.native(), INVALID_HANDLE_VALUE); 
    BOOST_CHECK_EQUAL(stdout_ends.child.native(), INVALID_HANDLE_VALUE); 
    BOOST_CHECK_EQUAL(stderr_ends.child.native(), INVALID_HANDLE_VALUE); 
#endif 
} 
