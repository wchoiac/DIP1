Compilation Steps

There are total of 9 modules(APIUserManager, DoctorApp, FullNode, GenesisMaker, HospitalApp, KeyGen, UserApp, Validator and ValidatorClient) in this project. The required modules for each subject could be shown as follows:

Authorities
- Validator
- APIUserManager
- GenesisMaker
- KeyGen (optional - may use other software for creating public keys)
- ValidatorClient (i.e. registration software)

Medical Organization
- FullNode
- APIUserManager
- KeyGen (optional)
- DoctorApp (i.e. doctor software)
- HospitalApp (i.e. receptionist software)

Patient
-UserApp

All the apk and jar files are attached in jars folder. 

For jar files, if wished, each module provides a gradle task (xxxJar) for creating a jar executable file (e.g. fullNodeJar). Note that two tasks are provided by FullNode and Validator modules; fullNodeInitializerJar and fullNodeJar for FullNode, and validatorInitializerJar and validatorJar for Validator.

For apk files, it requires an Android OS device which runs at least Android 6 and above. Once the file is moved into the device, it can be installed.

==========================================
Environment Setup Steps

VERY BEGINNING
- Create EC 256 bit (secp256k1) key pairs for initial authorities
- Get all the initial authorities public keys and use genesisBlockMaker.jar for creating a genesisblock

Authority
1. Setup a Validator node
	1.1 Create EC 256 bit (secp256k1) key pair
	1.2 Create two RSA 2048 bits keypairs for API and blockchain connections (with KeyGen or other software)
	1.3 Place all three key pairs (PEM files) and genesisblock into a place where the node is going to be setup
	1.4 Run validatorInitializer.jar at that directory and follow each step in the shown list to setup the environment
	1.5 Run apiManager.jar at the same directory for registering API users
	1.6 Remove all the key pairs (PEM files)
	1.7 Run validator.jar at the same directory for starting the node

2. Use Registration Software
	2.1 java -jar validatorClient.jar
	2.2 type API user details to login

Medical Organization
1. Setup Full node
	1.1 Create EC 256 bit (secp256k1) key pair and obtain certificate from an authority (with KeyGen or other software)
	1.2 Create two RSA keypairs for API and blockchain connections
	1.3 Place all three key pairs (PEM files) and genesisblock into a place where the node is going to be setup
	1.4 Run fullNodeInitializer.jar at that directory and follow each step in the shown list to setup the environment
	1.5 Run apiManager.jar at the same directory for registering API users
	1.6 Remove all the key pairs (PEM files)
	1.7 Run fullNode.jar at the same directory for starting the node

2. Setup Database
	2.1 Please follow the instruction on microsoft.com to download and install a SQL server
		Windows - https://docs.microsoft.com/en-us/sql/database-engine/install-windows/installation-for-sql-server?view=sql-server-2017
		Linux - https://docs.microsoft.com/en-us/sql/linux/sql-server-linux-overview?view=sql-server-2017
	2.2 Create a database after login to SQL server
		- CREATE DATABASE Customer
	2.3 Switch context to "Customer" database
		- USE Customer
	2.3 Create a table "Customer" for storing all medical records
		- CREATE TABLE Customer (PatientName varchar(50), Gender varchar(1), ID 		varchar(50), [Date of birth] varchar(50), Medname varchar(400), Records 			varchar(8000), NewRecords BIT, Timestamp varchar(50), phoneNum 			varchar(50), nationality varchar(200), extra varchar(8000), address varchar(8000), 		patientIdentifier varchar(8000))
	2.4 Create a table "PatientList" for registering patients
		- CREATE TABLE PatientList(PatientName varchar(50), ID varchar(50), 			encodePublicKey varchar(8000), patientIdentifier varchar(8000))


3. Use Receptionist Software
	3.1 java -jar validatorClient.jar
	3.2 type API and database user details to login

4. Use Doctor Software
	4.1 java -jar validatorClient.jar
	4.2 type database user details to login

Patient
1. Download apk file or build apk file using Android Studio and move it to the Android device
2. Install the apk file
3. Run the MediRec Application
4. Register PIN and use
	4.1 To show identity to validator or medical organization, use "GET RECORDS"
	4.2 To generate random key for encrypting the data, use "GENERATE KEY"
	4.3 To change PIN, use "CHANGE PIN"
	4.4 To export the key pair, use "EXPORT IDENTITY" - This requires the user to input another password for encryption
	4.5 To import the exported key pair, use "IMPORT IDENTITY" - This requires the user to input the password for decryption