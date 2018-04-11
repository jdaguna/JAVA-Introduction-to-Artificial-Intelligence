// @Joshua Daguna, 10/20/2013 -- calculated good heuristic values for evaluation
//
// GoodHeuristic - estimates the cheapest path from the state at node <em>n</em> to a goal state.
//
// This class extends the Heuristic class, providing a reasonable
// implementation of the heuristic function method.  The provided "good"
// heuristic function is admissible.


import java.util.Iterator;


public class GoodHeuristic extends Heuristic {
	// YOU CAN ADD ANYTHING YOU LIKE TO THIS CLASS ... WHATEVER WOULD
	// ASSIST IN THE CALCULATION OF YOUR GOOD HEURISTIC VALUE.
	private Map graph;
	public double maxSpeed =0;

	public GoodHeuristic(Location dest, Map graph) {
		super(dest);
		this.graph = graph;

		for (Iterator<Location> in = graph.locations.iterator(); in.hasNext();) {
			Location loc = (Location) in.next();
			for (Iterator<Road> in2 = loc.roads.iterator(); in2.hasNext();) {
				Road carPath = (Road) in2.next();
		//Euclidean coordinates to get true distance.
		//speed = distance/ time
		//find max speed which will be the upper bound of the max speed along the map
			double dl = Math.abs(carPath.toLocation.latitude - carPath.fromLocation.latitude);
			double dL = Math.abs(carPath.fromLocation.longitude - carPath.fromLocation.longitude);
			double distance = Math.sqrt(dl * dl + dL * dL);
			double speed = distance/carPath.cost;
			if(speed>maxSpeed){
				maxSpeed=speed;
			}
			}
		}
	}

	public double heuristicFunction(Waypoint wp) {
		double hVal = 0.0;
		hVal = maxSpeed;
		return (hVal);
	}
}
