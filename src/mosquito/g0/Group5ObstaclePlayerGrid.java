package mosquito.g0;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;

import mosquito.sim.Collector;
import mosquito.sim.Light;
import mosquito.sim.MoveableLight;

public class Group5ObstaclePlayerGrid extends mosquito.sim.Player  {

	private int numLights;
	private Point2D.Double lastLight;
	private Logger log = Logger.getLogger(this.getClass()); // for logging
	private HashSet<MoveableLight> upLights = new HashSet<MoveableLight> ();
	private HashMap<MoveableLight, Point2D.Double> greedyLights = new HashMap<MoveableLight, Point2D.Double> ();
	private HashMap<MoveableLight, Point2D.Double> start = new HashMap<MoveableLight, Point2D.Double> ();
	private HashMap<MoveableLight, Point2D.Double> moving = new HashMap<MoveableLight, Point2D.Double> ();
	private HashMap<Point2D.Double, Integer> mosquitos = new HashMap<Point2D.Double, Integer> ();
	private static HashMap<MoveableLight, ArrayList<Point2D.Double>> pathMap = new HashMap<MoveableLight, ArrayList<Point2D.Double>>();
	@Override
	public String getName() {
		return "G5Player A*";
	}
	
	private Set<Light> lights;
	private static Set<Line2D> walls;
	
	@Override
	public ArrayList<Line2D> startNewGame(Set<Line2D> w, int numLights) {
		this.numLights = numLights;
		walls = w;

		ArrayList<Line2D> lines = new ArrayList<Line2D>();
		Line2D line = new Line2D.Double(30, 30, 80, 80);
		lines.add(line);
		return lines;
	}


	public Set<Light> getLights(int[][] board) {
		//try fancy algorithm, if anything fails go back to hardcoding
		lights = new HashSet<Light>();
		try {	
			for(int i = 0; i < 100; i++) {
				for(int j = 0; j < 100; j++){
					mosquitos.put(new Point2D.Double(i, j), board[i][j]);
				}
			}
			LinkedList<Integer> vals = new LinkedList<Integer> (mosquitos.values());
			Collections.sort(vals);

			for(int i = 0; i<numLights;i++)
			{	
				int lookingFor = vals.get(10000-i);
				for(int k = 0; k < 100; k++) {
					for(int j = 0; j < 100; j++){
						if(board[k][j] == lookingFor)
						{
							MoveableLight l = new MoveableLight(k,j, true);
							lights.add(l);
						}
						if(lights.size() == numLights) { break; }
					}
				}
			}
		}catch (Exception e) {
			lights = new HashSet<Light> ();
			for(int i = 0; i<numLights;i++)
			{
				MoveableLight l = new MoveableLight(0,(100*i/numLights), true);
				lights.add(l);
			}
		}
		return lights;
	}
	
	/*
	 * This is called at the beginning of each step (before the mosquitoes have moved)
	 * If your Set contains additional lights, an error will occur. 
	 * Also, if a light moves more than one space in any direction, an error will occur.
	 * The board tells you where the mosquitoes are: board[x][y] tells you the
	 * number of mosquitoes at coordinate (x, y)
	 */
	public Set<Light> updateLights(int[][] board) {
		//for each light
		for (Light l : lights) {
			//standard setup
			MoveableLight ml = (MoveableLight)l;
			Point2D.Double p = new Point2D.Double(ml.getX(),ml.getY());
			Point2D.Double objective = new Point2D.Double(100,50);
			
			//NOW if an A* path has already been found for this light, continue its movement along that path towards the objective
			if(pathMap.containsKey(ml)){
				//see if next move is indeed to the objective, then move to it
				if(distanceBetween(p,objective) < 1){
					ml.moveTo(objective.x, objective.y);
				}
				//otherwise keep along path
				else{
					ArrayList<Point2D.Double> path = pathMap.get(ml);
					int idx = path.indexOf(p) + 1;
					Point2D.Double next = path.get(idx);
					ml.moveTo(next.x, next.y);
				}
			}
			//OTHERWISE create a new A* movement for the light - this will both set up the path to exist in pathMap as well as do the first movement.
			else{
				try{
				moveAStar(ml, objective);
				}catch(Exception e){log.debug("error in finding A*");}
			}
			
		}
		return lights;
	}

	@Override
	public Collector getCollector() {
		// this one just places a collector next to the last light that was added
		Collector c = new Collector(100, 50);
		return c;
	}
	
	//A* search method for optimized path. Implemented from the Wikipedia page's pseudo code.
	private static void moveAStar(MoveableLight ml, Point2D.Double destination) throws Exception{		
		Point2D.Double start = new Point2D.Double(ml.getX(),ml.getY());
		//visited is the set of nodes already evaluated
		HashSet<Point2D.Double> visited = new HashSet<Point2D.Double>();
		//openSet is the set of nodes tentatively to be evaluated, initialized with the start node
		HashSet<Point2D.Double> openSet = new HashSet<Point2D.Double>();
		openSet.add(start);
		//map to keep track of the path
		HashMap<Point2D.Double, Point2D.Double> cameFrom = new HashMap<Point2D.Double, Point2D.Double>();
		//the path if you want to give it to the light
		ArrayList<Point2D.Double> shortestPath = new ArrayList<Point2D.Double>();
		//map to keep track of the scores of the point locations and their costs - these are the nodes essentially
		HashMap<Point2D.Double, Float> g_score = new HashMap<Point2D.Double, Float>();
		HashMap<Point2D.Double, Float> f_score = new HashMap<Point2D.Double, Float>();
		
		//cost from node along best known path
		g_score.put(start, (float)0.0);
		//estimated total cost from start to goal through the heuristic (straight line)
		f_score.put(start, (float)distanceBetween(start, destination));
		
		//keep track of current node
		Point2D.Double current = new Point2D.Double();
		while (openSet.size() > 0){
			//get the next point to evaluate
			current = lowestScore(f_score, openSet);
			//if this point is within 1 from the goal, make the path and move the light to the next point
			if(distanceBetween(current, destination) <= 1.0){
				cameFrom.put(destination, current);
				shortestPath = reconstructPath(cameFrom, destination);
				pathMap.put(ml, shortestPath);
				//move the light
				Point2D.Double nextStep = shortestPath.get(1);
				ml.moveTo(nextStep.x, nextStep.y);
				break;
			}
			
			//remove current from openSet and add it to visited
			openSet.remove(current);
			visited.add(current);
			
			//for each neighbor to the current node, find the ones to add to openSet
			for (Point2D.Double neighbor : getNeighbors(current)){
				float tentative_g_score = g_score.get(current) + (float)distanceBetween(current, neighbor);
				float tentative_f_score = tentative_g_score + (float)distanceBetween(neighbor, destination);
				
				if(visited.contains(neighbor) && tentative_f_score > f_score.get(neighbor))
					continue;
				
				if(!openSet.contains(neighbor) || (f_score.containsKey(neighbor) && tentative_f_score < f_score.get(neighbor))){
					cameFrom.put(neighbor, current);
					g_score.put(neighbor, tentative_g_score);
					f_score.put(neighbor, tentative_f_score);
					if(!openSet.contains(neighbor))
						openSet.add(neighbor);
				}	
			}
		}
		
		//if you reach the end of the while loop, it is a failure. In our case, I throw an exception
		throw new Exception();		
	}
	
	//returns all the points that are distance 1 away by square from the current point
	private static HashSet<Point2D.Double> getNeighbors(Point2D.Double node){
		HashSet<Point2D.Double> neighbors = new HashSet<Point2D.Double>();
		for (int i=1; i<=4; i++){
			double x;
			double y;
			if(i==1){
				x = node.x + 1;
				y = node.y;
			}
			else if(i==2){
				x = node.x;
				y = node.y + 1;
			}
			else if(i==3){
				x = node.x - 1;
				y = node.y;
			}
			else{
				x = node.x;
				y = node.y -1;
			}
			Line2D potentialLine = new Line2D.Double(node.x,node.y,x,y);
			//find if the vector at this angle would either hit an obstacle or the edge of the board
			//if so, make the neighbor along the same angle but as long of a vector as possible before collision
			boolean add = true;
			if(x < 0 || x > 100 || y < 0 || y > 100){
				add = false;
			}
			for (Line2D obstacle : walls){
				if(potentialLine.intersectsLine(obstacle)){
						add = false;
						break;
				}
			}
			if(add == true){
				neighbors.add(new Point2D.Double(x, y));
			}
		}		
		return neighbors;
	}
	
	//returns the point of intersection of two lines (stolen from stackoverflow.com/questions/16314069/calculation-of-intersections-between-line-segments
	private static Point2D lineIntersect(Line2D l1, Line2D l2){
		double denom = (l2.getY2() - l2.getY1()) * (l1.getX2() - l1.getX1()) - (l2.getX2() - l2.getX1()) * (l1.getY2() - l1.getY1());
		//lines are parallel
		if(denom == 0.0d)
			return null;
		double ua = ((l2.getX2() - l2.getX1()) * (l1.getY1() - l2.getY1()) - (l2.getY2() - l2.getY1()) * (l1.getX1() - l2.getX1()))/denom;
		double ub = ((l1.getX2() - l1.getX1()) * (l1.getY1() - l2.getY1()) - (l1.getY2() - l1.getY1()) * (l1.getX1() - l2.getX1()))/denom;
		if(ua >= 0.0f && ua <= 1.0f && ub >= 0.0f && ub <= 1.0f){
			return new Point2D.Double(l1.getX1() + ua*(l1.getX2() - l1.getX1()), l1.getY1() + ub*(l1.getY2() - l1.getY1()));
		}
		return null;
	}
	
	//reconstructs the path from the end node to the start node
	private static ArrayList<Point2D.Double> reconstructPath(HashMap<Point2D.Double, Point2D.Double> cameFrom, Point2D.Double current){
		ArrayList<Point2D.Double> path = new ArrayList<Point2D.Double>();
		if(cameFrom.containsKey(current)){
			path = reconstructPath(cameFrom, cameFrom.get(current));
			path.add(current);
			return path;
		}			
		else{
			path = new ArrayList<Point2D.Double>();
			path.add(current);
			return path;	
		}
	}
	
	//our heuristic - straight line distance
	private static double distanceBetween(Point2D.Double start, Point2D.Double end){
		return Math.sqrt(Math.pow(end.x - start.x, 2) + Math.pow(end.y - start.y, 2));
	}
	
	//return the point that has the lowest value in the map if it is contained in the set.
	private static Point2D.Double lowestScore(HashMap<Point2D.Double, Float> map, HashSet<Point2D.Double> set){
		Point2D.Double lowest = new Point2D.Double(-1, -1);
		Float min = Float.valueOf(Float.POSITIVE_INFINITY );
		for(Map.Entry<Point2D.Double,Float> node : map.entrySet()){
			if(set.contains(node.getKey())){
			    if(min.compareTo(node.getValue())>0){
			        lowest = node.getKey();
			        min = node.getValue();
			    }
			}
		}
		return lowest;
		
		
	}
}
