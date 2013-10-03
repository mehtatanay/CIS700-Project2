package mosquito.g0;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.apache.log4j.Logger;

import mosquito.sim.Collector;
import mosquito.sim.Light;
import mosquito.sim.MoveableLight;

public class Group5ObstaclePlayer extends mosquito.sim.Player  {

	private int numLights;
	private static final double LIGHTRADIUS = 10.0;
	private Point2D.Double lastLight;
	private Logger log = Logger.getLogger(this.getClass()); // for logging
	private HashSet<MoveableLight> upLights = new HashSet<MoveableLight> ();
	private HashMap<MoveableLight, Point2D.Double> greedyLights = new HashMap<MoveableLight, Point2D.Double> ();
	private HashMap<MoveableLight, Point2D.Double> start = new HashMap<MoveableLight, Point2D.Double> ();
	private HashMap<Point2D.Double, Integer> mosquitos = new HashMap<Point2D.Double, Integer> ();
	private HashSet<Point2D.Double> takenSpaces = new HashSet<Point2D.Double> ();
	private LinkedList<Integer> topTen = new LinkedList<Integer> ();
	private Point2D collectorLocation;
	
	@Override
	public String getName() {
		return "G5Player";
	}
	
	private Set<Light> lights;
	private Set<Line2D> walls;
	
	@Override
	public ArrayList<Line2D> startNewGame(Set<Line2D> walls, int numLights) {
		this.numLights = numLights;
		this.walls = walls;
		
		this.collectorLocation = new Point2D.Double(50,50);

		ArrayList<Line2D> lines = new ArrayList<Line2D>();
		Line2D line = new Line2D.Double(30, 30, 80, 80);
		lines.add(line);
		return lines;
	}


	private static boolean withinLightRadius(Point2D startPoint, Point2D testPoint) {
		return (startPoint.distance(testPoint)) <= LIGHTRADIUS;
	}
	
	public Set<Light> getLights(int[][] board) {
		lights = new HashSet<Light>();
		
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
				}
			}
		}
		return lights;
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
	
	
	/*
	 * This is called at the beginning of each step (before the mosquitoes have moved)
	 * If your Set contains additional lights, an error will occur. 
	 * Also, if a light moves more than one space in any direction, an error will occur.
	 * The board tells you where the mosquitoes are: board[x][y] tells you the
	 * number of mosquitoes at coordinate (x, y)
	 */
	public Set<Light> updateLights(int[][] board) {
		for (Light l : lights) {
			MoveableLight ml = (MoveableLight)l;
			if (greedyLights.containsKey(ml)) {
				Point2D currLocation = ml.getLocation();
				Point2D destination = greedyLights.get(ml);
				
				// pick a new greedy location or the collector if we've reached our previous destination
				if (currLocation.equals(destination)) {
					Point2D newDestination = (currLocation.equals(collectorLocation)) ? 
											greedyLocation(board) : collectorLocation;
					greedyLights.put(ml, (Point2D.Double)newDestination);
				}	
			}
			
			else {
				Point2D newDestination = greedyLocation(board);
				greedyLights.put(ml, (Point2D.Double)newDestination);
			}
			
			moveAStar(ml, greedyLights.get(ml));
		}
		return lights;
	}

	@Override
	public Collector getCollector() {
		// this one just places a collector next to the last light that was added
		Collector c = new Collector(collectorLocation.getX(), collectorLocation.getY());
		return c;
	}
	
	private static void moveAStar(MoveableLight ml, Point2D.Double destination) {
		Point2D.Double start = new Point2D.Double(ml.getX(),ml.getY());
		//visited is the set of nodes already evaluated
		HashSet<Point2D.Double> visited = new HashSet<Point2D.Double>();
		//openSet is the set of nodes tentatively to be evaluated, initialized with the start node
		HashSet<Point2D.Double> openSet = new HashSet<Point2D.Double>();
		openSet.add(start);
		//map to keep track of the path
		HashMap<Point2D.Double, Point2D.Double> cameFrom = new HashMap<Point2D.Double, Point2D.Double>();
		//map to keep track of the scores of the point locations and their costs - these are the nodes essentially
		HashMap<Point2D.Double, Double> g_score = new HashMap<Point2D.Double, Double>();
		HashMap<Point2D.Double, Double> f_score = new HashMap<Point2D.Double, Double>();
		
		//cost from node along best known path
		g_score.put(start,0d);
		//estimated total cost from start to goal through y
		f_score.put(start, distanceBetween(start, destination));
		
		while (openSet.size() > 0){
			
		}
		
		
	}
	
	private static double distanceBetween(Point2D.Double start, Point2D.Double end){
		return Math.sqrt(Math.pow(end.x - start.x, 2) + Math.pow(end.y - start.y, 2));
	}
}
