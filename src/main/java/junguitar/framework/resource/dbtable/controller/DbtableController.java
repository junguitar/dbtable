package junguitar.framework.resource.dbtable.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import junguitar.framework.resource.dbtable.service.DbtableService;

@RestController
@RequestMapping("api/framework/dbtables")
public class DbtableController {
	@Autowired
	private DbtableService service;

	@GetMapping("info")
	public String info(@RequestParam(required = true) String schemaName,
			@RequestParam(required = false) String sequence) {
		return service.info(schemaName, sequence);
	}

	@GetMapping("columns/dictionaries/info")
	public String infoColumnsDictionaries(@RequestParam(required = true) String schemaName) {
		return service.infoColumnsDictionaries(schemaName);
	}

}
