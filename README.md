# interests-network
[![Build Status](https://travis-ci.com/vgkholla/interests-network.svg?token=xJUBNqLxdQWLRs7Mz3ya&branch=master)](https://travis-ci.com/vgkholla/interests-network)
[![Codacy Badge](https://app.codacy.com/project/badge/Grade/56437f45d4934e95b9e85589e853f657)](https://www.codacy.com/manual/vgkholla/interests-network?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=vgkholla/interests-network&amp;utm_campaign=Badge_Grade)

Playing with CosmosDB and GraphQL

To try, first create a CosmosDB account that has a Database with name "Groups" and container with name "groups"

Then run
```
./gradlew run --args="--cosmosDBAccountEndpoint COSMOS_DB_ACCOUNT_ENDPOINT --cosmosDBAccountKey COSMOS_DB_ACCOUNT_KEY --cosmosDBPreferredRegions COSMOS_DB_ACCOUNT_PREFERRED_REGION1,COSMOS_DB_ACCOUNT_PREFERRED_REGION2"
```

Navigate to `http://localhost:8080/` and try out the following queries

##### Create group

```
mutation {
  createGroup(input: { group: {
    id: "inet:group:mygroup",
    name: "MyGroup"
  }}) {
    name
  }
}
```

##### Get created group

```
{
  getGroup(input: { id: "inet:group:mygroup"}) {
    name
  }
}
```

##### Update group

```
mutation {
  updateGroup(input: { group: {
    id: "inet:group:mygroup",
    name: "MyGroupUpdated"
  }, shouldUpsert: true}) {
    name
  }
}
```

##### Get updated group

```
{
  getGroup(input: { id: "inet:group:mygroup"}) {
    name
  }
}
```

##### Delete group

```
mutation {
  deleteGroup(input: { id: "inet:group:mygroup"}) {
    _
  }
}
```

##### Try to get deleted group

```
{
  getGroup(input: { id: "inet:group:mygroup"}) {
    name
  }
}
```
