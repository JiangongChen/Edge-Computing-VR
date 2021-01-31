# Demo application 

The source code of the VR demo, including four versions of the server and a client.

The four servers are:
Pose only: only synchronize the pose between all clients. 
Server: thin client application, server stream both pose and frame, but no prediction inside, directly deliver last frame
Server with prediction: predict the pose in the next time slot, which is our design
Server composition: merger the above three versions together, to compare their performance

The client adapts some codes in the [FireFly](https://www.usenix.org/conference/atc20/presentation/liu-xing) paper. 

All of the frames are generated offline. 
