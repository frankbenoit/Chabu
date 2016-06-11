# Chabu

The CHAnnel BUndle.  
Combine multiple channels/streams over a single TCP/IP or USB connection.

![](https://docs.google.com/drawings/d/15oxSz6B9SY3RSW1WfdeyTtsfdNgkCknp1TS090HhUGM/pub?w=803&h=277)

Each channel has a pair of bidirectional streams.  

Each stream has an own flow control mechanism. This means, the sender is paused when the receiver is not yet ready. The protocol is implemented in a way, no data is copied more then needed. So the payload is passed directly from the network API to the application, no internal buffering is needed.  

The protocol is is implemented different languages.

 1. Java is the first implementation. The library has no external dependencies. It can be integrated in OSGI.
 1. C# in progress 
 1. C for embedded devices in progress 
 1. VHDL for embedded devices having an FPGA connected to a USB device. Implementation is pending.

## License

See the [LICENSE](LICENSE.md) file for license rights and limitations (MIT).

## Documentation
See the [Documentation](https://docs.google.com/document/d/1Wqa8rDi0QYcqcf0oecD8GW53nMVXj3ZFSmcF81zAa8g) 
