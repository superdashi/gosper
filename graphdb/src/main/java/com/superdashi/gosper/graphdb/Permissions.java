package com.superdashi.gosper.graphdb;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// only owner may modify permissions
//TODO enforce size limits on sets
public final class Permissions {

	// fields

	private final Part part;
	private final boolean accessible;

	// view - means that part is visible
	private final Set<String> view;
	// modify - means that the part may be modified
	private final Set<String> modify;
	// delete - means that part may be deleted
	private final Set<String> delete;

	// cached
	private final Set<String> declaredPerms;
	private final Map<String, Integer> codesByPerm;

	// constructors

	Permissions(Part part) {
		assert part != null;
		this.part = part;
		accessible = part.accessibleByViewer();
		int[] perms = part.data.perms;
		Lookup<String> lookup = part.visit.space.permissionLookup;
		if (perms.length == 0) {
			/* nothing to do, there are no permissions */
			view = new HashSet<>();
			modify = new HashSet<>();
			delete = new HashSet<>();
		} else {
			int vCount = perms[0] >> 16 & 0xffff;
			int mCount = perms[0] & 0xffff;
			int dCount = perms.length - vCount - mCount - 1;
			view = new HashSet<>(vCount);
			modify = new HashSet<>(mCount);
			delete = new HashSet<>(dCount);
			int j = 1;
			Map<Integer, String> pbc = lookup.getByCode();
			for (int i = 0; i < vCount; i++) {
				view.add(pbc.get(perms[j++]));
			}
			for (int i = 0; i < mCount; i++) {
				modify.add(pbc.get(perms[j++]));
			}
			for (int i = 0; i < dCount; i++) {
				delete.add(pbc.get(perms[j++]));
			}
			assert !view.contains(null) && !modify.contains(null) && !delete.contains(null);
		}
		codesByPerm = lookup.getByObj();
		declaredPerms = part.visit.view.declaredPermissions;
	}

	// view

	public boolean isViewableWith(String permission) {
		return getPermission(view, permission);
	}

	public boolean allowViewWith(String permission) {
		return addPermission(view, permission);
	}

	public boolean recindViewWith(String permission) {
		return removePermission(view, permission);
	}

	// modify

	public boolean isModifiableWith(String permission) {
		return getPermission(modify, permission);
	}

	public boolean allowModifyWith(String permission) {
		return addPermission(modify, permission);
	}

	public boolean recindModifyWith(String permission) {
		return removePermission(modify, permission);
	}

	// delete

	public boolean isDeletableWith(String permission) {
		return getPermission(delete, permission);
	}

	public boolean allowDeleteWith(String permission) {
		return addPermission(delete, permission);
	}

	public boolean recindDeleteWith(String permission) {
		return removePermission(delete, permission);
	}

	// package methods

	void populateData(List<Change> changes) {
		int sourceId = part.sourceId();
		int edgeId = part.edgeId();
		PartData data = part.data;
		int[] perms = data.perms;

		int vs = view.size();
		int ms = modify.size();
		int ds = delete.size();
		int len = 1 + vs + ms + ds;

		int nsCode = Name.nsCode(data.owner);
		int ovs = perms.length == 0 ? 0 : (perms[0] >> 16) & 0xff;

		int[] newPerms;
		if (len == 1) { // now no perms, remove all existing
			for (int i = 1; i <= ovs; i++) {
				new NSNKey(nsCode, perms[i], sourceId, edgeId).toPermRemoval().record(changes);
			}
			newPerms = PartData.NO_PERMS;
		} else {
			newPerms = new int[len];
			newPerms[0] = vs << 16 | ms & 0xffff;
			int j = 1;
			// record view codes
			next: for (String perm : view) {
				int p = codesByPerm.getOrDefault(perm, -1);
				assert p != -1;
				newPerms[j++] = p;
				for (int i = 1; i <= ovs; i++) {
					if (perms[i] != p) continue; // no match, keep looking
					perms[i] = -1; // clear the old value, so that we leave only the removed ones
					continue next; // we don't need to record an addition for this
				}
				// record that this code has been added as an addition
				new NSNKey(nsCode, p, sourceId, edgeId).toPermAddition().record(changes);
			}
			// record removed view codes
			if (ovs > 0) Arrays.stream(perms, 1, ovs + 1).filter(i -> i != -1).forEach(c -> new NSNKey(nsCode, c, sourceId, edgeId).toPermRemoval().record(changes));
			// record modify codes
			for (String perm : modify) {
				int p = codesByPerm.getOrDefault(perm, -1);
				assert p != -1;
				newPerms[j++] = p;
			}
			// record delete codes
			for (String perm : delete) {
				int p = codesByPerm.getOrDefault(perm, -1);
				assert p != -1;
				newPerms[j++] = p;
			}
		}
		data.perms = newPerms;
	}

	boolean isViewableIn(View view) {
		return isPermittedIn(this.view, view);
	}

	boolean isModifiableIn(View view) {
		return isPermittedIn(this.modify, view);
	}

	boolean isDeletableIn(View view) {
		return isPermittedIn(this.delete, view);
	}

	// private helper methods

	private boolean getPermission(Set<String> perms, String permission) {
		if (permission == null) throw new IllegalArgumentException("null permission");
		part.checkNotDeleted();
		checkOwned();
		return perms.contains(permission);
	}

	private boolean addPermission(Set<String> perms, String permission) {
		if (permission == null) throw new IllegalArgumentException("null permission");
		if (!declaredPerms.contains(permission)) throw new IllegalArgumentException("permission not granted");
		part.checkNotDeleted();
		checkOwned();
		boolean modified = perms.add(permission);
		if (!modified) return false;
		part.recordDirty(PartData.FLAG_DIRTY_PERMS);
		return true;
	}

	private boolean removePermission(Set<String> perms, String permission) {
		if (permission == null) throw new IllegalArgumentException("null permission");
		if (!declaredPerms.contains(permission)) throw new IllegalArgumentException("permission not granted"); // not strictly needed but helpful warning for client
		part.checkNotDeleted();
		checkOwned();
		boolean modified = perms.remove(permission);
		if (!modified) return false;
		part.recordDirty(PartData.FLAG_DIRTY_PERMS);
		return true;
	}

	private boolean isPermittedIn(Set<String> required, View view) {
		Set<String> set = view.grantedByNs.get(part.owner().ns);
		return set != null && !Collections.disjoint(required, set);
	}

	private void checkOwned() {
		if (!accessible) throw new ConstraintException(ConstraintException.Type.PART_NOT_OWNED);
	}
}
