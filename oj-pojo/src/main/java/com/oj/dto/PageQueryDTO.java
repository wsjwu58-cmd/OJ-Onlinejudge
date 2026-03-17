package com.oj.dto;

import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.github.xiaoymin.knife4j.core.util.StrUtil;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "分页查询条件实体")
public class PageQueryDTO {
    @Schema(description = "页码")
    private int page;
    @Schema(description = "每页数量")
    private int pageSize;
    @Schema(description = "排序字段")
    private String orderBy;
    @Schema(description = "是否升序")
    private Boolean order;
    public <T> Page<T> ToPage(OrderItem...  items){
        Page<T> pageObj=Page.of(this.page,this.pageSize);
        if( StrUtil.isNotBlank(orderBy)){
            // 修复：使用Boolean.TRUE.equals()避免空指针异常
            pageObj.addOrder(Boolean.TRUE.equals(order) ? OrderItem.asc(orderBy) : OrderItem.desc(orderBy));
        }else if(items!=null && items.length > 0){
            pageObj.addOrder(items);
        }
        return pageObj;
    }
    public <T> Page<T> ToPageDefaultSortByCreateTime(String defaultSortBy){
        return  ToPage(OrderItem.desc(defaultSortBy));
    }
}