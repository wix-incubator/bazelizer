#!/usr/bin/env bash

set -x
set -e

src="$1"
tmp_dir=$(mktemp -d -t ci-XXXXXXXXXX)

echoerr() {
  echo "$@" 1>&2;
}

assert() {
  $@ || (echo "FAILED: $@"; exit 1)
}

verify() {
  tar -xvf "${src}" -C "${tmp_dir}" && ls "${tmp_dir}" && ls "${tmp_dir}/org/apache"
}

assert verify
