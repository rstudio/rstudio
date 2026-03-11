% Fixture for test-completions-inherit-dot-params.R
% Mimics the Rd output that roxygen2 generates for a function documented with
% @inheritDotParams. The \item{...}{} block is intentionally structured to
% match roxygen2's generated output: the description begins with the sentinel
% text "Arguments passed on to" followed by a \describe block listing each
% forwarded argument as \item{\code{argname}}{description}.
\name{wrapper}
\alias{wrapper}
\title{Wrapper function}
\usage{wrapper(x, ...)}
\arguments{
\item{x}{A value.}
\item{...}{Arguments passed on to \code{\link[=target]{target}}
\describe{
\item{\code{alpha}}{Description of alpha.}
\item{\code{beta}}{Description of beta.}
\item{\code{gamma}}{Description of gamma.}
\item{\code{if}}{An argument with a reserved word name requiring backtick-quoting.}
}
}
}
\description{A wrapper that forwards dots to target.}
