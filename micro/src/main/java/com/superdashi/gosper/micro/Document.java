package com.superdashi.gosper.micro;

//TODO pass default document into constructor
public class Document extends DocumentComponent {

	// accessors

	public DocumentModel model() {
		return documentModel();
	}

	public void model(DocumentModel model) {
		documentModel(model);
	}
}
