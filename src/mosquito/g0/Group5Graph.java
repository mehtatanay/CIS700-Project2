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

public class Group5Graph extends mosquito.sim.Player  {

	private int numLights;
	private Point2D.Double lastLight;
	private static final double LIGHTRADIUS = 10.0;
	private static final double BOARDSIZE = 100.0;
	private Logger log = Logger.getLogger(this.getClass()); // for logging
	private Point2D collectorLocation;
	private MoveableLight collectorLight;
	private HashSet<Point2D.Double> vertices = new HashSet<Point2D.Double> ();
	private HashMap<Point2D.Double, Point2D.Double> edges = new HashMap<Point2D.Double, Point2D.Double> ();
	private HashMap<Point2D.Double, Point2D.Double> mst = new HashMap<Point2D.Double, Point2D.Double> ();
	
	@Override
	public String getName() {
		return "G5Player Graph";
	}
	
	private Set<Light> lights;
	private static Set<Line2D> walls;
		
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
	
	public Set<Light> getLights(int[][] board) {
		lights = new HashSet<Light>();
		for(int i = 0; i < numLights-1; i ++) {
			Light l = new MoveableLight(0, (100*i)/(numLights-1), true);
			lights.add(l);
		}
		
		//populate vertices
		for(int i = 0; i < 100; i = i+10) {
			for(int j = 0; j < 100; j = j+10) {
				vertices.add(new Point2D.Double(i, j));
			}
		}
		
		//populate edges - fucking awful runtime
		for(Point2D.Double s:vertices) {
			for(Point2D.Double t:vertices) {
				for(Line2D w:walls) {
					if(!w.intersectsLine(new Line2D.Double(s,t))) {
						edges.put(s, t);
					}
				}
			}
		}
		
		HashSet<Point2D.Double> seen = new HashSet<Point2D.Double> ();
		//compute mst		
		while(seen.size() != vertices.size()) {
			for(Point2D.Double p:vertices) {
				if(seen.size() == 0) {seen.add(p);}
				else {
					for(Point2D.Double e:edges.keySet()) {
						if(e.getX() == p.getX() && e.getY() == p.getY()) {
							if(!seen.contains(edges.get(e))) {
								mst.put(e, edges.get(e));
								seen.add(edges.get(e));
							}
						}
					}
				}
			}
		}
		
		return lights;
	}

	//TODO - UPDATE FOR GRAPH MST ALGO
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
