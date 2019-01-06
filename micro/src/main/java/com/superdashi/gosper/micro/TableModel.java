package com.superdashi.gosper.micro;

//TODO how to unify mutations with rows
public class TableModel extends Model {

	final Mutations mutations;
	final Rows rows;

	protected TableModel(ActivityContext context, Rows rows, Mutations mutations) {
		super(context);
		this.mutations = mutations;
		this.rows = rows;
	}

}
