package junguitar.framework.resource.dbtable.dto;

import java.util.List;

import io.swagger.annotations.ApiModelProperty;
import lombok.Data;

@Data
public class DiffOptions {
	@ApiModelProperty(value = "Exclusion")
	private List<String> exclusion;
}
