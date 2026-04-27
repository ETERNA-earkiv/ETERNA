#!/bin/bash

# run extension scripts
DIR=/docker-entrypoint.d

if [[ -d "$DIR" ]]; then
  for script in "$DIR"/*; do
    [[ -f "$script" && -x "$script" ]] && echo "Running $script" && "$script"
  done
fi

if [[ $# -eq 0 ]] ; then
    echo "Starting ETERNA (user: $(whoami))"
    exec java -jar /WhiteRed/bin/roda-wui-*.jar
fi

exec "$@"
