package com.ticket.dto.response;

import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

@Schema(description = "Generic paginated response wrapper")
public class PageResponse<T> {

    @Schema(description = "List of records for the current page")
    private List<T> records;

    @Schema(description = "Total number of records across all pages", example = "42")
    private long total;

    @Schema(description = "Current page number (1-based)", example = "1")
    private int page;

    @Schema(description = "Page size", example = "20")
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
