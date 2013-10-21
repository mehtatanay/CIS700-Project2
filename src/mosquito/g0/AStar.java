package mosquito.g0;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import mosquito.sim.MoveableLight;


public class AStar {
	//obstacles in the board
	private static Set<Line2D> walls;
	
	//constructor takes the obstacles
	public AStar(Set<Line2D> w){walls = w;}
	
	//A* search method for optimized path. Implemented from the Wikipedia page's pseudo code.
	//returns an arraylist of points that the light should go for the optimal path
	public ArrayList<Point2D.Double> getPath(MoveableLight ml, Point2D.Double destination, int[][] board) throws Exception{
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
		f_score.put(start, (float)absoluteDistanceBetween(start, destination));
		
		//keep track of current node
		Point2D.Double current = new Point2D.Double();
		while (openSet.size() > 0){
			//get the next point to evaluate
			current = lowestScore(f_score, openSet);
			//if this point is within 1 from the goal, make the path and return it
			if(absoluteDistanceBetween(current, destination) <= 1.0){
				cameFrom.put(destination, current);
				shortestPath = reconstructPath(cameFrom, destination);
				return shortestPath;
			}
			
			//remove current from openSet and add it to visited
			openSet.remove(current);
			visited.add(current);
			
			//for each neighbor to the current node, find the ones to add to openSet
			for (Point2D.Double neighbor : getNeighbors(current)){
				float tentative_g_score = g_score.get(current) + (float)distanceBetween(current, neighbor, board);
				float tentative_f_score = tentative_g_score + (float)absoluteDistanceBetween(neighbor, destination);
				
				//HACK TO NOT GO AROUND CORNERS
				for(Line2D w:walls) {
					if(w.getP1().distance(neighbor) < 2 || w.getP2().distance(neighbor) < 2) {
						tentative_f_score += 1000;
					}
				}
				
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
		//if you reach the end of the while loop, no path could be found. In our case, I throw a generic exception
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
	private static double absoluteDistanceBetween(Point2D.Double start, Point2D.Double end){
		return Math.sqrt(Math.pow(end.x - start.x, 2) + Math.pow(end.y - start.y, 2));
	}
	
	//straight line distance modified to include mosquito density
	private static double distanceBetween(Point2D.Double start, Point2D.Double end, int[][] board){
		/*int mosquitos = 0;
		for (int i=-6;i<=6;i++)
		{
			for (int j=-6; j<=6;j++)
			{
				int new_x = (int)(end.x + i);
				int new_y = (int)(end.y + j);
				if(new_x >=0 && new_x <=100 && new_y >=0 && new_y <=100){
					try{
						mosquitos += board[new_x][new_y];
					}catch(Exception e){}
				}
			}
		}
		if(mosquitos == 0)
			mosquitos = 1;*/
		return Math.sqrt(Math.pow(end.x - start.x, 2) + Math.pow(end.y - start.y, 2)) + 100;
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
