package com.superdashi.gosper.graphdb;

import com.superdashi.gosper.framework.Identity;
import com.superdashi.gosper.item.Value;

//TODO add methods with multiple values
//TODO will some tags be invisible?
//TODO support modify and delete permission (intersect by owner)
//TODO support anyNode and anyEdge
public abstract class Selector {

	// GENERIC PART

	private static final Selector any = new Selector() {

		@Override
		boolean matches(Part part) {
			return true;
		}

		@Override
		NodeSequence selectNodes(Resolver resolver) {
			return resolver.visit.indices.allNodes(resolver);
		}

		@Override
		EdgeSequence selectEdges(Resolver resolver) {
			return resolver.visit.indices.allEdges(resolver);
		}

	};


	private static final Selector none = new Selector() {

		@Override
		boolean matches(Part part) {
			return false;
		}

		@Override
		NodeSequence selectNodes(Resolver resolver) {
			return NodeSequence.empty;
		}

		@Override
		EdgeSequence selectEdges(Resolver resolver) {
			return EdgeSequence.empty;
		}

	};

	public static Selector any() {
		return any;
	}

	public static Selector none() {
		return none;
	}

	public static Selector ownedBy(Identity owner) {
		if (owner == null) throw new IllegalArgumentException("null owner");
		return new ByOwner(owner);
	}

	//TODO add owned by prefix when we have lookup

	public static Selector ownedByViewer() {
		return new ByViewer();
	}

	public static Selector ofType(Type type) {
		if (type == null) throw new IllegalArgumentException("null type");
		return new ByType(type);
	}

	public static Selector ofType(String typeName) {
		if (typeName == null) throw new IllegalArgumentException("null typeName");
		//TODO validate typename
		return new ByTypeName(typeName);
	}

	public static Selector viewableWithPermission(Identity permission) {
		if (permission == null) throw new IllegalArgumentException("null permission");
		return new ByViewPerm(permission);
	}

	public static Selector withTag(Tag tag) {
		if (tag == null) throw new IllegalArgumentException("null tag");
		return new ByTag(tag);
	}

	public static Selector withTag(String tagName) {
		if (tagName == null) throw new IllegalArgumentException("null tagName");
		return new ByTagName(tagName);
	}

	//TODO add method that takes prefixed tagname?

	public static Selector withAttr(AttrName attrName) {
		if (attrName == null) throw new IllegalArgumentException("null attrName");
		return new ByAttr(attrName);
	}

	public static Selector withAttr(String attrName) {
		if (attrName == null) throw new IllegalArgumentException("null attrName");
		return new ByAttrName(attrName);
	}

	public static Selector withValue(AttrName attrName, Value value) {
		if (attrName == null) throw new IllegalArgumentException("null attrName");
		if (value == null) throw new IllegalArgumentException("null value");
		return new ByValue(attrName, value);
	}

	public static Selector withValue(String attrName, Value value) {
		if (attrName == null) throw new IllegalArgumentException("null attrName");
		if (value == null) throw new IllegalArgumentException("null value");
		return new ByValueName(attrName, value);
	}

	// EDGE SPECIFIC

	public static Selector edgesFrom(Selector nodes) {
		return new BySource(nodes);
	}

	public static Selector edgesTo(Selector nodes) {
		return new ByTarget(nodes);
	}

	// IMPLEMENTATIONS

	private static class ByOwner extends Selector {

		private final Identity owner;

		ByOwner(Identity owner) {
			this.owner = owner;
		}

		@Override
		boolean matches(Part part) {
			//TODO can we do this without expanding part data?
			return owner.equals(part.owner());
		}

		@Override
		NodeSequence selectNodes(Resolver resolver) {
			return resolver.visit.indices.nodesWithOwner(resolver, owner);
		}

		@Override
		EdgeSequence selectEdges(Resolver resolver) {
			return resolver.visit.indices.edgesWithOwner(resolver, owner);
		}
	}

	private static class ByViewer extends Selector {

		ByViewer() { }

		@Override
		boolean matches(Part part) {
			return part.visit.view.namespace.equals(part.owner());
		}

		@Override
		NodeSequence selectNodes(Resolver resolver) {
			//TODO should cache visit.view.viewer - frequently used?
			return resolver.visit.indices.nodesWithOwner(resolver, resolver.visit.view.identity);
		}

		@Override
		EdgeSequence selectEdges(Resolver resolver) {
			return resolver.visit.indices.edgesWithOwner(resolver, resolver.visit.view.identity);
		}
	}

	private static class ByType extends Selector {

		private final Type type;

		ByType(Type type) {
			this.type = type;
		}

		@Override
		boolean matches(Part part) {
			return type.equals( part.type() );
		}

		@Override
		NodeSequence selectNodes(Resolver resolver) {
			return resolver.visit.indices.nodesWithType(resolver, type);
		}

		@Override
		EdgeSequence selectEdges(Resolver resolver) {
			return resolver.visit.indices.edgesWithType(resolver, type);
		}
	}

	private static class ByTypeName extends Selector {

		private final String typeName;

		ByTypeName(String typeName) {
			this.typeName = typeName;
		}

		@Override
		boolean matches(Part part) {
			return part.visit.view.type(typeName).equals(part.type());
		}

		@Override
		NodeSequence selectNodes(Resolver resolver) {
			return resolver.visit.indices.nodesWithType(resolver, type(resolver));
		}

		@Override
		EdgeSequence selectEdges(Resolver resolver) {
			return resolver.visit.indices.edgesWithType(resolver, type(resolver));
		}

		private Type type(Resolver resolver) {
			return resolver.visit.view.type(typeName);
		}
	}

	private static class ByViewPerm extends Selector {

		private final Identity perm;

		ByViewPerm(Identity perm) {
			this.perm = perm;
		}

		@Override
		boolean matches(Part part) {
			return perm.ns.equals(part.owner()) &&part.permissions().isViewableWith(perm.name);
		}

		@Override
		NodeSequence selectNodes(Resolver resolver) {
			return resolver.visit.indices.nodesWithPermission(resolver, perm);
		}

		@Override
		EdgeSequence selectEdges(Resolver resolver) {
			return resolver.visit.indices.edgesWithPermission(resolver, perm);
		}

	}

	private static class ByTag extends Selector {

		private final Tag tag;

		ByTag(Tag tag) {
			this.tag = tag;
		}

		@Override
		boolean matches(Part part) {
			return part.tags().contains(tag);
		}

		@Override
		NodeSequence selectNodes(Resolver resolver) {
			return resolver.visit.indices.nodesWithTag(resolver, tag);
		}

		@Override
		EdgeSequence selectEdges(Resolver resolver) {
			return resolver.visit.indices.edgesWithTag(resolver, tag);
		}
	}

	private static class ByTagName extends Selector {

		private final String tagName;

		ByTagName(String tagName) {
			this.tagName = tagName;
		}

		@Override
		boolean matches(Part part) {
			//TODO this is inefficient: keeps creating types
			return part.tags().contains( part.visit.newTag(tagName) );
		}

		@Override
		NodeSequence selectNodes(Resolver resolver) {
			return resolver.visit.indices.nodesWithTag(resolver, new Tag(resolver.visit.view.namespace, tagName));
		}

		@Override
		EdgeSequence selectEdges(Resolver resolver) {
			return resolver.visit.indices.edgesWithTag(resolver, new Tag(resolver.visit.view.namespace, tagName));
		}
	}

	private static class ByAttr extends Selector {

		private final AttrName attr;

		ByAttr(AttrName attr) {
			this.attr = attr;
		}

		@Override
		boolean matches(Part part) {
			return !part.attrs().get(attr).isEmpty();
		}

		@Override
		NodeSequence selectNodes(Resolver resolver) {
			return resolver.visit.indices.nodesWithValue(resolver, attr, null);
		}

		@Override
		EdgeSequence selectEdges(Resolver resolver) {
			return resolver.visit.indices.edgesWithValue(resolver, attr, null);
		}
	}

	private static class ByAttrName extends Selector {

		private final String name;

		ByAttrName(String name) {
			this.name = name;
		}

		@Override
		boolean matches(Part part) {
			return !part.attrs().get(name).isEmpty();
		}

		@Override
		NodeSequence selectNodes(Resolver resolver) {
			AttrName attrName = resolver.visit.view.attrName(name);
			return resolver.visit.indices.nodesWithValue(resolver, attrName, null);
		}

		@Override
		EdgeSequence selectEdges(Resolver resolver) {
			AttrName attrName = resolver.visit.view.attrName(name);
			return resolver.visit.indices.edgesWithValue(resolver, attrName, null);
		}
	}

	private static class ByValue extends Selector {

		private final AttrName attr;
		private final Value value;

		ByValue(AttrName attr, Value value) {
			this.attr = attr;
			this.value = value;
		}

		@Override
		boolean matches(Part part) {
			return !part.attrs().get(attr).equals(value);
		}

		@Override
		NodeSequence selectNodes(Resolver resolver) {
			return resolver.visit.indices.nodesWithValue(resolver, attr, value);
		}

		@Override
		EdgeSequence selectEdges(Resolver resolver) {
			return resolver.visit.indices.edgesWithValue(resolver, attr, value);
		}
	}

	private static class ByValueName extends Selector {

		private final String name;
		private final Value value;

		ByValueName(String name, Value value) {
			this.name = name;
			this.value = value;
		}

		@Override
		boolean matches(Part part) {
			return !part.attrs().get(name).equals(value);
		}

		@Override
		NodeSequence selectNodes(Resolver resolver) {
			AttrName attrName = resolver.visit.view.attrName(name);
			return resolver.visit.indices.nodesWithValue(resolver, attrName, value);
		}

		@Override
		EdgeSequence selectEdges(Resolver resolver) {
			AttrName attrName = resolver.visit.view.attrName(name);
			return resolver.visit.indices.edgesWithValue(resolver, attrName, value);
		}
	}

	private static class BySource extends Selector {

		private final Selector nodes;

		BySource(Selector nodes) {
			this.nodes = nodes;
		}

		@Override
		boolean matches(Part part) {
			return part.isEdge() && nodes.matches( ((Edge) part).sourceImpl() );
		}

		@Override
		NodeSequence selectNodes(Resolver resolver) {
			return NodeSequence.empty;
		}

		@Override
		EdgeSequence selectEdges(Resolver resolver) {
			NodeSequence seq = nodes.selectNodes(resolver);
			Indices indices = resolver.visit.indices;
			return () -> seq.stream().mapToObj(n -> indices.edgesWithSource(resolver, n)).flatMapToLong(s -> s.stream()).sorted().distinct().iterator();
		}

	}

	private static class ByTarget extends Selector {

		private final Selector nodes;

		ByTarget(Selector nodes) {
			this.nodes = nodes;
		}

		@Override
		boolean matches(Part part) {
			return part.isEdge() && nodes.matches( ((Edge) part).targetImpl() );
		}

		@Override
		NodeSequence selectNodes(Resolver resolver) {
			return NodeSequence.empty;
		}

		@Override
		EdgeSequence selectEdges(Resolver resolver) {
			NodeSequence seq = nodes.selectNodes(resolver);
			Indices indices = resolver.visit.indices;
			return () -> seq.stream().mapToObj(n -> indices.edgesWithTarget(resolver, n)).flatMapToLong(s -> s.stream()).sorted().distinct().iterator();
		}

	}

	// constructors

	Selector() { }

	// public method

	public Selector and(Selector that) {
		if (that == null) throw new IllegalArgumentException("null that");
		if (that == any) return this;
		if (that == none) return none;
		if (that == this) return this;
		return new Selector() {

			@Override
			boolean matches(Part part) {
				return Selector.this.matches(part) && that.matches(part);
			}

			@Override
			NodeSequence selectNodes(Resolver resolver) {
				return Selector.this.selectNodes(resolver).and(that.selectNodes(resolver));
			}

			@Override
			EdgeSequence selectEdges(Resolver resolver) {
				return Selector.this.selectEdges(resolver).and(that.selectEdges(resolver));
			}
		};
	}

	public Selector or(Selector that) {
		if (that == null) throw new IllegalArgumentException("null that");
		if (that == any) return any;
		if (that == none) return none;
		if (that == this) return this;
		return new Selector() {

			@Override
			boolean matches(Part part) {
				return Selector.this.matches(part) || that.matches(part);
			}

			@Override
			NodeSequence selectNodes(Resolver resolver) {
				return Selector.this.selectNodes(resolver).or(that.selectNodes(resolver));
			}

			@Override
			EdgeSequence selectEdges(Resolver resolver) {
				return Selector.this.selectEdges(resolver).or(that.selectEdges(resolver));
			}
		};
	}


//	public Selector not() {
//		if (this == any) return none;
//		if (this == none) return any;
//		return new Selector() {
//
//			@Override
//			boolean matches(Part part) {
//				return !this.matches(part);
//			}
//
//			@Override
//			NodeSequence selectNodes(Resolver resolver) {
//				return any.selectNodes(graph, resolver).except(this.selectNodes(graph, resolver));
//			}
//
//			@Override
//			EdgeSequence selectEdges(Resolver resolver) {
//				return any.selectEdges(graph, resolver).except(this.selectEdges(graph, resolver));
//			}
//		};
//	}

	// implementation methods

	abstract boolean matches(Part part);
	abstract NodeSequence selectNodes(Resolver resolver);
	abstract EdgeSequence selectEdges(Resolver resolver);
}
