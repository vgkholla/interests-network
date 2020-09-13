# interests-network
[![Build Status](https://travis-ci.com/vgkholla/interests-network.svg?token=xJUBNqLxdQWLRs7Mz3ya&branch=master)](https://travis-ci.com/vgkholla/interests-network)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/56437f45d4934e95b9e85589e853f657)](https://www.codacy.com/manual/vgkholla/interests-network?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=vgkholla/interests-network&amp;utm_campaign=Badge_Grade)

Playing with CosmosDB and GraphQL

To see demo (CRUD on Cosmos DB), run

```
./gradlew run --args="--cosmosDBAccountEndpoint COSMOS_DB_ACCOUNT_ENDPOINT --cosmosDBAccountKey COSMOS_DB_ACCOUNT_KEY --cosmosDBPreferredRegions COSMOS_DB_ACCOUNT_PREFERRED_REGION1,COSMOS_DB_ACCOUNT_PREFERRED_REGION2"
```

You will need a CosmosDB account that has a Database with name "Groups" and container with name "groups"
