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
	private static final double LIGHTRADIUS = 10.0;
	private static final double BOARDSIZE = 100.0;
	private Logger log = Logger.getLogger(this.getClass()); // for logging
	private HashSet<MoveableLight> upLights = new HashSet<MoveableLight> ();
	private HashMap<MoveableLight, Point2D.Double> greedyLights = new HashMap<MoveableLight, Point2D.Double> ();
	private HashMap<MoveableLight, Point2D.Double> start = new HashMap<MoveableLight, Point2D.Double> ();
	private HashMap<MoveableLight, Point2D.Double> moving = new HashMap<MoveableLight, Point2D.Double> ();
	private HashMap<Point2D.Double, Integer> mosquitos = new HashMap<Point2D.Double, Integer> ();
	private static HashMap<MoveableLight, ArrayList<Point2D.Double>> pathMap = new HashMap<MoveableLight, ArrayList<Point2D.Double>>();
	private Point2D collectorLocation;
	private MoveableLight collectorLight;
	
	@Override
	public String getName() {
		return "G5Player A*";
	}
	
	private Set<Light> lights;
	private static Set<Line2D> walls;
	
	private ArrayList<Line2D> extend (Line2D startLine) {
		ArrayList<Line2D> extended = new ArrayList<Line2D>();
		Point2D p1 = startLine.getP1();
		Point2D p2 = startLine.getP2();
		double extension = 5;
		double verticalMove = 5;
		double deltaX = p2.getX() - p1.getX();
		double deltaY = p2.getY() - p1.getY();
		
		boolean horizontal = (deltaY == 0);
		boolean vertical = (deltaX == 0);
		
		double slope = 0; 
		double intercept = 0;
		
		if (!vertical && !horizontal) {
			slope = deltaY/deltaX;
			intercept = p2.getY() - slope * p2.getX();
			double e1x = Math.max(1, p1.getX() - extension);
			double e1y = e1x * slope + intercept;
			e1y = Math.max(1, e1y);
			e1y = Math.min(BOARDSIZE -1 , e1y);
			
			Point2D e1 = new Point2D.Double(e1x, e1y);	
			
			double e2x = Math.min(BOARDSIZE -1, p2.getX() + extension);
			double e2y = e2x * slope + intercept;
			e2y = Math.max(1, e2y);
			e2y = Math.min(BOARDSIZE -1 , e2y);
			
			Point2D e2 = new Point2D.Double(e2x, e2y);
			
			extended.add(new Line2D.Double(e1, e2));
		}
		
		else if (vertical){ 
			double vxInside = Math.max(1, p1.getX()- verticalMove);
			double vxOutside = Math.min(BOARDSIZE - 1, p1.getX() + verticalMove);
			
			double v1y = Math.max(1, p1.getY() - extension);
			double v2y = Math.min(BOARDSIZE - 1, p2.getY() + extension);
			
			Point2D insideStart = new Point2D.Double(vxInside, v1y);
			Point2D insideEnd = new Point2D.Double(vxInside, v2y);
			
			Point2D outsideStart = new Point2D.Double(vxOutside, v1y);
			Point2D outsideEnd = new Point2D.Double(vxOutside, v2y);
			
			extended.add(new Line2D.Double(insideStart, insideEnd));
			extended.add(new Line2D.Double(insideStart, outsideStart));
			extended.add(new Line2D.Double(insideEnd, outsideEnd));
			extended.add(new Line2D.Double(outsideStart, outsideEnd));
		}
		
		else if (horizontal) {
			double originalY = p1.getY();
			double vyAbove = Math.max(1, originalY - extension);
			double vyBelow = Math.min(BOARDSIZE - 1, originalY + extension);
			
			double startX = Math.min(1, p1.getX() - extension);
			double endX = Math.max(BOARDSIZE -1, p2.getX() + extension);
			
			Point2D aboveStart = new Point2D.Double(startX, vyAbove);
			Point2D aboveEnd = new Point2D.Double(endX, vyAbove);
			
			Point2D belowStart = new Point2D.Double(startX, vyBelow);
			Point2D belowEnd = new Point2D.Double(endX, vyBelow);
			
			extended.add(new Line2D.Double(aboveStart, aboveEnd));
			extended.add(new Line2D.Double(belowStart, belowEnd));
			extended.add(new Line2D.Double(aboveStart, belowStart));
			extended.add(new Line2D.Double(aboveEnd, belowEnd));
		}
		
		return extended;
	}
	
	@Override
	public ArrayList<Line2D> startNewGame(Set<Line2D> w, int numLights) {
		this.numLights = numLights;
		collectorLocation = new Point2D.Double(100,50);
		greedyLights = new HashMap<MoveableLight, Point2D.Double>();
		walls = new HashSet<Line2D>();
			
		// make the walls longer so that we don't lose mosquitos by flying too close 
		for (Line2D wall : w) {
			ArrayList<Line2D> extended = extend(wall);
			walls.addAll(extended);
		}
		
		ArrayList<Line2D> lines = new ArrayList<Line2D>();
		Line2D line = new Line2D.Double(30, 30, 80, 80);
		lines.add(line);
		return lines;
	}
	

	private static boolean withinLightRadius(Point2D startPoint, Point2D testPoint) {
		return (startPoint.distance(testPoint)) <= LIGHTRADIUS;
	}

	private boolean isValidDestination(Point2D point) {
	boolean valid = true;	
	// check all current light positions and current light locations as valid
	
	for (Light l: lights) {
		MoveableLight ml = (MoveableLight)l;
		Point2D currentLocation = ml.getLocation();
		if (withinLightRadius(currentLocation, point)) {
	        valid = false; 
			break;
		}
		
		if (greedyLights.containsKey(ml)) {
			Point2D currentDestination = greedyLights.get(ml);
			if (withinLightRadius(currentDestination, point)) {
				valid = false;
				break;
			   }
		    }
		}
	
		return valid;
	}
	
	
	private Point2D greedyLocation(int [][] board) {
		Point2D.Double location = new Point2D.Double(50,50);
		Point2D.Double validLocation = null;
		int max = 0;
		int validmax = 0;
		
		for(int i = 0; i < 100; i+=10) {
			for(int j = 0; j < 100; j+=10) {
				//reset the sum for this chunk
				int sum = 0;
				for (int x = 0; x < 10; x++){
					for(int y= 0; y < 10; y++)
					{
						sum += board[i+x][j+y];
					}
				}
				
				if(sum > max) {
					location = new Point2D.Double(i + 5, j + 5);
					max = sum;
				}
				
				if (sum > validmax && isValidDestination(location)) {
					validLocation = new Point2D.Double(i+5,j+5);
					validmax = sum;
				}
			}
		}
		
		// if we haven't found a valid location, pick the max populated location
		if (validLocation == null) {
			validLocation = location;
		}
		
		return validLocation;
	}
	
	private int sumAround(Point2D center, int[][] board) {
		int startX = Math.max(0, (int)(center.getX() - LIGHTRADIUS/2));
		int endX = (int) Math.min(BOARDSIZE, (center.getX() + LIGHTRADIUS/2));
		
		int startY = Math.max(0, (int)(center.getY() - LIGHTRADIUS/2));
		int endY = (int) Math.min(BOARDSIZE, (center.getY() + LIGHTRADIUS/2));
		
		int sum = 0;
		for (int i = startX; i < endX; i++) {
			for (int j = startY; j < endY; j++){
				sum += board[i][j];
			}
		}
		
		return sum;
	}
	
	
	public Set<Light> getLights(int[][] board) {
		//me messing around with the lights
		lights = new HashSet<Light>();
		for(int i = 0; i< numLights - 1;i++)
		{
			if(i==0){
				MoveableLight l = new MoveableLight(0,0, true);
				lights.add(l);
			}
			if(i==1){
				MoveableLight l = new MoveableLight(0,33, true);
				lights.add(l);
			}
			if(i==2){
				MoveableLight l = new MoveableLight(0,66, true);
				lights.add(l);
			}
			if(i==3){
				MoveableLight l = new MoveableLight(0,100, true);
				lights.add(l);
			}

		}
		
		collectorLight = new MoveableLight(collectorLocation.getX() - 1, collectorLocation.getY(), true);
		lights.add(collectorLight);
		
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
			
			// don't move collector light
			if (ml.equals(collectorLight)) {
				continue;
			}
			
			Point2D.Double p = (Point2D.Double)ml.getLocation();
			
			if (p.distance(collectorLocation) > 3 * LIGHTRADIUS && ml.isOn() == false) {
				ml.turnOn();
			}
			
			Point2D.Double objective;
			
			if (greedyLights.containsKey(ml)) {
				objective = greedyLights.get(ml);
				
//				// stop moving towards a location that is no longer populated
//				if (objective.equals(collectorLocation) == false && sumAround(objective, board) < 1) {
//					pathMap.remove(ml);
//					objective = (Point2D.Double)greedyLocation(board);
//					if (sumAround(objective, board) > 1) {
//						greedyLights.put(ml, (Point2D.Double)objective);
//					}
//					else {
//						ml.turnOff();
//					}
//				}
				
			}
			
			else {
				objective = (Point2D.Double)collectorLocation;
				greedyLights.put(ml, objective);
			}
		
			//NOW if an A* path has already been found for this light, continue its movement along that path towards the objective
			if(pathMap.containsKey(ml)){
				//see if next move is indeed to the objective, then move to it
				if (p.distance(objective) < 1){
					ml.moveTo(objective.x, objective.y);
					
					// find a greedy location or move back to the collector
					Point2D newDestination = (objective.equals(collectorLocation)) ? 
							greedyLocation(board) : collectorLocation;
					pathMap.remove(ml);
					greedyLights.put(ml, (Point2D.Double) newDestination);
				}
				//otherwise keep along path
				else{
					ArrayList<Point2D.Double> path = pathMap.get(ml);
					int idx = path.indexOf(p) + 1;
					Point2D.Double next = new Point2D.Double(0,0);
					if (idx < path.size()) {
					   next = path.get(idx);
					}
					else {
						System.out.println("path out of boudns");
					}
					
					
					// don't go all the way to the collector since we have a light there
					if (objective.equals(collectorLocation) && next.distance(objective) <= LIGHTRADIUS) {
						objective = (Point2D.Double)greedyLocation(board);
						pathMap.remove(ml);
						greedyLights.put(ml, (Point2D.Double) objective);
						ml.turnOff();
					}
					
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
		Collector c = new Collector(collectorLocation.getX(), collectorLocation.getY());
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
