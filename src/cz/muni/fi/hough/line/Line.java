package cz.muni.fi.hough.line;

import org.opencv.core.Point;

/**
 * Rough port of java.awt.geom.Line2D.
 * 
 * @author Medo
 *
 */
public class Line {
	
	//X coordinate of starting point of line
	private double x1;
	
	//X coordinate of ending point of line
	private double x2;
	
	//Y coordinate of starting point of line
	private double y1;
	
	//Y coordinate of ending point of line
	private double y2;
	
	public Line(double x1, double x2, double y1, double y2) {
		this.x1 = x1;
		this.x2 = x2;
		this.y1 = y1;
		this.y2 = y2;
	}

	public void setLine(double x1, double x2, double y1, double y2) {
		this.x1 = x1;
		this.x2 = x2;
		this.y1 = y1;
		this.y2 = y2;
	}
	
	public void setLine(Line l) {
		this.x1 = l.getX1();
		this.x2 = l.getX2();
		this.y1 = l.getY1();
		this.y2 = l.getY2();
	}
	
	public double getX1() {
		return x1;
	}

	public void setX1(double x1) {
		this.x1 = x1;
	}

	public double getX2() {
		return x2;
	}

	public void setX2(double x2) {
		this.x2 = x2;
	}

	public double getY1() {
		return y1;
	}

	public void setY1(double y1) {
		this.y1 = y1;
	}

	public double getY2() {
		return y2;
	}

	public void setY2(double y2) {
		this.y2 = y2;
	}

	//Get starting point of line
	public Point getStart(){
		return new Point(x1,y1);
	}
	
	//Get ending point of line
	public Point getEnd(){
		return new Point(x2,y2);
	}
	
	//Get point of intersection of two lines
	public static Point getIntersectionPoint(Line line1, Line line2) {
	    if (! line1.intersectsLine(line2) ) return null;
	      double px = line1.getX1(),
	            py = line1.getY1(),
	            rx = line1.getX2()-px,
	            ry = line1.getY2()-py;
	      double qx = line2.getX1(),
	            qy = line2.getY1(),
	            sx = line2.getX2()-qx,
	            sy = line2.getY2()-qy;

	      double det = sx*ry - sy*rx;
	      if (det == 0) {
	        return null;
	      } else {
	        double z = (sx*(qy-py)+sy*(px-qx))/det;
	        if (z==0 ||  z==1) return null;  // intersection at end point!
	        return new Point((double)(px+z*rx), (double)(py+z*ry));
	      }
	}

	//Check whether two lines intersect
	public boolean intersectsLine(Line l) {
        return Line2D.linesIntersect(l.getX1(), l.getY1(), l.getX2(), l.getY2(), getX1(), getY1(), getX2(), getY2());
    }
	
	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		long temp;
		temp = Double.doubleToLongBits(x1);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(x2);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(y1);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		temp = Double.doubleToLongBits(y2);
		result = prime * result + (int) (temp ^ (temp >>> 32));
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Line other = (Line) obj;
		if (Double.doubleToLongBits(x1) != Double.doubleToLongBits(other.x1))
			return false;
		if (Double.doubleToLongBits(x2) != Double.doubleToLongBits(other.x2))
			return false;
		if (Double.doubleToLongBits(y1) != Double.doubleToLongBits(other.y1))
			return false;
		if (Double.doubleToLongBits(y2) != Double.doubleToLongBits(other.y2))
			return false;
		return true;
	}
	
}
