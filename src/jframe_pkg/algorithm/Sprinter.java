package jframe_pkg.algorithm;

import jframe_pkg.map.Cell;
import jframe_pkg.map.Mapper;
import jframe_pkg.map.MapConstant;
import jframe_pkg.robot.Robot;
import jframe_pkg.robot.RobotConstants;
import jframe_pkg.robot.RobotConstants.DIRECTION;
import jframe_pkg.robot.RobotConstants.MOVEMENT;
import jframe_pkg.utils.CommMgr;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Stack;

// @formatter:off
/**
 * Fastest path algorithm for the robot. Uses a version of the A* algorithm.
 *
 * g(n) = Real Cost from START to n
 * h(n) = Heuristic Cost from n to GOAL
 *
 */
// @formatter:on

public class Sprinter {
    private ArrayList<Cell> toVisit;        // array of Cells to be visited
    private ArrayList<Cell> visited;        // array of visited Cells
    private HashMap<Cell, Cell> parents;    // HashMap of Child --> Parent
    private Cell current;                   // current Cell
    private Cell[] neighbors;               // array of neighbors of current Cell
    private DIRECTION curDir;               // current direction of robot
    private double[][] gCosts;              // array of real cost from START to [row][col] i.e. g(n)
    private Robot bot;
    private Mapper map;
    private final Mapper realMap;
    private int loopCount;
    private boolean explorationMode;

    public Sprinter(Mapper map, Robot bot) {
        this.realMap = null;
        initObject(map, bot);
    }

    public Sprinter(Mapper map, Robot bot, Mapper realMap) {
        this.realMap = realMap;
        this.explorationMode = true;
        initObject(map, bot);
    }

    /**
     * Initialise the FastestPathAlgo object.
     */
    private void initObject(Mapper map, Robot bot) {
        this.bot = bot;
        this.map = map;
        this.toVisit = new ArrayList<>();
        this.visited = new ArrayList<>();
        this.parents = new HashMap<>();
        this.neighbors = new Cell[4];
        this.current = map.gridder.getCell(bot.getRobotPosRow(), bot.getRobotPosCol());
        this.curDir = bot.getRobotCurDir();
        this.gCosts = new double[MapConstant.MAP_X][MapConstant.MAP_Y];

        // Initialise gCosts array
        for (int i = 0; i < MapConstant.MAP_X; i++) {
            for (int j = 0; j < MapConstant.MAP_Y; j++) {
                Cell cell = map.gridder.getCell(i, j);
                if (!canBeVisited(cell)) {
                    gCosts[i][j] = RobotConstants.INFINITE_COST;
                } else {
                    gCosts[i][j] = -1;
                }
            }
        }
        toVisit.add(current);

        // Initialise starting point
        gCosts[bot.getRobotPosRow()][bot.getRobotPosCol()] = 0;
        this.loopCount = 0;
    }

    /**
     * Returns true if the cell can be visited.
     */
    private boolean canBeVisited(Cell c) {
        return c.getIsExplored() && !c.getIsObstacle() && !c.getIsVirtualWall();
    }

    /**
     * Returns the Cell inside toVisit with the minimum g(n) + h(n).
     */
    private Cell minimumCostCell(int goalRow, int getCol) {
        int size = toVisit.size();
        double minCost = RobotConstants.INFINITE_COST;
        Cell result = null;

        for (int i = size - 1; i >= 0; i--) {
            double gCost = gCosts[(toVisit.get(i).get_x())][(toVisit.get(i).get_y())];
            double cost = gCost + costH(toVisit.get(i), goalRow, getCol);
            if (cost < minCost) {
                minCost = cost;
                result = toVisit.get(i);
            }
        }

        return result;
    }

    /**
     * Returns the heuristic cost i.e. h(n) from a given Cell to a given [goalRow, goalCol] in the maze.
     */
    private double costH(Cell b, int goalRow, int goalCol) {
        // Heuristic: The no. of moves will be equal to the difference in the row and column values.
        double movementCost = (Math.abs(goalCol - b.get_y()) + Math.abs(goalRow - b.get_x())) * RobotConstants.MOVE_COST;

        if (movementCost == 0) return 0;

        // Heuristic: If b is not in the same row and column, one turn will be needed.
        double turnCost = 0;
        //if (goalCol - b.get_y() != 0 && goalRow - b.get_x() != 0) {
        if (goalCol - b.get_y() != 0 || goalRow - b.get_x() != 0) {
            turnCost = RobotConstants.TURN_COST;
        }

        return movementCost + turnCost;
    }

    /**
     * Returns the target direction of the bot from [botR, botC] to target Cell.
     */
    private DIRECTION getTargetDir(int botR, int botC, DIRECTION botDir, Cell target) {
        if (botC - target.get_y() > 0) {
            return DIRECTION.WEST;
        } else if (target.get_y() - botC > 0) {
            return DIRECTION.EAST;
        } else {
            if (botR - target.get_x() > 0) {
                return DIRECTION.SOUTH;
            } else if (target.get_x() - botR > 0) {
                return DIRECTION.NORTH;
            } else {
                return botDir;
            }
        }
    }

    /**
     * Get the actual turning cost from one DIRECTION to another.
     */
    private double getTurnCost(DIRECTION a, DIRECTION b) {
        int numOfTurn = Math.abs(a.ordinal() - b.ordinal());
        if (numOfTurn > 2) {
            numOfTurn = numOfTurn % 2;
        }
        return (numOfTurn * RobotConstants.TURN_COST);
    }

    /**
     * Calculate the actual cost of moving from Cell a to Cell b (assuming both are neighbors).
     */
    private double costG(Cell a, Cell b, DIRECTION aDir) {
        double moveCost = RobotConstants.MOVE_COST; // one movement to neighbor

        double turnCost;
        DIRECTION targetDir = getTargetDir(a.get_x(), a.get_y(), aDir, b);
        turnCost = getTurnCost(aDir, targetDir);

        return moveCost + turnCost;
    }

    /**
     * Find the fastest path from the robot's current position to [goalRow, goalCol].
     */
    public String runFastestPath(int goalRow, int goalCol) {
        
    	System.out.println("Calculating fastest path from (" + current.get_x() + ", " + current.get_y() + ") to goal (" + goalRow + ", " + goalCol + ")...");

        Stack<Cell> path;
        
        do {
            loopCount++;

            // Get cell with minimum cost from toVisit and assign it to current.
            current = minimumCostCell(goalRow, goalCol);

            // Point the robot in the direction of current from the previous cell.
            if (parents.containsKey(current)) {
                curDir = getTargetDir(parents.get(current).get_x(), parents.get(current).get_y(), curDir, current);
            }

            visited.add(current);       // add current to visited
            toVisit.remove(current);    // remove current from toVisit

            if (visited.contains(map.gridder.getCell(goalRow, goalCol))) {
                System.out.println("Goal visited. Path found!");
                path = getPath(goalRow, goalCol);
                printFastestPath(path);
                return executePath(path, goalRow, goalCol);
            }

            // Setup neighbors of current cell. [Top, Bottom, Left, Right].
            if (map.gridder.coordinate_validator(current.get_x() + 1, current.get_y())) {
                neighbors[0] = map.gridder.getCell(current.get_x() + 1, current.get_y());
                if (!canBeVisited(neighbors[0])) {
                    neighbors[0] = null;
                }
            }
            if (map.gridder.coordinate_validator(current.get_x() - 1, current.get_y())) {
                neighbors[1] = map.gridder.getCell(current.get_x() - 1, current.get_y());
                if (!canBeVisited(neighbors[1])) {
                    neighbors[1] = null;
                }
            }
            if (map.gridder.coordinate_validator(current.get_x(), current.get_y() - 1)) {
                neighbors[2] = map.gridder.getCell(current.get_x(), current.get_y() - 1);
                if (!canBeVisited(neighbors[2])) {
                    neighbors[2] = null;
                }
            }
            if (map.gridder.coordinate_validator(current.get_x(), current.get_y() + 1)) {
                neighbors[3] = map.gridder.getCell(current.get_x(), current.get_y() + 1);
                if (!canBeVisited(neighbors[3])) {
                    neighbors[3] = null;
                }
            }

            // Iterate through neighbors and update the g(n) values of each.
            for (int i = 0; i < 4; i++) {
                if (neighbors[i] != null) {
                    if (visited.contains(neighbors[i])) {
                        continue;
                    }

                    if (!(toVisit.contains(neighbors[i]))) {
                        parents.put(neighbors[i], current);
                        gCosts[neighbors[i].get_x()][neighbors[i].get_y()] = gCosts[current.get_x()][current.get_y()] + costG(current, neighbors[i], curDir);
                        toVisit.add(neighbors[i]);
                    } else {
                        double currentGScore = gCosts[neighbors[i].get_x()][neighbors[i].get_y()];
                        double newGScore = gCosts[current.get_x()][current.get_y()] + costG(current, neighbors[i], curDir);
                        if (newGScore < currentGScore) {
                            gCosts[neighbors[i].get_x()][neighbors[i].get_y()] = newGScore;
                            parents.put(neighbors[i], current);
                        }
                    }
                }
            }
        } while (!toVisit.isEmpty());

        System.out.println("Path not found!");
        return null;
    }

    /**
     * Generates path in reverse using the parents HashMap.
     */
    private Stack<Cell> getPath(int goalRow, int goalCol) {
        Stack<Cell> actualPath = new Stack<>();
        Cell temp = map.gridder.getCell(goalRow, goalCol);

        while (true) {
            actualPath.push(temp);
            temp = parents.get(temp);
            if (temp == null) {
                break;
            }
        }

        return actualPath;
    }

    /**
     * Executes the fastest path and returns a StringBuilder object with the path steps.
     */
    private String executePath(Stack<Cell> path, int goalRow, int goalCol) {
        StringBuilder outputString = new StringBuilder();

        Cell temp = path.pop();
        DIRECTION targetDir;

        ArrayList<MOVEMENT> movements = new ArrayList<>();
        
        Robot tempBot = null;
        
        int temp_row = map.bot.getRobotPosRow();
        int temp_col = map.bot.getRobotPosCol();
        
        //System.out.println("My debug codes - goalRow: " + goalRow + " goalCol: " + goalCol + "\n");
        //System.out.println("My debug codes - temp_goalRow: " + temp_row + " temp_goalCol: " + temp_col + "\n");
        
        
        if (goalRow == MapConstant.GOAL_Y && goalCol == MapConstant.GOAL_X) 
        {
        	//tempBot = new Robot (MapConstant.WAYPOINT_X, MapConstant.WAYPOINT_Y, false);
        	tempBot = new Robot (map.gridder.waypoint_x, map.gridder.waypoint_y, false); // waypoints in mapper, trial
        
        }
        else if (goalRow == map.gridder.temp_row && goalCol == map.gridder.temp_col) // if goal is set to next 
        {
        	tempBot = new Robot (temp_row, temp_col, false); // new start point from original startpoint in mapper, trial
        }
        else if (goalRow == 1 && goalCol == 1) // if going back after inner loop exploration
        {
        	tempBot = new Robot (map.gridder.temp_row, map.gridder.temp_col, false);
        }
        else
        	tempBot = new Robot(1, 1, false);
        
        
        tempBot.setCurPostScreen(bot.getCurPostScreen());
    	tempBot.setMonitorScreen(bot.getMonitorScreen());
        
        tempBot.setSpeed(0);
        while ((tempBot.getRobotPosRow() != goalRow) || (tempBot.getRobotPosCol() != goalCol)) {
            if (tempBot.getRobotPosRow() == temp.get_x() && tempBot.getRobotPosCol() == temp.get_y()) {
                temp = path.pop();
            }

            targetDir = getTargetDir(tempBot.getRobotPosRow(), tempBot.getRobotPosCol(), tempBot.getRobotCurDir(), temp);

            MOVEMENT m;
            if (tempBot.getRobotCurDir() != targetDir) {
                m = getTargetMove(tempBot.getRobotCurDir(), targetDir);
            } else {
                m = MOVEMENT.FORWARD;
            }

            System.out.println("Movement " + MOVEMENT.print(m) + " from (" + tempBot.getRobotPosRow() + ", " + tempBot.getRobotPosCol() + ") to (" + temp.get_x() + ", " + temp.get_y() + ")");

            tempBot.move(m);
            movements.add(m);
            outputString.append(MOVEMENT.print(m));
        }

        //we already have the fastest path in outputString, we dont have to go through the bottom codes - Joey
        
/*        if (!bot.getRealBot() || explorationMode) {
        	System.out.println("in HK first Loop!");
        	
            for (MOVEMENT x : movements) {
                if (x == MOVEMENT.FORWARD) {
                    if (!canMoveForward()) {
                        System.out.println("Early termination of fastest path execution.");
                        return "T";
                    }
                }
                	
                System.out.println("Going to bot.move(x)");
                System.out.println("What is x?");
                System.out.println(x.toString());
                bot.move(x);
                //here HK
                
                this.map.repaint();

                // During exploration, use sensor data to update map.
                if (explorationMode) {
                    bot.setSensors();
                    bot.sense(this.map, this.realMap);
                    this.map.repaint();
                }
            }
        } else {

            int fCount = 0;
            for (MOVEMENT x : movements) {
                if (x == MOVEMENT.FORWARD) {
                    fCount++;
                    if (fCount == 10) {
                        bot.moveForwardMultiple(fCount);
                        fCount = 0;
                        map.repaint();
                    }
                } else if (x == MOVEMENT.RIGHT || x == MOVEMENT.LEFT) {
                    if (fCount > 0) {
                        bot.moveForwardMultiple(fCount);
                        fCount = 0;
                        map.repaint();
                    }

                    bot.move(x);
                    map.repaint();
                }
            }

            if (fCount > 0) {
                bot.moveForwardMultiple(fCount);
                map.repaint();
            }
        }
*/
        
        
        //fastest path, move forward multiple steps - Joey
        
        	int fCount = 0;

        	for (MOVEMENT x : movements) {
                if (x == MOVEMENT.FORWARD) {
                    fCount++;
                } else if (x == MOVEMENT.RIGHT || x == MOVEMENT.LEFT) {
                    
                	if (fCount > 0) {
                        
                		CommMgr.getCommMgr().sendMsg("U,"+fCount);
                        fCount = 0;
                        //acknowledgement
                    	while(true)
            	   		{
            	   			String doneMsg = CommMgr.getCommMgr().revMsg(); 
            	   			if (doneMsg.equals("!")) 
            	   				break;
            	   		} 
                        
                        
                	}
                	//send to move either left or right
                	CommMgr.getCommMgr().sendMsg(String.valueOf(MOVEMENT.print(x)));
                    
                	//acknowledgement
                	while(true)
        	   		{
        	   			String doneMsg = CommMgr.getCommMgr().revMsg(); 
        	   			if (doneMsg.equals("!")) 
        	   				break;
        	   		}
                	
                }
        	}
        
        	
        	
        System.out.println("\nMovements: " + outputString.toString());
        
        
        
        
        //TODO: sending string of FP,FFFFFFFFFRFFFFFFFFFFFFLFFFFFFFFFFFR.....
        //CommMgr.getCommMgr().sendMsg("FP,"+outputString.toString());
        return outputString.toString();
    }

    /**
     * Returns true if the robot can move forward one cell with the current heading.
     */
    private boolean canMoveForward() {
        int row = bot.getRobotPosRow();
        int col = bot.getRobotPosCol();

        switch (bot.getRobotCurDir()) {
       
            case NORTH:
                if (!map.gridder.isObstacleCell(row + 2, col - 1) && !map.gridder.isObstacleCell(row + 2, col) && !map.gridder.isObstacleCell(row + 2, col + 1)) {
                    return true;
                }
                break;
            case EAST:
                if (!map.gridder.isObstacleCell(row + 1, col + 2) && !map.gridder.isObstacleCell(row, col + 2) && !map.gridder.isObstacleCell(row - 1, col + 2)) {
                    return true;
                }
                break;
            case SOUTH:
                if (!map.gridder.isObstacleCell(row - 2, col - 1) && !map.gridder.isObstacleCell(row - 2, col) && !map.gridder.isObstacleCell(row - 2, col + 1)) {
                    return true;
                }
                break;
            case WEST:
                if (!map.gridder.isObstacleCell(row + 1, col - 2) && !map.gridder.isObstacleCell(row, col - 2) && !map.gridder.isObstacleCell(row - 1, col - 2)) {
                    return true;
                }
                break;
        }

        return false;
    }

    /**
     * Returns the movement to execute to get from one direction to another.
     */
    private MOVEMENT getTargetMove(DIRECTION a, DIRECTION b) {
        switch (a) {
            case NORTH:
                switch (b) {
                    case NORTH:
                        return MOVEMENT.ERROR;
                    case SOUTH:
                        return MOVEMENT.LEFT;
                    case WEST:
                        return MOVEMENT.LEFT;
                    case EAST:
                        return MOVEMENT.RIGHT;
                }
                break;
            case SOUTH:
                switch (b) {
                    case NORTH:
                        return MOVEMENT.LEFT;
                    case SOUTH:
                        return MOVEMENT.ERROR;
                    case WEST:
                        return MOVEMENT.RIGHT;
                    case EAST:
                        return MOVEMENT.LEFT;
                }
                break;
            case WEST:
                switch (b) {
                    case NORTH:
                        return MOVEMENT.RIGHT;
                    case SOUTH:
                        return MOVEMENT.LEFT;
                    case WEST:
                        return MOVEMENT.ERROR;
                    case EAST:
                        return MOVEMENT.LEFT;
                }
                break;
            case EAST:
                switch (b) {
                    case NORTH:
                        return MOVEMENT.LEFT;
                    case SOUTH:
                        return MOVEMENT.RIGHT;
                    case WEST:
                        return MOVEMENT.LEFT;
                    case EAST:
                        return MOVEMENT.ERROR;
                }
        }
        return MOVEMENT.ERROR;
    }

    /**
     * Prints the fastest path from the Stack object.
     */
    private void printFastestPath(Stack<Cell> path) {
        System.out.println("\nLooped " + loopCount + " times.");
        System.out.println("The number of steps is: " + (path.size() - 1) + "\n");

        Stack<Cell> pathForPrint = (Stack<Cell>) path.clone();
        Cell temp;
        System.out.println("Path:");
        while (!pathForPrint.isEmpty()) {
            temp = pathForPrint.pop();
            if (!pathForPrint.isEmpty()) System.out.print("(" + temp.get_x() + ", " + temp.get_y() + ") --> ");
            else System.out.print("(" + temp.get_x() + ", " + temp.get_y() + ")");
        }

        System.out.println("\n");
    }

    /**
     * Prints all the current g(n) values for the cells.
     */
    public void printGCosts() {
        for (int i = 0; i < MapConstant.MAP_X; i++) {
            for (int j = 0; j < MapConstant.MAP_Y; j++) {
                System.out.print(gCosts[MapConstant.MAP_X - 1 - i][j]);
                System.out.print(";");
            }
            System.out.println("\n");
        }
    }
}
