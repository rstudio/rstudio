% Fixture for test-completions-inherit-dot-params.R
% Mimics @inheritDotParams with specific args excluded, e.g.:
%   @inheritDotParams target -gamma
\name{wrapper_partial}
\alias{wrapper_partial}
\title{Wrapper with partial inheritDotParams}
\usage{wrapper_partial(x, ...)}
\arguments{
\item{x}{A value.}
\item{...}{Arguments passed on to \code{\link[=target]{target}}
\describe{
\item{\code{alpha}}{Description of alpha.}
\item{\code{beta}}{Description of beta.}
}
}
}
\description{A wrapper that forwards only some dots to target.}
