package jframe_pkg.map;

//import javax.swing.*;
import java.awt.*;
import java.awt.Graphics;

import javax.swing.JPanel;

import jframe_pkg.map.Gridder;
import jframe_pkg.map.MapConstant;

public class Mapper extends JPanel {
	public Gridder gridder = new Gridder();
	
	public Mapper() //bot, gridder
	{
		//this.bot = bot;
		this.gridder = gridder;
		//this.repaint();
	}
    
    /**
     * Overrides JComponent's paintComponent() method. It creates a two-dimensional array of _DisplayCell objects
     * to store the current map state. Then, it paints square cells for the grid with the appropriate colors as
     * well as the robot on-screen.
     */
    public void paintComponent(Graphics g) {
        // Create a two-dimensional array of _DisplayCell objects for rendering.
        _DisplayCell[][] _mapCells = new _DisplayCell[MapConstant.MAP_X][MapConstant.MAP_Y];
        for (int x = 0; x < MapConstant.MAP_X; x++) {
            for (int y = 0; y < MapConstant.MAP_Y; y++) {
                _mapCells[x][y] = new _DisplayCell(y * GraphicsConstant.CELL_SIZE, x * GraphicsConstant.CELL_SIZE, GraphicsConstant.CELL_SIZE);
            }
        }

        // Paint the cells with the appropriate colors.
        for (int x = 0; x < MapConstant.MAP_X; x++) {
            for (int y = 0; y < MapConstant.MAP_Y; y++) {
                Color cellColor;

                if (gridder.in_start(x, y))
                    cellColor = GraphicsConstant.C_START;
                else if (gridder.in_goal(x, y)) //TODO: fix the goal state paint 3x3
                    cellColor = GraphicsConstant.C_GOAL;
                else {
                    if (!gridder.get_Grid()[x][y].getIsExplored())
                        cellColor = GraphicsConstant.C_UNEXPLORED;
                    else if (gridder.get_Grid()[x][y].getIsObstacle())
                        cellColor = GraphicsConstant.C_OBSTACLE;
                    else
                        cellColor = GraphicsConstant.C_FREE;
                }

                g.setColor(cellColor);
                g.fillRect(_mapCells[x][y].cellX + GraphicsConstant.MAP_X_OFFSET, _mapCells[x][y].cellY, _mapCells[x][y].cellSize, _mapCells[x][y].cellSize);
            }
        }

        /** Paint the robot on-screen.
        //g.setColor(GraphicsConstant.C_ROBOT);
        //int r = bot.getRobotPosRow();
        //int c = bot.getRobotPosCol();
        //g.fillOval((c - 1) * GraphicsConstant.CELL_SIZE + GraphicsConstant.ROBOT_X_OFFSET + GraphicsConstant.MAP_X_OFFSET, GraphicsConstant.MAP_H - (r * GraphicsConstant.CELL_SIZE + GraphicsConstant.ROBOT_Y_OFFSET), GraphicsConstant.ROBOT_W, GraphicsConstant.ROBOT_H);
		**/
        
        /** Paint the robot's direction indicator on-screen.
        //g.setColor(GraphicsConstant.C_ROBOT_DIR);
        //RobotConstants.DIRECTION d = bot.getRobotCurDir();
        switch (d) {
            case NORTH:
                g.fillOval(c * GraphicsConstant.CELL_SIZE + 10 + GraphicsConstant.MAP_X_OFFSET, GraphicsConstant.MAP_H - r * GraphicsConstant.CELL_SIZE - 15, GraphicsConstant.ROBOT_DIR_W, GraphicsConstant.ROBOT_DIR_H);
                break;
            case EAST:
                g.fillOval(c * GraphicsConstant.CELL_SIZE + 35 + GraphicsConstant.MAP_X_OFFSET, GraphicsConstant.MAP_H - r * GraphicsConstant.CELL_SIZE + 10, GraphicsConstant.ROBOT_DIR_W, GraphicsConstant.ROBOT_DIR_H);
                break;
            case SOUTH:
                g.fillOval(c * GraphicsConstant.CELL_SIZE + 10 + GraphicsConstant.MAP_X_OFFSET, GraphicsConstant.MAP_H - r * GraphicsConstant.CELL_SIZE + 35, GraphicsConstant.ROBOT_DIR_W, GraphicsConstant.ROBOT_DIR_H);
                break;
            case WEST:
                g.fillOval(c * GraphicsConstant.CELL_SIZE - 15 + GraphicsConstant.MAP_X_OFFSET, GraphicsConstant.MAP_H - r * GraphicsConstant.CELL_SIZE + 10, GraphicsConstant.ROBOT_DIR_W, GraphicsConstant.ROBOT_DIR_H);
                break;
        }**/
    }

    private class _DisplayCell {
        public final int cellX;
        public final int cellY;
        public final int cellSize;

        public _DisplayCell(int borderX, int borderY, int borderSize) {
            this.cellX = borderX + GraphicsConstant.CELL_LINE_WEIGHT;
            this.cellY = GraphicsConstant.MAP_H - (borderY - GraphicsConstant.CELL_LINE_WEIGHT);
            this.cellSize = borderSize - (GraphicsConstant.CELL_LINE_WEIGHT * 2);
        }
    }

}