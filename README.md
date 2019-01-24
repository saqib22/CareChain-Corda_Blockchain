# Care Chain Project

<p align="center">
  <img src="https://www.corda.net/wp-content/uploads/2016/11/fg005_corda_b.png" alt="Corda" width="500">
</p>

Welcome to the our Corda project Care Chain which is a block chain patient record management system.
Medical records should be stored on a distributed ledger or blockchain using tools such as Hyperledger Composer. The blockchain is shared with the patient, healthcare provider, insurer, and the government. In this case, the government acts as the regulator.

All the medical information related to the patient such as past and present ailments, treatments, family history of medical problems will be stored in the blockchain. This will make every record permanent, transferable and accessible which will prevent the medical records from being lost or modified.

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

