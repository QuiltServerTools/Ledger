# Accessing the Ledger Database

Ledger has a database with access to potentially hundreds of thousands or even millions of data points. This page describes how you can make use of our database in your own mods

### What about extensions?

The database extension API is extremely simple - it is designed to allow other database types, not actually accessing the databse

### What language?

While we would strongly recommend Kotlin, which grants  you access to all of our database methods, removing the need for an API, it is possible to write using Java with our LedgerApi. Please see the Java section.

### Negatable parameters

When searching the database, some parameters can be negative. Making the world parameter negative, for example, would show all results where the world is not the one specified.

## Kotlin

With Kotlin, you can access our database from within the `Ledger` coroutine.

```kotlin
object LedgerExamples {
    fun search(targetWorld: ServerWorld) {
        // We make a search parameters object showing results only in the world
        val params = ActionSearchParams.build {
            this.worlds = mutableSetOf(Negatable.allow(targetWorld.getRegistryKey().getValue()))
        }
        Ledger.launch {
            // Run the query and select actions on page 1
            val results: SearchResults = DataaseManager.searchActions(targetWorld)
            // Store the actions of the current page in a variable
            val actions: List<ActionType> = results.actions
        }
    }
    fun insert(world: World,
        player: PlayerEntity,
        pos: BlockPos,
        state: BlockState,
        context: ItemPlacementContext,
        entity: BlockEntity?) {
        // Create action object
        val action = ActionFactory.blockPlaceAction(world, pos, state, player, entity)
        // Insert action into the database
        ActionQueueService.addToQueue(action)
    }
}
```

## Java

With Java, you can access our database with the API which can be obtained with `Ledger.getApi()`.
You can see the API [here](https://github.com/QuiltServerTools/Ledger/blob/master/src/main/java/com/github/quiltservertools/ledger/api/LedgerApi.java).

```java
public class LedgerExamples {
    public SearchResults getSearchResults(ServerWorld world) {
        // Create parameters
        ActionSearchParams.Builder params = new ActionSearchParams.Builder();
        Set<Negatable<Identifier>> worlds = new HashSet<>();
        worlds.add(Negatable.allow(world.getRegistryKey().getValue()));
        params.setWorlds(worlds);
        // Run search
        CompletableFuture<SearchResults> future = Ledger.getApi().searchActions(params.build(), 0);
        // Blocks thread for result
        SearchResults results = future.get();
    }
    
    public void insert(World world,
                       PlayerEntity player,
                       BlockPos pos,
                       BlockState state,
                       ItemPlacementContext context,
                       BlockEntity entity) {
        // Create action
        ActionType action = ActionFactory.INSTANCE.blockPlaceAction(world, pos, state, player, entity);
        // Log the action
        Ledger.getApi().logAction(action);
    }
}
```


### Negatable

Normal behaviour: `Negatable.allow(object)`

Negative behaviour: `Negatable.deny(object)`

### ActionSearchParams

This class is used for filtering searches.

#### Kotlin

Set the parameters when creating the `ActionSearchParams.Builder` object

#### Java

Use the `setPropertyName` method on the `ActionSearchParams.Builder` object

Current params:

`min`: `BlockPos`

Forms one corner of the cubic block selection

`max`: `BlockPos`

Forms the other corner of the cubic block selection

`before`: `Instant`

Selects all actions which occurred before the specified point in time

`after`: `Instant`

Selects all actions which occurred after the specified point in time

`actions`: `MutableSet<Negatable<String>>`

A list of action types with OR filtering if not negative and AND filtering if negative

`objects`: `MutableSet<Negatable<Identifier>>`

A list of identifiers containing object names, such as `minecraft:diamond_ore`. Uses OR filtering if not negative and AND filtering if negative

`sourceNames`: `MutableSet<Negatable<String>>`

A list of non-player source names, such as `tnt`, with OR filtering if not negative and AND filtering if negative

`sourcePlayerNames`: `MutableSet<Negatable<String>>`

A list of player names to filter by. Uses OR filtering if not negative and AND filtering if negative


`worlds` : `MutableSet<Negatable<Identifier>>`

A list of world identifiers to filter by. Uses OR filtering if not negative and AND filtering if negative