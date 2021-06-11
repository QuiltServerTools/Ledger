# Parameters
Parameters are used in many commands such as [search](commands/search.md) and [preview](commands/preview.md).
They allow you to refine your selection so you only get the actions you want.
Currently, there are 6 different parameters which can be found below.

## Action
Key - `action:`  
Value - `Action Identifier`  
Multiple Allowed - `Yes`  
Example - `action:block-break`

This parameter allows you to filter your selection based on the [action type](actions.md).
An Action Identifier is a string with the group and type divided by a `-`.

## Dimension
Key - `world:`  
Value - `World Identifier`  
Multiple Allowed - `Yes`  
Example - `world:minecraft:the_end`

This parameter allows you to filter your selection based on the dimension.
An identifier is minecraft's ID system of a namespace and a path divided by `:`.

## Object
Key - `object:`  
Value - `Object Identifier`
Multiple Allowed - `Yes`  
Example - `object:minecraft:stone`

This parameter allows you to filter your selection based on the object.
An object is ledger's name for an identifier that could be a block, item or entity.
An identifier is minecraft's ID system of a namespace and a path divided by `:`.

## Range
Key - `range:`  
Value - `Integer > 1`  
Multiple Allowed - `No`  
Example - `range:5`

This parameter allows you to filter your selection based on your location.
This will not filter by your dimension, so you may still get results from other worlds.
Use the [dimension parameter](#dimension) to filter by world.

## Source
Key - `source:`  
Value - `Source Name`  
Multiple Allowed - `Yes`  
Example - `source:Potatoboy9999` `source:@tnt`

This parameter allows you to filter your selection based on the source of the action.
To filter based on a specific player, simply use that players name.
To filter based on any other source, you must use an `@` symbol.

## Time
Key - `time:`  
Value - `Time Duration`  
Multiple Allowed - `No`  
Example - `time:3h`

This parameter allows you to filter your selection based on time.
It will select actions within the duration specified.
You can specify durations with `s`econd, `m`inute, `h`our, `d`ay and `w`eek.
You can also combine multiple durations for example `7w4d31m42s`.