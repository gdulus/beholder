#!/bin/bash

start() {
  echo 'Starting beholder ...'
  lein ring server
}

"$@"