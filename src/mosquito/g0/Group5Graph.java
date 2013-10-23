package mosquito.g0;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Random;
import java.util.Set;


import org.apache.log4j.Logger;

import mosquito.sim.Collector;
import mosquito.sim.Light;
import mosquito.sim.MoveableLight;

public class Group5Graph extends mosquito.sim.Player  {

	private int numLights;
	private Point2D.Double lastLight;
	private static final double LIGHTRADIUS = 10.0;
	private static final double BOARDSIZE = 100.0;
	private Point2D collectorLocation;
	private MoveableLight collectorLight;
	private ArrayList<Point2D.Double> vertices = new ArrayList<Point2D.Double> ();
	private HashMap<Point2D.Double, ArrayList<Point2D.Double>> graph = new HashMap<Point2D.Double, ArrayList<Point2D.Double>>();
	private HashMap<Point2D.Double, Point2D.Double> edges = new HashMap<Point2D.Double, Point2D.Double> ();
	private HashMap<Point2D.Double, ArrayList<Point2D.Double>> mst = new HashMap<Point2D.Double, ArrayList<Point2D.Double>> ();
	private HashMap<Light, ArrayList<Point2D>> paths = new HashMap<Light, ArrayList<Point2D>>();
	private HashMap<Light, ArrayList<Point2D.Double>> astarPaths = new HashMap<Light, ArrayList<Point2D.Double>>();
	private HashMap<MoveableLight, Point2D.Double> greedyLights = new HashMap<MoveableLight, Point2D.Double> ();
	private HashSet<Point2D.Double> greedyLocations = new HashSet<Point2D.Double> ();
	private HashMap<MoveableLight, Integer> waitTurns = new HashMap<MoveableLight, Integer>();
	private static final int waits = 5;
	private HashMap<MoveableLight, ArrayList<Direction>> lastTenTurns = new HashMap<MoveableLight, ArrayList<Direction>>();
	private int turns = 0;
	
	ArrayList<Point2D> zigZagPath;
	
	private Logger logger =  Logger.getLogger(this.getClass()); 
	private AStar astar;

	
	@Override
	public String getName() {
		return "G5Player Graph";
	}
	
	private Set<Light> lights;
	private Set<Line2D> walls;
	private Set<Line2D> extendedwalls;	
	
	@Override
	public ArrayList<Line2D> startNewGame(Set<Line2D> walls, int numLights) {
		logger.trace("logging works");
		this.numLights = numLights;
		this.walls = new HashSet<Line2D>();
		this.extendedwalls = new HashSet<Line2D>();
		
		for (Line2D w: walls) {
			ArrayList<Line2D> extended = extend(w);
			this.extendedwalls.addAll(extended);
		}
		
		this.walls = walls;
		this.astar = new AStar(this.walls);
		this.collectorLocation = collLocation();
		
		
		ArrayList<Line2D> lines = new ArrayList<Line2D>();
		Line2D line = new Line2D.Double(30, 30, 80, 80);
		lines.add(line);
		return lines;
	}
	
	private Point2D.Double collLocation() {
		Point2D.Double returnVal = new Point2D.Double(95,95);
		int max = Integer.MIN_VALUE;
		int current = 0;
		//identify the 10*10 area that is least obstructed
		for(int a = 0; a < 100; a+=10) {
			for(int b = 0; b < 100; b+=10) {
				current = 0;
				for(int c = 0; c < 100; c+=10) {
					for(int d = 0; d < 100; d+=10) {
						if(!isObstructed(new Point2D.Double(a,b), new Point2D.Double(c,d))) {
							current ++;
						}
					}
				}
				if(current > max) {
					returnVal = (Point2D.Double)notOnWall(new Point2D.Double(a + 4, b + 5));
					max = current;
				}
			}
		}
		return (Point2D.Double)notOnWall(returnVal);
	}
	private static boolean withinLightRadius(Point2D startPoint, Point2D testPoint) {
		return (startPoint.distance(testPoint)) <= (2 * LIGHTRADIUS);
	
	}
	
	private boolean isValidDestination(Point2D point) {
	boolean valid = true;	
	// check all current light positions and current light locations as valid
	
	for (Light l: lights) {
		MoveableLight ml = (MoveableLight)l;
		if (point.distance(ml.getLocation()) < 20) {
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
	
	for (Point2D greedypoint: greedyLocations) {
		if (point.distance(greedypoint) < 20) {
			valid = false;
			break;
		}
	}

	
		return valid;
	}
	
	private Point2D.Double farthestGreedyPoint() {
		double distance = (double)Integer.MIN_VALUE;
		Point2D.Double farthest = (Point2D.Double) collectorLocation;
		for (Point2D.Double greedyPoint : greedyLocations) {
			double currdistance = collectorLocation.distance(greedyPoint);
			if (currdistance > distance) {
				farthest = greedyPoint;
				distance = currdistance;
			}
		}
		
		return farthest;
	}
	
	private Point2D.Double closestGreedyPoint(Point2D.Double startPoint) {
		double distance = (double)Integer.MAX_VALUE;
		Point2D.Double closest = (Point2D.Double) collectorLocation;
		for (Point2D.Double greedyPoint : greedyLocations) {
			double currdistance = startPoint.distance(greedyPoint);
			if (currdistance < distance) {
				closest = greedyPoint;
				distance = currdistance;
			}
		}
		
		return closest;
	}
	
	private void computeGreedyLocations (int [][] board) {
		greedyLocations = new HashSet<Point2D.Double> ();
		
		for(int i = 0; i < 100; i+=10) {
			for(int j = 0; j < 100; j+=10) {
				//reset the sum for this chunk
				int sum = 0;
				double xsum = 0;
				double ysum = 0;
 			     Point2D.Double center = (Point2D.Double)notOnWall(new Point2D.Double(i+5,j+5));
						
				for (int x = 0; x < 10; x++){
					for(int y= 0; y < 10; y++)
					{
						 
//						if (!isObstructed(new Point2D.Double(i + x, j + y), 
//								center)) {
						if (board[i+x][j+y]> 0) {
							int nummosquitos = board[i+x][j+y];
						    xsum += nummosquitos * (i+x);
							ysum += nummosquitos * (j+y);
							sum += nummosquitos;
							center = (Point2D.Double)notOnWall(new Point2D.Double(i+x,j+y));
							}
//						}
					}
				}
				
//				Point2D.Double center = (Point2D.Double) notOnWall(new Point2D.Double(Math.round(xsum/sum), Math.round(ysum/sum)));
				if (sum > 0 && isValidDestination(center)) {
					greedyLocations.add(center);
				}
			}
		}
	}
	
	private Point2D greedyLocation(int [][] board) {

		//populate greedy locations set
		greedyLocations = new HashSet<Point2D.Double> ();
		Point2D.Double location = new Point2D.Double(50,50);
		Point2D.Double validLocation = null;
		
		for(int i = 0; i < 100; i+=10) {
			for(int j = 0; j < 100; j+=10) {
				//reset the sum for this chunk
				int sum = 0;
				for (int x = 0; x < 10; x++){
					for(int y= 0; y < 10; y++)
					{
						if (!isObstructed(new Point2D.Double(i + x, j + y), 
								new Point2D.Double(i + 5, j+ 5))) {
							sum += board[i+x][j+y];
						}
					}
				}
				
				if (sum > 0 && isValidDestination(location)) {
					greedyLocations.add(new Point2D.Double(i+5,j+5));
				}
			}
		}
		
		if(greedyLocations.isEmpty()) { 
			return (Point2D.Double)collectorLocation; 
		}
		
		//pick greedy location furthest away from collector
		double maxDistance = 0;
		for(Point2D.Double greed:greedyLocations) {
			if(greed.distance((Point2D.Double)collectorLocation) > maxDistance) {
				validLocation = greed;
				maxDistance = greed.distance((Point2D.Double)collectorLocation);
			}
		}
		
		// if we haven't found a valid location, pick the max populated location
		if (validLocation == null) {
			validLocation = (Point2D.Double)collectorLocation;
		}
		
		return notOnWall(validLocation);
	}
	
	
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
		if (vertical) { 
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
		
		else {
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
		
		return extended;
	}
	
	
	private static HashMap<MoveableLight, ArrayList<Point2D.Double>> history = new HashMap<MoveableLight, ArrayList<Point2D.Double>> ();

	private static boolean stuck (MoveableLight ml, Point2D.Double currentPosition){
			if(history.containsKey(ml))
			{
				ArrayList<Point2D.Double> h = history.get(ml);
				h.add(currentPosition);

				if(h.size() >= 3 && (h.get(h.size()-3).equals(h.get(h.size()-2)) && h.get(h.size()-2).equals(h.get(h.size()-1)))){
					return true;
				}
				
				history.put(ml, h);
			}
			else {
				ArrayList<Point2D.Double> h = new ArrayList<Point2D.Double>();
				h.add(currentPosition);
				history.put(ml, h);
			}
			return false;
		}
	
	
	private void computePaths() {
		for (Light l : lights) {
			Point2D.Double u = (Point2D.Double)l.getLocation();
			LinkedList<Point2D> queue = new LinkedList<Point2D>();
			queue.push(u);
			ArrayList <Point2D> path = new ArrayList<Point2D>();
			path.add(u);
			HashSet<Point2D> visited = new HashSet<Point2D>();
			
			
			while (queue.isEmpty() == false && u.equals(collectorLocation) == false) {
				u = (Point2D.Double)queue.pop();
				visited.add(u);
				ArrayList<Point2D.Double> adjacent = graph.get(u);
		
				for (Point2D.Double v:adjacent) {
					if (visited.contains(v) == false) {
						queue.push(v);
						Point2D.Double lastPoint = (Point2D.Double)path.get(path.size() - 1);
						boolean intersects = isObstructed(lastPoint, v);
						if (!intersects) {
							path.add(v);
							break;
						}
					}
				}
			}
			
			path.add(collectorLocation);
			paths.put(l, path);
		}
	}
	

	
	private void buildGraph() {
		//populate vertices
		for (int i = 0; i < 100; i = i+20) {
			for (int j = 0; j < 100; j = j+20) {
				Point2D.Double newPoint = new Point2D.Double(i + 5, j + 5);
				vertices.add(newPoint);
			}
		}
		
		// populate edges based on adjacency on the board
		for(Point2D.Double s:vertices) {
			ArrayList<Point2D.Double> adjacent = new ArrayList<Point2D.Double>();
			
			// LEFT
			if (s.getX() - 20 >=0) {
				Point2D.Double left = new Point2D.Double(s.getX() - 20, s.getY());
				if (vertices.contains(left)) {
					adjacent.add(left);
				}
			}
			
			// RIGHT
			if (s.getX() + 20 <= BOARDSIZE) {
				Point2D.Double right = new Point2D.Double(s.getX() + 20, s.getY());	
				if (vertices.contains(right)) {
					adjacent.add(right);
				}
			}
			
			// UP
			if (s.getY() - 20 >=0) {
				Point2D.Double up = new Point2D.Double(s.getX(), s.getY() - 20);
				if (vertices.contains(up)) {
					adjacent.add(up);
				}
			}
			
			// DOWN
			if (s.getY() + 20 <= BOARDSIZE) {
				Point2D.Double down = new Point2D.Double(s.getX(), s.getY() + 20);
				if (vertices.contains(down)) {
					adjacent.add(down);
				}
			}
			
			for(int i = 0; i < adjacent.size(); i++) {
				Point2D.Double t = adjacent.get(i);
				Iterator<Line2D> wallIterator = walls.iterator();
				
				while(wallIterator.hasNext()) {
					Line2D w = wallIterator.next();
					if (w.intersectsLine(new Line2D.Double(s,t))) {
						adjacent.remove(t);
					}
				}
			}
			
			graph.put(s, adjacent);
		}
	}
	
	
	
	/*
	 *  computes a zig zag path throughout the board
	 */
	private void zigZagPaths() {
		int pathSize = zigZagPath.size()/(numLights - 1);
		for (Light l: lights) {
			int index = 0;
			ArrayList<Point2D> nextPath = new ArrayList<Point2D>();
			while(zigZagPath.get(index).equals(l.getLocation()) == false) {
				index++;
			}
			
			for (int i = 0; i < pathSize; i++) {
				nextPath.add(zigZagPath.get(index + i));
			}
			
			nextPath.add(collectorLocation);
			paths.put(l, nextPath);
		}
	}
	
	/*
	 * returns true if the distance from point to line is < 0.01 and false otherwise
	 */
	private boolean isOnLine(Line2D line, Point2D point) {
		boolean onLine = false;
	    double distance =  line.ptLineDist(point);
		onLine = (Math.abs(distance) < 1);
		return onLine;
	}
	
	
	private boolean intersectsWall(Point2D.Double point) {
		boolean intersects = false;
		for (Line2D w: walls) {
			if (isOnLine(w, point)) {
				intersects = true;
				break;
			}
		}
		
		return intersects;
	}
	
	private Point2D notOnWall(Point2D.Double point) {
		Point2D freepoint = point;
		for (Line2D w : walls) {
			if (isOnLine(w, freepoint)) {
				if (intersectsWall(new Point2D.Double(point.getX() - 1, point.getY())) == false) {
					freepoint = new Point2D.Double(point.getX() - 1, point.getY()); 
					break;
				}
				else if (intersectsWall(new Point2D.Double(point.getX() + 1, point.getY())) == false) {
					freepoint = new Point2D.Double(point.getX() + 1, point.getY());
					break;
				}
				else if (intersectsWall(new Point2D.Double(point.getX(), point.getY() - 1)) == false) {
					freepoint = new Point2D.Double(point.getX(), point.getY() - 1);
					break;
				}
				else if (intersectsWall(new Point2D.Double(point.getX(), point.getY() + 1)) == false) {
					freepoint = new Point2D.Double(point.getX(), point.getY() + 1);
					break;
				}
				else {
					Random rand = new Random();
					int direction = rand.nextInt(4);
					switch (direction) {
					case 0:
						if (freepoint.getX() > 1) {
							freepoint = notOnWall(new Point2D.Double(freepoint.getX() - 1, freepoint.getY()));
						}
						else 
							freepoint = notOnWall(new Point2D.Double(freepoint.getX() + 1, freepoint.getY()));
						break;
					case 1:
						if (freepoint.getX() < BOARDSIZE) {
							freepoint = notOnWall(new Point2D.Double(freepoint.getX() + 1, freepoint.getY()));
						}
						else 
							freepoint = notOnWall(new Point2D.Double(freepoint.getX() - 1, freepoint.getY()));
						break;
					case 2:
						if (freepoint.getY() > 1) {
							freepoint = notOnWall(new Point2D.Double(freepoint.getX(), freepoint.getY() - 1));
						}
						else 
							freepoint = notOnWall(new Point2D.Double(freepoint.getX(), freepoint.getY() + 1));
						break;
					case 3:
						if (freepoint.getY() < BOARDSIZE) {
							freepoint = notOnWall(new Point2D.Double(freepoint.getX(), freepoint.getY() + 1));
						}
						else 
							freepoint = notOnWall(new Point2D.Double(freepoint.getX(), freepoint.getY() - 1));
						break;
					default:
						if (freepoint.getY() < BOARDSIZE) {
							freepoint = notOnWall(new Point2D.Double(freepoint.getX(), freepoint.getY() + 1));
						}
						else 
							freepoint = notOnWall(new Point2D.Double(freepoint.getX(), freepoint.getY() - 1));
						break;
					}
					
				}
			}
		}
		
		return freepoint;
	}
	
	private ArrayList<Point2D> zigZagPath() {
		ArrayList<Point2D> path = new ArrayList<Point2D>();
		boolean movingRight = true;
		for (int i = 5; i < BOARDSIZE; i+= 20) {
			int j = (movingRight) ? 5 : 85;
			int step = (movingRight) ? 20 : -20;
			while (j < BOARDSIZE && j > 0) {
				Point2D.Double nextPoint = (Point2D.Double)notOnWall(new Point2D.Double(i, j));
				while (nextPoint.equals(collectorLocation)) {
					double nextX = Math.max(BOARDSIZE, nextPoint.getX() + 1);
					nextPoint = (Point2D.Double)notOnWall(new Point2D.Double(nextX, nextPoint.getY()));
				}
				path.add(nextPoint);
				j+= step;
			}
			movingRight = !movingRight;
		}
		
		path.add(collectorLocation);
		return path;
	}
	
	
	private void computeMST() {
		HashSet<Point2D.Double> seen = new HashSet<Point2D.Double>();
		//compute mst		
		while(seen.size() != vertices.size()) {
			for(Point2D.Double p:vertices) {
				ArrayList<Point2D.Double> adjacent = graph.get(p);
				seen.add(p);
				
				for (Point2D.Double a:adjacent) {
					if (!seen.contains(a)) {
						ArrayList<Point2D.Double> pAdjacent;
						ArrayList<Point2D.Double> aAdjacent;

						if (mst.containsKey(p)) {
							pAdjacent = mst.get(p);
						}
						else {
							pAdjacent = new ArrayList<Point2D.Double>();
						}

						pAdjacent.add(a);
						mst.put(p, pAdjacent);
						seen.add(a);

						if (mst.containsKey(a)) {
							aAdjacent = mst.get(a);
						}
						else {
							aAdjacent = new ArrayList<Point2D.Double>();
						}

						aAdjacent.add(p);
						mst.put(a, aAdjacent);
					}
				}
			}	
		}
	}
	
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
	
	boolean isObstructedExtended(Point2D.Double startPoint, Point2D.Double endPoint) {
		Iterator<Line2D> wallIterator = extendedwalls.iterator();		
		Line2D.Double testLine = new Line2D.Double(startPoint, endPoint);
		
		boolean intersects = false;
		while (wallIterator.hasNext()) {
			Line2D.Double wall = (Line2D.Double)wallIterator.next();
			if (lineIntersect(testLine, wall) != null) {
					return true;
				}
			
		}
		
		return intersects;
	}
	
	
	Line2D getExtendedObstruction(Point2D.Double startPoint, Point2D.Double endPoint) {
		Iterator<Line2D> wallIterator = extendedwalls.iterator();		
		Line2D.Double testLine = new Line2D.Double(startPoint, endPoint);
		while (wallIterator.hasNext()) {
			Line2D.Double wall = (Line2D.Double)wallIterator.next();
			if (lineIntersect(testLine, wall) != null) {
					return wall;
				}
			
		}
		
		return null;
	}
	
	boolean isObstructed(Point2D.Double startPoint, Point2D.Double endPoint) {
		Iterator<Line2D> wallIterator = walls.iterator();		
		Line2D.Double testLine = new Line2D.Double(startPoint, endPoint);
		
		boolean intersects = false;
		while (wallIterator.hasNext()) {
			Line2D.Double wall = (Line2D.Double)wallIterator.next();
//			if (lineIntersect(testLine, wall) != null) {
				if (testLine.intersectsLine(wall)) {	
					return true;
				}
			
		}
		
		return intersects;
	}

	private boolean captured(Point2D.Double p) {
		boolean withinLightRad = false;
		for (Light l : lights) {
			Point2D.Double lightLocation = (Point2D.Double)l.getLocation();
			if (withinLightRadius(lightLocation, p) && isObstructed(lightLocation, p) == false) {
				withinLightRad = true;
				break;
			}
		}
		
		return withinLightRad;
	}
	
	private boolean allMosquitosCaptured(int[][] board) {
		for (int i = 0; i < board.length; i ++) {
			for (int j = 0; j < board[0].length; j ++) {
				if (board[i][j] > 0) {
					if (!captured(new Point2D.Double(i, j))) {
							return false;
						}
				}
			}
		}
		
		return true;
	}
	
	public Set<Light> getLights(int[][] board) {
		lights = new HashSet<Light>();
		buildGraph();	
		
		this.zigZagPath = zigZagPath();
		int pathSize = zigZagPath.size()/(numLights - 1);
		
		// initialize lights
		for(int i = 0; i < numLights-1; i ++) {
			Point2D nextstart = zigZagPath.get(i * pathSize);
			Light l = new MoveableLight(nextstart.getX(), nextstart.getY(), true);
			lights.add(l);
		}
		
		zigZagPaths();
		if(collectorLocation.getX() > 0) { 
			collectorLight = new MoveableLight(collectorLocation.getX() - 1, collectorLocation.getY(), true);
		} else {
			collectorLight = new MoveableLight(collectorLocation.getX() + 1, collectorLocation.getY(), true);
		}
		
		lights.add(collectorLight);
		
		// initialize moves map
		ArrayList<Direction> start = new ArrayList<Direction>();
		start.add(Direction.STAY);
		start.add(Direction.STAY);
		start.add(Direction.STAY);
		start.add(Direction.STAY);
		start.add(Direction.STAY);
		start.add(Direction.STAY);
		start.add(Direction.STAY);
		start.add(Direction.STAY);
		start.add(Direction.STAY);
		start.add(Direction.STAY);
		
		for (Light l : lights) {
			lastTenTurns.put((MoveableLight)l, start);
			waitTurns.put((MoveableLight)l, 10);
		}
		
		return lights;
	}
	
	private boolean locallyCollected(MoveableLight ml, int [][] board) {
		boolean collected = true;
		Point2D.Double lightLocation = (Point2D.Double)ml.getLocation();
		int lightX = (int)Math.round(lightLocation.getX());
		int lightY = (int)Math.round(lightLocation.getY());
		int startX = Math.max(0, lightX - 20);
		int startY = Math.max(0, lightY - 20);
		
		int endX = Math.min(100, lightX + 20);
		int endY =  Math.min(100, lightY + 20);
		
		for (int i = startX; i < endX; i ++) {
			for (int j = startY; j < endY; j ++) {
				Point2D.Double curr = new Point2D.Double(i, j);
				if (board[i][j] > 0 && !isObstructed(curr, lightLocation) && curr.distance(lightLocation) > 5) {
					boolean obstructed = isObstructed(curr, lightLocation);
					return false;
				}
			}
		}
		
		return collected;
	}
	
	public Set<Light> updateLights(int[][] board) {		
		turns ++;
		
		//for each light
		for (Light l : lights) {
			
			//standard setup
			MoveableLight ml = (MoveableLight)l;
			Point2D.Double dest = null;

			// don't move collector light
			if (ml.equals(collectorLight)) {
				if (ml.getLocation().equals(collectorLocation) == false) {
					ml.moveTo(collectorLocation.getX(), collectorLocation.getY());
				}	
				continue;
			}
			
			else if (ml.getLocation().distance(collectorLocation) < 10 && 
						!isObstructed((Point2D.Double) ml.getLocation(), (Point2D.Double)collectorLocation)) {
				if (!greedyLights.containsKey(ml) || greedyLights.get(ml).distance(collectorLocation) > 20) {
					ml.turnOff();
				}
			}
			
			else if (ml.getLocation().distance(collectorLocation) > 20 && !ml.isOn()) {
				ml.turnOn();
			}
			
			
			Point2D.Double p = (Point2D.Double)ml.getLocation();			
			ArrayList<Point2D> path = paths.get(l);
			if (path.size() == 1 && !allMosquitosCaptured(board)) {
//				if (greedyLocations.size() < 1) {
					computeGreedyLocations(board);
//				}
				
//				Point2D.Double newPoint = closestGreedyPoint(p);
				Point2D.Double newPoint = farthestGreedyPoint();
				
				if (newPoint.distance(p) > 1) {
					path.add(0, newPoint);
					greedyLights.put(ml, newPoint);
					greedyLocations.remove(newPoint);
				}
				
				else {
					greedyLocations.remove(newPoint);
				}
				
				paths.put(l, path);
				
				if (astarPaths.containsKey(ml)) {
					astarPaths.remove(ml);
				}
			}
			
			dest = (Point2D.Double)path.get(0);
			
			if (allMosquitosCaptured(board)) {
				path = new ArrayList<Point2D>();
				path.add(collectorLocation);
				paths.put(ml, path);
				dest = (Point2D.Double) collectorLocation;
				
				if (astarPaths.containsKey(ml)) {
					ArrayList<Point2D.Double> astarPath = astarPaths.get(ml);
					int lastIndex  = astarPath.size() - 1; 
					if (astarPath.size() == 0 || (astarPath.get(lastIndex).equals(collectorLocation) == false)) {
						astarPaths.remove(ml);
					}
				}
			}
			
			// wait if we are changing directions
			if (waitTurns.containsKey(l)) {
				int waitsleft = waitTurns.get(l);
				waitsleft --;
				if (waitsleft == 0) {
					waitTurns.remove(l);
				}
				else {
					waitTurns.put(ml, waitsleft);
					continue;
				}
			}
			
			// check if we are changing directions
			if (changingDirections(ml)) {
				waitTurns.put(ml, waits);
				continue;
			}
			
			// if we're at the destination, get the next destination point
			if (p.getX() == dest.getX() && p.getY() == dest.getY() && path.size() > 1) {
				path.remove(0);
				if (greedyLights.containsKey(ml) && greedyLights.get(ml).equals(p)) {
					greedyLights.remove(ml);
				}
				
				Point2D.Double nextPoint = (Point2D.Double)(path.get(0));
				
				// create an astar path if the next destination is obstructed
				if (isObstructed(p, nextPoint)) {
					try {
						ArrayList<Point2D.Double> astarPath = astar.getPath(ml, nextPoint, board);
						Point2D.Double firstPoint = astarPath.get(0);
						if (p.distance(firstPoint) > 1) {
							System.out.println("illegal move here");
						}
						
						ml.moveTo(firstPoint.getX(), firstPoint.getY());
						Direction nextDirection = computeNextDirection(p, firstPoint);
						addNextDirection(ml, nextDirection);
						
						astarPath.remove(0);
						astarPaths.put(ml, astarPath);
					}
					catch (Exception e) {
						logger.trace("caught exception 1");
					}
				}
				
				else {
					moveTowards(ml, nextPoint);
				}
			}
		
			// if this light is moving astar, get the next point
			else if (astarPaths.containsKey(ml)) {
				ArrayList<Point2D.Double> astarPath = astarPaths.get(ml);
				if (astarPath.size() > 0) {
					Point2D.Double nextPoint = astarPath.get(0);
					astarPath.remove(0);
					if (p.distance(nextPoint) > 1) {
						System.out.println("illegal move here");
					}
					
					ml.moveTo(nextPoint.getX(), nextPoint.getY());
					
					Direction nextDirection = computeNextDirection(p, nextPoint);
					addNextDirection(ml, nextDirection);
					astarPaths.put(ml, astarPath);
				}
				
				else {
					astarPaths.remove(ml);
				}
			}
			
			else if (isObstructed(p, dest)) {
				try {
					ArrayList<Point2D.Double> astarPath = astar.getPath(ml, dest, board);
					Point2D.Double firstPoint = astarPath.get(0);
					
					// keep track of which direction we're moving
					if (p.distance(firstPoint) > 1) {
						System.out.println("illegal move here");
					}
					
					Direction nextDirection = computeNextDirection(p, firstPoint);
					addNextDirection(ml, nextDirection);
					
					ml.moveTo(firstPoint.getX(), firstPoint.getY());
					astarPath.remove(0);
					astarPaths.put(ml, astarPath);
				}
				catch (Exception e) {
					logger.trace("caught exception 2");
				}
			}
			
			else {
					moveTowards(ml, dest);
			}
			
		}
		return lights;
	}
	
	private Direction computeNextDirection(Point2D.Double current, Point2D.Double next) {
		Direction nextDirection = Direction.STAY;
		if (next.getX() > current.getX()) {
			nextDirection = Direction.RIGHT;
		}
		else if (next.getX() < current.getX()) {
			nextDirection = Direction.LEFT;
		}
		else if (next.getY() > current.getY()) {
			nextDirection = Direction.DOWN;
		}
		else if (next.getY() < current.getY()) {
			nextDirection = Direction.UP;
		}
		return nextDirection;
	}

	private void addNextDirection(MoveableLight l, Direction next) {
		int index = turns % 10;
		ArrayList<Direction> directions = lastTenTurns.get(l);
		directions.set(index, next);
		lastTenTurns.put(l, directions);
	}
	
	private boolean changingDirections(MoveableLight l) {
		if (turns < 10) {
			return false;
		}
		
		ArrayList<Direction> pastTenTurns = lastTenTurns.get(l);
		boolean changing = false;
		boolean wasConstant = true;
		int startIndex = (turns - 10) % 10;
		
		// if we were just staying put, return false
		if (pastTenTurns.get((turns - 1) % 10) == Direction.STAY) {
			return false;
		}
		
		for (int i = 0; i < 8; i++) {
			int ind1 = startIndex;
			int ind2 = (startIndex + 1) % 10;
			if (pastTenTurns.get(ind1) != pastTenTurns.get(ind2)) {
				wasConstant = false;
				break;
			}
			
			startIndex = (startIndex + 1) % 10;
		}
		
		if (wasConstant && pastTenTurns.get(turns % 10) != pastTenTurns.get((turns - 1) % 10)) {
			changing = true;
		}
		return changing;
	}
	
	private boolean moveTowards(MoveableLight l, Point2D.Double dest) {
		Direction nextDirection = Direction.STAY;
		Point2D.Double current = (Point2D.Double) l.getLocation();
		boolean moved = false;
		double xdiff = current.getX() - dest.getX();
		double ydiff = current.getY() - dest.getY();
		Line2D obstruction = getExtendedObstruction(current, dest);
		
		if (obstruction != null) {
			int buffer = 10;
			while (buffer > 0 && moved == false) {
				Point2D.Double below = new Point2D.Double(current.getX(), Math.min(BOARDSIZE, current.getY() + buffer));
				Point2D.Double above = new Point2D.Double(current.getX(), Math.max(0, current.getY() - buffer));
				Point2D.Double left = new Point2D.Double(Math.max(0,current.getX() - buffer), current.getY());
				Point2D.Double right = new Point2D.Double(Math.min(BOARDSIZE,current.getX() + buffer), current.getY());
				if (isObstructedExtended(above, dest) == false && ydiff > 0) {
					l.moveUp();
					nextDirection = Direction.UP;
					moved = true;
				}
				else if (isObstructedExtended(below, dest) == false && ydiff < 0) {
					l.moveDown();
					nextDirection = Direction.DOWN;
					moved = true;
				}
				else if (isObstructedExtended(left, dest) == false && xdiff > 0) {
					l.moveLeft();
					nextDirection = Direction.LEFT;
					moved = true;
				}
				else if (isObstructedExtended(right, dest) == false && xdiff < 0) {
					l.moveRight();
					nextDirection = Direction.RIGHT;
					moved = true;
				}
				
				buffer --;
			}
		}
		
		if (!moved) {
			if (Math.abs(xdiff) > Math.abs(ydiff)) {
				if (current.getX() > dest.getX()) {
					l.moveLeft();
					nextDirection = Direction.LEFT;
					moved = true;
				}
				else if (current.getX() < dest.getX()){
					l.moveRight();
					nextDirection = Direction.RIGHT;
					moved = true;
				}
			}
			else {
				if (current.getY() > dest.getY()) {
					l.moveUp();
					nextDirection = Direction.UP;
					moved = true;
				}
				else if (current.getY() < dest.getY()) {
					l.moveDown();
					nextDirection = Direction.DOWN;
					moved = true;
				}
			}
		}
		
		addNextDirection(l, nextDirection);
		return moved;
	}
	
	
	@Override
	public Collector getCollector() {
		// this one just places a collector next to the last light that was added
		Collector c = new Collector(collectorLocation.getX(), collectorLocation.getY());
		return c;
	}
	
}


