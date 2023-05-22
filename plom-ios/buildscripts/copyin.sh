#!/bin/bash
. vars.sh
cd ../../plom-core
${MVN} install -DskipTests
mkdir -p ../plom-ios/html/plom
cp -r target/plom-core-0.1-SNAPSHOT/ ../plom-ios/html/plom
cp -r ../plom-android/app/src/main/assets/templates ../plom-ios/html
cp src/main/webapp/plomweb.html ../plom-ios/html/templates/simple/web/index.html
cd ../plom-ios/html/plom
mv plomcore/*.cache.js plomcore/plomdirect.js
sed -i '' -e "s/plomcore.nocache.js/plomdirect.js/g" index.html
