package template; 
 
//the list of imports 
import java.util.ArrayList; 
import java.util.List; 
import java.util.Random; 
import java.util.*; 
import java.util.HashMap; 
 
import logist.Measures; 
import logist.behavior.AuctionBehavior; 
import logist.agent.Agent; 
import logist.simulation.Vehicle; 
import logist.plan.Plan; 
import logist.task.Task; 
import logist.task.TaskDistribution; 
import logist.task.TaskSet; 
import logist.topology.Topology; 
import logist.topology.Topology.City; 
 
/** 
 * A very simple auction agent that assigns all tasks to its first vehicle and 
 * handles them sequentially. 
 *  
 */ 
@SuppressWarnings("unused") 
public class AuctionTemplate implements AuctionBehavior { 
 
    private Topology topology;
    private TaskDistribution distribution; 
    private Agent agent; 
    private Random random; 
    private Vehicle vehicle; 
    private City currentCity;
    private List<Vehicle> vehicles;
    HashMap <Task, Long> myBid = new HashMap <Task, Long> ();
    HashMap <Task, Long> oppBid = new HashMap <Task, Long> ();
    ArrayList <Double> ratioList = new ArrayList <Double> ();
    ArrayList <Double> ratioCum = new ArrayList <Double> ();
    ArrayList <Double> ratioAvg = new ArrayList <Double> ();
    private double damping = 0.6;
    private double myPart = 0.4;
    private double lastRatio = 1;
    
    HashMap <Integer, Double> oppMargin = new HashMap <Integer, Double> ();
    HashSet <Task> myTask = new HashSet <Task> ();
    HashSet <Task> oppTask = new HashSet <Task> ();
    
    @Override 
    public void setup(Topology topology, TaskDistribution distribution, 
            Agent agent) { 
 
        this.topology = topology; 
        this.distribution = distribution; 
        this.agent = agent; 
        this.vehicle = agent.vehicles().get(0);
        this.vehicles = agent.vehicles();
        this.currentCity = vehicle.homeCity();
 
        long seed = -9019554669489983951L * currentCity.hashCode() * agent.id(); 
        this.random = new Random(seed); 
    } 
 
    @Override
    public void auctionResult(Task previous, int winner, Long[] bids) {
        int myself = agent.id();
        int opp = 1- agent.id();
    	
        if (winner == myself) {              //i am the winner 
        	System.out.println("I win!!!!!!!!!!!!!");
            myTask.add(previous); 
        }	else{                                    // he is the winner
        		System.out.println("I lose................");
        		oppTask.add(previous);
        } 
        
        System.out.println("Myself" +" bids for " + bids[myself]);
    	System.out.println("Opponent" + " bids for " + bids[opp]);
    	
        myBid.put(previous, bids[myself]);
        oppBid.put(previous, bids[opp]);
        if (this.oppMargin.get(previous.id) > 0) {
        	double ratio = (double) (bids[opp] / this.oppMargin.get(previous.id));
        	if (ratio < 3) {
        		System.out.printf("The realtime ratio is : %.2f \n", ratio);
            	ratioList.add(ratio);
//            	double newRatio = damping*lastRatio + (1 - damping)*ratio;
//            	lastRatio = newRatio;
//            	ratioCum.add(newRatio);
        	} else {
        		System.out.printf("The realtime ratio is : %.2f \n", ratio);
//            	ratioList.add(ratio);
//            	double newRatio = damping*lastRatio + (1 - damping)*((double) 2);
//            	lastRatio = newRatio;
//            	ratioCum.add(newRatio);
        	}
        }
    }
     
    public double getCumRatio() { 
     if(ratioCum.size() == 0 ) {
    	 ratioCum.add(lastRatio);
    	 return 1;
     } else return ratioCum.get(ratioCum.size() - 1);
    }
    
    public double getRatio(){ 
        double avg = 0;
        double total = 0;
         
        for(double ratio : this.ratioList) { 
            total = total + ratio;
        }
        
        if(ratioList.size() == 0) {
        	avg = 1;
        } else {
        	 avg = total / ratioList.size();
        	 this.ratioAvg.add(avg);
        }
        return avg;
    }
     
    @Override
    public Long askPrice(Task task) {     	
        if (vehicle.capacity() < task.weight) 
            return null;
    	
        double myMarginCost = getMarginCost(myTask, task);
        double oppMarginCost = getMarginCost(oppTask, task);
        System.out.println("*******************************");
        System.out.println("Myself marginal cost: " + myMarginCost);
    	System.out.println("Opponent marginal cost: " + oppMarginCost);
        
        this.oppMargin.put(task.id, oppMarginCost);

        double ratio = getRatio();
//      double ratio = getCumRatio();
        System.out.printf("The Average ratio is : %.2f \n", ratio);
        double bid = 0;
        if ((oppMarginCost * ratio) >= myMarginCost)
            bid = (oppMarginCost * ratio*(1-myPart) + myMarginCost*myPart);        //double bid = ratio * MarginCost;
        else 
            bid = myMarginCost * 1.1;
        return (long) Math.round(bid);
    }
 
    @Override
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) {
    	
    	System.out.println("*******************************");
        System.out.println("MySelf has tasks number" + tasks.size());
        System.out.println("Opponent has tasks number" + oppTask.size());
        
        System.out.println("*************Ratio in Realtime******************");
        for (double ratio : this.ratioList) {
        	System.out.printf("%.2f,  ", ratio);
        }
        System.out.println("");
        
        System.out.println("****************Ratio Average for use***************");
        for (double ratio : this.ratioAvg) {
        	System.out.printf("%.2f,  ", ratio);
        }
        System.out.println("");

        
        HashSet<Task> tsk = new HashSet<Task> ();
        for(Task t:tasks) {
        	tsk.add(t);
        }
 
        CSP csp = new CSP(vehicles, tsk);
        Encode Aold = csp.Initialize();
         
        csp.displayEncode(Aold);
        System.out.println(csp.computeCost(Aold));
        Encode Aoptimal = csp.SLS(Aold);
         
        csp.displayEncode(Aoptimal);
        System.out.println(Aoptimal.cost);
 
        List<Plan> optimalPlans = csp.computePlan(Aoptimal);
        return optimalPlans;
    }
     
    public double getMarginCost(HashSet<Task> tasksPre, Task newTask) {
        double cost;
        double costPre;
        CSP csp;
        HashSet<Task> tasks = new HashSet<Task> ();
        
        for(Task task : tasksPre) {
        	tasks.add(task);
        }
    
        if (tasks.size() == 0) {
        	costPre = 0;
        } else{
    		csp = new CSP(this.vehicles, tasks);
    		Encode Aold = csp.Initialize();
    		costPre = csp.computeCost(Aold);
        }
        
        tasks.add(newTask); 		//Add new task;
        
        csp = new CSP(this.vehicles, tasks);
        Encode Aold1 = csp.Initialize();
        double costAfter = csp.computeCost(Aold1);
        return costAfter - costPre;
    } 
     
 
}

