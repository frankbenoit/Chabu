# Chabu

The **CHA**nnel **BU**ndle.  
Combine multiple channels/streams over a single TCP/IP or USB connection.

![](https://docs.google.com/drawings/d/15oxSz6B9SY3RSW1WfdeyTtsfdNgkCknp1TS090HhUGM/pub?w=803&h=277)

Each channel has a pair of bidirectional streams.  

Each stream has an own flow control mechanism. This means, the sender is paused when the receiver is not yet ready. The protocol is implemented in a way, no data is copied more then needed. So the payload is passed directly from the network API to the application, no internal buffering is needed.  

Why not use multiple TCP connections?  
Why not use multiple USB endpoints?  

 1. The USB standard only allowed a very limitted amount of endpoints. For embedded devices, the available resources might be too limitted to have many TCP connections.
 1. The transmition chain may go over different stages, TCP, USB, PCI. And all those stages need to know how to handle those mutliple connections in parallel. With Chabu, this is only a single connection and the underlaying stages don't know about the individual streams. The application on the higher level can easily change the channels configuration without changes on the lower levels.
 1. The transport can be implemented more efficiently. When many connection have only small data packets, each must be packed xmitted and confirmed individually. With Chabu, those small packets are composed to bigger packets and transferred together.
 
The protocol is is implemented different languages.

| Language | Status |
|----------|--------|
| Java     | Basics implemented. Not yet: DAVAIL, RESET |
| C        | Basics implemented. Not yet: DAVAIL, RESET                |
| C#       | Basics implemented. Not yet: DAVAIL, RESET. Help welcome! |
| VHDL+C   | Not yet done. Help welcome! |

## License

See the [LICENSE](LICENSE.md) file for license rights and limitations (MIT).

## Documentation
See the [Documentation](https://docs.google.com/document/d/1Wqa8rDi0QYcqcf0oecD8GW53nMVXj3ZFSmcF81zAa8g) 
