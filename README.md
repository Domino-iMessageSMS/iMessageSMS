# iMessageSMS

iMessageSMS - javaaddin that sends out SMS (via Twilio)

# Build

1) Install Maven
2) Add Notes.jar to your maven repo, that will make it accessible to Maven

```
mvn install:install-file -Dfile=path\to\Notes.jar
```

3) Build iMessageSMS.jar

```
mvn package
```

This should create a iMessageSMS.jar for you which you can deploy on Domino server after all.

# How to register iMessageSMS on Domino server

1) Upload file to Domino server (on Windows it's in the Domino executable folder).

JavaAddin\iMessageSMS.jar

2) Register Java addin in Domino environment (if you already have Addins there, keep in mind that separator is semicolon on Windows and colon on Linux) 

```
JAVAUSERCLASSES=.\JavaAddin\iMessageSMS.jar
```

# iMessageSMS.nsf - where to put it?

Put it under Domino\Data folder.
We used to read it using following code

```
m_database = m_session.getDatabase(null, "iMessageSMS.nsf");
```

# How to run iMessageSMS

```
load runjava iMessageSMS
```

# Example of commands

```
tell iMessageSMS help
```

```
tell iMessageSMS info
```

```
tell iMessageSMS quit
```
