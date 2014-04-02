package cz.muni.fi.hough.line;

/*
 * Copyright (c) 1997, 2006, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/**
 * This <code>Line2D</code> represents a line segment in {@code (x,y)}
 * coordinate space.  This class, like all of the Java 2D API, uses a
 * default coordinate system called <i>user space</i> in which the y-axis
 * values increase downward and x-axis values increase to the right.  For
 * more information on the user space coordinate system, see the
 * <a href="http://java.sun.com/j2se/1.3/docs/guide/2d/spec/j2d-intro.fm2.html#61857">
 * Coordinate Systems</a> section of the Java 2D Programmer's Guide.
 * <p>
 * This class is only the abstract superclass for all objects that
 * store a 2D line segment.
 * The actual storage representation of the coordinates is left to
 * the subclass.
 *
 * @author      Jim Graham
 * @since 1.2
 */
public abstract class Line2D {

    /**
     * Returns an indicator of where the specified point
     * {@code (px,py)} lies with respect to the line segment from
     * {@code (x1,y1)} to {@code (x2,y2)}.
     * The return value can be either 1, -1, or 0 and indicates
     * in which direction the specified line must pivot around its
     * first end point, {@code (x1,y1)}, in order to point at the
     * specified point {@code (px,py)}.
     * <p>A return value of 1 indicates that the line segment must
     * turn in the direction that takes the positive X axis towards
     * the negative Y axis.  In the default coordinate system used by
     * Java 2D, this direction is counterclockwise.
     * <p>A return value of -1 indicates that the line segment must
     * turn in the direction that takes the positive X axis towards
     * the positive Y axis.  In the default coordinate system, this
     * direction is clockwise.
     * <p>A return value of 0 indicates that the point lies
     * exactly on the line segment.  Note that an indicator value
     * of 0 is rare and not useful for determining colinearity
     * because of floating point rounding issues.
     * <p>If the point is colinear with the line segment, but
     * not between the end points, then the value will be -1 if the point
     * lies "beyond {@code (x1,y1)}" or 1 if the point lies
     * "beyond {@code (x2,y2)}".
     *
     * @param x1 the X coordinate of the start point of the
     *           specified line segment
     * @param y1 the Y coordinate of the start point of the
     *           specified line segment
     * @param x2 the X coordinate of the end point of the
     *           specified line segment
     * @param y2 the Y coordinate of the end point of the
     *           specified line segment
     * @param px the X coordinate of the specified point to be
     *           compared with the specified line segment
     * @param py the Y coordinate of the specified point to be
     *           compared with the specified line segment
     * @return an integer that indicates the position of the third specified
     *                  coordinates with respect to the line segment formed
     *                  by the first two specified coordinates.
     * @since 1.2
     */
    public static int relativeCCW(double x1, double y1,
                                  double x2, double y2,
                                  double px, double py)
    {
        x2 -= x1;
        y2 -= y1;
        px -= x1;
        py -= y1;
        double ccw = px * y2 - py * x2;
        if (ccw == 0.0) {
            // The point is colinear, classify based on which side of
            // the segment the point falls on.  We can calculate a
            // relative value using the projection of px,py onto the
            // segment - a negative value indicates the point projects
            // outside of the segment in the direction of the particular
            // endpoint used as the origin for the projection.
            ccw = px * x2 + py * y2;
            if (ccw > 0.0) {
                // Reverse the projection to be relative to the original x2,y2
                // x2 and y2 are simply negated.
                // px and py need to have (x2 - x1) or (y2 - y1) subtracted
                //    from them (based on the original values)
                // Since we really want to get a positive answer when the
                //    point is "beyond (x2,y2)", then we want to calculate
                //    the inverse anyway - thus we leave x2 & y2 negated.
                px -= x2;
                py -= y2;
                ccw = px * x2 + py * y2;
                if (ccw < 0.0) {
                    ccw = 0.0;
                }
            }
        }
        return (ccw < 0.0) ? -1 : ((ccw > 0.0) ? 1 : 0);
    }

    /**
     * Tests if the line segment from {@code (x1,y1)} to
     * {@code (x2,y2)} intersects the line segment from {@code (x3,y3)}
     * to {@code (x4,y4)}.
     *
     * @param x1 the X coordinate of the start point of the first
     *           specified line segment
     * @param y1 the Y coordinate of the start point of the first
     *           specified line segment
     * @param x2 the X coordinate of the end point of the first
     *           specified line segment
     * @param y2 the Y coordinate of the end point of the first
     *           specified line segment
     * @param x3 the X coordinate of the start point of the second
     *           specified line segment
     * @param y3 the Y coordinate of the start point of the second
     *           specified line segment
     * @param x4 the X coordinate of the end point of the second
     *           specified line segment
     * @param y4 the Y coordinate of the end point of the second
     *           specified line segment
     * @return <code>true</code> if the first specified line segment
     *                  and the second specified line segment intersect
     *                  each other; <code>false</code> otherwise.
     * @since 1.2
     */
    public static boolean linesIntersect(double x1, double y1,
                                         double x2, double y2,
                                         double x3, double y3,
                                         double x4, double y4)
    {
        return ((relativeCCW(x1, y1, x2, y2, x3, y3) *
                 relativeCCW(x1, y1, x2, y2, x4, y4) <= 0)
                && (relativeCCW(x3, y3, x4, y4, x1, y1) *
                    relativeCCW(x3, y3, x4, y4, x2, y2) <= 0));
    }

 }