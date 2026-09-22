# AACP opcodes

AACP (Apple Accessory Communication Protocol) uses various opcodes to define different types of actions and commands. Each opcode is a 16-bit integer that specifies the kind of operation being performed. The opcode is sent in little-endian format as part of the AACP packet structure.

Topics without links have no standalone notes in this checkout.

| Opcode (Hex) | Destination | Description                                                        |
| ------------ | ----------- | ------------------------------------------------------------------ |
| 0x0001       | Accessory   | Unknown                                                            |
| 0x0004       | Host        | Battery report                          |
| 0x0006       | Host        | Ear detection                     |
| 0x0009       | Both        | [Control commands](control_commands.md)                      |
| 0x000D       | Accessory   | Audio source req                          |
| 0x000E       | Host        | Audio source resp                         |
| 0x000F       | Accessory   | Notification register            |
| 0x0010       | Accessory   | Smart routing relay           |
| 0x0011       | Host        | Smart routing response     |
| 0x0014       | Accessory   | Send connected device MAC                                          |
| 0x0017       | Both        | Multiple things - undocumented                                     |
| 0x0019       | Host        | Stem press                                  |
| 0x001B       | Accessory   | Timestamp                                    |
| 0x001D       | Host        | [Device Information](device-info.md)                         |
| 0x001E       | Accessory   | Rename device                                   |
| 0x0022       | Accessory   | Unknown                                                            |
| 0x0029       | Accessory   | Host capabilities (?) |
| 0x002B       | Host        | Paired devices (?)                                                 |
| 0x002D       | Accessory   | List of connected dev. req      |
| 0x002E       | Host        | List of connected devices    |
| 0x0030       | Accessory   | BLE keys req                                  |
| 0x0031       | Host        | BLE keys response                             |
| 0x004B       | Host        | Conversation awareness        |
| 0x004D       | Accessory   | Host capabilities                    |
| 0x004F       | Both        | Information req/res (doesn't work, even with apple's DID)          |
| 0x0053       | Both        | EQ data                                             |
