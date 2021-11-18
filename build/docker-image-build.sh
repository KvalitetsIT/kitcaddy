#!/bin/sh

FOLDER_PATH=$1
FILE_NAME=$2
TAG_NAME=$3

if [ -z $FOLDER_PATH ]; then
  echo "FOLDER_PATH variable not set."
  exit 1
fi
if [ -z $FILE_NAME ]; then
  echo "FILE_NAME variable not set."
  exit 1
fi
if [ -z $TAG_NAME ]; then
  echo "FILE_NAME variable not set."
  exit 1
fi

docker build -f $FOLDER_PATH/$FILE_NAME -t $TAG_NAME --no-cache $FOLDER_PATH