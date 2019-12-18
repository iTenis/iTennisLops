#!/usr/bin/env bash
rm -rf jobs
git pull https://github.com/iTenis/iTennisLops.git
mvn clean compile
mvn clean package
mkdir -p jobs
cp -avf boot jobs/
cp -avf conf jobs/
cp -avf target/lib jobs/
cp -avf target/*.jar jobs/
cp -avf src/main/resources/application.yml jobs/
cd jobs
#nohub java -jar -Dspring.config.location=application.yml iTennisLops-1.0-SNAPSHOT.jar &> /dev/null &
java -jar -Dspring.config.location=application.yml iTennisLops-1.0-SNAPSHOT.jar &> /dev/null &

