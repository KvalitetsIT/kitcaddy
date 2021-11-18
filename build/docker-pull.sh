#!/bin/sh

IMAGE_NAME=$1

if [ -z $IMAGE_NAME ]; then
  echo "IMAGE_NAME variable not set."
  exit 1
fi

docker pull $IMAGE_NAME