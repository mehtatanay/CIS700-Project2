package src.mosquito.g0;

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

public class Group5ObstaclePlayerSmartAStar extends mosquito.sim.Player  {

	private int numLights;
	private static final double LIGHTRADIUS = 10.0;
	private static final double BOARDSIZE = 100.0;
	private Logger log = Logger.getLogger(this.getClass()); // for logging
	private HashMap<MoveableLight, Point2D.Double> greedyLights = new HashMap<MoveableLight, Point2D.Double> ();
	private HashMap<Point2D.Double, Integer> mosquitos = new HashMap<Point2D.Double, Integer> ();
	private static HashMap<MoveableLight, ArrayList<Point2D.Double>> pathMap = new HashMap<MoveableLight, ArrayList<Point2D.Double>>();
	private Point2D collectorLocation;
	private MoveableLight collectorLight;
	
	private static int[][] board;
	
	private AStar a_star;
	
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
		
		this.a_star = new AStar(walls);
		
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
		Group5ObstaclePlayerSmartAStar.board = board;
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
							if(lights.size() >= numLights) {
								break;
							}
						}
					}
				}
			}
		} catch(Exception e) {
			for(int i = 0; i < numLights; i ++) {
				lights = new HashSet<Light>();
				Light l = new MoveableLight(0, (100*i)/numLights, true);
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
				ArrayList<Point2D.Double> shortestPath = a_star.getPath(ml, objective, board);
				pathMap.put(ml, shortestPath);
				Point2D.Double firstStep = shortestPath.get(1);
				ml.moveTo(firstStep.x, firstStep.y);
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
	
}
