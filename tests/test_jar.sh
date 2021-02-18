#!/usr/bin/env bash

set -x
set -e

echoerr() {
  echo "$@" 1>&2;
}

assert() {
  $@ || (echo "FAILED: $@"; exit 1)
}

contains() {
  ls $@
}


assert ls "${1}"
assert zipinfo "$1" | grep "${2}"
