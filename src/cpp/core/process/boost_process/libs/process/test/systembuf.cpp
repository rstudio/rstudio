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
#   include <fcntl.h> 
#elif defined(BOOST_WINDOWS_API) 
#   include <windows.h> 
#else 
#   error "Unsupported platform." 
#endif 

#define BOOST_TEST_MAIN 
#include "util/boost.hpp" 
#include <istream> 
#include <ostream> 
#include <fstream> 
#include <cstddef> 

void write_data(std::ostream &os, std::size_t length) 
{ 
    char c = 'A'; 
    for (std::size_t i = 0; i < length; ++i) 
    { 
        os << c; 
        if (c == 'Z') 
            c = 'A'; 
        else 
            ++c; 
    } 
    os.flush(); 
} 

void check_data(std::istream &is, std::size_t length) 
{ 
    char ch = 'A'; 
    std::size_t i = 0; 
    char c; 
    while (is >> c) 
    { 
        BOOST_CHECK_EQUAL(c, ch); 
        if (ch == 'Z') 
            ch = 'A'; 
        else 
            ++ch; 
        ++i; 
    } 
    BOOST_CHECK_EQUAL(i, length); 
} 

BOOST_AUTO_TEST_CASE(test_systembuf_read) 
{ 
    std::ofstream f("test_read.txt"); 
    BOOST_REQUIRE(f.good()); 
    write_data(f, 1024); 
    f.close(); 

#if defined(BOOST_POSIX_API) 
    int fd = open("test_read.txt", O_RDONLY); 
    BOOST_REQUIRE(fd != -1); 
    bpd::systembuf sb(fd); 
    std::istream is(&sb); 
    check_data(is, 1024); 
    close(fd); 
#elif defined(BOOST_WINDOWS_API) 
    HANDLE h = CreateFileA("test_read.txt", GENERIC_READ, 0, NULL, 
        OPEN_EXISTING, FILE_ATTRIBUTE_NORMAL, NULL); 
    BOOST_REQUIRE(h != INVALID_HANDLE_VALUE); 
    bpd::systembuf sb(h); 
    std::istream is(&sb); 
    check_data(is, 1024); 
    CloseHandle(h); 
#endif 

    BOOST_REQUIRE(bfs::remove("test_read.txt")); 
} 

BOOST_AUTO_TEST_CASE(test_systembuf_write) 
{ 
#if defined(BOOST_POSIX_API) 
    int fd = open("test_write.txt", O_WRONLY | O_CREAT | O_TRUNC, 
        S_IRUSR | S_IWUSR | S_IRGRP | S_IROTH); 
    BOOST_REQUIRE(fd != -1); 
    bpd::systembuf sb(fd); 
    std::ostream os(&sb); 
    write_data(os, 1024); 
    close(fd); 
#elif defined(BOOST_WINDOWS_API) 
    HANDLE h = CreateFileA("test_write.txt", GENERIC_WRITE, 0, NULL, 
        CREATE_ALWAYS, FILE_ATTRIBUTE_NORMAL, NULL); 
    BOOST_REQUIRE(h != INVALID_HANDLE_VALUE); 
    bpd::systembuf sb(h); 
    std::ostream os(&sb); 
    write_data(os, 1024); 
    CloseHandle(h); 
#endif 

    std::ifstream f("test_write.txt"); 
    BOOST_REQUIRE(f.good()); 
    check_data(f, 1024); 
    f.close(); 

    BOOST_REQUIRE(bfs::remove("test_write.txt")); 
} 
