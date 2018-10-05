package io.sixhours.crawler;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import io.sixhours.crawler.CrawlSupervisor.StartCrawling;

public class CrawlerMain {

  public static void main(String[] args) {
    ActorSystem system = ActorSystem.create("crawler-system");

    ActorRef crawlSupervisor = system
        .actorOf(CrawlSupervisor.props("http://www.burgerking.no/"), CrawlSupervisor.NAME);

    crawlSupervisor.tell(new StartCrawling(), ActorRef.noSender());
  }
}