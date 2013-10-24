package mosquito.g0;

import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Point2D.Double;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class graph {
	
	private static Set<Line2D> walls;
	public HashMap<Point2D.Double,ArrayList<Point2D.Double>> g;
	private int gridSize = 20;
	private int numlights;
	private double angleIncrement = Math.PI/32;
	private Point2D.Double start;
	private HashSet<Point2D.Double> discovered = new HashSet<Point2D.Double>();
	
	public graph(Set<Line2D> w, int numlights){
		walls = w;
		this.numlights = numlights;
		g = new HashMap<Point2D.Double,ArrayList<Point2D.Double>>();
		start = new Point2D.Double(Math.floor(gridSize/2),Math.floor(gridSize/2));
		
		this.buildGraph((Point2D.Double)start);
		
		Set<Point2D.Double> prunedset = new HashSet<Point2D.Double>();
		Set<Point2D.Double> leafset = new HashSet<Point2D.Double>();
		//prune leaves
		for(Point2D.Double key : g.keySet()){
			ArrayList<Point2D.Double> children = g.get(key);
			if(children.size()==0){
				leafset.add(key);
			}
			for(Point2D.Double child : children){
				if(!g.containsKey(child))
					leafset.add(child);
			}
		}
		for(Point2D.Double leaf : leafset){
			for(Point2D.Double test : g.keySet()){
				if(!test.equals(leaf) && !prunedset.contains(test)){
					for(Point2D.Double test2 : g.get(test)){
						if(!test2.equals(leaf)){
							Line2D testLine = new Line2D.Double(test,test2);
							if(testLine.ptLineDist(leaf) < gridSize)
								prunedset.add(leaf);
						}
					}
				}
			}
		}
		for(Point2D.Double key : g.keySet()){
			ArrayList<Point2D.Double> neighbors = g.get(key);
			for(Point2D.Double leaf : prunedset){
				if(neighbors.contains(leaf))
					neighbors.remove(leaf);
			}
			g.put(key, neighbors);
		}
	}
	
	public ArrayList<Point2D.Double> getPath(){
		ArrayList<Point2D.Double> path = new ArrayList<Point2D.Double>();
//		ArrayList<Point2D.Double> Q = new ArrayList<Point2D.Double>();
//		HashSet<Point2D.Double> V = new HashSet<Point2D.Double>();
//		Q.add(start);
//		V.add(start);
//		while(Q.size()>0){
//			Point2D.Double t = Q.remove(0);
//			path.add(t);
//			for(Point2D.Double u : g.get(t)){
//				if(!V.contains(u)){
//					V.add(u);
//					Q.add(u);
//				}
//			}
//		}
		dfs(start,path);
		return path;
	}
	
	private void dfs(Point2D.Double v,ArrayList<Point2D.Double>path){
//		procedure DFS(G,v):
//			2      label v as discovered
//			3      for all edges e in G.adjacentEdges(v) do
//			4          if edge e is unexplored then
//			5              w = G.adjacentVertex(v,e)
//			6              if vertex w is unexplored then
//			7                  label e as a discovered edge
//			8                  recursively call DFS(G,w)
//			9              else
//			10                 label e as a back edge
//			11      label v as explored
		discovered.add(v);
		path.add(v);
		for(Point2D.Double w : g.get(v)){
			if(!discovered.contains(w)){
				dfs(w,path);
			}
		}
	}
	
	private void buildGraph(Point2D.Double start){
		ArrayList<Point2D.Double> neighbors = getNeighbors(start);
		if(neighbors.size()>0){
			g.put(start, neighbors);
			for(Point2D.Double neighbor : neighbors){
				if(!g.containsKey(neighbor))
					this.buildGraph(neighbor);
			}
		}
	}
	
	private ArrayList<Point2D.Double> getNeighbors(Point2D.Double p){
		ArrayList<Point2D.Double> neighbors = new ArrayList<Point2D.Double>();
		//try radially by gridSize
		for(float angle=0;angle<2*Math.PI;angle+=angleIncrement){
			Point2D.Double goal = new Point2D.Double(p.x+Math.floor(gridSize*Math.cos(angle)),p.y+Math.floor(gridSize*Math.sin(angle)));
			if(pathExists(p,goal)){
				//if it isn't within a gridsize free straight distance from a point already in the graph
				boolean add = true;
				Set<Point2D.Double> considerationSet = new HashSet<Point2D.Double>();
				considerationSet.addAll(g.keySet());
				for(Point2D.Double n : neighbors){
					considerationSet.add(n);
				}
				for(Point2D.Double key : considerationSet){
					Line2D test = new Line2D.Double(goal,key);
					boolean intersection = false;
					if(goal.distance(key)<gridSize){
						intersection = false;
						for(Line2D wall : walls){
							if(test.intersectsLine(wall))
								intersection = true;
						}
						if(!intersection){
							add = false;
							break;
						}
					}
				}
				if(add)
					neighbors.add(goal);
			}
		}
		return neighbors;
	}
	
	private boolean pathExists(Point2D.Double start, Point2D.Double goal){
		//do a bfs around the gridsize area
		List<Point2D.Double> Q = new ArrayList<Point2D.Double>();
		Set<Point2D.Double> V = new HashSet<Point2D.Double>();
		Q.add(start);
		V.add(start);
		while(Q.size()>0){
			Point2D.Double t = Q.remove(0);
			if(t.equals(goal))
				return true;
			for(Point2D.Double neighbor : bfs_neighbors(t,start.x-gridSize,start.y-gridSize,start.x+gridSize,start.y+gridSize)){
				if(!V.contains(neighbor)){
					V.add(neighbor);
					Q.add(neighbor);
				}
			}
		}
		return false;
	}
	
	private ArrayList<Point2D.Double> bfs_neighbors(Point2D.Double start, double min_x, double min_y, double max_x, double max_y){
		ArrayList<Point2D.Double> n = new ArrayList<Point2D.Double>();
		if(min_x<0)
			min_x = 0;
		if(min_y<0)
			min_y=0;
		if(max_x>100)
			max_x=100;
		if(max_y>100)
			max_y=100;
		//check above
		if(start.y + 1 <= max_y){
			boolean add = true;
			Point2D.Double line_end = new Point2D.Double(start.x, start.y + 1);
			for (Line2D obstacle : walls){
				if(obstacle.intersectsLine(new Line2D.Double(start,line_end)) || obstacle.ptLineDist(line_end)<=0.00)
					add = false;
			}
			if(add)
				n.add(new Point2D.Double(start.x,start.y + 1));
		}
		//check below
		if(start.y - 1 >= min_y){
			boolean add = true;
			Point2D.Double line_end = new Point2D.Double(start.x, start.y - 1);
			for (Line2D obstacle : walls){
				if(obstacle.intersectsLine(new Line2D.Double(start,line_end)) || obstacle.ptLineDist(line_end)<=0.00)
					add = false;
			}
			if(add)
				n.add(new Point2D.Double(start.x,start.y - 1));
		}
		//check right
		if(start.x + 1 <= max_x){
			boolean add = true;
			Point2D.Double line_end = new Point2D.Double(start.x+1, start.y);
			for (Line2D obstacle : walls){
				if(obstacle.intersectsLine(new Line2D.Double(start,line_end)) || obstacle.ptLineDist(line_end)<=0.00)
					add = false;
			}
			if(add)
				n.add(new Point2D.Double(start.x+1,start.y));
		}
		//check left
		if(start.x - 1 >= min_x){
			boolean add = true;
			Point2D.Double line_end = new Point2D.Double(start.x - 1, start.y);
			for (Line2D obstacle : walls){
				if(obstacle.intersectsLine(new Line2D.Double(start,line_end)) || obstacle.ptLineDist(line_end)<=0.00)
					add = false;
			}
			if(add)
				n.add(new Point2D.Double(start.x -1,start.y));
		}
		return n;
	}
	
	public ArrayList<Point2D.Double> get(Point2D.Double point){
		ArrayList<Point2D.Double> neighbors = new ArrayList<Point2D.Double>();
		if(g.containsKey(point))
			neighbors = g.get(point);
		return neighbors;
	}
	
}

