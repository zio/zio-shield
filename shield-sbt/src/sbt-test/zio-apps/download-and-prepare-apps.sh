#!/usr/bin/env bash

SCRIPT_DIR="$(dirname $0)"

REPOS=(
  https://github.com/freskog/stm-partitioning
  https://github.com/mschuwalow/zio-todo-backend
  #https://github.com/wix-incubator/zorechka-bot
  #https://github.com/loicdescotte/pureWebappSample
)

cd $SCRIPT_DIR || exit

for repo in "${REPOS[@]}"; do
  repo_name=${repo##*/}
  if [ -d $repo_name ]; then
    echo "$repo_name already exists, skipping it..."
  else
    echo "Downloading and preparing $repo_name..."
    git clone $repo $repo_name || return
    echo -e '\naddSbtPlugin("zio.shield" % "zio-shield" % sys.props("plugin.version") cross CrossVersion.full)\n'\
      >> $repo_name/project/plugins.sbt
    echo -e '> shield' > $repo_name/test
    echo -e '\ninThisBuild(shieldDebugOutput := true)\n' >> $repo_name/build.sbt
  fi
done