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

verify_jar() {
  zipinfo -1 "$1" | grep "${2}"
}

assert ls "${1}"
assert zipinfo -1 "$1" | grep "${2}"