import java.awt.Graphics;
import java.awt.Image;
import java.awt.image.*;
import java.awt.event.*;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;

import javax.swing.JFrame;
import javax.swing.Timer;
import javax.swing.border.*;

import java.util.logging.*;
import java.util.Random;
import java.util.ArrayList;
import java.util.regex.*;
import java.util.Date;

import java.text.SimpleDateFormat;

import jason.environment.grid.Location;

/**
 * View component for iDM. 
 * 
 * @author rv
 */
public class iDMView extends JFrame
{
	private iDM			env = null;
	private Timer		timer;
	private long		startTime = 0 ;
	
    private Logger		logger = Logger.getLogger("iDM.mas2j."+iDMView.class.getName());

    protected 			iDMIsland	island;						/** world model */
	
	protected static	int			windowSizeW = 0 ;
	protected static 	int			windowSizeH = 0 ;
    protected static 	int 		cellSizeW = 0;
    protected static 	int 		cellSizeH = 0;
	protected static 	boolean 	redrawAll = true ;
	protected static 	boolean		seaAnimated = true ;
	protected static	boolean 	flashText = true ;
	protected static 	boolean		drawPaths	= true ;
	protected static 	int			drawGrid	= 1 ;			/** 1 = draw a grid or 0 = not ; not a boolean because used as an offset */
	protected static 	boolean		drawDashboard	= false ;
	protected static 	int			sizeDashboard	= 160 ;

	private		 		Image 		buffer;						/** buffer for drawing */
	private		 		ArrayList	dirtyMap = new ArrayList();	/** list of dirty element of the map to redraw */
	private				ArrayList	seaCells = new ArrayList();

    protected  			Random		random = new Random();
	protected 			Font		defaultFont = new Font("Time New Roman", Font.BOLD, 12);
	protected 			Font		simutimeFont = new Font("Arial", Font.BOLD, 12);

	protected static final	Color 	SeaColor = new Color(50,50,200) ;
	protected static final	Color 	GroundColor = new Color(50,200,50) ;
 	protected static final	Color 	BeachColor = new Color(200,200,50) ;
 	protected static final	Color 	HouseColor = new Color(100,100,100) ;
 	protected static final	Color 	ForestColor = new Color(50,180,50) ;
 	protected static final	Color 	TreeColor = new Color(50,100,50) ;
 	protected static final	Color 	VillageColor = new Color(150,100,100) ;
 	protected static final	Color 	BridgeColor = new Color(100,50,50) ;
 	protected static final	Color 	WallColor = new Color(0,0,0) ;
	protected static final	Color 	GridColor = new Color(100,100,200) ;

    public iDMView(iDMIsland world, String title, int windowWidth, int windowHeight, boolean animation, boolean grid, boolean displayInfos) 
	{
        super(title);
        this.island = world;
        initComponents(windowWidth, windowHeight);

		seaAnimated = animation ;
		drawGrid = (grid == true ? 1 : 0);
		drawPaths = displayInfos ;

		// create a list to update and anim the sea if required
		if (seaAnimated == true)
		{
			for (int x = 0; x < island.getWidth(); x++) 
			{
				for (int y = 0; y < island.getHeight(); y++) 
				{
					if (island.isMap(x,y,island.SEA))
					{
						seaCells.add(new Cell(x,y));
					}
				}
			}
		}
		
		// draw the full map when starting
		updateAll() ;
		
		if ( timer == null )
		{
			timer = new Timer(200, timerListener);
			timer.start();
		}
		
		startTime = System.currentTimeMillis() ;
		repaint();
		setVisible(true);
    }

    /** sets the size of the frame and adds the components */
    public void initComponents(int width, int height) 
	{
        setSize(width, height);
		setResizable(false);
		windowSizeW = width ;
		windowSizeH = height ;
		cellSizeW = windowSizeW / island.getWidth();
		cellSizeH = windowSizeH / island.getHeight();
	
		// Add keyboard event listener to Canvas
		addKeyListener(new KeyAdapter()
		{
			public void keyPressed(KeyEvent e)
			{
				boolean redraw = false ;
				
				Location l = island.getPlayerPos() ;
				
				switch( e.getKeyCode() )
				{
					case KeyEvent.VK_UP:
						redraw = movePlayer(l,0,-1) ;
						break;
						
					case KeyEvent.VK_DOWN:
						redraw = movePlayer(l,0,+1) ;
						break;
						
					case KeyEvent.VK_LEFT:
						redraw = movePlayer(l,-1,0) ;
						break;
						
					case KeyEvent.VK_RIGHT:
						redraw = movePlayer(l,+1,0) ;
						break;
						
					case KeyEvent.VK_SPACE:
						drawDashboard = !drawDashboard ;
						int offset = (drawDashboard == true ? sizeDashboard : -sizeDashboard) ;
						windowSizeW = getSize().width;
						windowSizeH = getSize().height;
						setSize(windowSizeW + offset, windowSizeH) ;
						updateAll();
						break;
				}
				
				if ( redraw == true )
				{
					repaint() ;
				}
			}
		});

/*
///		Border border = getBorder();
		
		addMouseListener(new MouseListener() {
			public void mouseClicked(MouseEvent e)
			{
//				logger.warning("mouseClicked");
			}
			public void mouseExited(MouseEvent e) {}
			public void mouseEntered(MouseEvent e) {}
			public void mousePressed(MouseEvent e) 
			{
//				logger.warning("mousePressed");
			}
			public void mouseReleased(MouseEvent e) {}
        });
		
		addMouseMotionListener(new MouseMotionListener() {
			public void mouseDragged(MouseEvent e) 
			{
//				logger.warning("mouseDragged");
			}
			public void mouseMoved(MouseEvent e)
			{
//				logger.warning("mouseMoved");
			}
        });
*/
		// resize handler
		addComponentListener(new ComponentAdapter() 
		{
			public void componentResized(ComponentEvent event) 
			{
				updateAll();
			}
		});		
		
		// Set focus	
		setFocusable(true) ;
	
    }

	ActionListener timerListener = new ActionListener() 
	{
		public void actionPerformed(ActionEvent e) 
		{
			// timer management
			flashText = !flashText ;
			repaint() ;
		}
	};
 
	public boolean movePlayer(Location l, int dx, int dy)
	{
		// check validity of new location in the map
		int x = l.x + dx ;
		int y = l.y + dy ;
		if ( island.isMap(x,y,island.SEA) == false
				&& island.isMap(x,y,island.TREE) == false
				&& island.isMap(x,y,island.WALL) == false
// TODO				&& x>=0 && x<island.getWidth() 
// TODO				&& y>=0 && y<island.getHeight() 
			)
		{
			// allowed, move the agent to new location
			updateAround(l.x,l.y) ;
			return island.setPlayerPos(x,y) ;
		}
		// not allowed, return 
		return false ;
	}
	
	synchronized public void update(int x, int y)
	{
		dirtyMap.add(new Cell(x,y));
	}
	
	synchronized public void updateAround(int x, int y)
	{
		dirtyMap.add(new Cell(x-1,y-1));
		dirtyMap.add(new Cell(x,y-1));
		dirtyMap.add(new Cell(x+1,y-1));
		dirtyMap.add(new Cell(x-1,y));
		dirtyMap.add(new Cell(x,y));
		dirtyMap.add(new Cell(x+1,y));
		dirtyMap.add(new Cell(x-1,y+1));
		dirtyMap.add(new Cell(x,y+1));
		dirtyMap.add(new Cell(x+1,y+1));
	}

	synchronized public void updateAll()
	{
		redrawAll = true ;
	}

	/** agents drawing methods **/

	public void drawAgents(Graphics g)
	{
		// display agents
		for (int i = 0; i < island.getNbOfAgs(); i++) 
		{
			drawAgent(g, island.getAg(i)) ;
		}
		
		// display Player
		drawPlayer(g, flashText) ;
	}

	Pattern digitsInString = Pattern.compile("(\\d+)");

    public void drawAgent(Graphics g, Agent ag) 
	{
		if ( ag.isAlive() == true )
		{
			// display a colored shape
			g.setColor(ag.getColor());
			g.fillOval(ag.getX() * cellSizeW - 3, ag.getY() * cellSizeH - 3, cellSizeW + 6, cellSizeH + 6);
			g.setColor(Color.black);
			
			// display name
			// by default, display first letter to uppercase
			String agName = ag.getName().substring(0,1).toUpperCase() ;
			// search for numbers and display them when found
			Matcher match = digitsInString.matcher(ag.getName());
			if (match.find()) 
			{
				agName = match.group(1) ;
			}
			// display found name
			drawString(g, ag.getX(), ag.getY(), defaultFont, agName );
		}
		else
		{
			// dead! draw a bloody shape
			g.setColor(Color.red);
			g.fillOval(ag.getX() * cellSizeW + 1, ag.getY() * cellSizeH + 1, cellSizeW - 1, cellSizeH - 1);
			g.setColor(Color.black);
			drawString(g, ag.getX(), ag.getY(), defaultFont, "†");
		}
    }
	
	public void drawPlayer(Graphics g, boolean text)
	{
		// display the player
        g.setColor(island.getPlayerColor());
        g.fillOval(island.getPlayerPos().x * cellSizeW - 3, island.getPlayerPos().y * cellSizeH - 3, cellSizeW + 6, cellSizeH + 6);
		if ( text == true )
		{
			g.setColor(Color.white);
			drawString(g, island.getPlayerPos().x, island.getPlayerPos().y, defaultFont, island.getPlayerName().substring(0,1).toUpperCase());
		}
	}

    public void drawString(Graphics g, int x, int y, Font f, String s) 
	{
        g.setFont(f);
        FontMetrics metrics = g.getFontMetrics();
        int width = metrics.stringWidth( s );
        int height = metrics.getHeight();
        g.drawString( s, x*cellSizeW+(cellSizeW/2-width/2)+1, y*cellSizeH+(cellSizeH/2+height/2) - 2);
    }

	private void drawPaths(Graphics g)
	{
		if ( drawPaths == true )
		{
			for (int i = 0; i < island.getNbOfAgs(); i++) 
			{
				drawPath(g, island.getAg(i)) ;
			}
		}
	}
	
	private void drawPath(Graphics g, Agent ag)
	{
		Path path = ag.getPath();
		if ( path != null && drawPaths == true )
		{
			// draw path
			g.setColor(ag.getColor());
			int L = path.getLength() - 1;
			for (int i=0 ; i<L; i++)
			{
				g.drawLine(	path.getX(i) * cellSizeW + cellSizeW/2, 
							path.getY(i) * cellSizeH + cellSizeH/2,
							path.getX(i+1) * cellSizeW + cellSizeW/2, 
							path.getY(i+1) * cellSizeH + cellSizeH/2);
								
				update(path.getX(i), path.getY(i));
			}
			update(path.getX(L), path.getY(L));
		}
	}
	
	/** map drawing methods **/
	
    private void drawCell(Graphics g, int x, int y) 
	{
		if ( (island.map[x][y] & iDMIsland.SEA) != 0)
		{
			g.setColor(seaAnimated == true ? new Color(50,50,190+random.nextInt(20)) : SeaColor);
			g.fillRect(x * cellSizeW + drawGrid, y * cellSizeH + drawGrid, cellSizeW - drawGrid, cellSizeH - drawGrid);
		}
		else if ( (island.map[x][y] & iDMIsland.GROUND) != 0) 
		{
			g.setColor(GroundColor);
			g.fillRect(x * cellSizeW + drawGrid, y * cellSizeH + drawGrid, cellSizeW - drawGrid, cellSizeH - drawGrid);
		}
		else if ( (island.map[x][y] & iDMIsland.BEACH) != 0) 
		{
			g.setColor(BeachColor);
			g.fillRect(x * cellSizeW + drawGrid, y * cellSizeH + drawGrid, cellSizeW - drawGrid, cellSizeH - drawGrid);
		}
		else if ( (island.map[x][y] & iDMIsland.FOREST) != 0) 
		{
			g.setColor(ForestColor);
			g.fillRect(x * cellSizeW + drawGrid, y * cellSizeH + drawGrid, cellSizeW - drawGrid, cellSizeH - drawGrid);
		}
		else if ( (island.map[x][y] & iDMIsland.TREE) != 0) 
		{
			// always on top of a forest region
			g.setColor(ForestColor);
			g.fillRect(x * cellSizeW + drawGrid, y * cellSizeH + drawGrid, cellSizeW - drawGrid, cellSizeH - drawGrid);
			// draw a tree
			g.setColor(TreeColor);
			g.fillOval(x * cellSizeW + drawGrid*2, y * cellSizeH + drawGrid*2, cellSizeW - drawGrid*4, cellSizeH - drawGrid*4);
		}
		else if ( (island.map[x][y] & iDMIsland.VILLAGE) != 0) 
		{
			g.setColor(VillageColor);
			g.fillRect(x * cellSizeW + drawGrid, y * cellSizeH + drawGrid, cellSizeW - drawGrid, cellSizeH - drawGrid);
		}
		else if ( (island.map[x][y] & iDMIsland.HOUSE) != 0) 
		{
			g.setColor(HouseColor);
			g.fillRect(x * cellSizeW + drawGrid, y * cellSizeH + drawGrid, cellSizeW - drawGrid, cellSizeH - drawGrid);
		}
		else if ( (island.map[x][y] & iDMIsland.BRIDGE) != 0) 
		{
			g.setColor(BridgeColor);
			g.fillRect(x * cellSizeW + drawGrid, y * cellSizeH + drawGrid, cellSizeW - drawGrid, cellSizeH - drawGrid);
		}
		else if ( (island.map[x][y] & iDMIsland.WALL) != 0) 
		{
			g.setColor(WallColor);
			g.fillRect(x * cellSizeW + drawGrid, y * cellSizeH + drawGrid, cellSizeW - drawGrid, cellSizeH - drawGrid);
		}
    }

	private void drawMapInfos(Graphics g)
	{
		String string;
		
		g.setFont(defaultFont);
		FontMetrics metrics = g.getFontMetrics();
			
		// House of Pain
		g.setColor(Color.white) ;
		string = "House of pain"; 
		g.drawString( string, 
			island.House.x*cellSizeW+(cellSizeW/2-metrics.stringWidth( string )/2), 
			island.House.y*cellSizeH+(cellSizeH/2+metrics.getHeight()/2));
		
		// Village of monsters
		string = "Village"; 
		g.drawString( string, 
			island.Village.x*cellSizeW+(cellSizeW/2-metrics.stringWidth( string )/2),
			island.Village.y*cellSizeH+(cellSizeH/2+metrics.getHeight()/2));
			
		// food
		g.setColor(Color.green) ;
		string = "food"; 
		g.drawString( string, 
			island.Food.x*cellSizeW+(cellSizeW/2-metrics.stringWidth( string )/2),
			island.Food.y*cellSizeH+(cellSizeH/2+metrics.getHeight()/2));
			
	}			

	private void dashboard(Graphics g)
	{
		if ( drawDashboard == true )
		{
			Color textColor = new Color(220,220,255);
			
			// calculate dashboard coordinates
			int dashboardX = cellSizeW * island.getWidth() ;
			int	dashboardY = 0 ;
			int dashboardW = getSize().width ;
			int dashboardH = cellSizeH * island.getHeight() ;
			
			// draw the dashboard background
			g.setColor(Color.gray);
			g.fillRect(dashboardX, dashboardY, dashboardW, dashboardH);
			g.setColor(Color.darkGray);
			g.drawRect(dashboardX + 1, dashboardY + 1, dashboardW - 1, dashboardH - 1);

			// offset for texts
			dashboardY += 40 ;
			
			// display simulation time
			g.setFont(simutimeFont);
			g.setColor(textColor);
			long time = System.currentTimeMillis() - startTime ;
			String stringTime = returnFormattedTime(time) ;
			g.drawString(stringTime, dashboardX + 5, dashboardY);
			
			// display agents infos
			dashboardY += 20;
			g.setFont(defaultFont);
			FontMetrics metrics = g.getFontMetrics();
			int fheight = metrics.getHeight() - 2;
			for (int i = 0; i < island.getNbOfAgs(); i++) 
			{
				Agent ag = island.getAg(i) ;
				int agLife = ag.getLife() ;
				g.setColor(ag.isAlive() == true ? textColor : Color.red);
				g.drawString( ag.getName(), dashboardX + 5, dashboardY + (i*fheight));
				g.setColor(agLife > 75 ? Color.green : agLife > 25 ? Color.yellow : Color.red);
				g.fillRect(dashboardX + 85, dashboardY + (i*fheight) - fheight + 5, agLife/2, fheight - 4);
			}
		}
	}
	
	public static String returnFormattedTime(long time) 
	{
		//the seconds
		long seconds = time / 1000;
		
		//the hours
		long hours = seconds / 3600;
		
		//seconds adjusted after hours
		seconds = seconds % 3600;
		
		//the minutes
		long minutes = seconds / 60;
		
		//seconds adjusted after minutes
		seconds = seconds % 60;
		
		//milliseconds left over.
		long millis = time % 1000;

		// Print out the results
		return new StringBuffer().append(hours).append("h : ")
			.append(minutes).append("mn : ").append(seconds).append("s : ").append(millis).append("ms")
			.toString();
	}
	
	private void drawMap(Graphics g)
	{
		for (int x = 0; x < island.getWidth(); x++) 
		{
			for (int y = 0; y < island.getHeight(); y++) 
			{
				drawCell(g, x, y);
			}
		}
	}
	
	private void drawGrid(Graphics g)
	{
		if ( drawGrid != 0 )
		{
			g.setColor(GridColor) ;
			
			for (int l = 1; l <= island.getHeight(); l++) 
			{
				g.drawLine(0, l * cellSizeH, island.getWidth() * cellSizeW, l * cellSizeH);
			}
				
			for (int c = 1; c <= island.getWidth(); c++) 
			{
				g.drawLine(c * cellSizeW, 0, c * cellSizeW, island.getHeight() * cellSizeH);
			}
		}
	}

	public void paint(Graphics graphics) 
	{
		// handle dynamically resize
		windowSizeW = getSize().width - (drawDashboard == true ? sizeDashboard : 0);
		windowSizeH = getSize().height;
		cellSizeW = windowSizeW / island.getWidth() ;
		cellSizeH = windowSizeH / island.getHeight() ;
		
		// create a back buffer
		buffer = new BufferedImage(getSize().width, windowSizeH, BufferedImage.TYPE_INT_ARGB);			
		Graphics g = buffer.getGraphics();
		
		// draw island's all map when needed
		if (redrawAll == true)
		{
			// draw the full grid
			drawGrid(g);

			// draw all the map elements
			drawMap(g) ;
			
			dirtyMap.clear();
			redrawAll = false ;
		}
		else
		{
			// or only dirty elements
			restoreCells(g) ;
		}

		// Write infos
		drawMapInfos(g);
		
		// Draw paths
		drawPaths(g);
		
		// draw agents
		drawAgents(g) ;

		// Write agents infos
		dashboard(g);
		
		// finally draw the buffer to the real graphics context
		graphics.drawImage(buffer, 0, 0, null);		
	}	

	public void restoreCells(Graphics g)
	{
		// anim sea?
		if (seaCells.isEmpty() == false)
		{
			for (int i = 0; i < seaCells.size(); i++) 
			{
				Cell cell = (Cell)seaCells.get(i);
				
				// draw the cell
				drawCell(g, cell.x, cell.y);
			}
		}

		// redraw dirty cells of the map
		if ( dirtyMap.isEmpty() == false )
		{
			for (int i = 0; i < dirtyMap.size(); i++) 
			{
				Cell cell = (Cell)dirtyMap.get(i);
				
				// draw the cell
				drawCell(g, cell.x, cell.y);
				
				// draw the grid aspect fo a cell if required
				if ( drawGrid != 0 )
				{
					g.setColor(GridColor);
					g.drawRect(cell.x * cellSizeW, cell.y * cellSizeH, cellSizeW, cellSizeH);
				}
			}
			dirtyMap.clear();
		}
	}

    public static void main(String[] args) throws Exception 
	{
        iDM env = new iDM();
        env.init(new String[] {
			"./data/island.gif", 	// map 
			"7", 					// numbre of rabbits
			"100",					// rate for rabbits regeneration
			"640","640",			// window size
			"yes", 					// display extra animations (sea...)
			"yes", 					// display grid
			"yes", 					// display infos (paths...)
			"50"					// sleeptime
		});
    }

	/** Class to encapsulate a cell of the map */
	private class Cell
	{
		// coordinates
		public 	int 	x ;
		public	int		y ;
		
		public Cell(int x, int y) 
		{
			this.x = x ;
			this.y = y ;
		}
	}	
	
}
