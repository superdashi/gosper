package com.superdashi.gosper.core;

import java.util.Optional;

import com.superdashi.gosper.framework.Identity;

public interface ComponentRegistry<C extends Component> {

	Optional<C> componentWithIdentity(Identity identity);

}
