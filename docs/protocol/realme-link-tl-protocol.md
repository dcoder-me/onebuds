# Realme Link earbuds protocol (tl stack)

Source: realme Link 5.8.811.2814 (com.realme.link), decompiled with JADX 1.5.6.
Package: `com.realme.iot.headset.tl`. Names in the app are obfuscated; meanings below come from log strings and usage.

## Channel

- RFCOMM UUID `0000079A-D102-11E1-9B23-00025B00A5A5` (`tl/internal/connect/common/BaseBRDevice.java`)
- Max frame size the app assumes: 2000 bytes (`OPPOv1Wrapper.o`)

## Frame (`tl/internal/message/wrapper/OPPOv1Wrapper.java`)

```
AA | LEB128 length | control | 00 | [fragment index] | packet
```

- `control` bits 0-1 = fragment state: `0` whole, `1` first, `2` middle, `3` last
- `control` bit 2 (`0x04`) and bit 3 (`0x08`) = flags set by the sender; seen as `04` in replies
- Fragment index byte is present only when fragment state is not `0`
- A fragmented message is the packets of all fragments joined in order, reassembled on state `3`

## Packet (`tl/protocol/packet/Packet.java`)

```
command (2, LE) | seq (1) | payload length (2, LE) | payload
```

- Reply command = request command | `0x8000`
- Reply payload byte 0 = status (`0` = ok)
- Seq runs 0..255 per device and wraps (`PacketFactory.f`)

## Connect sequence (`PollCommandManager.i`)

1. `0x0100` get capabilities -> `0x8100`: status, then a bitmap
2. Each set bit `n` unlocks the commands in `Protocol.b2[n]` (table below)
3. The app then queries every supported state (battery, ANC, EQ, keys, switches, ...)
4. `0x0200` get notification capabilities -> `0x8200`, then `0x0202` register each event
   (or `0x0205` register a set at once on newer models)

Always allowed without the bitmap: `0x0100`-`0x0104`, `0x010B`, `0x0F00`.

## Capability bit -> commands (`Protocol.b2`)

| Bit | Query | Set | Meaning (from log text) |
|---|---|---|---|
| 0 | 0x0105 | | |
| 1 | 0x0106 | | battery |
| 2 | 0x0107 | | |
| 3 | 0x0108 | 0x0401 | key / gesture functions |
| 4 | 0x0109 | | earbud status (in case / in ear) |
| 5 | | 0x0400 | find my buds |
| 6 | | 0x0402 | "free music" (buds -> phone, phone must reply `00`) |
| 7 | 0x010D | 0x0403 | feature switches |
| 8 | 0x010C | 0x0404 | noise control (ANC / transparency) |
| 9 | | 0x0405 | ear fit test |
| 10 | 0x010F | 0x0406 | EQ preset |
| 11 | 0x0110 | 0x0407 | high volume |
| 13 | | 0x0408 | |
| 14 | | 0x0409 | |
| 15 | 0x0111 | 0x040A | auto power-off time |
| 19 | 0x0115, 0x0116 | 0x040D, 0x040E | hearing detection |
| 25 | 0x0118 | 0x0411 | restore data |
| 26 | 0x011A | 0x0412 | personalised noise reduction |
| 29 | 0x0112 | 0x040B | multipoint (dual device) |
| 30 | 0x011E, 0x011F | 0x0415 | ear scan |
| 34 | 0x0122 | 0x0418 | custom EQ bands |
| 38 | 0x0124 | 0x041B | bass engine |
| 43 | | 0x041E | spatial audio |
| 46 | 0x0128 | 0x050E | |
| 58 | 0x0131 | 0x0428 | AI / translation status |
| 60 | | 0x042C | mic state |

## Set payloads (`SetCommandManager.java`)

| Command | Payload |
|---|---|
| 0x0400 find buds | `[on 1/0]` |
| 0x0401 keys | `[count]` then per key `[device side, button, action, function]` |
| 0x0403 switch | `[feature id, on 1/0]` |
| 0x0404 noise | `[action 1/2, type 1/2, value]` |
| 0x0406 EQ preset | `[preset id]` |
| 0x0407 high volume | `[on 1/0]` |
| 0x040A auto power-off | `[value]` |
| 0x0418 custom EQ | see `EqInfo` / `SetCommandManager.p` |
| 0x041B bass engine | 4 bytes, see `SetCommandManager.l` |
| 0x041E spatial audio | `[type]` |

Query for current noise mode: `0x010C` with payload `[01 01]`.

## Still to confirm on a real T310

- Which capability bits the T310 sets (log the `0x8100` reply)
- Noise `value` meanings for off / ANC / transparency (log `0x810C` while switching in Realme Link)
- Feature switch ids used by the T310 (log `0x810D`)
- T310 is not named in the app code; model features come from the server by product id, so the bitmap is the source of truth
