<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

# Care Chain Project

Welcome to the our Corda project Care Chain which is a block chain patient record management system.

### How to Run
1. Clone or download the project to you local machine.
2. Open the project in `IntelliJ` make sure to select `Gradle` leaving the defaults.
3. Open the terminal window in the project directory.
4. Build the nodes with our Cordapp using the following command.
  * Unix/Mac OSX :  `./gradlew deployNodes`
  * Windows :       `gradlew.bat deployNodes`
5. After the build finishes, you will see the generated nodes in the java-source/build/nodes folder
6. Start the nodes by running the following command.
  * Unix/Mac OSX :  `java-source/build/nodes/runnodes`
  * Windows :       `java-source/build/nodes/runnodes.bat`
  
### Interacting with the example CorDapp
### Via HTTP

The nodes’ webservers run locally on the following ports:

 * PartyA: localhost:10009
 * PartyB: localhost:10012
 * PartyC: localhost:10015

These ports are defined in each node’s node.conf file under java-source/build/nodes/NodeX/node.conf.

Each node webserver exposes the following endpoints:

 * /api/example/me
 * /api/example/peers
 * /api/example/ious
 * /api/example/create-iou

There is also a web front-end served from /web/example.

