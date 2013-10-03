package mosquito.g0;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.util.ArrayList;
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
	private Point2D.Double lastLight;
	private Logger log = Logger.getLogger(this.getClass()); // for logging
	private HashSet<MoveableLight> upLights = new HashSet<MoveableLight> ();
	private HashMap<MoveableLight, Point2D.Double> greedyLights = new HashMap<MoveableLight, Point2D.Double> ();
	private HashMap<MoveableLight, Point2D.Double> start = new HashMap<MoveableLight, Point2D.Double> ();
	private HashMap<MoveableLight, Point2D.Double> moving = new HashMap<MoveableLight, Point2D.Double> ();
	private HashMap<Point2D.Double, Integer> mosquitos = new HashMap<Point2D.Double, Integer> ();
	
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

		ArrayList<Line2D> lines = new ArrayList<Line2D>();
		Line2D line = new Line2D.Double(30, 30, 80, 80);
		lines.add(line);
		return lines;
	}


	public Set<Light> getLights(int[][] board) {
		lights = new HashSet<Light>();
		for(int i = 0; i<numLights;i++)
		{
			
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
		for (Light l : lights) {
			MoveableLight ml = (MoveableLight)l;
			
			
		}
		return lights;
	}

	@Override
	public Collector getCollector() {
		// this one just places a collector next to the last light that was added
		Collector c = new Collector(50, 50);
		log.debug("Positioned a Collector at (" + (lastLight.getX()+1) + ", " + (lastLight.getY()+1) + ")");
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
		g_score.put(start, 0d);
		//estimated total cost from start to goal through y
		f_score.put(start, distanceBetween(start, destination));
		
		while (openSet.size() > 0){
			
		}
		
		
	}
	
	private static double distanceBetween(Point2D.Double start, Point2D.Double end){
		return Math.sqrt(Math.pow(end.x - start.x, 2) + Math.pow(end.y - start.y, 2));
	}
}
