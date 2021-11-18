#!/bin/sh

TAG_NAME=$1
PATH=$2
if [ -z $TAG_NAME ]; then
  echo "TAG_NAME variable not set."
  exit 1
fi
if [ -z PATH ]; then
  echo "PATH variable not set."
  exit 1
fi

docker build -t $TAG_NAME $PATH