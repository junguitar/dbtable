package junguitar.framework.resource.dbtable;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;

import junguitar.framework.resource.dbtable.dto.DiffOptions;
import junguitar.framework.resource.dbtable.dto.Table;
import junguitar.framework.resource.dbtable.service.DbtableService;
import junguitar.framework.resource.dbtable.service.comment.CommentService;
import junguitar.framework.resource.dbtable.service.table.TableService;
import junguitar.framework.resource.dbtable.util.CollectionOut;

@RestController
public class DbtableControllerImpl implements DbtableController {
	@Autowired
	private DbtableService service;

	@Autowired
	private TableService tableService;

	@Autowired
	private CommentService commentService;

	@Override
	public CollectionOut<Table> getCollection(String schema) {
		return tableService.getCollection(schema);
	}

	@Override
	public String infoDiff(String schema, String schema2, DiffOptions options) {
		return service.infoDiff(schema, schema2, options);
	}

	@Override
	public String info(String schema, String sheetPath, String sheetName, String dictionaySheetName) {
		return service.info(schema, sheetPath, sheetName, dictionaySheetName);
	}

	@Override
	public String infoColumnsDictionaries(String schema, String sheetPath, String sheetName, String tablesSheetName) {
		return service.infoColumnsDictionaries(schema, sheetPath, sheetName, tablesSheetName);
	}

	@Override
	public String infoCommentListByExcel(String sheetPath, String sheetName) {
		return commentService.infoListByExcel(sheetPath, sheetName);
	}

//	@PutMapping("comments/by-excel")
//	public void putCommentListByExcel(String path,
//			String sheetName) {
//		commentService.putCommentListByExcel(path, sheetName);
//	}

}
