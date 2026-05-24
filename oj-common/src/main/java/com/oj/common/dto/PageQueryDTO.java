package com.oj.common.dto;

import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;
import org.springframework.util.StringUtils;

@Data
public class PageQueryDTO {
    private int page;
    private int pageSize;
    private String orderBy;
    private Boolean order;

    public <T> Page<T> toPage(OrderItem... items) {
        Page<T> pageObj = Page.of(this.page, this.pageSize);
        if (StringUtils.hasText(orderBy)) {
            pageObj.addOrder(Boolean.TRUE.equals(order) ? OrderItem.asc(orderBy) : OrderItem.desc(orderBy));
        } else if (items != null && items.length > 0) {
            pageObj.addOrder(items);
        }
        return pageObj;
    }

    public <T> Page<T> toPageDefaultSortByCreateTime(String defaultSortBy) {
        return toPage(OrderItem.desc(defaultSortBy));
    }
}
