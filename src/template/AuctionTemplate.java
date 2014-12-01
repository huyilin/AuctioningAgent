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
    HashMap<Task,Long>myBid = new HashMap<Task,Long>(); 
    HashMap<Task,Long>hisBid = new HashMap<Task,Long>(); 
    HashMap<Task,Double>ratio = new HashMap<Task,Double>(); 
    TaskSet myTask; 
    TaskSet hisTask; 
    //ArrayList<Task>myTask = new ArrayList<Task>(); 
    //ArrayList<Task>hisTask = new ArrayList<Task>(); 
     
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
        if (winner == agent.id()) {              //i am the winner 
            //currentCity = previous.deliveryCity; 
            myTask.add(previous); 
            myBid.put(previous, bids[winner]); 
            hisBid.put(previous, bids[~winner]); 
            ratio.put(previous, (double) (bids[~winner] / bids[winner])); 
        } 
        else{                                    // he is the winner 
            hisTask.add(previous); 
            myBid.put(previous, bids[~winner]); 
            hisBid.put(previous, bids[winner]); 
            ratio.put(previous, (double) (bids[~winner] / bids[winner])); 
        } 
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
        avg = total / ratio.size(); 
         
        return avg; 
    } 
     
     
     
    @Override 
    public Long askPrice(Task task) { 
 
        if (vehicle.capacity() < task.weight) 
            return null; 
 
        long distanceTask = task.pickupCity.distanceUnitsTo(task.deliveryCity); 
        long distanceSum = distanceTask 
                + currentCity.distanceUnitsTo(task.pickupCity); 
        //double marginalCost = Measures.unitsToKM(distanceSum 
        //        * vehicle.costPerKm()); 
        double mymarginalCost = getMarginalCost(myTask,task); 
        double hismarginalCost = getMarginalCost(hisTask,task); 
        //double ratio = 1.0 + (random.nextDouble() * 0.05 * task.id); 
        double ratio = getRatio(); 
        double bid = 0; 
         
        if ((hismarginalCost * ratio) >= mymarginalCost) 
            bid = hismarginalCost * ratio * 0.995;        //double bid = ratio * marginalCost; 
        else 
            bid = mymarginalCost * 1.05;         
        return (long) Math.round(bid); 
    } 
 
    @Override 
    public List<Plan> plan(List<Vehicle> vehicles, TaskSet tasks) { 
         
        System.out.println("Agent " + agent.id() + " has tasks " + tasks); 
        System.out.println("tasks number : " + tasks.size()); 
 
        CSP csp = new CSP(vehicles, tasks); 
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
     
    public double getMarginalCost(TaskSet tasksPre, Task newTask) { 
        double cost; 
        CSP csp = new CSP(this.vehicles, tasksPre); 
        Encode Aold = csp.Initialize(); 
        double costPre = csp.computeCost(Aold); 
        tasksPre.add(newTask); 
        TaskSet tasksAfter = tasksPre; 
        csp = new CSP(this.vehicles, tasksAfter); 
        Encode Aold1 = csp.Initialize(); 
        double costAfter = csp.computeCost(Aold1); 
        return costAfter - costPre; 
    } 
     
 
}
