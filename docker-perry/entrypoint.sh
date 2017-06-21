#!/bin/bash

if ([ -z "$DEV_MODE" ] || ! $DEV_MODE); then
  echo "PROD MODE"
  PERRY_CONFIG="--spring.config.location=config/perry-prod.yml"
  JAVA_OPTS="-Dspring.profiles.active=prod"

else
  echo "DEV MODE"
  PERRY_CONFIG="--spring.config.location=config/perry-dev.yml"
  JAVA_OPTS="-Dspring.profiles.active=dev"
fi

java ${JAVA_OPTS} -jar perry.jar server ${PERRY_CONFIG}
