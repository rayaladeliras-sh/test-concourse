# token-mgt
Identity Token Management Service

## Related Resources
**Identity Services**:
    * [AppEngine](https://console.cloud.google.com/appengine?referrer=search&authuser=1&project=identity-dev-40077)

**Spanner Databases**:
  * [token-mgt-db](https://console.cloud.google.com/spanner/instances/spanner-dev/databases/token-mgt-db)
  * [customer-identity-db](https://console.cloud.google.com/spanner/instances/spanner-dev/databases/customeridentity-db)


## Open API Docs
* [About](docs/About%20credentials.md)
* [OpenAPI](docs/Identity.Token-mgt.postman_collection.json)
* [ENV Vars](docs/Identity_dev.postman_environment.json)

## Concourse Pipelines
### Dev Environment
```bash
fly -t identity login --team-name stubhub-identity-dev --concourse-url https://concourse.npstubsys.cloud

fly -t identity set-pipeline --pipeline token-mgt-ups -c ci/pipelines/qa/pipeline-create-ups.yml -l ci/pipelines/qa/parameters-ups.yml

fly -t identity set-pipeline --pipeline token-mgt-db -c ci/pipelines/qa/pipeline-db.yml -l ci/pipelines/qa/parameters-db.yml

fly -t identity set-pipeline --pipeline token-mgt-release -c ci/pipelines/qa/pipeline-release.yml -l ci/pipelines/qa/parameters.yml

fly -t identity set-pipeline --pipeline token-mgt-development -c ci/pipelines/qa/pipeline-development.yml -l ci/pipelines/qa/parameters.yml

fly -t identity set-pipeline --pipeline token-mgt-performance -c ci/pipelines/qa/pipeline-performance.yml -l ci/pipelines/qa/parameters-performance.yml

fly -t identity set-pipeline --pipeline token-mgt-pr -c ci/pipelines/qa/pipeline-pr.yml -l ci/pipelines/qa/parameters.yml
```

### Staging Environment
```bash
fly -t identity login --team-name stubhub-identity-stg --concourse-url https://concourse.npstubsys.cloud

fly -t identity set-pipeline --pipeline token-mgt-ups -c ci/pipelines/staging/pipeline-create-ups.yml -l ci/pipelines/staging/parameters-ups.yml

fly -t identity set-pipeline --pipeline token-mgt-db -c ci/pipelines/staging/pipeline-db.yml -l ci/pipelines/staging/parameters-db.yml

fly -t identity set-pipeline --pipeline token-mgt -c ci/pipelines/staging/pipeline.yml -l ci/pipelines/staging/parameters.yml
```


### Production Environment
```bash
fly -t identity login --team-name stubhub-identity-prd --concourse-url https://concourse.prdstubsys.cloud/

fly -t identity set-pipeline --pipeline token-mgt-ups -c ci/pipelines/prod/pipeline-create-ups.yml -l ci/pipelines/prod/parameters-ups.yml

fly -t identity set-pipeline --pipeline token-mgt-db -c ci/pipelines/prod/pipeline-db.yml -l ci/pipelines/prod/parameters-db.yml

fly -t identity set-pipeline --pipeline token-mgt -c ci/pipelines/prod/pipeline.yml -l ci/pipelines/prod/parameters.yml
```
