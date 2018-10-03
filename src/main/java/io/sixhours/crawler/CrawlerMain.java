package io.sixhours.crawler;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import io.sixhours.crawler.CrawlSupervisor.StartCrawling;
import java.io.IOException;

public class CrawlerMain {

//  public static void startCrawling(){
//    ActorSystem system = ActorSystem.create("crawler-system");
////    ActorRef master = system.actorOf(
////        Props.create(ParallelMaster.class, new IndexerImpl(writer), new HtmlParserPageRetriever(path)));
////    master.tell(path, actorSystem.guardian());
////    actorSystem.awaitTermination();
//    ActorRef crawlSupervisor = system.actorOf(CrawlSupervisor.props(), CrawlSupervisor.NAME);
//    crawlSupervisor.tell(new StartCrawling("http://www.burgerking.no/"), ActorRef.noSender());
//  }

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