package io.pivotal.pde.sample.airline.ui;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;

import org.apache.geode.cache.CacheFactory;
import org.apache.geode.cache.query.CqAttributesFactory;
import org.apache.geode.cache.query.CqClosedException;
import org.apache.geode.cache.query.CqEvent;
import org.apache.geode.cache.query.CqException;
import org.apache.geode.cache.query.CqListener;
import org.apache.geode.cache.query.CqQuery;
import org.apache.geode.cache.query.FunctionDomainException;
import org.apache.geode.cache.query.NameResolutionException;
import org.apache.geode.cache.query.Query;
import org.apache.geode.cache.query.QueryInvalidException;
import org.apache.geode.cache.query.QueryInvocationTargetException;
import org.apache.geode.cache.query.QueryService;
import org.apache.geode.cache.query.RegionNotFoundException;
import org.apache.geode.cache.query.SelectResults;
import org.apache.geode.cache.query.TypeMismatchException;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import com.vaadin.annotations.Push;
import com.vaadin.annotations.Theme;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.data.Item;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.shared.communication.PushMode;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.ListSelect;
import com.vaadin.ui.OptionGroup;
import com.vaadin.ui.Table;
import com.vaadin.ui.TextField;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.Window;

import io.pivotal.pde.sample.airline.domain.Fare;
import io.pivotal.pde.sample.airline.domain.Flight;
import io.pivotal.pde.sample.airline.domain.FlightAvailability;
import io.pivotal.pde.sample.airline.keys.FlightKey;
import io.pivotal.pde.sample.gemfire.util.AvailBookClient;

/**
 * This UI is the application entry point. A UI may either represent a browser window 
 * (or tab) or some part of a html page where a Vaadin application is embedded.
 * <p>
 * The UI is initialized using {@link #init(VaadinRequest)}. This method is intended to be 
 * overridden to add component to the user interface and initialize non-component functionality.
 */
@Theme("mytheme")
@Push(PushMode.MANUAL)
public class MyUI extends UI {

	private ListSelect newAirportListSelect(String caption){
		ListSelect result = new ListSelect(caption);
		Set<String> airports = AppContext.getAirportRegion().keySetOnServer();
		for(String airportCode: airports) result.addItem(airportCode);
		
		result.setNullSelectionAllowed(false);
		result.setRows(1);
		result.setValue("ATL");
		return result;
	}
	
	
	private void showFlights(){
		//TODO there is totally no validation on any of the query inputs
		
		// Close both CQs
		if (flightCQ != null){
			try {
				flightCQ.close();
			} catch (CqException x){
				x.printStackTrace(System.err);
				throw new RuntimeException(x);
			}
			flightCQ = null;
		}
		
		if (availCQ != null){
			try {
				availCQ.close();
			} catch (CqException x){
				x.printStackTrace(System.err);
				throw new RuntimeException(x);
			}
			availCQ = null;
		}
		
		// clear Out the flight table
		flightTable.removeAllItems();
		
		// query Flights and Availability,  re-populate flight table
		String from, to, travelDate;
		from = (String) fromSelection.getValue();
		to = (String) toSelection.getValue();
		travelDate = this.travelDate.getValue();
		
		String queryString = String.format("SELECT * from /Flight WHERE origin = '%s' AND destination = '%s' AND departureDate = '%s'",from, to, travelDate);
		
		QueryService qs = CacheFactory.getAnyInstance().getQueryService();
		Query q = qs.newQuery(queryString);
		SelectResults<Flight> sr = null;
		try {
			sr = (SelectResults<Flight>) q.execute();
		} catch(FunctionDomainException | TypeMismatchException | NameResolutionException | QueryInvocationTargetException e){
			e.printStackTrace(System.err);
			throw new RuntimeException(e);
		}
		
		List<Flight> flights = sr.asList();
		Collections.sort(flights, new FlightComparator());
				
		if (flights.size() == 0)
			flightTable.setCaption(String.format("No flights from %s to %s on %s", from, to, travelDate));
		else if (flights.size() == 1)
			flightTable.setCaption(String.format("1 flight from %s to %s on %s", from, to, travelDate));
		else
			flightTable.setCaption(String.format("%d flights from %s to %s on %s", flights.size(), from, to, travelDate));
		
		flightTable.setPageLength(5);
		flightTable.setVisible(true);
				
		// call the availability Function for each Flight
		for(Flight f: flights){
			//TODO add a pricing line above or below the table
			ArrayList<FlightAvailability> avails = AvailBookClient.doAvail(f.getKey()); 
			int aAvailability=0, bAvailability= 0, cAvailability =0;
			for(FlightAvailability avail: avails){
				if (avail.getFareClass().equals("A")) 
					aAvailability = avail.getAvailableSeats();
				else if (avail.getFareClass().equals("B"))
					bAvailability = avail.getAvailableSeats();
				else if (avail.getFareClass().equals("C"))
					cAvailability = avail.getAvailableSeats();
			}
			
			Button purchaseButton = new Button("Book Now");
			purchaseButton.addStyleName("v-button-tiny");
			BookNowListener listener = new BookNowListener(this, f);
			purchaseButton.addClickListener(listener);  
			flightTable.addItem(new Object[]{f.getFlightNumber(), f.getDepartureTime(),f.getArrivalTime(), f.getCapacity(), f.getSeatsSold(),aAvailability,bAvailability,cAvailability, purchaseButton }, f.getKey());
		}
		
		//show fares
		fareLabelParagraph.setVisible(true);
		q = qs.newQuery("SELECT * FROM /Fare WHERE origin = $1 AND destination = $2 AND startingTravelDate <= $3 and endingTravelDate >= $3");
		SelectResults<Fare> fares = null;
		try {
			fares = (SelectResults<Fare>) q.execute(new Object[]{from, to, travelDate});
		} catch (FunctionDomainException | TypeMismatchException | NameResolutionException
				| QueryInvocationTargetException e1) {
			e1.printStackTrace();
			throw new RuntimeException(e1);
		}

		fareALabel.setValue("");
		fareBLabel.setValue("");
		fareCLabel.setValue("");

		for(Fare fare: fares){
			if (fare.getFareClass().equals("A"))
				fareALabel.setValue(String.format("Fare Class A - Premium Class, Refundable $%3.2f", fare.getFare()));
			else if (fare.getFareClass().equals("B"))
				fareBLabel.setValue(String.format("Fare Class B - Business Class $%3.2f", fare.getFare()));
			else if (fare.getFareClass().equals("C"))
				fareCLabel.setValue(String.format("Fare Class C - Economy Class, Saturday Stay, Not Refundable  $%3.2f", fare.getFare()));
		}
		
		// start up new flight CQ using existing flight query string
		CqAttributesFactory cqaf = new CqAttributesFactory();
		cqaf.addCqListener(new FlightListener(this));
		
		try {
			flightCQ = CacheFactory.getAnyInstance().getQueryService().newCq(queryString, cqaf.create());
			flightCQ.execute();
		} catch (QueryInvalidException | CqException  | CqClosedException | RegionNotFoundException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}		
		
		// start up new avail CQ 
		cqaf = new CqAttributesFactory();
		cqaf.addCqListener(new AvailListener(this));
		queryString = String.format("SELECT * from /FlightAvailability WHERE origin = '%s' AND destination = '%s' AND flightDate = '%s'",from, to, travelDate);
	
		try {
			availCQ = CacheFactory.getAnyInstance().getQueryService().newCq(queryString, cqaf.create());
			availCQ.execute();
		} catch (QueryInvalidException | CqException  | CqClosedException | RegionNotFoundException e) {
			e.printStackTrace();
			throw new RuntimeException(e);
		}		
	}
		
	private ListSelect fromSelection = null;
	private ListSelect toSelection = null;
	private TextField  travelDate = null;
	private Table      flightTable = null;
	private CqQuery flightCQ = null;
	private CqQuery availCQ = null;
	private Label fareALabel = null;
	private Label fareBLabel = null;
	private Label fareCLabel = null;
	private VerticalLayout fareLabelParagraph = null;
	
    @Override
    protected void init(VaadinRequest vaadinRequest) {
        final VerticalLayout layout = new VerticalLayout();
        layout.addComponent(new Label("<h1>GemAir Reservation System</h1>", ContentMode.HTML));
        
        layout.addComponent(new Label("<h2>Enter your travel plans below to see flight availability<h2>", ContentMode.HTML));
        
        HorizontalLayout twoColumnLayout = new HorizontalLayout();
        twoColumnLayout.setSpacing(true);
        layout.addComponent(twoColumnLayout);
        
        VerticalLayout queryBoxLayout = new VerticalLayout();
        queryBoxLayout.setSpacing(true);
        HorizontalLayout horizontal = new HorizontalLayout();
        horizontal.setSpacing(true);
        fromSelection = newAirportListSelect("From");
        toSelection = newAirportListSelect("To");
        travelDate = new TextField("Travel Date (e.g. 20170322)");
        
        horizontal.addComponent(fromSelection);
        horizontal.addComponent(toSelection);
        horizontal.addComponent(travelDate);
        horizontal.setSpacing(true);
        queryBoxLayout.addComponent(horizontal);
        
        Button showMeButton = new Button("Show Me");
        showMeButton.addClickListener(new Button.ClickListener() {
            public void buttonClick(ClickEvent event) {
            	showFlights();
            }
        });        
        queryBoxLayout.addComponent(showMeButton);
        twoColumnLayout.addComponent(queryBoxLayout);
        
        fareALabel = new Label("fareA");
        fareBLabel = new Label("fareB");
        fareCLabel = new Label("fareC");
        fareLabelParagraph = new VerticalLayout();
        fareLabelParagraph.setSpacing(true);
        fareLabelParagraph.addComponent(fareALabel);
        fareLabelParagraph.addComponent(fareBLabel);
        fareLabelParagraph.addComponent(fareCLabel);
        fareLabelParagraph.setVisible(false);
        
        Label spacer = new Label();
        spacer.setWidth("6em");
        twoColumnLayout.addComponent(spacer);
        twoColumnLayout.addComponent(fareLabelParagraph);
      
        
        flightTable = new Table("Set Me");
        flightTable.addContainerProperty("Flight", Integer.class, null);
        flightTable.addContainerProperty("Departure Time", String.class, null);
        flightTable.addContainerProperty("Arrival Time", String.class, null);
        flightTable.addContainerProperty("Capacity", Integer.class, null);
        flightTable.addContainerProperty("Seats Sold", Integer.class, null);
        flightTable.addContainerProperty("Available (A)", Integer.class, null);
        flightTable.addContainerProperty("Available (B)", Integer.class, null);
        flightTable.addContainerProperty("Available (C)", Integer.class, null);
        flightTable.addContainerProperty("Purchase", Button.class, null);
        
        flightTable.setVisible(false);
        layout.addComponent(flightTable);
        
        layout.setMargin(true);
        layout.setSpacing(true);
        
        setContent(layout);
    }

    
    @WebServlet(urlPatterns = "/*", name = "MyUIServlet", asyncSupported = true)
    @VaadinServletConfiguration(ui = MyUI.class, productionMode = false)
    public static class MyUIServlet extends VaadinServlet {
    	
		@Override
		public synchronized void destroy() {
			ClassPathXmlApplicationContext ctx = (ClassPathXmlApplicationContext) this.getServletContext().getAttribute(AppContext.SPRING_CONTEXT_ATTRIBUTE);
			if (ctx != null) ctx.close();
			super.destroy();
		}
        

		@Override        
        protected synchronized void servletInitialized()
                throws ServletException {
            super.servletInitialized();
			ClassPathXmlApplicationContext ctx = (ClassPathXmlApplicationContext) this.getServletContext().getAttribute(AppContext.SPRING_CONTEXT_ATTRIBUTE);
			if (ctx == null){
	        	ctx = new ClassPathXmlApplicationContext("application-context.xml");
	        	this.getServletContext().setAttribute(AppContext.SPRING_CONTEXT_ATTRIBUTE, ctx);
			}
        }
    	
    	
    }
    
    private static class FlightComparator implements Comparator<Flight>{

		@Override
		public int compare(Flight left, Flight right) {
			return left.getDepartureTime().compareTo(right.getDepartureTime());
		}
    	
    }
    
    // UI Updater Thread 
    private static class FlightUpdater implements Runnable {
    	private MyUI ui;
    	private Flight flight;
    	
    	public FlightUpdater(MyUI ui, Flight f){
    		this.ui = ui;
    		this.flight = f;
    	}
    	
    	@Override
    	public void run(){
			Item item = ui.flightTable.getItem(flight.getKey());
			if (item != null){
				item.getItemProperty("Seats Sold").setValue(flight.getSeatsSold());
				ui.push();
			}
    		
    	}
    }
    
    private static class AvailUpdater implements Runnable {
    	private MyUI ui;
    	private FlightAvailability avail;
    	
    	public AvailUpdater(MyUI ui, FlightAvailability f){
    		this.ui = ui;
    		this.avail = f;
    	}
    	
    	@Override
    	public void run(){
			Item item = ui.flightTable.getItem(new FlightKey(avail.getFlightNumber(), avail.getDate()));
			if (item != null){
				String itemPropName = String.format("Available (%s)", avail.getFareClass());
				item.getItemProperty(itemPropName).setValue(avail.getAvailableSeats());
				
				ui.push();
			}
    		
    	}
    }

    // CQ Listener
    private static class FlightListener implements CqListener {
    	
    	private MyUI ui;
    	
    	public FlightListener(MyUI ui){
    		this.ui = ui;
    	}
    	
		@Override
		public void close() {
		}

		@Override
		public void onError(CqEvent event) {
			System.err.println("error in CQ listener");
		}

		@Override
		public void onEvent(CqEvent event) {
			if (event.getQueryOperation().isUpdate()){
				Flight changedFlight = (Flight) event.getNewValue();
				System.out.println("Flight Changed: " + changedFlight.toString());
				ui.access(new FlightUpdater(ui, changedFlight));
			} 
			//NOT HANDLING REMOVES or ADDS in THIS DEMO
		}
    	
    }
	
    // CQ Listener
    private static class AvailListener implements CqListener {
    	
    	private MyUI ui;
    	
    	public AvailListener(MyUI ui){
    		this.ui = ui;
    	}
    	
		@Override
		public void close() {
		}

		@Override
		public void onError(CqEvent event) {
			System.err.println("error in CQ listener");
		}

		@Override
		public void onEvent(CqEvent event) {
			if (event.getQueryOperation().isUpdate() || event.getQueryOperation().isCreate()){
				FlightAvailability changedFlightAvail = (FlightAvailability) event.getNewValue();
				System.out.println("Flight Availability  Changed: " + changedFlightAvail.toString());
				ui.access(new AvailUpdater(ui, changedFlightAvail));
			} 
			//NOT HANDLING REMOVES or ADDS in THIS DEMO
		}
    	
    }
    
    // BookNow Click Listener
    private static class BookNowListener implements ClickListener {

    	private MyUI ui;
    	private Window thisWindow;
    	private Flight flight;
    	
    	private OptionGroup fareClass;
    	private TextField seatCount;
    	private Label message;
    	private Button cancelButton;
    	private Button purchaseButton;

    	public BookNowListener(MyUI ui, Flight flight){
    		this.ui = ui;
    		this.flight = flight;
    	}
    	
		@Override
		public void buttonClick(ClickEvent event) {
			thisWindow = new Window("Purchase Tickets");
			thisWindow.setModal(false);
			VerticalLayout content = new VerticalLayout();
			content.setMargin(true);
			content.setSpacing(true);
			
			message = new Label("", ContentMode.HTML);
			content.addComponent(message);
			
			fareClass = new OptionGroup("Fare Class");
			fareClass.addItems("A","B","C");
			fareClass.setItemCaption("A", ui.fareALabel.getValue());
			fareClass.setItemCaption("B", ui.fareBLabel.getValue());
			fareClass.setItemCaption("C", ui.fareCLabel.getValue());
			fareClass.setValue("B");
			content.addComponent(fareClass);
			
			seatCount = new TextField("How Many Seats ?");
			content.addComponent(seatCount);
			
			HorizontalLayout buttonLayout = new HorizontalLayout();
			buttonLayout.setSpacing(true);
			cancelButton = new Button("Cancel");
			cancelButton.addClickListener(new ClickListener(){

				@Override
				public void buttonClick(ClickEvent event) {
					thisWindow.close();
				}				
			});
			buttonLayout.addComponent(cancelButton);
			
			purchaseButton = new Button("Purchase");
			purchaseButton.addClickListener(new ClickListener(){
				@Override
				public void buttonClick(ClickEvent event) {
					int count = Integer.parseInt(seatCount.getValue());
					if (AvailBookClient.doBook(flight.getKey(), (String) fareClass.getValue(), count)){
						message.setValue("<h3>Purchase Successful</h3>");
						cancelButton.setCaption("Close");
					} else {
						message.setValue("<h3>Purchase Could Not Be Completed</h3>");						
					}
				}
			});
			
			buttonLayout.addComponent(purchaseButton);
			
			content.addComponent(buttonLayout);
						
			thisWindow.setContent(content);
			ui.addWindow(thisWindow);
		}
    	
    }
    
}
	