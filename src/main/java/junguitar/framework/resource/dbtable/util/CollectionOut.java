package junguitar.framework.resource.dbtable.util;

import java.util.Collection;
import java.util.Collections;

import lombok.Data;

@Data
public class CollectionOut<T> {
	private Collection<T> content = Collections.emptyList();
}
