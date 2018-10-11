# Akka Crawler

Application for crawling web sites and printing the list of all visited urls. 

>NOTE: Please use different url than the one specified in code. This one is just for illustration purposes.

## Getting Started

These instructions will explain you how to get a copy of the project up and running on your local machine for 
development and testing purposes.

### Prerequisites

In order to build the project you need to have [Java 1.8+](http://www.oracle.com/technetwork/java/javase/downloads/index.html) 
installed on your machine.

To check if Java is already installed on your machine, open the terminal window and run the following command:

    java --version
    
### Running Application 

> It is assumed that you've opened your terminal window and navigated to the directory of this document.

In order to run the application, you can execute the gradle `run` task with specified command line arguments `--args`. 
This command will compile the application and start the main class. 

    ./gradlew run --args="some arguments"

## Running Tests

    ./gradlew clean test

### Examples

> We should pass 1 argument when running the application and it should be the url of a web site we want to crawl.

* Diabetic patients die without insulin: 

    Command:
        
        ./gradlew run --args="http://www.burgerking.no/"
        
## Built With

* [Gradle](https://gradle.org)


