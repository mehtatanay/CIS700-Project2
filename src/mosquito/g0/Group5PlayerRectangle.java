package mosquito.g0;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;

import org.apache.log4j.Logger;

import mosquito.sim.Collector;
import mosquito.sim.Light;
import mosquito.sim.MoveableLight;

public class Group5PlayerRectangle extends mosquito.sim.Player  {

	private int numLights;
	private Point2D.Double lastLight;
	private Logger log = Logger.getLogger(this.getClass()); // for logging
	private HashSet<MoveableLight> upLights = new HashSet<MoveableLight> ();
	private HashSet<MoveableLight> greedyLights = new HashSet<MoveableLight> ();
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
			int y = (i % 2 == 0) ? 99 : 1;
			lastLight = new Point2D.Double(i*(100/numLights), y);
			MoveableLight l = new MoveableLight(lastLight.getX(),lastLight.getY(), true);
			start.put(l, lastLight);
			if(y == 99) { upLights.add(l); }
			log.trace("Positioned a light at (" + lastLight.getX() + ", " + lastLight.getY() + ")");
			lights.add(l);
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
			
			if(moving.containsKey(ml)) {
				Point2D.Double destination = moving.get(ml);
				if(Math.abs(destination.getX() - ml.getX()) < .5) {
					if(Math.floor(destination.getY()) > Math.floor(ml.getY())) {
						ml.moveDown();
					}else if(Math.floor(destination.getY()) < Math.floor(ml.getY())) {
						ml.moveUp();
					}else {
						moving.remove(ml);
						greedyLights.add(ml);
					}	
				}
				else if(Math.floor(destination.getX()) > Math.floor(ml.getX())) {
					ml.moveRight();
				} else if(Math.floor(destination.getX()) < Math.floor(ml.getX())) {
					ml.moveLeft();
				} 
				
			} else if(!greedyLights.contains(ml)){
				if(!upLights.contains(ml)) {
					if((int) Math.ceil(ml.getY()) == 100)
					{
						moving.put(ml, new Point2D.Double(50,50));
						upLights.add(ml);
						ml.moveRight();
					}else {
						ml.moveDown();
					}
				} else  {
					if((int) Math.floor(ml.getY()) == 0) { 
						moving.put(ml, new Point2D.Double(50,50));
						ml.moveRight();
					} else {
						ml.moveUp();
					}
				}
			}
			
			if(greedyLights.contains(ml)) {
				for(int i = 0; i < 100; i++) {
					for(int j = 0; j < 100; j++) {
						mosquitos.put(new Point2D.Double(i,j), board[i][j]);
					}
				}
				
			}
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
}
