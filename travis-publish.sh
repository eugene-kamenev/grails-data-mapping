#!/bin/bash

# use travis_after_all.py for publishing only after all builds are successfull.
if [[ "$BUILD_LEADER" == "YES" ]]; then
  if [[ "$BUILD_AGGREGATE_STATUS" != "others_succeeded" ]]; then
    echo "Some builds failed, not publishing."
    exit 0
  fi
else
  # not build leader, exit
  echo "Not build leader, exiting"
  exit 0
fi

echo "Publishing..."

EXIT_STATUS=0

if [[ $TRAVIS_REPO_SLUG == "grails/grails-data-mapping" && $TRAVIS_PULL_REQUEST == 'false' && $EXIT_STATUS -eq 0 ]]; then

  echo "Publishing archives"
  echo "org.gradle.jvmargs=-XX\:MaxPermSize\=1024m -Xmx1500m -Dfile.encoding\=UTF-8 -Duser.country\=US -Duser.language\=en -Duser.variant" >> ~/.gradle/gradle.properties
  echo "org.gradle.daemon=true" >> ~/.gradle/gradle.properties
  ./gradlew --stop


  gpg --keyserver keyserver.ubuntu.com --recv-key $SIGNING_KEY
  if [[ $TRAVIS_TAG =~ ^v[[:digit:]] ]]; then
    # for releases we upload to Bintray and Sonatype OSS
    ./gradlew -Psigning.keyId="$SIGNING_KEY" -Psigning.password="$SIGNING_PASSPHRASE" -Psigning.secretKeyRingFile="${TRAVIS_BUILD_DIR}/secring.gpg" publish uploadArchives bintrayUpload -x grails2-plugins/neo4j:publish -x grails2-plugins/hibernate4:publish -x grails2-plugins/mongodb:publish -x grails2-plugins/neo4j:uploadArchives -x grails2-plugins/hibernate4:uploadArchives -x grails2-plugins/mongodb:uploadArchives || EXIT_STATUS=$?
    if [[ $EXIT_STATUS -eq 0 ]]; then
    ./gradlew grails2-plugins/neo4j:publish grails2-plugins/hibernate4:publish grails2-plugins/mongodb:publish || EXIT_STATUS=$?
    ./gradlew --stop
    fi

  else
    # for snapshots only to repo.grails.org
    ./gradlew publish -x grails2-plugins/neo4j:publish -x grails2-plugins/hibernate4:publish -x grails2-plugins/mongodb:publish || EXIT_STATUS=$?
    ./gradlew --stop
    if [[ $EXIT_STATUS -eq 0 ]]; then
    ./gradlew grails2-plugins/neo4j:publish grails2-plugins/hibernate4:publish grails2-plugins/mongodb:publish || EXIT_STATUS=$?
    ./gradlew --stop
    fi
  fi

  if [[ $EXIT_STATUS -eq 0 ]]; then
      ./gradlew closeAndPromoteRepository
      echo "Trigger Travis Functional Test build"
      ./trigger-dependent-build.sh
#      ./gradlew travisciTrigger -i
      ./gradlew --stop

      echo "Building documentation"
      ./gradlew allDocs || EXIT_STATUS=$?

      git config --global user.name "$GIT_NAME"
      git config --global user.email "$GIT_EMAIL"
      git config --global credential.helper "store --file=~/.git-credentials"
      echo "https://$GH_TOKEN:@github.com" > ~/.git-credentials

      git clone https://${GH_TOKEN}@github.com/${TRAVIS_REPO_SLUG}.git -b gh-pages gh-pages --single-branch > /dev/null
      cd gh-pages

      # If this is the master branch then update the snapshot
#      if [[ $TRAVIS_BRANCH == 'master' ]]; then
#        mkdir -p snapshot
#        cp -r ../build/docs/. ./snapshot/
#
#        git add snapshot/*
#      fi

      # If there is a tag present then this becomes the latest
      if [[ $TRAVIS_TAG =~ ^v[[:digit:]] ]]; then
        version="$TRAVIS_TAG"
        version=${version:1}

        mkdir -p latest
        cp -r ../build/docs/. ./latest/
        git add latest/*

        majorVersion=${version:0:4}
        majorVersion="${majorVersion}x"

        mkdir -p "$version"
        cp -r ../build/docs/. "./$version/"
        git add "$version/*"

        mkdir -p "$majorVersion"
        cp -r ../build/docs/. "./$majorVersion/"
        git add "$majorVersion/*"

        git commit -a -m "Updating docs for Travis build: https://travis-ci.org/$TRAVIS_REPO_SLUG/builds/$TRAVIS_BUILD_ID"
        git push origin HEAD
        cd ..
        rm -rf gh-pages
      fi
  else
      echo "Error occured during publishing, skipping docs"
  fi

fi

if [[ $EXIT_STATUS -eq 0 ]]; then
  echo "Publishing Successful."
fi
exit $EXIT_STATUS
