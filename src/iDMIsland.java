import java.util.logging.*;
import java.util.Random;
import java.util.ArrayList;
import java.io.*;
import java.awt.image.BufferedImage;
import java.awt.Color;
import javax.imageio.ImageIO ;
import jason.environment.grid.Location;

public class iDMIsland
{
    private Logger logger = Logger.getLogger("iDM.mas2j."+iDMIsland.class.getName());
	
	private	iDM					environment = null;

    protected int				width 		= 0;
    protected int				height 		= 0;
    protected int[][]			map 		= null; 
	
	/** logical exclusive tags for the map */
    public static final int		SEA		= 1;
    public static final int		GROUND	= 2;
    public static final int		BEACH	= 4;
    public static final int		FOREST	= 8;
    public static final int		TREE	= 16;
    public static final int		VILLAGE	= 32;
    public static final int		HOUSE	= 64;
    public static final int		BRIDGE	= 128;
    public static final int		WALL	= 256;

	/** predefined colors associated with tags for the map */
    public static final int		rgbSEA		= 0xFF0000FF;
    public static final int		rgbGROUND	= 0xFF00FF00;
    public static final int		rgbBEACH	= 0xFFFFFF00;
    public static final int		rgbFOREST	= 0xFF008000;
    public static final int		rgbTREE		= 0xFF004000;
    public static final int		rgbVILLAGE	= 0xFF804040;
    public static final int		rgbHOUSE	= 0xFF808080;
    public static final int		rgbBRIDGE	= 0xFF800000;
    public static final int		rgbWALL		= 0xFF000000;
	
	/** biotope */
	protected iDMBiotope		Biotope			= new iDMBiotope(1000);	// period = 1000 ms 
    public static int			bioRandomRange	= 100;

	/** facilities */	
	public	Location			House		= new Location(0,0);
	public	Location			Village		= new Location(0,0);
	public	Location			Food		= new Location(0,0);

	/** agents */	
	private		Agent			player 		= null;
	private		ArrayList		agents 		= new ArrayList();
	
    protected	Random			random 		= new Random();
	private		BufferedImage 	img;
	
	/***************************************************************************
		World (Island) construction
	***************************************************************************/
    protected iDMIsland(iDM env, String filename, int nbAgs, int rate) 
	{
		environment = env;
		try 
		{
			img = ImageIO.read(new File(filename));
			
			width  = img.getWidth();
			height = img.getHeight();
		
			// build map
			map = new int[width][height];
			
			int HouseCount = 0 ;
			int VillageCount = 0 ;
			
			for (int i = 0; i < width; i++) 
			{
				for (int j = 0; j < height; j++) 
				{
					switch( img.getRGB(i,j) )
					{
						case rgbSEA:
							map[i][j] = SEA;
							break;
							
						case rgbGROUND:
							map[i][j] = GROUND;
							break;
							
						case rgbBEACH:
							map[i][j] = BEACH;
							break;
							
						case rgbFOREST:
							map[i][j] = FOREST;
							break;
							
						case rgbVILLAGE:
							map[i][j] = VILLAGE;
							Village.x += i ;
							Village.y += j ;
							VillageCount++ ;
							break;
							
						case rgbHOUSE:
							map[i][j] = HOUSE;
							House.x += i ;
							House.y += j ;
							HouseCount++ ;
							break;
							
						case rgbBRIDGE:
							map[i][j] = BRIDGE;
							break;
							
						case rgbWALL:
							map[i][j] = WALL;
							break;
							
						case rgbTREE:
							map[i][j] = TREE;
							break;
					}
				}
			}
			
			if ( HouseCount>0 )
			{
				House.x /= HouseCount;
				House.y /= HouseCount;
			}
			
			if ( VillageCount>0 )
			{
				Village.x /= VillageCount;
				Village.y /= VillageCount;
			}
			
			// place food in village
			Food = findInMap(VILLAGE);
			
			// Init Moreau's pos (player) in HOUSE
			// TODO: check HOUSE exists in map else select GROUND
			player = new Agent("moreau", new Location(House.x, House.y), Color.black, 0, 0);
			
			// create nbAgs rabbits ; only in FOREST
			// TODO: check FOREST exists in map else select GROUND
			for (int i = 0; i < nbAgs; i++) 
			{
				agents.add( new Agent("rabbit" + Integer.toString(i+1), findInMap(FOREST), Color.white, ~FOREST, 5));
			}
		
			// create the mandog
			// TODO: check VILLAGE exists in map else select GROUND
			agents.add( new Agent("mandog", findInMap(VILLAGE), Color.blue, SEA | TREE | FOREST | WALL, 10));
			nbAgs++;

			// create the hyenapig
			// TODO: check VILLAGE exists in map else select GROUND
			agents.add( new Agent("hyenapig", findInMap(GROUND), Color.yellow, SEA | TREE | WALL | HOUSE, 10));
			nbAgs++;

			// create the leopardman
			// TODO: check VILLAGE exists in map else select GROUND
			agents.add( new Agent("leopardman", findInMap(FOREST), Color.red, SEA | TREE | WALL | HOUSE, 10));
			nbAgs++;

			// start Biotope (thread)
			bioRandomRange = (rate > 0 ? rate : 1);
			Biotope.start();

		}
		catch (IOException e) 
		{
    		logger.warning("Error: " + e.getMessage() 
							+ " unable to load \"" + filename 
							+ "\"" );
		}
    }

	/***************************************************************************
		World (Island) methods
	***************************************************************************/

    public BufferedImage getMap() 
	{
        return img;
    }
	
    public int getWidth() 
	{
        return width;
    }

    public int getHeight() 
	{
        return height;
    }
	
	public int getData(int x, int y)
	{
		return map[x][y] ;
	}

	// excluding x,y
	synchronized public	Location findInMap(int x, int y, int type)
	{
		Location l ;
		do
		{
			l = findInMap(type);
		} while ( l.x == x && l.y == y) ;
		return l ;
	}

	synchronized public	Location findInMap(int type)
	{
		int x, y ;
		do
		{
			x = random.nextInt(width);
			y = random.nextInt(height);
		} while( isMap(x,y,type) == false ) ;
		return new Location(x,y) ;
	}
	
	public boolean isMap(int x, int y, int c)
	{
		return (map[x][y] & c) != 0 ;
	}
	
    public void setMap(int type, int x, int y) 
	{
        map[x][y] = type;
    }
    
    public void addToMap(int type, int x, int y) 
	{
        map[x][y] |= type;
    }

    public void removeFromMap(int type, int x, int y) 
	{
        map[x][y] &= ~type;
    }

	/***************************************************************************
		Agents methods
	***************************************************************************/

    public String getPlayerName() 
	{
		return player.getName();
    }
	
    public boolean setPlayerPos(Location l) 
	{
		return player.setPos(l);
    }

    public boolean setPlayerPos(int x, int y) 
	{
		return player.setPos(x,y);
    }

    public Location getPlayerPos() 
	{
		return player.getPos() ;
    }

    public Color getPlayerColor() 
	{
		return player.getColor();
    }
	
	public Agent getPlayerAg() 
	{
		return (Agent)player ;
    }

	public int getNbOfAgs() 
	{
		return agents.size();
    }

	public Agent getAg(int agId) 
	{
		return (Agent)agents.get(agId) ;
    }

    synchronized public boolean removeAg(Agent ag) 
	{
		for (int i = 0; i < agents.size(); i++) 
		{
			if (agents.get(i) == ag)
			{
				agents.remove(i) ;
				return true ;
			}
		}
		return false ;
	}
	
    synchronized public Agent getAgBasedOnName(String agNamed) 
	{
		for (int i = 0; i < agents.size(); i++) 
		{
			if ( agNamed.compareToIgnoreCase(((Agent)agents.get(i)).getName()) ==0)
			{
				return (Agent)agents.get(i) ;
			}
		}
		return null ;
    }
	
	synchronized public boolean isAgent(Agent ag, int x, int y)
	{
		// check if player is at x,y
		if ( player.getX() == x && player.getY() == y )
		{
			return true ;
		}
		
		// if not, check if an agent is at x,y
		for (int i = 0; i < agents.size(); i++) 
		{
			Agent agent = (Agent)agents.get(i) ;
			if ( agent != ag && agent.getX() == x && agent.getY() == y )
			{
				return true ;
			}
		}
		
		// if not, free
		return false ;
	}
	
	/***************************************************************************
		Thread Class Biotope
	***************************************************************************/
	class iDMBiotope extends Thread
	{
		private long 		period		= 0 ;
		private	ArrayList	deadPool 	= new ArrayList();
		
		public iDMBiotope(long period)
		{
			this.period = period ;
		}

		public boolean addTodeadPool(Agent ag)
		{
			deadPool.add(ag);
			return true ;
		}
		
		public void run()
		{
			try 
			{
				while ( true )
				{
					// decrease life of agents
					for(int i = 0 ; i < getNbOfAgs() ; i++)
					{
						getAg(i).decreaseLife() ;
					}
					
					// randomly bring back to life dead agents
					if (deadPool.isEmpty() == false && random.nextInt(bioRandomRange) == 0 )
					{
						// bring back to life (FIFO) one from the deadpool
						Agent ag = (Agent)deadPool.get(0);
						deadPool.remove(0);
						ag.setLife(100);
						ag.setPos(findInMap(FOREST));
						ag.setBlocked(false);
						environment.createJasonAgent(ag);
			    		logger.warning(" Biotope created " + ag.getName() + "!!!");
					}

					// sleep
					if (period > 0)
					{
						sleep(period);
					}
				}
			}
			catch (InterruptedException e)
			{
			}
		}
	}
}
