/*
 * Copyright (C) 2018 Dashi Ltd.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.superdashi.gosper.model;

import com.tomgibara.geom.core.Point;

public final class Daub {

	public final int mat;
	public final Point p1;
	public final Point p2;
	public final Point p3;

	public Daub(int mat) {
		this.mat = mat;
		p3 = p2 = p1 = Point.ORIGIN;
	}

	public Daub(int mat, Point p1, Point p2, Point p3) {
		this.mat = mat;
		this.p1 = p1;
		this.p2 = p2;
		this.p3 = p3;
	}
}
