#GDX-Proto - A lightweight 3D engine built with [libgdx](http://libgdx.badlogicgames.com)

When I decided to create GDX-Proto, I had been playing with libgdx's 3D API for about 6 months on and off. I often see libgdx users at a loss when it comes to starting with 3D.  The 2D side of libgdx is well documented and has many examples and tutorials, but when it comes to 3D, there are much less resources available (side note: Xoppa's [3D tutorial series](http://blog.xoppa.com/basic-3d-using-libgdx-2/) are a must read!)

I made GDX-Proto to show how one might implement all the basic pieces you need to create a simple FPS (first person shooter) with libgdx.  I don't recommend using GDX-Proto to create a game, since it might be a little rough in some areas, but I think it is a great resource to see how one could implement physics, graphics, and networking for a 3d game.

![screenshot](img/fps-demo-screen2.jpg)

## Overview of Features

### Graphics
- Basic 3d rendering with the new libgdx 3D API.
- 3D Particle system based on the new libgdx 3d particle system (version 1.2.1+, not included in 1.2.0)
- Game entities that can be rendered as 3d models or flat sprites that face the camera

### Physics
- The Bullet physics library is used for collision detection, but not for collision resolution.  This allows for fast and efficient collision detection without the performance hit of a fully simulated bullet world.  A default collision resolution system is included in the Physics class.
- Raycasting for projectile hit detection 

### Networking
- Supports local or online play
- KryoNet based
- Mix of TCP and UDP where appropriate
- Entity interpolation
- Client prediction (for movement only, not yet implemented for projectiles)
- Simple chat system
- Supports libgdx headless backend for creating a headless server, such as on a VPS
- Server transmits level geometry to client upon connection
- "The server is the man": Most logic is run server-side to prevent cheats or hacking.

### Other
- Basic Entity system with DynamicEntities, represented by either Decals (Billboard sprites) or 3D models
- Movement component class handles acceleration, velocity, position, rotation, max speeds
- Subclasses of Movement: GroundMovement and FlyingMovement

## Demo - First Person Shooter

**A pre-built jar of the [desktop demo can be downloaded here](https://github.com/jrenner/gdx-proto/blob/master/bin/desktop-1.0.jar?raw=true).**

**[Android APK](https://github.com/jrenner/gdx-proto/blob/master/bin/android.apk?raw=true)**
The apk is currently setup just to launch a localclient

To play a simple local, singleplayer demo run the `desktop:run` gradle task
There are also many command line options available, such as setting screen size, starting a server, choosing what hostname to connect to, etc. To see a full list:
Run the `desktop:dist` gradle task to create the desktop jar.
the desktop jar file should be located in: `desktop/build/libs`
Then run the desktop jar with the command argument `-h` or `--help`

To start a local, non-networked single-player demo:
`java -jar desktop.jar`
To start a server:
`java -jar desktop.jar -s`
To start a server and connect to it yourself:
`java -jar desktop.jar -s -c`
To connect to an online server, www.example.com, port 19000:
`java -jar desktop.jar -c -a "www.example.com" -p 19000`

If you wish to run a headless server, run the `:headless:dist` gradle task, and run:
`java -jar headless.jar -s`

Here is an example usage output, but for a more up-to-date version run -h on the command line.
```
 -a,--address <arg>      supply hostname address to connect to
 -c,--client             connect to server as a client
 -d,--lag-delay <arg>    simulate lag with argument = milliseconds of lag
 -h,--help               print help
 -p,--port <arg>         specify TCP port to either host on (server) or
                         connect to (client)
 -s,--server             start online server
 -z,--screensize <arg>   suppley screen size in the form of WIDTHxHEIGHT,
                         i.e. 1920x1080
```

## Contributions
Contributions are welcome! If you have a large contribution you wish to make, make an issue first for discussion to make sure it's something that could be included in the project.

## TODO
- Profile and improve networking
- General refactoring, check all disposables are disposed (avoid memory leaks)
- Implement frustum and distance culling

## License
Apache 2.0
