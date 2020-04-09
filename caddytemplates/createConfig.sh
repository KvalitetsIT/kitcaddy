#! /bin/ash

envsubst < ${TEMPLATE_FILE} > ${CADDYFILE}

if [ "$CADDYFILE_APPEND_TO" = "" ]
then
   envsubst < ${TEMPLATE_FILE} > ${CADDYFILE}
else

   envsubst < ${TEMPLATE_FILE} > /tmp/output
   jq -s '.[0] * .[1]' ${CADDYFILE_APPEND_TO} /tmp/output > ${CADDYFILE}
fi

cat ${CADDYFILE}
