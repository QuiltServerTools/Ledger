# Networking

Ledger supports numerous custom packets for interacting with supported client mods

## Packet Types

The server will not respond to packets unless the player has the correct permissions, which is `ledger.networking` and the relevant command permission

## Client to Server

### Inspect packet

Inspects a block at a given position, using the player's current dimension. This may be changed in the future

Channel: `ledger:inspect`

Buf content:

Position: `BlockPos`

Return packet type: `ledger.action`

### Search packet

Channel: `ledger:search`

Buf content:

Input: `String`

String formatted in the same way as a `/lg search` command would be formatted

Return packet type: `ledger.action`

### Handshake packet

Channel: `ledger:handshake`

Buf content:

Mod NBT: `NbtCompound`

Mod NBT should contain the following:

- Mod Version (`version`) [`String`] : Fabric Loader user friendly string

- Mod ID (`modid`) [`String`] : Mod identifier of the mod

- Protocol version (`protocol_version`) [`int`] : Ledger protocol version

## Server to client

### Action packet

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

Additional NBT: `String`

### Handshake

Sends information about Ledger to compatible clients

Channel: `ledger:handshake`

Buf content:

Protocol Version: `int` - Version of Ledger networking protocol. Latest version (Release 1.0.0) is `0`

Mod allowed: `boolean`

