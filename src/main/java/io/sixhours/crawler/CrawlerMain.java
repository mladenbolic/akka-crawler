package io.sixhours.crawler;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import io.sixhours.crawler.CrawlSupervisor.StartCrawling;
import java.io.IOException;

public class CrawlerMain {

  public static void main(String[] args) throws IOException {
    ActorSystem system = ActorSystem.create("crawler-system");

    try {
      ActorRef crawlSupervisor = system.actorOf(CrawlSupervisor.props(), CrawlSupervisor.NAME);
      crawlSupervisor.tell(new StartCrawling("http://www.burgerking.no/"), ActorRef.noSender());

      System.out.println("Press ENTER to exit the system");
      System.in.read();
    } finally {
      system.terminate();
    }
  }

}