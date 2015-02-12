package org.openpixi.pixi.physics.fields;

import org.openpixi.pixi.parallel.cellaccess.CellAction;
import org.openpixi.pixi.physics.grid.Cell;
import org.openpixi.pixi.physics.grid.Grid;

	public class SolveForFields implements CellAction {

		private int CORRECTION;
		private double[][] phi;
		
		SolveForFields(double[][] phi) {
			this.phi = phi;
			
		}
		
		public void execute(Cell cell) {
			throw new UnsupportedOperationException();
		}

		public void execute(Grid grid, int x, int y, int z) {
	
			//the electric field in x direction is equal to the negative derivative of the 
			//potential in x direction, analogous for y direction
			//using central difference, omitting imaginary part since it should be 0 anyway
			/*
			grid.setEx(x, y, -(phi[x+1][2*y] - phi[x-1][2*y]) / (2 * grid.getCellWidth()));
			grid.setEy(x, y, -(phi[x][2*(y+1)] - phi[x][2*(y-1)]) / (2 * grid.getCellHeight()));
			*/
			System.out.println("DON'T USE SolveForFields!!!");

			
		}
		
		public void execute(Grid grid, int x, int y) {
			
			//the electric field in x direction is equal to the negative derivative of the 
			//potential in x direction, analogous for y direction
			//using central difference, omitting imaginary part since it should be 0 anyway
			/*
			grid.setEx(x, y, -(phi[x+1][2*y] - phi[x-1][2*y]) / (2 * grid.getCellWidth()));
			grid.setEy(x, y, -(phi[x][2*(y+1)] - phi[x][2*(y-1)]) / (2 * grid.getCellHeight()));
			*/
			System.out.println("DON'T USE SolveForFields!!!");

			
		}
		
		private int index(int clientIdx) {
			return CORRECTION + clientIdx;
		}
	}