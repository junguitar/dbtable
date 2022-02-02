package junguitar.framework.resource.dbtable.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import junguitar.framework.resource.dbtable.service.DbtableService;
import junguitar.framework.resource.dbtable.service.comment.CommentService;

@RestController
@RequestMapping("api/framework/dbtables")
public class DbtableController {
	@Autowired
	private DbtableService service;

	@Autowired
	private CommentService commentService;

	@GetMapping("info")
	public String info(@RequestParam(required = true) String schemaName,
			@RequestParam(required = false) String sequence) {
		return service.info(schemaName, sequence);
	}

	@GetMapping("columns/dictionaries/info")
	public String infoColumnsDictionaries(@RequestParam(required = true) String schemaName) {
		return service.infoColumnsDictionaries(schemaName);
	}

	@GetMapping("comments/info-by-excel")
	public String infoCommentListByExcel(@RequestParam(required = true) String path,
			@RequestParam(required = true) String sheetName) {
		return commentService.infoCommentListByExcel(path, sheetName);
	}

//	@PutMapping("comments/by-excel")
//	public void putCommentListByExcel(@RequestParam(required = true) String path,
//			@RequestParam(required = true) String sheetName) {
//		commentService.putCommentListByExcel(path, sheetName);
//	}

}
