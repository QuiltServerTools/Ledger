# Developing Extensions

Extensions provide an easy way to add your own functionality to Ledger

### Note

The docs shown here have Kotlin code, but you can write your extension in Java should you wish

## Set up project

1. Set up your mod environment as usual
2. Publish Ledger to `mavenLocal`
3. Add the following to your `build.gradle`:

```groovy
modImplementation(include("com.github.quiltservertools:ledger:LATEST_LEDGER_VERSION"))
```

## Creating your extension

```kotlin
object Extension : DatabaseExtension {
    override fun getDatabase(server: MinecraftServer): Database {
        TODO("Tutorial")
    }

    override fun getIdentifier(): Identifier {
        TODO("Tutorial")
    }
    override fun getConfigSpecs(): List<ConfigSpec> {
        return ArrayList()
    }
}
```

We now need to fill out the methods.

`getDatabase`: Returns an Exposed `Database` object. This is how you add new database types to Ledger

`getIdentifier`: Should return a new `Identifier(YOUR_MODID, DATABASE_TYPE)` object.

## Register extension

In the entrypoint of your mod, add the relevant call to `ExtensionManager#registerExtension`

```kotlin
object MyMod : ModInitializer {
    override fun onInitialize() {
        ExtensionManager.registerExtension(Extension)
    }
}
```

It's as simple as that

## Command extension

Create a command class:

```kotlin
object MyLedgerSubCommand : BuildableCommand {
    override fun build(): LiteralCommandNode<ServerCommandSource> {
        return CommandManager.literal("subcommand")
            .executes { ctx.source.sendFeedback(LiteralText("Subcommand run"), false) }
    }
}
```

Now you create your extension class and register it like shown above

```kotlin
object Extension : CommandExtension {
    fun registerSubcommands(): List<BuildableCommand> {
        val list = mutableListOf<BuildableCommand>()
        list.add(MyLedgerSubCommand)
        return list
    }
    override fun getConfigSpecs(): List<ConfigSpec> {
        return ArrayList()
    }
}
```

## Config specs

Ledger uses the Konf library for configuration. You can add your own config specs to Ledger's file if your extension needs it

Create a config spec:

```kotlin
object ExtensionSpec : ConfigSpec() {
    val myProperty by optional(true)
    val myRequiredProperty by required<Boolean>()
}
```

Add the relevant line to your config file:

```toml
# Config file above

[extension]
myProperty = false
myRequiredProperty = true
```

You should make all properties optional unless your extension cannot function without a value. This is because existing config files will not have these values automatically added.

Register your spec:

```kotlin
override fun getConfigSpecs(): List<ConfigSpec> {
        return listOf(ExtensionSpec)
    }
```
