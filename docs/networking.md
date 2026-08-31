# Networking

Ledger supports numerous custom packets for interacting with supported client mods

## Versions

All packets are correct for Ledger Networking v3, which is included in Ledger 1.3.0 and later.

The information on this page is applicable for Ledger Networking version 3, which is the version in Ledger versions `1.3.0` and later

## Permissions

The `ledger:networking` permission is required to interact with Ledger over the networking API. There is no fallback to a permission level: you **must** explicitly give players this permission to be able to use Ledger's functionality over the networking API.

Each packet also requires the permissions that the corresponding command does, but note that this does have fallback to permission level.

### Permissions prior to version 1.3.24

Permission codes were changed in version 1.3.24 to enable support for the Fabric Permissions API v1. In older versions, the `ledger.networking` permission from lucko's [Fabric Permissions API](https://github.com/lucko/fabric-permissions-api) was required in addition to the corresponding packet type permissions, with no fallback to permission level provided for this permission.

## Packet Types


#### Notation
Types shown here are the Java variable types. They have the equivalent value (if applicable) in Kotlin when used in Ledger's internal code

---
## Client to Server

### Response

Once one of the c2s packets have been received, the server will send a response packet with the packet type it was responding to and the response code. See below for more information

### Inspect Packet

Inspects a block at a given position, using the player's current dimension. This may be changed in the future

Channel: `ledger:inspect`

Buf content:

Position: `BlockPos`

Number of pages: `int`

Return packet type: `ledger.action`

### Search Packet

Channel: `ledger:search`

Buf content:

Input: `String`

Pages: `int`

String formatted in the same way as a `/lg search` command would be formatted

Return packet type: `ledger.action`

### Handshake Packet

Channel: `ledger:handshake`

Buf content:

Mod NBT: `NbtCompound`

Mod NBT should contain the following:

- Mod Version (`version`) [`String`] : Fabric Loader user friendly string

- Mod ID (`modid`) [`String`] : Mod identifier of the mod

- Protocol version (`protocol_version`) [`int`] : Ledger protocol version (currently 3)

### Purge Packet

Channel: `ledger.purge`

Buf content:

Params: `String` - same string as used in the search command

### Rollback Packet

Channel: `ledger.rollback`

Buf content:

Restore: `Boolean` - To restore must be true, for to rollback must be false

Params: `String` - same string as used in the search command

---

## Server to client

### Action Packet

Represents a logged action from the database

Channel: `ledger:action`

Buf content:

Position: `BlockPos`

Type: `String`

Dimension: `Identifier`

Old Object: `Identifier`

New Object: `Identifier`

Source: `String`

Epoch second: `long`

Rolled back: `boolean`

Additional NBT: `String`

### Handshake Packet

Sends information about Ledger to compatible clients

Channel: `ledger:handshake`

Buf content:

Protocol Version: `int` - Version of Ledger networking protocol. Ledger `1.1.0` and later uses version `1`

Mod allowed: `boolean`

## Response

Registers the server receiving a Ledger packet and contains information about what the server is doing

Channel: `ledger.response`

### Packet structure

Type: `Identifier` - Packet type being responded to

Response code: `int`

### Response Codes

`0`: No permission

`1`: Executing command

`2`: Completed command

`3`: Error while executing command

`4`: Cannot execute command at this time

