package com.kalvin.kvf.gen.controller;


import cn.hutool.core.date.DateUtil;
import cn.hutool.core.util.StrUtil;
import com.kalvin.kvf.gen.comm.ConfigConstant;
import com.kalvin.kvf.gen.comm.TemplateTypeEnum;
import com.kalvin.kvf.gen.utils.AuxiliaryKit;
import com.kalvin.kvf.controller.BaseController;
import com.kalvin.kvf.dto.R;
import com.kalvin.kvf.gen.dto.TableColumnDTO;
import com.kalvin.kvf.gen.service.IGenService;
import com.kalvin.kvf.gen.service.ITableService;
import com.kalvin.kvf.gen.utils.VelocityKit;
import com.kalvin.kvf.gen.vo.GenConfigVO;
import org.apache.velocity.Template;
import org.apache.velocity.VelocityContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.ModelAndView;

import java.io.StringWriter;
import java.util.List;

/**
 * <p>
 * 代码生成 前端控制器
 * </p>
 *
 * @author Kalvin
 * @since 2019-05-10
 */
@RestController
@RequestMapping("gen")
public class GenController extends BaseController {

    @Autowired
    private ITableService tableService;

    @Autowired
    private IGenService genService;

    @GetMapping(value = "table/index")
    public ModelAndView table() {
        return new ModelAndView("gen/table");
    }

    @GetMapping(value = "setting/{tableName}")
    public ModelAndView setting(@PathVariable String tableName) {
        ModelAndView mv = new ModelAndView("gen/setting");
        List<TableColumnDTO> tableColumnDTOS = tableService.listTableColumn(tableName);
        mv.addObject("tableName", tableName);
        mv.addObject("tableColumns", AuxiliaryKit.handleTableColumns(tableColumnDTOS));
        return mv;
    }

    @GetMapping(value = "list/tableData")
    public R listTableData(String tableName, int current, int size) {
        return R.ok(tableService.listTablePage(tableName, current, size));
    }

    @PostMapping(value = "custom/generate/code")
    public R customGenerateCode(@RequestBody GenConfigVO genConfigVO) {
        LOGGER.info("genConfig={}", genConfigVO);
        String tableType = genConfigVO.getTableType();
        String tableTplName = tableType.equals("tree_grid") ? "treegrid.vm" : "table.vm";

        // 处理表列值说明关系
        AuxiliaryKit.handleAndSetColumnsValueRelations(genConfigVO.getColumns());
        // 设置处理所有表列数据
        List<TableColumnDTO> tableColumnDTOS = tableService.listTableColumn(genConfigVO.getTableName());
        tableColumnDTOS = AuxiliaryKit.handleTableColumns(tableColumnDTOS);
        AuxiliaryKit.handleAndSetAllColumnsValueRelations(tableColumnDTOS);
        genConfigVO.setAllColumns(AuxiliaryKit.handleTableColumns(tableColumnDTOS));
        genConfigVO.setFirstCapFunName(StrUtil.upperFirst(genConfigVO.getFunName()));
        genConfigVO.setPkCamelCase(StrUtil.toCamelCase(genConfigVO.getPrimaryKey()));

        VelocityContext ctx = VelocityKit.getContext();
        ctx.put("config", genConfigVO);
        Template t = VelocityKit.getTemplate(tableTplName);
        StringWriter sw = new StringWriter();
        t.merge(ctx, sw);

        // 生成所有模板代码
        VelocityKit.allToFile(genConfigVO);
        return R.ok(sw.toString());
    }

    @PostMapping(value = "quickly/generate/code")
    public R quicklyGenerateCode(String tableName, String tableType, String tableComment) {
        String tableTplName = tableType.equals("tree_grid") ? "treegrid.vm" : "table.vm";
        GenConfigVO config = genService.init(tableName, tableType, tableComment);
        VelocityContext ctx = VelocityKit.getContext();
        ctx.put("config", config);
        Template t = VelocityKit.getTemplate(tableTplName);
//        String destPath = AuxiliaryKit.getGenerateCodePath(TemplateTypeEnum.ENTITY, config.getModuleName(), config.getFunName());
//        LOGGER.info("destPath={}", destPath);
        StringWriter sw = new StringWriter();
        t.merge(ctx, sw);

        // 生成所有模板代码
        VelocityKit.allToFile(config);
        return R.ok(sw.toString());
    }

}

