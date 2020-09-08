# interests-network
[![Build Status](https://travis-ci.com/vgkholla/interests-network.svg?token=xJUBNqLxdQWLRs7Mz3ya&branch=master)](https://travis-ci.com/vgkholla/interests-network)

Playing with CosmosDB and GraphQL

To see demo (CRUD on Cosmos DB), run
```
./gradlew run --args="--cosmosDBAccountEndpoint COSMOS_DB_ACCOUNT_ENDPOINT --cosmosDBAccountKey COSMOS_DB_ACCOUNT_KEY --cosmosDBPreferredRegions COSMOS_DB_ACCOUNT_PREFERRED_REGION1,COSMOS_DB_ACCOUNT_PREFERRED_REGION2"
```
You will need a CosmosDB account that has a Database with name "Groups" and container with name "groups"