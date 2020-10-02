# plant-tracker
[![Build Status](https://travis-ci.com/vgkholla/interests-network.svg?token=xJUBNqLxdQWLRs7Mz3ya&branch=master)](https://travis-ci.com/vgkholla/interests-network)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/56437f45d4934e95b9e85589e853f657)](https://www.codacy.com/manual/vgkholla/interests-network?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=vgkholla/interests-network&amp;utm_campaign=Badge_Grade)

Playing with CosmosDB and GraphQL

To try, first create a CosmosDB account that has a Database with name "Plants" and container with name "plants"

Then run
```
./gradlew run --args="--cosmosDBAccountEndpoint COSMOS_DB_ACCOUNT_ENDPOINT --cosmosDBAccountKey COSMOS_DB_ACCOUNT_KEY --cosmosDBPreferredRegions COSMOS_DB_ACCOUNT_PREFERRED_REGION1,COSMOS_DB_ACCOUNT_PREFERRED_REGION2"
```

Navigate to `http://localhost:8080/` and try out the following queries

##### Create plant

```
mutation {
  createPlant(input: { plant: {
    id: "ptracker:plant:myplant",
    name: "MyPlant"
  }}) {
    name
  }
}
```

##### Get created plant

```
{
  getPlant(input: { id: "ptracker:plant:myplant"}) {
    name
  }
}
```

##### Update plant

```
mutation {
  updatePlant(input: { plant: {
    id: "ptracker:plant:myplant",
    name: "MyPlantUpdated"
  }, shouldUpsert: true}) {
    name
  }
}
```

##### Get updated plant

```
{
  getPlant(input: { id: "ptracker:plant:myplant"}) {
    name
  }
}
```

##### Delete plant

```
mutation {
  deletePlant(input: { id: "ptracker:plant:myplant"}) {
    _
  }
}
```

##### Try to get deleted plant

```
{
  getPlant(input: { id: "ptracker:plant:myplant"}) {
    name
  }
}
```
