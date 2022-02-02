#include <cpp11.hpp>

// This is a simple example of exporting a C++ function to R. You can
// source this function into an R session using `cpp11::cpp_source()`.

// Learn more about cpp11 at: 
//
//   https://cpp11.r-lib.org/index.html

[[cpp11::register]]
double sum(cpp11::doubles x) {
    double total = 0.0;
    for (double value: x) {
        total += value;
    }
    return total;
}
