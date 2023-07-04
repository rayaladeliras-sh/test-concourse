# How to run integration test

## local environment

1. go to the root path of token-mgt project

2. run: mvn clean install verify --projects test -Pintegration -DENV=stubhubdev -DBASE_URL=http://localhost:8080 -DENV_TYPE=QA -e

3. check report at "test/target/failsafe-reports/emailable-report.html" 
