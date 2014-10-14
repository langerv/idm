import java.util.logging.*;
import java.util.ArrayList;
import java.util.Collections;

/**
 * A path finder implementation that uses the AStar heuristic based algorithm
 * to determine a path in a grid world representation.
 * 
 * @author adapted from Kevin Glass
 */
public class aStarPathfinder
{
    private Logger logger = Logger.getLogger("iDM.mas2j."+Pathfinder.class.getName());

	private int 		width, height ;
	
	private Node[][]	nodes ;
	
	/** The set of nodes that have been searched through */
	private ArrayList closed = new ArrayList();
	
	/** The set of nodes that we do not yet consider fully searched */
	private SortedList open = new SortedList();	

    public aStarPathfinder(int w, int h)
	{
		width = w ;
		height = h ;
		nodes = new Node[width][height] ;
		for (int i = 0; i < width; i++) 
		{
			for (int j = 0; j < height; j++) 
			{
				nodes[i][j] = new Node(i,j) ;
			}
		}
	}
	
	public void aStarSetType(int i, int j, int type)
	{
		try
		{
			nodes[i][j].type = type ;
		}
		catch (ArrayIndexOutOfBoundsException e)
		{
			logger.warning("Array index out of bounds in setNodeType: "+e) ;
		}
	}
	
	public Path aStarFindPath(int sx, int sy, int gx, int gy, int invalid)
	{
		// clear lists and restore states
		clearLists();

		// init start, put it in open
		nodes[sx][sy].cost = 0 ;
		addToOpen(nodes[sx][sy]);
		
		// init goal (no parent yet)
		nodes[gx][gy].parent = null ;
		
		while (open.size() != 0) 
		{
			Node current = getFirstInOpen() ;

			if ( current == nodes[gx][gy] )
			{
				break ;
			}
			
			removeFromOpen(current) ;
			addToClosed(current) ;
			
			for (int x = -1; x < 2; x++)
			{
				for (int y = -1; y < 2; y++) 
				{
					// not a neighbour, its the current tile
					if (x == 0 && y == 0) 
					{
						continue;
					}
					
					int xp = x + current.x ;
					int yp = y + current.y ;
					
					if ( isValidLocation(sx, sy, xp, yp, invalid ) )
					{
						// the cost to get to this node is cost the current plus the movement
						// cost to reach this node. Note that the heursitic value is only used
						// in the sorted open list
						float nextStepCost = current.cost + getMapCost(current.x, current.y, xp, yp) ;
						
						Node neighbour = nodes[xp][yp];
						
						// if the new cost we've determined for this node is lower than 
						// it has been previously makes sure the node hasn'e've
						// determined that there might have been a better path to get to
						// this node so it needs to be re-evaluated
						if (nextStepCost < neighbour.cost) 
						{
							if (inOpenList(neighbour)) 
							{
								removeFromOpen(neighbour);
							}
							if (inClosedList(neighbour)) 
							{
								removeFromClosed(neighbour);
							}
						}
						
						// if the node hasn't already been processed and discarded then
						// reset it's cost to our current cost and add it as a next possible
						// step (i.e. to the open list)
						if (!inOpenList(neighbour) && !(inClosedList(neighbour))) 
						{
							neighbour.cost = nextStepCost;
							neighbour.heuristic = getHeuristicCost(xp, yp, gx, gy);
							neighbour.parent = current ;
							addToOpen(neighbour);
						}						
					}
				}
			}								
		}
		
		// since we'e've run out of search 
		// there was no path. Just return null
		if (nodes[gx][gy].parent == null) 
		{
			return null;
		}
		
		// At this point we've definitely found a path so we can uses the parent
		// references of the nodes to find out way from the target location back
		// to the start recording the nodes on the way.
		Path path = new Path();
		Node goal = nodes[gx][gy];
		while (goal != nodes[sx][sy]) 
		{
			path.prependStep(goal.x, goal.y);
			goal = goal.parent;
		}
		path.prependStep(sx,sy);
		
		// thats it, we have our path 
		return path;
	}

	protected float getMapCost(int sx, int sy, int tx, int ty) 
	{
		// diagonals more costly
		return (float)(Math.abs(tx - sx) + Math.abs(ty - sy)) ;
	}
	
	protected float getHeuristicCost(int x1, int y1, int x2, int y2) 
	{
		// use Euclidean distance
		float dx = x2 - x1;
		float dy = y2 - y1;
		return (float) (Math.sqrt((dx*dx)+(dy*dy)));
	}

	/**
	 * Check if a given location is valid
	 * 
	 * @param sx The starting x coordinate
	 * @param sy The starting y coordinate
	 * @param x The x coordinate of the location to check
	 * @param y The y coordinate of the location to check
	 * @param invalidTypes The invalid types of the node to check
	 * @return True if the location is valid
	 */
	protected boolean isValidLocation(int sx, int sy, int x, int y, int invalidTypes) 
	{
		boolean invalid = (x < 0) || (y < 0) || (x >= width) || (y >= height);
		
		if ((!invalid) && ((sx != x) || (sy != y))) 
		{
			invalid = (nodes[x][y].type & invalidTypes) != 0 ;
		}
		
		return !invalid;
	}
	
	/**
	 * Clear open and closed lists
	 * Restore open and closed states in nodes
	 */
	protected void clearLists() 
	{
		for(int i=0; i<closed.size();i++)
		{
			((Node)closed.get(i)).inClosed = false ;
		}
		closed.clear() ;
		
		for(int i=0; i<open.size();i++)
		{
			((Node)open.get(i)).inOpen = false ;
		}
		open.clear() ;
	}		

	
	/**
	 * Get the first element from the open list. This is the next
	 * one to be searched.
	 * 
	 * @return The first element in the open list
	 */
	protected Node getFirstInOpen() 
	{
		return (Node)open.first();
	}
	
	/**
	 * Add a node to the open list
	 * 
	 * @param node The node to be added to the open list
	 */
	protected void addToOpen(Node node) 
	{
		node.inOpen = true ;
		open.add(node);
	}
	
	/**
	 * Check if a node is in the open list
	 * 
	 * @param node The node to check for
	 * @return True if the node given is in the open list
	 */
	protected boolean inOpenList(Node node) 
	{
		return node.inOpen ;
	}
	
	/**
	 * Remove a node from the open list
	 * 
	 * @param node The node to remove from the open list
	 */
	protected void removeFromOpen(Node node) 
	{
		node.inOpen = false ;
		open.remove(node);
	}
	
	/**
	 * Add a node to the closed list
	 * 
	 * @param node The node to add to the closed list
	 */
	protected void addToClosed(Node node) 
	{
		node.inClosed = true ;
		closed.add(node);
	}
	
	/**
	 * Check if the node supplied is in the closed list
	 * 
	 * @param node The node to search for
	 * @return True if the node specified is in the closed list
	 */
	protected boolean inClosedList(Node node) 
	{
		return node.inClosed ;
	}
	
	/**
	 * Remove a node from the closed list
	 * 
	 * @param node The node to remove from the closed list
	 */
	protected void removeFromClosed(Node node) 
	{
		node.inClosed = false ;
		closed.remove(node);
	}
		
		
	/**
	 * A simple sorted list
	 *
	 * @author kevin
	 */
	private class SortedList 
	{
		/** The list of elements */
		private ArrayList list = new ArrayList();
		
		/**
		 * Retrieve the first element from the list
		 *  
		 * @return The first element from the list
		 */
		public Object first() 
		{
			return list.get(0);
		}
		
		/**
		 * Retrieve the element i from the list
		 *  
		 * @return The element i from the list
		 */
		public Object get(int i) 
		{
			return list.get(i);
		}
		
		/**
		 * Empty the list
		 */
		public void clear() 
		{
			list.clear();
		}
		
		/**
		 * Add an element to the list - causes sorting
		 * 
		 * @param o The element to add
		 */
		public void add(Object o) 
		{
			list.add(o);
			Collections.sort(list);
		}
		
		/**
		 * Remove an element from the list
		 * 
		 * @param o The element to remove
		 */
		public void remove(Object o) 
		{
			list.remove(o);
		}
	
		/**
		 * Get the number of elements in the list
		 * 
		 * @return The number of element in the list
 		 */
		public int size() 
		{
			return list.size();
		}
		
		/**
		 * Check if an element is in the list
		 * 
		 * @param o The element to search for
		 * @return True if the element is in the list
		 */
		public boolean contains(Object o) 
		{
			return list.contains(o);
		}
	}
	
	private class Node implements Comparable
	{
		// coordinates
		private int 	x ;
		private	int		y ;
		
		// type
		private int		type ;
			
		// heuristics
		private float	cost ;
		private float 	heuristic ;
	
		// node states
		private boolean	inOpen = false ;
		private boolean inClosed = false ;
		
		// path 		
		private Node	parent ;
		
		public Node(int x, int y) 
		{
			this.x = x ;
			this.y = y ;
		}
		
		public int compareTo(Object other) 
		{
			Node o = (Node) other;
			
			float f = heuristic + cost;
			float of = o.heuristic + o.cost;
			
			if (f < of) 
			{
				return -1;
			}
			else if (f > of) 
			{
				return 1;
			}
			else 
			{
				return 0;
			}
		}		
	}
}


