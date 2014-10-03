// Environment code for project iDM.mas2j

import java.util.logging.*;
import java.util.Random;

import jason.asSyntax.*;
import jason.environment.*;
import jason.environment.grid.Location;

public class iDM extends Environment 
{
    private	Logger				logger = Logger.getLogger("iDM.mas2j."+iDM.class.getName());
	
	private static int			sleepTime	= 0 ;
	
	private iDMIsland			island ;
	private iDMView				view ;
	private aStarPathfinder		pathFinder ;

    protected	Random			random 		= new Random();
	
    /** Called before the MAS execution with the args informed in .mas2j */

    @Override
    public void init(String[] args) 
	{
    	try 
		{
			// create a world model (island)
			island = new iDMIsland(this, args[0], Integer.parseInt(args[1]), Integer.parseInt(args[2])) ;
			
			// create a view with an instance of the world model
			view = new iDMView(island, "The Island of Doctor Moreau", 
				Integer.parseInt(args[3]), 
				Integer.parseInt(args[4]), 
				args[5].equals("yes"), 
				args[6].equals("yes"),
				args[7].equals("yes")
				);

			// init sleep time
			sleepTime = Integer.parseInt(args[8]) ;
			
			// init pathfinder
			int width = island.getWidth() ;
			int height = island.getHeight() ;
			pathFinder = new aStarPathfinder(width, height);
			for (int i = 0; i < width; i++) 
			{
				for (int j = 0; j < height; j++) 
				{
					pathFinder.aStarSetType(i,j,island.getData(i,j));
				}
			}

			// Create percepts
            clearPercepts();
            updateAgsPercept();        
            informAgsEnvironmentChanged();
    	}
		catch (Exception e) 
		{
    		logger.warning("Error creating world "+e);
    	}
    }

    @Override
    public boolean executeAction(String agName, Structure action) 
	{
        boolean result = false;
		
        try 
		{
			if (sleepTime>0)
			{
				Thread.sleep(sleepTime);
			}

			try
			{
				Agent ag = island.getAgBasedOnName(agName);
	
				// rule checking if the agent has been killed, exit (and flush queue of actions)
//				if (getEnvironmentInfraTier().getRuntimeServices().getAgentsNames().contains(ag.getName()) == false) 
				if(ag.isAlive() == false || ag.isBlocked() == true)
				{
					return true ;
				}
				
				// get current location
				Location l = ag.getPos() ;

				// RANDOM_MOVE action processing
				if (action.getFunctor().equals("random_move"))
				{
					int dx = 0 ;
					int dy = 0 ;
					switch(random.nextInt(10))
					{
						case 0: dx = -1	; dy = -1 ;	break ;
						case 1: dx = 0	; dy = -1 ;	break ;
						case 2: dx = +1	; dy = -1 ;	break ;
						case 3: dx = -1	; dy = 0 ;	break ;
						case 4: dx = +1	; dy = 0 ;	break ;
						case 5: dx = -1	; dy = +1 ;	break ;
						case 6: dx = 0	; dy = +1 ;	break ;
						case 7: dx = +1	; dy = +1 ;	break ;
						default: dx = 0	; dy = 0 ;
							int agLife = ag.getLife() ;
							ag.setLife(agLife < 100 ? agLife + random.nextInt(3) : agLife);
							break ;
					}
					// check validity of position
					if ( dx!=0 && dy!=0 &&
							island.isMap(l.x+dx,l.y+dy,ag.getObstacles()) == false &&
							/** optional */ island.isAgent(ag,l.x+dx,l.y+dy) == false)
					{
						result = ag.setPos(l.x + dx, l.y + dy);
						//logger.warning(agName + " at position (" + ag.getX() + "," + ag.getY() + ")");
					}
					else
					{
						// else do not move (always true)
						result = true ;
					}
				}
				// FIND_PATH action processing
				else if (action.getFunctor().equals("find_path"))
				{
					synchronized(this)
					{
						// select a new destination
						Location newDestination ;
						int x = (int)((NumberTerm)action.getTerm(0)).solve();
						int y = (int)((NumberTerm)action.getTerm(1)).solve();
						if (x == -1 && y == -1)
						{
							// destination unknowned, find one
							newDestination = island.findInMap(ag.getX(), ag.getY(), ~ag.getObstacles()) ;
						}
						else
						{
							// destination given by agent
							newDestination = new Location(x,y) ;
						}
						// build a new path to destination
//					long t0 = System.currentTimeMillis() ;
						Path path = pathFinder.aStarFindPath(l.x, l.y, newDestination.x, newDestination.y, ag.getObstacles());
//					long t1 = System.currentTimeMillis() ;
//					logger.warning(agName + " found a path in " + (t1 - t0) + " ms.");
						ag.setPath(path);
						
//						result = (path != null) & ag.setDestination(newDestination) ;
						if (path != null)
						{
							result = ag.setDestination(newDestination);
						}
						else
						{
							// 31 March 09:
							// we failed to find a path : this could happen when the agent moves to the target location
							// this move seems unavoidable and due to actions still exist from queue when intentions are dropped 
							// Jomi's todo list for next version.
							logger.warning("***************************************************************************************************");
							logger.warning(agName + " error when trying to find a path  from (" + l.x + "," + l.y + ") to (" + x + "," + y + ")");
							logger.warning("***************************************************************************************************");
							// undo 
							result = ag.clearDestination();
						}
					}
				}
				// MOVE_ON_PATH action processing
				else if (action.getFunctor().equals("move_on_path"))
				{
					// move to current step
					// logger.warning(agName + " performing step " + ag.getIndex() + " on " + (ag.getPath().getLength()-1));
					result = ag.setPos(ag.getStepX(), ag.getStepY()) ;
					// perform next step, if arrived to destination then clear it
					if (ag.nextStep() == false)
					{
						ag.clearDestination();
					}
				}
				// ATTACK action processing
				else if (action.getFunctor().equals("attack"))
				{
					// attack succeeds or fails depending on congruing positions
					String victimName = action.getTerm(0).toString() ;
					Agent victim = island.getAgBasedOnName(victimName) ;
					result = ag.getPos().equals(victim.getPos()) ;
					// block the victim: this a mecanism to lock the asynchronous nature
					// of this interaction between a prey and its predator.
					// Before to be killed, preys want to flee
					// but testing life is not the best because life will be transfered 
					// later to the predator ("eat" action). Then blocking (locking) the prey
					// ensure the predator to keep its prey until it can kill it.
					// Note: a "block" action will not solve the problem while a "move"
					// is stills possible between "attack" and "move". The case for 
					// granularitry of actions is here: "attack" must be mixed with "block"
					// to ensure the synchronicity of this interaction. However if we want
					// to go a bit further we could imagine a "block" action where we 
					// calculate if the prey can escape from its predator... and ven split "block"
					// from "attack". Granularity of actions is key here and depends what we want 
					// to achieve.
					victim.setBlocked(result);
				}
				// EAT action processing
				else if (action.getFunctor().equals("eat"))
				{
					String foodName = action.getTerm(0).toString() ;
					if (foodName.equals("food") == true)
					{
						// food is not an agent
						result = ag.setLife(100);
					}
					else
					{
						// food is an agent
						Agent victim = island.getAgBasedOnName(foodName);
						int victimLife = victim.getLife();
						victim.setLife(0) ;
						// add to biotope deadpool for late rebirth
						island.Biotope.addTodeadPool(victim) ;
						// and update map around victim's position
						view.updateAround(victim.getX(),victim.getY());
						// feed killer
						result = ag.setLife(victimLife);
					}
				}
				// WATCH action processing
				else if (action.getFunctor().equals("watch"))
				{
					result = true ;
				}
				// FLEE action processing
				else if (action.getFunctor().equals("flee"))
				{
					// build target to follow
					Location target = new Location(
						(int)((NumberTerm)action.getTerm(0)).solve(),
						(int)((NumberTerm)action.getTerm(1)).solve()
					);
					// find a distant position
					Location distantPosition = (Location)l.clone();
					float dmax = (float)distantPosition.distanceEuclidean(target);
					for (int i = l.x - 1; i < l.x + 2; i++) 
					{
						for (int j = l.y - 1; j < l.y + 2; j++) 
						{
							Location position = new Location(i,j);
							if (position.equals(l) == true)
							{
								continue;
							}
							float d = (float)position.distanceEuclidean(target);
							if (d > dmax &&	island.isMap(i, j, ag.getObstacles()) == false)
							{
								dmax = d;
								distantPosition = position;
							}
						}
					}
					result = ag.setPos(distantPosition.x, distantPosition.y);
				}
				// FOLLOW action processing
				else if (action.getFunctor().equals("follow"))
				{
					// build target to follow
					Location target = new Location(
						(int)((NumberTerm)action.getTerm(0)).solve(),
						(int)((NumberTerm)action.getTerm(1)).solve()
					);
					// find a closer position
					Location closestPosition = (Location)l.clone();
					float dmin = (float)closestPosition.distanceEuclidean(target);
					for (int i = l.x - 1; i < l.x + 2; i++) 
					{
						for (int j = l.y - 1; j < l.y + 2; j++) 
						{
							Location position = new Location(i,j);
							if (position.equals(l) == true)
							{
								continue;
							}
							float d = (float)position.distanceEuclidean(target);
							if (d < dmin &&	island.isMap(i, j, ag.getObstacles()) == false)
							{
								dmin = d;
								closestPosition = position;
							}
						}
					}
					result = ag.setPos(closestPosition.x, closestPosition.y);
				}
				// 
				else 
				{
					logger.info("executing: " + action + ", but not implemented!");
				}
				
				if (result) 
				{
					// update beliefs
					updateAgPercept(ag);
					// update view
					view.updateAround(l.x,l.y);
					view.repaint() ;
					return true;
				}
			}
			catch (NullPointerException e)
			{
				logger.log(Level.SEVERE, agName + ": error null pointer! ", e);
			}
        } 
		catch (InterruptedException e) 
		{
        } 
		catch (Exception e) 
		{
            logger.log(Level.SEVERE, "error executing " + action + " for " + agName, e);
        }
		
		return false ;
    }
	
	public boolean createJasonAgent(Agent ag)
	{
		try
		{
			getEnvironmentInfraTier().getRuntimeServices().createAgent
			(
				ag.getName(),  	   							// agent name
				ag.getName().replaceAll("\\d","") + ".asl", // AgentSpeak source (with removed digits)
				null,            							// default agent class
				null,            							// default architecture class
				null,            							// default belief base parameters
				null										// default setting
			);
			
			// update percepts
			updateAgPercept(ag);
			return true ;
		}
		catch(Exception e)
		{
    		logger.log(Level.SEVERE, "Error creating agent " + ag.getName() + e);
		}
		return false ;
	}
	
    private void updateAgsPercept() 
	{
        for (int i = 0; i < island.getNbOfAgs(); i++) 
		{
            updateAgPercept(island.getAg(i));
        }
    }

	private void updateAgPercept(Agent ag) 	
	{
		String agName = ag.getName() ;
		
		clearPercepts(agName);
		
		// exit for preys
		if (agName.contains("rabbit"))
		{
			return ;
		}
		
		// inform agent's current state
		addPercept(agName, Literal.parseLiteral("life(" + ag.getLife() + ")"));

		// inform agent's position
		addPercept(agName, Literal.parseLiteral("at(" + ag.getX() + "," + ag.getY() + ")"));
		
		// perceive other agents around
		Location l = ag.getPos();
		Agent closer = null ;
		for (int i = 0 ; i < island.getNbOfAgs() ; i++)
		{
			Agent agent = island.getAg(i) ;
			int distance = (int)l.distanceEuclidean(agent.getPos());
			if (agent != ag && agent.isAlive() == true && distance < ag.getRadius())
			{
				addPercept(agName, Literal.parseLiteral("other(" + agent.getName() + "," + agent.getX() + "," + agent.getY() + "," + distance + ")"));
			}
		}
		
		// perceive humans
		Agent agent = island.getPlayerAg() ;
		int distance = (int)l.distanceEuclidean(agent.getPos());
		if (agent != ag && agent.isAlive() == true && distance < ag.getRadius())
		{
			addPercept(agName, Literal.parseLiteral("human(" + agent.getName() + "," + agent.getX() + "," + agent.getY() + "," + distance + ")"));
		}

		// specific to man-beast
		
		// inform agent's where to find some food
		addPercept(agName, Literal.parseLiteral("food(" + island.Food.x + "," + island.Food.y + ")"));
		
		// if a new destination has been calculated, create the belief
		if ( ag.getDestination() != null )
		{
			l = ag.getDestination() ;
			addPercept(agName, Literal.parseLiteral("goingto(" + l.x + "," + l.y + ")"));
		}
    }
	
    /** Called before the end of MAS execution */

    @Override
    public void stop() 
	{
        super.stop();
    }

}


