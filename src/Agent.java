import java.awt.Color;
import jason.environment.grid.Location;

/**
 * A Class to encapsulate data and methods for agents
 * 
 * @author Herve Lange
 */
public class Agent
{
	/** Variables */
	protected int			agLife = 100 ;			// current life of the agent
	protected String		agName ;				// name (param)
    protected Location		agPos;					// current position (param)
	protected Color			agColor;				// color (param)
	protected int			agObstacles ;			// invalid locations (param)
	protected int			agRadius ;				// radius of perception
	protected Path			agPath = null;			// current path if exist
	protected int			agPathIndex = 0 ;		// current index (step) in path
	protected Location		agDestination = null ;	// agent's destination
	protected boolean		agBlocked = false ;		// true if agent's free to move
		
	/**
	 * Agent constructor
	 * 
	 * @param name		: agent's name
	 * @param pos		: agent's default position
	 * @param color		: agent's color
	 * @param obstacles	: agent's invalid locations (logic operator)
	 */
	public Agent(String name, Location pos, Color color, int obstacles, int radius) 
	{
		agName = name ;
		agPos = pos ;
		agColor = color ;
		agObstacles = obstacles ;
		agRadius = radius ;
	}

	/**
	 * test if the Agent is stills alive
	 * 
	 * @return true if alive else false
	 */
	synchronized public boolean isAlive()
	{
		return agLife>0 ;
	}

	/**
	 * set life of the agent
	 * 
	 * @param newlife	: new life
	 * @return true if the new life has been set 
	 */
	synchronized public boolean setLife(int newlife)
	{
		agLife = newlife ;
		return true ;
	}
	
	/**
	 * get current life of the agent
	 * 
	 * @return int life of the agent
	 */
	synchronized public int getLife()
	{
		return agLife ;
	}
	
	/**
	 * increase life of the agent of 1
	 * 
	 * @return true if the new life has been increased
	 */
	synchronized public boolean increaseLife()
	{
		agLife++ ;
		return true ;
	}

	/**
	 * decrease life of the agent of 1
	 * 
	 * @return true if the new life has been decreased
	 */
	synchronized public boolean decreaseLife()
	{
		if ( isAlive() == true )
		{
			agLife-- ;
			return true ;
		}
		return false ;
	}

	/**
	 * get the agent's radius of perception 
	 * 
	 * @return the perception radius
	 */
	synchronized public int getRadius()
	{
		return agRadius ;
	}

	/**
	 * get the agent's name
	 * 
	 * @return the name as a String
	 */
	synchronized public String getName()
	{
		return agName ;
	}
	
	/**
	 * set the agent's position
	 * 
	 * @param l	: new location
	 * @return true if the new position has been set
	 */
	synchronized public boolean setPos(Location l)
	{
		agPos = l ;
		return true ;
	}
	
	/**
	 * set the agent's position
	 * 
	 * @param x,y	: new position by x,y
	 * @return true if the new position has been set
	 */
	synchronized public boolean setPos(int x, int y)
	{
		agPos.x = x ;
		agPos.y = y ;
		return true ;
	}
	
	/**
	 * get the agent's position
	 * 
	 * @return the current location
	 */
	synchronized public Location getPos()
	{
		return (Location)agPos.clone() ;
	}

	/**
	 * get the agent's X position
	 * 
	 * @return the current X
	 */
	synchronized public int getX()
	{
		return agPos.x ;
	}
	
	/**
	 * get the agent's X position
	 * 
	 * @return the current Y
	 */
	synchronized public int getY()
	{
		return agPos.y ;
	}
	
	/**
	 * get the agent's color
	 * 
	 * @return the current Color
	 */
	synchronized public Color getColor()
	{
		return agColor ;
	}
	
	/**
	 * get the agent's obstacles
	 * 
	 * @return agent's obstacles
	 */
	synchronized public int getObstacles()
	{
		return agObstacles ;
	}
	
	/**
	 * set the agent's Path
	 * 
	 * @param new Path
	 * @return true if the new path has been set
	 */
	synchronized public boolean setPath(Path p)
	{
		agPath = p ;
		agPathIndex = 0 ;
		return true ;
	}

	/**
	 * get the agent's current Path
	 * 
	 * @return the current Path
	 */
	synchronized public Path getPath()
	{
		return agPath ;
	}

	/**
	 * get the agent's current step X position in path
	 * 
	 * @return the current step position X
	 */
	synchronized public int getStepX()
	{
		return agPath.getX(agPathIndex) ;
	}
	
	/**
	 * get the agent's current step Y position in path
	 * 
	 * @return the current step position X
	 */
	synchronized public int getStepY()
	{
		return agPath.getY(agPathIndex) ;
	}
	
	/**
	 * check the agent reached the final destination of the current path
	 * 
	 * @return true if arrived (index reached destination), else false
	 */
	synchronized public boolean isArrived()
	{
		return (agPathIndex == agPath.getLength()-1) ;
	}

	/**
	 * go to next step in path
	 * 
	 * @return true next step possible else false if arrived
	 */
	synchronized public boolean nextStep()
	{
		if (isArrived() == true)
		{
			return false ;
		}
		agPathIndex++ ;
		return true ;
	}

	/**
	 * get the current step index
	 * 
	 * @return path current index
	 */
	synchronized public int getIndex()
	{
		return agPathIndex ;
	}

	
	/**
	 * Clear path data
	 * 
	 */
	synchronized public void clearPath()
	{
		agPath = null ;
		agPathIndex = 0 ;
	}
	
	/**
	 * Clear destination
	 * 
	 * @return true if destination is cleared
	 */
	synchronized public boolean clearDestination()
	{
		agDestination = null ;
		return true ;
	}
	
	/**
	 * set the agent's destination
	 * 
	 * @param Location l : new destination
	 * @return true if a new destination is available
	 */
	synchronized public boolean setDestination(Location newDestination)
	{
		agDestination = newDestination ;
		return true ;
	}

	/**
	 * get the agent's current destination
	 * 
	 * @return the agent's destination
	 */
	synchronized public Location getDestination()
	{
		if ( agDestination == null )
		{
			return null ;
		}
		return (Location)agDestination.clone();
	}	

	/**
	 * set blocked status of the agent
	 * 
	 * @param blocked : blocking status
	 */
	synchronized public void setBlocked(boolean blocked)
	{
		agBlocked = blocked ;
	}
	
	/**
	 * set blcoked status of the agent
	 * 
	 * @return true if blocked, else false
	 */
	synchronized public boolean isBlocked()
	{
		return agBlocked ;
	}
	
	
}
