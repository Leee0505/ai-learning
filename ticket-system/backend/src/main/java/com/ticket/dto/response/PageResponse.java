package com.ticket.dto.response;

import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

public class PageResponse<T> {
    private List<T> records;
    private long total;
    private int page;
    private int size;

    private PageResponse() {}

    public static <T> PageResponse<T> of(IPage<?> page, List<T> records) {
        PageResponse<T> r = new PageResponse<>();
        r.records = records;
        r.total = page.getTotal();
        r.page = (int) page.getCurrent();
        r.size = (int) page.getSize();
        return r;
    }

    public List<T> getRecords() { return records; }
    public long getTotal() { return total; }
    public int getPage() { return page; }
    public int getSize() { return size; }
}