package bookTrading;

import jade.core.Agent;
import jade.core.AID;
import jade.core.behaviours.*;
import jade.lang.acl.ACLMessage;
import jade.lang.acl.MessageTemplate;
import jade.domain.DFService;
import jade.domain.FIPAException;
import jade.domain.FIPAAgentManagement.DFAgentDescription;
import jade.domain.FIPAAgentManagement.ServiceDescription;

public class BookBuyerAgent extends Agent {
  private BookBuyerGui myGui;
  private String targetBookTitle;
  //lista znanych sprzedawcow
  private AID[] sellerAgents;
  
	protected void setup() {
	  targetBookTitle = "";
	  System.out.println("Witam! Agent kupujacy "+getAID().getLocalName()+" czeka na dyspozycje kupna.");
	  myGui = new BookBuyerGui(this);
	  myGui.display();
		//interwal czasowy dla kupujacego pomiedzy wysylaniem kolejnych cfp
		//przekazywany jako argument linii polecen
		int interval = 20000;
		Object[] args = getArguments();
		if (args != null && args.length > 0) interval = Integer.parseInt(args[0].toString());
	  addBehaviour(new TickerBehaviour(this, interval)
	  {
		  protected void onTick()
		  {
			  //szukaj tylko jesli zlecony zostal tytul pozycji
			  if (!targetBookTitle.equals(""))
			  {
				  System.out.println("Szukam pozycji " + targetBookTitle);
				  //aktualizuj liste znanych sprzedawcow
				  DFAgentDescription template = new DFAgentDescription();
				  ServiceDescription sd = new ServiceDescription();
				  sd.setType("book-selling");
				  template.addServices(sd);
				  try
				  {
					  DFAgentDescription[] result = DFService.search(myAgent, template);
					  System.out.println("Znaleziono sprzedajacych:");
					  sellerAgents = new AID[result.length];
					  for (int i = 0; i < result.length; ++i)
					  {
						  sellerAgents[i] = result[i].getName();
						  System.out.println(sellerAgents[i].getLocalName());
					  }
				  }
				  catch (FIPAException fe)
				  {
					  fe.printStackTrace();
				  }

				  myAgent.addBehaviour(new RequestPerformer());
			  }
		  }
	  });
  }

	//metoda wywolywana przez gui, gdy skladana jest dyspozycja kupna ksiazki
	public void lookForTitle(final String title)
	{
		addBehaviour(new OneShotBehaviour()
		{
			public void action()
			{
				targetBookTitle = title;
				System.out.println("Poszukiwana ksiazka to " + targetBookTitle);
			}
		});
	}

    protected void takeDown() {
		myGui.dispose();
		System.out.println("Agent kupujacy " + getAID().getName() + " zakonczyl.");
	}
  
	private class RequestPerformer extends Behaviour {
	  private AID bestSeller;
	  private int bestPrice;
	  private int repliesCnt = 0;
	  private MessageTemplate mt;
	  private int step = 0;
	
	  public void action() {
	    switch (step) {
	    case 0:
	      //call for proposal (cfp) do znalezionych sprzedajacych
	      ACLMessage cfp = new ACLMessage(ACLMessage.CFP);
	      for (int i = 0; i < sellerAgents.length; ++i) {
	        cfp.addReceiver(sellerAgents[i]);
	      } 
	      cfp.setContent(targetBookTitle);
	      cfp.setConversationId("book-trade");
	      cfp.setReplyWith("cfp"+System.currentTimeMillis()); //unikalna wartosc
	      myAgent.send(cfp);
	      mt = MessageTemplate.and(MessageTemplate.MatchConversationId("book-trade"),
	                               MessageTemplate.MatchInReplyTo(cfp.getReplyWith()));
	      step = 1;
	      break;
	    case 1:
	      //odbior ofert od sprzedajacych
	      ACLMessage reply = myAgent.receive(mt);
	      if (reply != null) {
	        if (reply.getPerformative() == ACLMessage.PROPOSE) {
	          //otrzymano oferte
	          int price = Integer.parseInt(reply.getContent());
	          if (bestSeller == null || price < bestPrice) {
	            //jak na razie to najlepsza oferta
	            bestPrice = price;
	            bestSeller = reply.getSender();
	          }
	        }
	        repliesCnt++;
	        if (repliesCnt >= sellerAgents.length) {
	          //otrzymano wszystkie oferty -> nastepny krok
	          step = 2; 
	        }
	      }
	      else {
	        block();
	      }
	      break;
	    case 2:
	      //zakup najlepszej oferty
	      ACLMessage order = new ACLMessage(ACLMessage.ACCEPT_PROPOSAL);
          order.addReceiver(bestSeller);
	      order.setContent(targetBookTitle);
	      order.setConversationId("book-trade");
	      order.setReplyWith("order"+System.currentTimeMillis());
	      myAgent.send(order);
	      mt = MessageTemplate.and(MessageTemplate.MatchConversationId("book-trade"),
	                               MessageTemplate.MatchInReplyTo(order.getReplyWith()));
	      step = 3;
	      break;
	    case 3:      
	      //potwierdzenie zakupu przez agenta sprzedajacego
	      reply = myAgent.receive(mt);
	      if (reply != null) {
	        if (reply.getPerformative() == ACLMessage.INFORM) {
	          //zakup zakonczony powodzeniem
	          System.out.println(targetBookTitle+" kupiona za "+bestPrice+" od "+reply.getSender().getLocalName());
			  System.out.println("Czekam na nowa dyspozycje kupna.");
			  targetBookTitle = "";
	          //myAgent.doDelete();
	        }
	        else {
	          System.out.println("Zakup nieudany. "+targetBookTitle+" zostala sprzedana w miedzyczasie.");
	        }
	        step = 4;	//konczy cala interakcje, ktorej celem jest kupno
	      }
	      else {
	        block();
	      }
	      break;
	    }        
	  }
	
	  public boolean done() {
	  	if (step == 2 && bestSeller == null) {
	  		System.out.println(targetBookTitle+" nie ma w sprzedazy");
	  	}
	    //koniec jesli ksiazki nie ma w sprzedazy lub nie udalo sie kupic
	    return ((step == 2 && bestSeller == null) || step == 4);
	  }
	}

}
