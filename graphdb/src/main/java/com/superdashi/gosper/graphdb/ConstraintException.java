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
package com.superdashi.gosper.graphdb;

public class ConstraintException extends IllegalStateException {

	public enum Type {

		GENERIC("unknown error"),
		INTERNAL_ERROR("internal error"),
		INCOMPATIBLE_TYPE("incompatible type"),
		LOOKUP_STATE("incorrect lookup state"),
		SPACE_STATE("incorrect space state"),
		VISIT_STATE("incorrect visit state"),
		INCIDENT_EDGES("incident edges"),
		DELETED_PART("deleted part"),
		UNMODIFIABLE_PART("unmodifiable part"),
		UNDELETABLE_PART("undeletable part"),
		NON_EXISTENT_PART("non-existent part"),
		NON_UNIQUE_PART("non-unique part"),
		PART_NOT_OWNED("part not owned"),
		REENTRANT_EDIT("reentrant edit"),
		;

		private final String message;

		private Type(String message) {
			this.message = message;
		}

		public String message() {
			return message;
		}
	}

	private static Type checkedType(Type type) {
		if (type == null) throw new IllegalArgumentException("null type");
		return type;
	}

	private final Type type;

	public ConstraintException() {
		super();
		type = Type.GENERIC;
	}

	// standard constructors

	public ConstraintException(String message, Throwable cause) {
		super(message, cause);
		type = Type.GENERIC;
	}

	public ConstraintException(String message) {
		super(message);
		type = Type.GENERIC;
	}

	public ConstraintException(Throwable cause) {
		super(cause);
		type = Type.GENERIC;
	}

	// typed constructors

	public ConstraintException(Type type) {
		super(checkedType(type).message());
		this.type = type;
	}

	public ConstraintException(Type type, String message) {
		super(message);
		this.type = checkedType(type);
	}

	// accessors

	public Type type() {
		return type;
	}

}
