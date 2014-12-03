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
    HashMap <Task, Double> ratio = new HashMap <Task, Double> (); 

    HashMap <Integer, Double> oppMargin = new HashMap <Integer, Double> ();
    //TaskSet myTask = null; 
    //TaskSet oppTask = null;
    HashSet <Task> myTask = new HashSet <Task> ();
    HashSet <Task> oppTask = new HashSet <Task> ();

    public int numMyTasks = 0;
    public int numOppTasks = 0;
    //ArrayList<Task>myTask = new ArrayList<Task>(); 
    //ArrayList<Task>oppTask = new ArrayList<Task>(); 
     
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
    	//System.out.println("agent id : " + agent.id());
    	//System.out.println("winner : " + winner);
        int myself = agent.id();
        int opp = 1- agent.id();
    	System.out.println("Myself" +" bids for " + bids[myself]);
    	System.out.println("Opponent" + " bids for " + bids[opp]);
    	
        if (winner == myself) {              //i am the winner 
        	System.out.println("I win!!!!!!!!!!!!!");
        	System.out.println("*******************************");
            //currentCity = previous.deliveryCity; 
            myTask.add(previous); 
            numMyTasks = numMyTasks + 1;
        } 
        else{                                    // he is the winner 
        	System.out.println("I lose................");
        	System.out.println("*******************************");
            oppTask.add(previous);
            numOppTasks = numOppTasks + 1;
        } 
            myBid.put(previous, bids[myself]);
            oppBid.put(previous, bids[opp]);
//            System.out.println(this.oppMargin);
//            System.out.println(previous);
//            System.out.println(this.oppMargin.get(previous.id));
//            System.out.println(bids[opp]);
            ratio.put(previous, (double) (bids[opp] / this.oppMargin.get(previous.id)));
        //System.out.println("winneris:" + winner); 
        //System.out.println(bids.length); 
 
        //for(long i : bids) { 
            //System.out.print(i + " "); 
        //} 
        //System.out.print("\n"); 
    } 
     
    public double getRatio(){ 
        double avg = 0;
        double total = 0; 
         
        for(Map.Entry<Task, Double> entry : ratio.entrySet()){ 
            total = total + (double) entry.getValue(); 
        }
        
        if(ratio.size() == 0) {
        	avg = 1;
        } else {
        	 avg = total / ratio.size();
        }
        return avg;
    }
     
    @Override 
    public Long askPrice(Task task) {     	
        if (vehicle.capacity() < task.weight) 
            return null; 
 
        //double MarginCost = Measures.unitsToKM(distanceSum 
        //        * vehicle.costPerKm());
        
        double myMarginCost = getMarginCost(myTask,task,numMyTasks);
        double oppMarginCost = getMarginCost(oppTask,task, numOppTasks);
        this.oppMargin.put(task.id, oppMarginCost);

        //double ratio = 1.0 + (random.nextDouble() * 0.05 * task.id);
        double ratio = getRatio();
        double bid = 0;
        if ((oppMarginCost * ratio) >= myMarginCost)
            bid = oppMarginCost * ratio * 0.995;        //double bid = ratio * MarginCost; 
        else 
            bid = myMarginCost * 1.2;
        return (long) Math.round(bid); 
    }
 
    @Override 
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) { 
         
        System.out.println("MySelf has tasks number" + tasks.size());
        System.out.println("Opponent has tasks number" + oppTask.size());
        
        HashSet<Task> tsk = new HashSet<Task>();
        for(Task t:tasks){
        	tsk.add(t);
        }
 
        CSP csp = new CSP(vehicles, tsk);
        Encode Aold = csp.Initialize();
         
        csp.displayEncode(Aold);
        System.out.println(csp.computeCost(Aold));
        Encode Aoptimal = csp.SLS(Aold);
         
        csp.displayEncode(Aoptimal); 
        System.out.println(Aoptimal.cost); 
        //System.out.print(csp.computeCost(Aoptimal)); 
 
        List<Plan> optimalPlans = csp.computePlan(Aoptimal);
        return optimalPlans;
    } 
     
    public double getMarginCost(HashSet<Task> tasksPre, Task newTask, int numTasks) {
        double cost; 
        double costPre;
        CSP csp;
        if (numTasks == 0){
        	costPre = 0;
        	//tasksPre.add(newTask);
        }
        else{
        	csp = new CSP(this.vehicles, tasksPre);
            Encode Aold = csp.Initialize(); 
            costPre = csp.computeCost(Aold);
        }
        //CSP csp = new CSP(this.vehicles, tasksPre); 
        //Encode Aold = csp.Initialize(); 
        //double costPre = csp.computeCost(Aold); 
        tasksPre.add(newTask);
        HashSet<Task> tasksAfter = tasksPre;
        csp = new CSP(this.vehicles, tasksAfter);
        Encode Aold1 = csp.Initialize();
        double costAfter = csp.computeCost(Aold1);
        return costAfter - costPre;
    } 
     
 
}
