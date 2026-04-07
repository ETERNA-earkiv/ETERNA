#!/bin/bash
SCRIPT_DIR=$( cd -- "$( dirname -- "${BASH_SOURCE[0]}" )" &> /dev/null && pwd )
cd "$SCRIPT_DIR"

for MODULE_PATH in $(find "$PWD"/target/roda-wui-?.?.?*/ -mindepth 1 -maxdepth 1 -type d -name 'org.roda.wui.*'); do
    MODULE_NAME=$(basename "$MODULE_PATH")

    # Copy .gwt.rpc files to src/main/webapp (used at WAR-document-root level)
    mkdir -p "src/main/webapp/$MODULE_NAME/"
    find "$MODULE_PATH" -name '*.gwt.rpc' -exec cp -v {} "src/main/webapp/$MODULE_NAME/" \;

    # Copy compiled GWT module to target/classes/static so Spring Boot's
    # classpath:/static/ resource handler serves it during spring-boot:run
    STATIC_DIR="$PWD/target/classes/static/$MODULE_NAME"
    mkdir -p "$STATIC_DIR"
    rsync -a --delete "$MODULE_PATH/" "$STATIC_DIR/"
    echo "Synced $MODULE_NAME → target/classes/static/$MODULE_NAME"
done
