#!/bin/bash
. vars.sh
rm -r ../html/plom
cd ../../plom-core
${MVN} clean
