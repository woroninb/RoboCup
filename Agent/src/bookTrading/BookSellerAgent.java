package bookTrading;

import jade.core.Agent;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

import java.util.*;

public class BookSellerAgent extends Agent {
  private Hashtable catalogue;
  private BookSellerGui myGui;

  protected void setup() {
    catalogue = new Hashtable();
    myGui = new BookSellerGui(this);
    myGui.display();

    //rejestracja sprzedazy w katalogu df
    DFAgentDescription dfd = new DFAgentDescription();
    dfd.setName(getAID());
    ServiceDescription sd = new ServiceDescription();
    sd.setType("book-selling");
    sd.setName("JADE-book-trading");
    dfd.addServices(sd);
    try {
      DFService.register(this, dfd);
    }
    catch (FIPAException fe) {
      fe.printStackTrace();
    }
    
    addBehaviour(new OfferRequestsServer());

    addBehaviour(new PurchaseOrdersServer());
  }

  protected void takeDown() {
    //wyrejestrowanie sprzedazy z katalogu df
    try {
      DFService.deregister(this);
    }
    catch (FIPAException fe) {
      fe.printStackTrace();
    }
  	myGui.dispose();
    System.out.println("Agent sprzedajacy "+getAID().getName()+" zakonczyl.");
  }

  //metoda wywolywana przez gui, gdy dodawana jest nowa pozycja
  public void updateCatalogue(final String title, final int price) {
    addBehaviour(new OneShotBehaviour() {
      public void action() {
		catalogue.put(title, new Integer(price));
		System.out.println(title+" umieszczona w katalogu. Cena = "+price);
      }
    } );
  }
  
	private class OfferRequestsServer extends CyclicBehaviour {
	  public void action() {
	    //tylko oferty
		MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.CFP);
		ACLMessage msg = myAgent.receive(mt);
	    if (msg != null) {
	      String title = msg.getContent();
	      ACLMessage reply = msg.createReply();
	      Integer price = (Integer) catalogue.get(title);
	      if (price != null) {
	        //pozycja istnieje w katalogu, zwroc cene jako oferte
	        reply.setPerformative(ACLMessage.PROPOSE);
	        reply.setContent(String.valueOf(price.intValue()));
	      }
	      else {
	        //pozycji nie ma w katalogu
	        reply.setPerformative(ACLMessage.REFUSE);
	        reply.setContent("not-available");
	      }
	      myAgent.send(reply);
	    }
	    else {
	      block();
	    }
	  }
	}

	
	private class PurchaseOrdersServer extends CyclicBehaviour {
	  public void action() {
	    //tylko zlecenia kupna, ktore stanowia akceptacje oferty
		MessageTemplate mt = MessageTemplate.MatchPerformative(ACLMessage.ACCEPT_PROPOSAL);
		ACLMessage msg = myAgent.receive(mt);
	    if (msg != null) {
	      String title = msg.getContent();
	      ACLMessage reply = msg.createReply();
	      Integer price = (Integer) catalogue.remove(title);
	      if (price != null) {
	        reply.setPerformative(ACLMessage.INFORM);
	        System.out.println(title+" sprzedana agentowi "+msg.getSender().getLocalName());
	      }
	      else {
	        //pozycji nie ma w katalogu, poniewaz w miedzyczasie (juz po zlozeniu oferty) zostala sprzedana innemu agentowi
	        reply.setPerformative(ACLMessage.FAILURE);
	        reply.setContent("not-available");
	      }
	      myAgent.send(reply);
	    }
	    else {
		  block();
		}
	  }
	}

}
