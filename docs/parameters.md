# Parameters
Parameters are used in many commands such as [search](commands/search.md) and [preview](commands/preview.md).
They allow you to refine your selection, so you only get the actions you want.
Currently, there are 7 different parameters which can be found below.
Negative Parameters Example - `action:!entity-kill`

## Action
Key - `action:`  
Value - `Action Identifier`  
Multiple Allowed - `Yes`  
Negative Allowed - `Yes`    
Example - `action:block-break`

This parameter allows you to filter your selection based on the [action type](actions.md).
An Action Identifier is a string with the group and type divided by a `-`.

## Dimension
Key - `world:`  
Value - `World Identifier`  
Negative Allowed - `Yes`  
Multiple Allowed - `Yes`  
Example - `world:minecraft:the_end`

This parameter allows you to filter your selection based on the dimension.
An identifier is Minecraft's ID system of a namespace and a path divided by `:`.

## Object
Key - `object:`  
Value - `Object Identifier`  
Negative Allowed - `Yes`  
Multiple Allowed - `Yes`  
Example - `object:minecraft:stone`

This parameter allows you to filter your selection based on the object.
An object is ledger's name for an identifier that could be a block, item or entity.
An identifier is Minecraft's ID system of a namespace and a path divided by `:`.

## Range
Key - `range:`  
Value - `Integer > 1`  
Negative Allowed - `No`  
Multiple Allowed - `No`  
Example - `range:5`

This parameter allows you to filter your selection based on your location.
This will not filter by your dimension, so you may still get results from other worlds.
Use the [dimension parameter](#dimension) to filter by world.

## Source
Key - `source:`  
Value - `Source Name`  
Negative Allowed - `Yes`  
Multiple Allowed - `Yes`  
Example - `source:Potatoboy9999` `source:@tnt`

This parameter allows you to filter your selection based on the source of the action.
To filter based on a specific player, simply use that players name.
To filter based on any other source, you must use an `@` symbol.

## Time

### Before
Key - `before:`  
Value - `Time Duration`  
Negative Allowed - `No`  
Multiple Allowed - `No`  
Example - `before:3h`

### After
Key - `after:`  
Value - `Time Duration`  
Negative Allowed - `No`  
Multiple Allowed - `No`  
Example - `after:3h`

These parameters allow you to filter your selection based on time.
It will select actions within the duration specified.
You can specify durations with `s`econd, `m`inute, `h`our, `d`ay and `w`eek.
You can also combine multiple durations for example `7w4d31m42s`.
The `after` parameter is the same as the `time` parameter in older versions.
The `before` parameter selects all results before the point in time that was the provided, and after that point for `after`.
An easy way to remember the difference between `before:1d` and `after:1d` is to think about it like this.
If you go back in time 1 day, do you want everything that happened `before` then or `after` then.
Usually you want `after`.
